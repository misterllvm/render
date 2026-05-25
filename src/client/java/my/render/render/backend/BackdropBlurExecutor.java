package my.render.render.backend;

import my.render.render.backend.plan.BackdropBlurDrawData;
import my.render.render.backend.plan.RenderBatch;
import my.render.render.backend.plan.ResolvedDraw;
import my.render.render.base.UiRect;
import my.render.render.effect.EffectChain;
import my.render.render.effect.EffectSpec;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;

import java.util.List;
import java.util.Objects;

final class BackdropBlurExecutor {
	private final PipelineBinder pipelineBinder;
	private final GpuCommandEncoder commandEncoder;
	private final BackdropBlurBatchEncoder batchEncoder;
	private final OffscreenPassManager offscreenPasses;
	private EffectChainExecutor effectChainExecutor;

	BackdropBlurExecutor(PipelineBinder pipelineBinder, GpuCommandEncoder commandEncoder, BackdropBlurBatchEncoder batchEncoder, OffscreenPassManager offscreenPasses) {
		this.pipelineBinder = Objects.requireNonNull(pipelineBinder, "pipelineBinder");
		this.commandEncoder = Objects.requireNonNull(commandEncoder, "commandEncoder");
		this.batchEncoder = Objects.requireNonNull(batchEncoder, "batchEncoder");
		this.offscreenPasses = Objects.requireNonNull(offscreenPasses, "offscreenPasses");
	}

	void bindEffectChainExecutor(EffectChainExecutor effectChainExecutor) {
		this.effectChainExecutor = Objects.requireNonNull(effectChainExecutor, "effectChainExecutor");
	}

	void execute(RenderBatch batch, ExecutionContext context) {
		Objects.requireNonNull(batch, "batch");
		Objects.requireNonNull(context, "context");

		List<ResolvedDraw> draws = batch.draws();
		for (ResolvedDraw draw : draws) {
			this.executeDraw(draw, context);
		}
	}

	private void executeDraw(ResolvedDraw draw, ExecutionContext context) {
		if (draw.material().executionFamily() != ExecutionFamily.BACKDROP_BLUR) {
			throw new IllegalStateException("Non-backdrop draw entered BackdropBlurExecutor");
		}

		BackdropBlurDrawData data = draw.compiledData(BackdropBlurDrawData.class);
		EffectChain blurChain = EffectChain.of(new EffectSpec.Blur(data.blurRadiusPx(), this.blurPasses(data.blurRadiusPx())));
		boolean croppedCapture = this.isFullyInside(context.target(), data.rectPx());
		UiRect blurSourceBounds = croppedCapture
			? new UiRect(0.0F, 0.0F, data.rectPx().width(), data.rectPx().height())
			: data.rectPx();

		if (this.effectChainExecutor == null) {
			throw new IllegalStateException("BackdropBlurExecutor is not bound to an EffectChainExecutor");
		}

		try (RenderTargetLease backdropCapture = this.captureBackdrop(context.target(), data.rectPx());
		     EffectChainExecutor.EffectOutput output = this.effectChainExecutor.apply(blurChain, backdropCapture.target(), blurSourceBounds, context.stats())) {
			PipelineBinding binding = this.pipelineBinder.bind(draw.pipelineSpec());
			QuadBufferUpload upload = this.batchEncoder.encode(
				data,
				new QuadBufferUpload.TextureSource.TargetTexture(output.target().targetId()),
				this.normalizedSampleUvRect(output.target(), output.sourceRect())
			);
			context.stats().recordPipelineUse(draw.pipelineKey());
			context.stats().recordTextureUse(output.target().targetId());
			if (this.commandEncoder.execute(binding, upload, context.target(), context.scissor())) {
				context.stats().recordDrawCall();
			} else {
				context.stats().recordSkippedDraw();
			}
		}
	}

	private int blurPasses(float blurRadiusPx) {
		return Math.max(1, Math.round(Math.max(blurRadiusPx, 1.0F) / 6.0F));
	}

	private RenderTargetLease captureBackdrop(ExecutionTarget sourceTarget, UiRect sourceRect) {
		if (this.isFullyInside(sourceTarget, sourceRect)) {
			UiRect captureBounds = new UiRect(0.0F, 0.0F, sourceRect.width(), sourceRect.height());
			RenderTargetLease capture = this.offscreenPasses.acquire(captureBounds, EffectChain.of());
			this.blitColor(sourceTarget, sourceRect, capture.target(), captureBounds);
			return capture;
		}

		UiRect captureBounds = new UiRect(0.0F, 0.0F, sourceTarget.width(), sourceTarget.height());
		RenderTargetLease capture = this.offscreenPasses.acquire(captureBounds, EffectChain.of());
		this.blitColor(sourceTarget, new UiRect(0.0F, 0.0F, sourceTarget.width(), sourceTarget.height()), capture.target(), captureBounds);
		return capture;
	}

	private void blitColor(ExecutionTarget source, UiRect sourceRect, ExecutionTarget destination, UiRect destinationRect) {
		int previousReadFramebuffer = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
		int previousDrawFramebuffer = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
		PhysicalRect sourcePixels = this.toPhysicalRect(source, sourceRect);
		PhysicalRect destinationPixels = this.toPhysicalRect(destination, destinationRect);
		try {
			GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, source.renderTarget().framebufferId());
			GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, destination.renderTarget().framebufferId());
			GL30C.glBlitFramebuffer(
				sourcePixels.x0(),
				sourcePixels.y0(),
				sourcePixels.x1(),
				sourcePixels.y1(),
				destinationPixels.x0(),
				destinationPixels.y0(),
				destinationPixels.x1(),
				destinationPixels.y1(),
				GL11C.GL_COLOR_BUFFER_BIT,
				GL11C.GL_LINEAR
			);
		} finally {
			GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
			GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer);
		}
	}

	private boolean isFullyInside(ExecutionTarget target, UiRect rect) {
		return rect.x() >= 0.0F && rect.y() >= 0.0F && rect.right() <= target.width() && rect.bottom() <= target.height();
	}

	private UiRect normalizedSampleUvRect(ExecutionTarget source, UiRect sourceRect) {
		float u0 = clamp01(sourceRect.x() / (float) Math.max(1, source.width()));
		float v0 = clamp01(sourceRect.y() / (float) Math.max(1, source.height()));
		float u1 = clamp01(sourceRect.right() / (float) Math.max(1, source.width()));
		float v1 = clamp01(sourceRect.bottom() / (float) Math.max(1, source.height()));
		float insetU = Math.min(0.5F / (float) Math.max(1, source.width()), Math.max(0.0F, (u1 - u0) * 0.5F));
		float insetV = Math.min(0.5F / (float) Math.max(1, source.height()), Math.max(0.0F, (v1 - v0) * 0.5F));
		u0 += insetU;
		v0 += insetV;
		u1 -= insetU;
		v1 -= insetV;
		return new UiRect(u0, v0, u1 - u0, v1 - v0);
	}

	private static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	private PhysicalRect toPhysicalRect(ExecutionTarget target, UiRect rect) {
		float scaleX = (float) target.physicalWidth() / (float) Math.max(target.width(), 1);
		float scaleY = (float) target.physicalHeight() / (float) Math.max(target.height(), 1);
		int x0 = Math.round(rect.x() * scaleX);
		int y0 = Math.round((target.height() - rect.bottom()) * scaleY);
		int x1 = Math.round(rect.right() * scaleX);
		int y1 = Math.round((target.height() - rect.y()) * scaleY);
		return new PhysicalRect(x0, y0, x1, y1);
	}

	private record PhysicalRect(int x0, int y0, int x1, int y1) {
	}
}
