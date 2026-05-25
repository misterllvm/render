package my.render.render.base;

import my.render.render.backend.DrawCommand;
import my.render.render.backend.DrawCommandBuffer;
import my.render.render.backend.RenderStateGuard;
import my.render.render.core.ResourceId;
import my.render.render.effect.EffectChain;
import my.render.render.font.PreparedTextLayout;
import my.render.render.frame.SurfaceMetrics;
import my.render.render.pipeline.PipelineKey;
import my.render.render.pipeline.StandardPipelines;

import java.util.ArrayDeque;
import java.util.Objects;

public final class UiRenderContext {
	private final SurfaceMetrics surface;
	private final DrawCommandBuffer commands;
	private final ArrayDeque<UiRect> scissorStack = new ArrayDeque<>();

	public UiRenderContext(SurfaceMetrics surface, DrawCommandBuffer commands) {
		this.surface = Objects.requireNonNull(surface, "surface");
		this.commands = Objects.requireNonNull(commands, "commands");
	}

	public SurfaceMetrics surface() {
		return this.surface;
	}

	public DrawCommandBuffer commands() {
		return this.commands;
	}

	public void fillRoundedRect(UiRect rect, CornerRadii radii, RgbaColor color) {
		this.fillRoundedRect(StandardPipelines.ROUNDED_FILL, rect, radii, Gradient4.solid(color));
	}

	public void fillRoundedRect(PipelineKey pipeline, UiRect rect, CornerRadii radii, Gradient4 fill) {
		this.commands.add(new DrawCommand.FillRoundedRect(Objects.requireNonNull(pipeline, "pipeline"), Objects.requireNonNull(rect, "rect"), Objects.requireNonNull(radii, "radii"), Objects.requireNonNull(fill, "fill")));
	}

	public void drawBorder(UiRect rect, CornerRadii radii, StrokeStyle stroke) {
		this.drawBorder(StandardPipelines.BORDER, rect, radii, stroke);
	}

	public void drawBorder(PipelineKey pipeline, UiRect rect, CornerRadii radii, StrokeStyle stroke) {
		this.commands.add(new DrawCommand.DrawBorder(Objects.requireNonNull(pipeline, "pipeline"), Objects.requireNonNull(rect, "rect"), Objects.requireNonNull(radii, "radii"), Objects.requireNonNull(stroke, "stroke")));
	}

	public void drawShadow(UiRect rect, CornerRadii radii, float softnessPx, RgbaColor color) {
		this.drawShadow(StandardPipelines.SHADOW, rect, radii, softnessPx, color);
	}

	public void drawShadow(PipelineKey pipeline, UiRect rect, CornerRadii radii, float softnessPx, RgbaColor color) {
		this.commands.add(new DrawCommand.DrawShadow(
			Objects.requireNonNull(pipeline, "pipeline"),
			Objects.requireNonNull(rect, "rect"),
			Objects.requireNonNull(radii, "radii"),
			softnessPx,
			Objects.requireNonNull(color, "color")
		));
	}

	public void drawBackdropBlur(UiRect rect, CornerRadii radii, float blurRadiusPx, RgbaColor tint) {
		this.drawBackdropBlur(StandardPipelines.BACKDROP_BLUR, rect, radii, blurRadiusPx, tint);
	}

	public void drawBackdropBlur(PipelineKey pipeline, UiRect rect, CornerRadii radii, float blurRadiusPx, RgbaColor tint) {
		this.commands.add(new DrawCommand.DrawBackdropBlur(
			Objects.requireNonNull(pipeline, "pipeline"),
			Objects.requireNonNull(rect, "rect"),
			Objects.requireNonNull(radii, "radii"),
			blurRadiusPx,
			Objects.requireNonNull(tint, "tint")
		));
	}

	public void drawText(PreparedTextLayout layout, float x, float y, RgbaColor tint) {
		this.drawText(StandardPipelines.TEXT, layout, x, y, tint);
	}

	public void drawText(PipelineKey pipeline, PreparedTextLayout layout, float x, float y, RgbaColor tint) {
		this.commands.add(new DrawCommand.DrawText(Objects.requireNonNull(pipeline, "pipeline"), Objects.requireNonNull(layout, "layout"), x, y, Objects.requireNonNull(tint, "tint")));
	}

	public void pushScissor(UiRect rect) {
		UiRect next = Objects.requireNonNull(rect, "rect");
		if (!this.scissorStack.isEmpty()) {
			next = this.scissorStack.peek().intersection(next);
		}

		this.scissorStack.push(next);
		this.commands.add(new DrawCommand.PushScissor(next));
	}

	public void popScissor() {
		if (this.scissorStack.isEmpty()) {
			throw new IllegalStateException("Cannot pop scissor when no scissor is active");
		}

		this.scissorStack.pop();
		this.commands.add(new DrawCommand.PopScissor());
	}

	public RenderStateGuard scopedScissor(UiRect rect) {
		this.pushScissor(rect);
		return RenderStateGuard.once(this::popScissor);
	}
}
