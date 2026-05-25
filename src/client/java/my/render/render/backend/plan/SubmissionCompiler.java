package my.render.render.backend.plan;

import my.render.render.backend.DrawCommand;
import my.render.render.backend.ExecutionFamily;
import my.render.render.backend.diagnostics.SubmissionStats;
import my.render.render.backend.material.MaterialDescriptor;
import my.render.render.backend.material.MaterialResolver;
import my.render.render.base.Gradient4;
import my.render.render.base.StrokeStyle;
import my.render.render.base.TextureRegion;
import my.render.render.base.UiRect;
import my.render.render.effect.EffectChain;
import my.render.render.font.PreparedTextLayout;
import my.render.render.frame.UiFrameContext;
import my.render.render.pipeline.PipelineLibrary;
import my.render.render.pipeline.PipelineSpec;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SubmissionCompiler {
	private final PipelineLibrary pipelines;
	private final MaterialResolver materialResolver;

	public SubmissionCompiler(PipelineLibrary pipelines, MaterialResolver materialResolver) {
		this.pipelines = Objects.requireNonNull(pipelines, "pipelines");
		this.materialResolver = Objects.requireNonNull(materialResolver, "materialResolver");
	}

	public CompiledSubmission compile(UiFrameContext frame, List<DrawCommand> commands, SubmissionStats stats) {
		Objects.requireNonNull(frame, "frame");
		Objects.requireNonNull(commands, "commands");
		Objects.requireNonNull(stats, "stats");

		stats.beginShadowCompile();

		try {
			this.validate(commands);
			CompiledPass root = this.compilePassTree(frame, commands, stats);
			int passCount = countPasses(root);
			int drawCount = countResolvedDraws(root);
			stats.finishShadowCompile(drawCount, passCount);
			return new CompiledSubmission(frame, root, drawCount, passCount);
		} catch (Throwable throwable) {
			stats.failShadowCompile(throwable);
			throw throwable;
		}
	}

	private void validate(List<DrawCommand> commands) {
		int scissorDepth = 0;
		int effectDepth = 0;

		for (DrawCommand command : commands) {
			Objects.requireNonNull(command, "command");

			if (command instanceof DrawCommand.PushScissor) {
				scissorDepth++;
			} else if (command instanceof DrawCommand.PopScissor) {
				if (scissorDepth == 0) {
					throw new IllegalStateException("Encountered popScissor without a matching pushScissor");
				}
				scissorDepth--;
			} else if (command instanceof DrawCommand.PushEffectLayer) {
				effectDepth++;
			} else if (command instanceof DrawCommand.PopEffectLayer) {
				if (effectDepth == 0) {
					throw new IllegalStateException("Encountered popEffectLayer without a matching pushEffectLayer");
				}
				effectDepth--;
			}
		}

		if (scissorDepth != 0) {
			throw new IllegalStateException("Unbalanced scissor stack at compile time");
		}
		if (effectDepth != 0) {
			throw new IllegalStateException("Unbalanced effect stack at compile time");
		}
	}

	private CompiledPass compilePassTree(UiFrameContext frame, List<DrawCommand> commands, SubmissionStats stats) {
		ArrayDeque<PassBuilder> passStack = new ArrayDeque<>();
		ArrayDeque<ScissorState> scissorStack = new ArrayDeque<>();
		UiRect rootBounds = new UiRect(0.0F, 0.0F, frame.surface().guiWidth(), frame.surface().guiHeight());
		PassBuilder root = new PassBuilder(0, PipelineSpec.TargetKind.MAIN_COLOR, rootBounds, EffectChain.of(), null);
		passStack.push(root);
		scissorStack.push(new ScissorState(0, null));

		int nextPassId = 1;
		int nextScissorId = 1;
		int nextDependencyId = 1;
		int nextOrderIndex = 0;

		for (DrawCommand command : commands) {
			PassBuilder currentPass = passStack.peek();
			ScissorState currentScissor = scissorStack.peek();

			if (command instanceof DrawCommand.PushScissor pushScissor) {
				UiRect nextRect = currentScissor.rect() == null ? pushScissor.rect() : currentScissor.rect().intersection(pushScissor.rect());
				ScissorState nextScissor = new ScissorState(nextScissorId++, nextRect);
				scissorStack.push(nextScissor);
				currentPass.addStep(new ScissorMarker(true, nextScissor.id(), this.toPassLocal(nextScissor.rect(), currentPass.bounds())));
				continue;
			}
			if (command instanceof DrawCommand.PopScissor) {
				if (scissorStack.size() <= 1) {
					throw new IllegalStateException("Attempted to pop the root scissor state");
				}
				ScissorState removed = scissorStack.pop();
				currentPass.addStep(new ScissorMarker(false, removed.id(), this.toPassLocal(removed.rect(), currentPass.bounds())));
				continue;
			}
			if (command instanceof DrawCommand.PushEffectLayer pushEffectLayer) {
				UiRect initialScissor = this.toPassLocal(currentScissor.rect(), pushEffectLayer.bounds());
				PassBuilder child = new PassBuilder(nextPassId++, PipelineSpec.TargetKind.OFFSCREEN_COLOR, pushEffectLayer.bounds(), pushEffectLayer.effects(), initialScissor);
				currentPass.addDeferredChild(child);
				passStack.push(child);
				continue;
			}
			if (command instanceof DrawCommand.PopEffectLayer) {
				if (passStack.size() <= 1) {
					throw new IllegalStateException("Encountered popEffectLayer without an active child pass");
				}
				PassBuilder completed = passStack.pop();
				passStack.peek().sealDeferredChild(completed.build());
				continue;
			}
			if (command.pipeline() == null) {
				continue;
			}

			PipelineSpec spec = this.pipelines.resolve(command.pipeline());
			MaterialDescriptor material = this.materialResolver.resolve(command, spec);
			int dependencyId = material.dedicated() ? nextDependencyId++ : 0;
			CompiledDrawData compiledData = this.compileDrawData(command, material, currentPass.bounds());
			ResolvedDraw resolved = new ResolvedDraw(
				command,
				command.pipeline(),
				spec,
				material,
				compiledData,
				currentPass.passId(),
				nextOrderIndex++,
				currentScissor.id(),
				this.toPassLocal(currentScissor.rect(), currentPass.bounds()),
				currentPass.bounds(),
				dependencyId
			);
			currentPass.addStep(new DrawStep(resolved));
		}

		if (passStack.size() != 1) {
			throw new IllegalStateException("Unbalanced pass stack after compile");
		}

		return root.build();
	}

	private CompiledDrawData compileDrawData(DrawCommand command, MaterialDescriptor material, UiRect passBounds) {
		if (material.executionFamily() == ExecutionFamily.ANALYTIC_QUAD) {
			return this.compileAnalyticQuad(command, passBounds);
		}
		if (material.executionFamily() == ExecutionFamily.SAMPLED_QUAD) {
			return this.compileSampledQuad(command, passBounds);
		}
		if (material.executionFamily() == ExecutionFamily.TEXT_QUAD) {
			return this.compileTextQuad(command, passBounds);
		}
		if (material.executionFamily() == ExecutionFamily.BACKDROP_BLUR) {
			return this.compileBackdropBlur(command, passBounds);
		}
		throw new IllegalStateException("Unsupported execution family during compile: " + material.executionFamily());
	}

	private AnalyticQuadDrawData compileAnalyticQuad(DrawCommand command, UiRect passBounds) {
		if (command instanceof DrawCommand.FillRect fillRect) {
			UiRect rect = this.toPassLocal(fillRect.rect(), passBounds);
			return AnalyticQuadDrawData.fill(rect, fillRect.fill());
		}
		if (command instanceof DrawCommand.FillRoundedRect fillRoundedRect) {
			UiRect rect = this.toPassLocal(fillRoundedRect.rect(), passBounds);
			return AnalyticQuadDrawData.roundedFill(rect, fillRoundedRect.radii(), fillRoundedRect.fill());
		}
		if (command instanceof DrawCommand.DrawBorder drawBorder) {
			UiRect shapeRect = this.toPassLocal(drawBorder.rect(), passBounds);
			UiRect geometryRect = this.borderGeometryRect(shapeRect, drawBorder.stroke());
			return AnalyticQuadDrawData.border(
				shapeRect,
				geometryRect,
				drawBorder.radii(),
				Gradient4.solid(drawBorder.stroke().color()),
				drawBorder.stroke().widthPx(),
				this.strokeAlignCode(drawBorder.stroke().align())
			);
		}
		if (command instanceof DrawCommand.DrawShadow drawShadow) {
			UiRect shapeRect = this.toPassLocal(drawShadow.rect(), passBounds);
			UiRect geometryRect = shapeRect.inset(-(drawShadow.softnessPx() * 3.0F + 2.0F));
			return AnalyticQuadDrawData.shadow(
				shapeRect,
				geometryRect,
				drawShadow.radii(),
				Gradient4.solid(drawShadow.color()),
				drawShadow.softnessPx()
			);
		}
		throw new IllegalArgumentException("Unsupported analytic draw command: " + command.getClass().getSimpleName());
	}

	private SampledQuadDrawData compileSampledQuad(DrawCommand command, UiRect passBounds) {
		if (command instanceof DrawCommand.DrawTexture drawTexture) {
			return new SampledQuadDrawData(
				drawTexture.textureId(),
				this.toPassLocal(drawTexture.rect(), passBounds),
				this.uvRect(drawTexture.region()),
				drawTexture.radii(),
				drawTexture.tint(),
				drawTexture.edgeSoftnessPx()
			);
		}
		throw new IllegalArgumentException("Unsupported sampled draw command: " + command.getClass().getSimpleName());
	}

	private TextQuadDrawData compileTextQuad(DrawCommand command, UiRect passBounds) {
		if (command instanceof DrawCommand.DrawText drawText) {
			float baseX = drawText.x() - passBounds.x();
			float baseY = drawText.y() - passBounds.y();
			List<TextQuadDrawData.GlyphQuad> glyphs = new ArrayList<>(drawText.layout().glyphs().size());

			for (PreparedTextLayout.Glyph glyph : drawText.layout().glyphs()) {
				UiRect planeBounds = glyph.planeBounds();
				UiRect glyphRect = new UiRect(
					baseX + planeBounds.x(),
					baseY + planeBounds.y(),
					planeBounds.width(),
					planeBounds.height()
				);
				glyphs.add(new TextQuadDrawData.GlyphQuad(glyphRect, this.uvRect(glyph.atlasRegion())));
			}

			return new TextQuadDrawData(
				drawText.layout().font(),
				drawText.layout().pxRange(),
				TextQuadDrawData.DEFAULT_EDGE_SOFTNESS_PX,
				TextQuadDrawData.DEFAULT_SHARPNESS,
				drawText.tint(),
				glyphs
			);
		}
		throw new IllegalArgumentException("Unsupported text draw command: " + command.getClass().getSimpleName());
	}

	private BackdropBlurDrawData compileBackdropBlur(DrawCommand command, UiRect passBounds) {
		if (command instanceof DrawCommand.DrawBackdropBlur drawBackdropBlur) {
			return new BackdropBlurDrawData(
				this.toPassLocal(drawBackdropBlur.rect(), passBounds),
				drawBackdropBlur.radii(),
				drawBackdropBlur.blurRadiusPx(),
				drawBackdropBlur.tint()
			);
		}
		throw new IllegalArgumentException("Unsupported backdrop blur command: " + command.getClass().getSimpleName());
	}

	private UiRect toPassLocal(UiRect rect, UiRect passBounds) {
		if (rect == null || (passBounds.x() == 0.0F && passBounds.y() == 0.0F)) {
			return rect;
		}
		return new UiRect(rect.x() - passBounds.x(), rect.y() - passBounds.y(), rect.width(), rect.height());
	}

	private UiRect borderGeometryRect(UiRect rect, StrokeStyle stroke) {
		float expansion = switch (stroke.align()) {
			case INSIDE -> 0.0F;
			case CENTER -> stroke.widthPx() * 0.5F;
			case OUTSIDE -> stroke.widthPx();
		};
		return rect.inset(-expansion);
	}

	private UiRect uvRect(TextureRegion region) {
		float u0 = (float) region.u() / (float) region.textureWidth();
		float v0 = (float) region.v() / (float) region.textureHeight();
		float u1 = (float) (region.u() + region.width()) / (float) region.textureWidth();
		float v1 = (float) (region.v() + region.height()) / (float) region.textureHeight();
		return new UiRect(u0, v0, u1 - u0, v1 - v0);
	}

	private float strokeAlignCode(StrokeStyle.Align align) {
		return switch (align) {
			case INSIDE -> 0.0F;
			case CENTER -> 1.0F;
			case OUTSIDE -> 2.0F;
		};
	}

	private static int countPasses(CompiledPass pass) {
		int count = 1;
		for (CompiledStep step : pass.steps()) {
			if (step instanceof ChildPassStep childPassStep) {
				count += countPasses(childPassStep.child());
			}
		}
		return count;
	}

	private static int countResolvedDraws(CompiledPass pass) {
		int count = 0;
		for (CompiledStep step : pass.steps()) {
			if (step instanceof DrawStep) {
				count++;
			} else if (step instanceof ChildPassStep childPassStep) {
				count += countResolvedDraws(childPassStep.child());
			}
		}
		return count;
	}

	public record CompiledSubmission(UiFrameContext frame, CompiledPass rootPass, int resolvedDrawCount, int passCount) {
		public CompiledSubmission {
			Objects.requireNonNull(frame, "frame");
			Objects.requireNonNull(rootPass, "rootPass");
			if (resolvedDrawCount < 0) {
				throw new IllegalArgumentException("resolvedDrawCount must be non-negative");
			}
			if (passCount < 0) {
				throw new IllegalArgumentException("passCount must be non-negative");
			}
		}
	}

	public record CompiledPass(int passId, PipelineSpec.TargetKind targetKind, UiRect bounds, EffectChain effects, UiRect initialScissor, List<CompiledStep> steps) {
		public CompiledPass(int passId, PipelineSpec.TargetKind targetKind, UiRect bounds, EffectChain effects, UiRect initialScissor, List<CompiledStep> steps) {
			Objects.requireNonNull(targetKind, "targetKind");
			Objects.requireNonNull(bounds, "bounds");
			Objects.requireNonNull(effects, "effects");
			if (passId < 0) {
				throw new IllegalArgumentException("passId must be non-negative");
			}
			this.passId = passId;
			this.targetKind = targetKind;
			this.bounds = bounds;
			this.effects = effects;
			this.initialScissor = initialScissor;
			this.steps = List.copyOf(steps);
		}
	}

	public sealed interface CompiledStep permits DrawStep, ScissorMarker, ChildPassStep {
	}

	public record DrawStep(ResolvedDraw draw) implements CompiledStep {
		public DrawStep {
			Objects.requireNonNull(draw, "draw");
		}
	}

	public record ScissorMarker(boolean push, int scissorId, UiRect rect) implements CompiledStep {
		public ScissorMarker {
			if (scissorId < 0) {
				throw new IllegalArgumentException("scissorId must be non-negative");
			}
		}
	}

	public record ChildPassStep(CompiledPass child) implements CompiledStep {
		public ChildPassStep {
			Objects.requireNonNull(child, "child");
		}
	}

	private record ScissorState(int id, UiRect rect) {
		private ScissorState {
			if (id < 0) {
				throw new IllegalArgumentException("id must be non-negative");
			}
		}
	}

	private static final class PassBuilder {
		private final int passId;
		private final PipelineSpec.TargetKind targetKind;
		private final UiRect bounds;
		private final EffectChain effects;
		private final UiRect initialScissor;
		private final List<CompiledStep> steps = new ArrayList<>();
		private PassBuilder deferredChild;

		private PassBuilder(int passId, PipelineSpec.TargetKind targetKind, UiRect bounds, EffectChain effects, UiRect initialScissor) {
			this.passId = passId;
			this.targetKind = Objects.requireNonNull(targetKind, "targetKind");
			this.bounds = Objects.requireNonNull(bounds, "bounds");
			this.effects = Objects.requireNonNull(effects, "effects");
			this.initialScissor = initialScissor;
		}

		private int passId() {
			return this.passId;
		}

		private UiRect bounds() {
			return this.bounds;
		}

		private void addStep(CompiledStep step) {
			this.steps.add(Objects.requireNonNull(step, "step"));
		}

		private void addDeferredChild(PassBuilder child) {
			if (this.deferredChild != null) {
				throw new IllegalStateException("Only one deferred child pass can be active at a time");
			}
			this.deferredChild = Objects.requireNonNull(child, "child");
		}

		private void sealDeferredChild(CompiledPass child) {
			if (this.deferredChild == null) {
				throw new IllegalStateException("No deferred child pass is waiting to be sealed");
			}
			this.steps.add(new ChildPassStep(child));
			this.deferredChild = null;
		}

		private CompiledPass build() {
			if (this.deferredChild != null) {
				throw new IllegalStateException("Cannot build pass while a child pass is still open");
			}
			return new CompiledPass(this.passId, this.targetKind, this.bounds, this.effects, this.initialScissor, this.steps);
		}
	}
}
