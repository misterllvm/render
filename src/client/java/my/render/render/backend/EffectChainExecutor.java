package my.render.render.backend;

import my.render.render.backend.diagnostics.SubmissionStats;
import my.render.render.backend.material.SamplerMode;
import my.render.render.base.RgbaColor;
import my.render.render.base.UiRect;
import my.render.render.effect.EffectChain;
import my.render.render.effect.EffectSpec;
import my.render.render.pipeline.PipelineKey;
import my.render.render.pipeline.StandardPipelines;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class EffectChainExecutor {
	private final OffscreenPassManager offscreenPasses;
	private final MaterialExecutionRouter executionRouter;

	EffectChainExecutor(OffscreenPassManager offscreenPasses, MaterialExecutionRouter executionRouter) {
		this.offscreenPasses = Objects.requireNonNull(offscreenPasses, "offscreenPasses");
		this.executionRouter = Objects.requireNonNull(executionRouter, "executionRouter");
	}

	EffectOutput apply(EffectChain effects, ExecutionTarget source, UiRect sourceBounds, SubmissionStats stats) {
		Objects.requireNonNull(effects, "effects");
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(sourceBounds, "sourceBounds");
		Objects.requireNonNull(stats, "stats");

		if (effects.isEmpty()) {
			return new EffectOutput(source, sourceBounds, List.of());
		}

		List<RenderTargetLease> temporaries = new ArrayList<>();
		ExecutionTarget current = source;
		UiRect currentSourceRect = sourceBounds;
		UiRect workingBounds = new UiRect(0.0F, 0.0F, sourceBounds.width(), sourceBounds.height());

		try {
			for (EffectSpec stage : effects.stages()) {
				if (stage instanceof EffectSpec.Blur blur) {
					float blurDownscale = adaptiveBlurDownscale(blur.radiusPx(), workingBounds);
					UiRect blurBounds = scaledBounds(workingBounds, blurDownscale);
					RenderTargetLease ping = this.offscreenPasses.acquire(blurBounds, EffectChain.of());
					RenderTargetLease pong = this.offscreenPasses.acquire(blurBounds, EffectChain.of());
					temporaries.add(ping);
					temporaries.add(pong);

					int totalPasses = blurPassCount(blur, workingBounds);
					ExecutionTarget read = current;
					UiRect readRect = currentSourceRect;
					ExecutionTarget write = ping.target();

					for (int pass = 0; pass < totalPasses; pass++) {
						boolean horizontal = pass % 2 == 0;
						float effectiveRadius = scaledBlurRadius(blur.radiusPx(), read, readRect, workingBounds);
						this.blit(
							horizontal ? StandardPipelines.BLUR_DOWN : StandardPipelines.BLUR_UP,
							read,
							readRect,
							write,
							blurBounds,
							RgbaColor.WHITE,
							new QuadBufferUpload.BlurUniforms(
								horizontal ? 1.0F : 0.0F,
								horizontal ? 0.0F : 1.0F,
								1.0F / (float) Math.max(1, read.width()),
								1.0F / (float) Math.max(1, read.height()),
								effectiveRadius
							),
							null,
							stats
						);

						read = write;
						readRect = blurBounds;
						write = write == ping.target() ? pong.target() : ping.target();
					}

					current = read;
					currentSourceRect = blurBounds;
					workingBounds = currentSourceRect;
				} else if (stage instanceof EffectSpec.Tint tint) {
					RenderTargetLease tinted = this.offscreenPasses.acquire(workingBounds, EffectChain.of());
					temporaries.add(tinted);
					this.blit(
						StandardPipelines.COMPOSITE,
						current,
						currentSourceRect,
						tinted.target(),
						workingBounds,
						RgbaColor.WHITE,
						new QuadBufferUpload.CompositeUniforms(1.0F, tint.color()),
						null,
						stats
					);
					current = tinted.target();
					currentSourceRect = workingBounds;
					workingBounds = currentSourceRect;
				} else if (stage instanceof EffectSpec.Custom) {
					RenderTargetLease customTarget = this.offscreenPasses.acquire(workingBounds, EffectChain.of());
					temporaries.add(customTarget);
					this.blit(
						StandardPipelines.COMPOSITE,
						current,
						currentSourceRect,
						customTarget.target(),
						workingBounds,
						RgbaColor.WHITE,
						new QuadBufferUpload.CompositeUniforms(1.0F, RgbaColor.WHITE),
						null,
						stats
					);
					current = customTarget.target();
					currentSourceRect = workingBounds;
					workingBounds = currentSourceRect;
				}
			}

			return new EffectOutput(current, currentSourceRect, temporaries);
		} catch (Throwable throwable) {
			closeLeases(temporaries);
			throw throwable;
		}
	}

	void execute(EffectChain effects, ExecutionTarget source, UiRect bounds, ExecutionTarget destination, UiRect destinationBounds, UiRect destinationScissor, SubmissionStats stats) {
		Objects.requireNonNull(effects, "effects");
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(bounds, "bounds");
		Objects.requireNonNull(destination, "destination");
		Objects.requireNonNull(destinationBounds, "destinationBounds");
		Objects.requireNonNull(stats, "stats");

		try (EffectOutput output = this.apply(effects, source, bounds, stats)) {
			UiRect destinationRect = this.rebaseRect(bounds, destinationBounds);
			this.blit(
				StandardPipelines.COMPOSITE,
				output.target(),
				output.sourceRect(),
				destination,
				destinationRect,
				RgbaColor.WHITE,
				new QuadBufferUpload.CompositeUniforms(1.0F, RgbaColor.WHITE),
				destinationScissor,
				stats
			);
		}
	}

	private void blit(
		PipelineKey pipelineKey,
		ExecutionTarget source,
		UiRect sourceRect,
		ExecutionTarget destination,
		UiRect destinationRect,
		RgbaColor tint,
		QuadBufferUpload.Uniforms uniforms,
		UiRect scissor,
		SubmissionStats stats
	) {
		BlitRegion region = this.clipBlitRegion(source, sourceRect, destinationRect);
		if (region == null) {
			return;
		}

		if (source.targetId() == destination.targetId()) {
			throw new IllegalStateException("Effect blit would sample and draw the same target: " + source.targetId());
		}

		this.executionRouter.executePostProcess(
			new PostProcessDrawData(
				pipelineKey,
				new QuadBufferUpload.TextureSource.TargetTexture(source.targetId()),
				SamplerMode.LINEAR_CLAMP,
				region.destinationRect(),
				this.normalizedUvRect(source, region.sourceRect()),
				tint,
				uniforms
			),
			new ExecutionContext(destination, scissor, stats)
		);
	}

	private UiRect normalizedUvRect(ExecutionTarget source, UiRect sourceRect) {
		float u0 = clamp01(sourceRect.x() / (float) Math.max(1, source.width()));
		float v0 = clamp01(sourceRect.y() / (float) Math.max(1, source.height()));
		float u1 = clamp01(sourceRect.right() / (float) Math.max(1, source.width()));
		float v1 = clamp01(sourceRect.bottom() / (float) Math.max(1, source.height()));
		return new UiRect(u0, v0, u1 - u0, v1 - v0);
	}

	private UiRect fullBounds(ExecutionTarget target) {
		return new UiRect(0.0F, 0.0F, target.width(), target.height());
	}

	private static UiRect scaledBounds(UiRect bounds, float scale) {
		float clampedScale = Math.max(0.25F, Math.min(scale, 1.0F));
		return new UiRect(
			0.0F,
			0.0F,
			Math.max(1.0F, (float) Math.ceil(bounds.width() * clampedScale)),
			Math.max(1.0F, (float) Math.ceil(bounds.height() * clampedScale))
		);
	}

	private static float adaptiveBlurDownscale(float radiusPx, UiRect bounds) {
		float minDimension = Math.min(bounds.width(), bounds.height());
		if (radiusPx <= 6.0F || minDimension <= 96.0F) {
			return 1.0F;
		}
		if (radiusPx <= 14.0F || minDimension <= 144.0F) {
			return 0.75F;
		}
		if (radiusPx <= 24.0F || minDimension <= 220.0F) {
			return 0.5F;
		}
		return 0.25F;
	}

	private static int blurPassCount(EffectSpec.Blur blur, UiRect bounds) {
		if (blur.radiusPx() <= 6.0F || Math.max(bounds.width(), bounds.height()) <= 96.0F) {
			return 2;
		}
		if (blur.radiusPx() <= 14.0F) {
			return 4;
		}
		return Math.max(4, blur.passes() * 2);
	}

	private static float scaledBlurRadius(float radiusPx, ExecutionTarget read, UiRect readRect, UiRect referenceBounds) {
		float widthScale = readRect.width() / Math.max(referenceBounds.width(), 1.0F);
		float heightScale = readRect.height() / Math.max(referenceBounds.height(), 1.0F);
		float textureScale = Math.min(widthScale, heightScale);
		return Math.max(radiusPx * Math.max(textureScale, 0.35F), 1.0F);
	}

	private UiRect rebaseRect(UiRect rect, UiRect destinationBounds) {
		return new UiRect(
			rect.x() - destinationBounds.x(),
			rect.y() - destinationBounds.y(),
			rect.width(),
			rect.height()
		);
	}

	private BlitRegion clipBlitRegion(ExecutionTarget source, UiRect sourceRect, UiRect destinationRect) {
		UiRect clippedSource = sourceRect.intersection(this.fullBounds(source));
		if (clippedSource.isEmpty()) {
			return null;
		}
		if (clippedSource.equals(sourceRect)) {
			return new BlitRegion(sourceRect, destinationRect);
		}

		float sourceWidth = Math.max(sourceRect.width(), 0.0001F);
		float sourceHeight = Math.max(sourceRect.height(), 0.0001F);
		float leftRatio = (clippedSource.x() - sourceRect.x()) / sourceWidth;
		float topRatio = (clippedSource.y() - sourceRect.y()) / sourceHeight;
		float rightRatio = (sourceRect.right() - clippedSource.right()) / sourceWidth;
		float bottomRatio = (sourceRect.bottom() - clippedSource.bottom()) / sourceHeight;

		UiRect clippedDestination = new UiRect(
			destinationRect.x() + destinationRect.width() * leftRatio,
			destinationRect.y() + destinationRect.height() * topRatio,
			Math.max(0.0F, destinationRect.width() * (1.0F - leftRatio - rightRatio)),
			Math.max(0.0F, destinationRect.height() * (1.0F - topRatio - bottomRatio))
		);
		if (clippedDestination.isEmpty()) {
			return null;
		}

		return new BlitRegion(clippedSource, clippedDestination);
	}

	private static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	private record BlitRegion(UiRect sourceRect, UiRect destinationRect) {
	}

	record EffectOutput(ExecutionTarget target, UiRect sourceRect, List<RenderTargetLease> leases) implements AutoCloseable {
		EffectOutput {
			Objects.requireNonNull(target, "target");
			Objects.requireNonNull(sourceRect, "sourceRect");
			Objects.requireNonNull(leases, "leases");
		}

		@Override
		public void close() {
			closeLeases(this.leases);
		}
	}

	private static void closeLeases(List<RenderTargetLease> leases) {
		RuntimeException failure = null;
		for (int index = leases.size() - 1; index >= 0; index--) {
			try {
				leases.get(index).close();
			} catch (RuntimeException exception) {
				if (failure == null) {
					failure = exception;
				} else {
					failure.addSuppressed(exception);
				}
			}
		}
		if (failure != null) {
			throw failure;
		}
	}
}
