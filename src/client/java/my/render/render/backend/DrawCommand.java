package my.render.render.backend;

import my.render.render.base.*;
import my.render.render.base.*;
import my.render.render.core.ResourceId;
import my.render.render.effect.EffectChain;
import my.render.render.font.PreparedTextLayout;
import my.render.render.pipeline.PipelineKey;

import java.util.Objects;

public sealed interface DrawCommand permits DrawCommand.FillRect, DrawCommand.FillRoundedRect, DrawCommand.DrawBorder, DrawCommand.DrawShadow, DrawCommand.DrawBackdropBlur, DrawCommand.DrawTexture, DrawCommand.DrawText, DrawCommand.PushScissor, DrawCommand.PopScissor, DrawCommand.PushEffectLayer, DrawCommand.PopEffectLayer {
	default PipelineKey pipeline() {
		return null;
	}

	default DrawCommand translate(float offsetX, float offsetY) {
		return this;
	}

	record FillRect(PipelineKey pipeline, UiRect rect, Gradient4 fill) implements DrawCommand {
		public FillRect {
			Objects.requireNonNull(pipeline, "pipeline");
			Objects.requireNonNull(rect, "rect");
			Objects.requireNonNull(fill, "fill");
		}

		@Override
		public DrawCommand translate(float offsetX, float offsetY) {
			return new FillRect(this.pipeline, translateRect(this.rect, offsetX, offsetY), this.fill);
		}
	}

	record FillRoundedRect(PipelineKey pipeline, UiRect rect, CornerRadii radii, Gradient4 fill) implements DrawCommand {
		public FillRoundedRect {
			Objects.requireNonNull(pipeline, "pipeline");
			Objects.requireNonNull(rect, "rect");
			Objects.requireNonNull(radii, "radii");
			Objects.requireNonNull(fill, "fill");
		}

		@Override
		public DrawCommand translate(float offsetX, float offsetY) {
			return new FillRoundedRect(this.pipeline, translateRect(this.rect, offsetX, offsetY), this.radii, this.fill);
		}
	}

	record DrawBorder(PipelineKey pipeline, UiRect rect, CornerRadii radii, StrokeStyle stroke) implements DrawCommand {
		public DrawBorder {
			Objects.requireNonNull(pipeline, "pipeline");
			Objects.requireNonNull(rect, "rect");
			Objects.requireNonNull(radii, "radii");
			Objects.requireNonNull(stroke, "stroke");
		}

		@Override
		public DrawCommand translate(float offsetX, float offsetY) {
			return new DrawBorder(this.pipeline, translateRect(this.rect, offsetX, offsetY), this.radii, this.stroke);
		}
	}

	record DrawShadow(PipelineKey pipeline, UiRect rect, CornerRadii radii, float softnessPx, RgbaColor color) implements DrawCommand {
		public DrawShadow {
			Objects.requireNonNull(pipeline, "pipeline");
			Objects.requireNonNull(rect, "rect");
			Objects.requireNonNull(radii, "radii");
			Objects.requireNonNull(color, "color");
			if (softnessPx <= 0.0F) {
				throw new IllegalArgumentException("softnessPx must be positive");
			}
		}

		@Override
		public DrawCommand translate(float offsetX, float offsetY) {
			return new DrawShadow(this.pipeline, translateRect(this.rect, offsetX, offsetY), this.radii, this.softnessPx, this.color);
		}
	}

	record DrawBackdropBlur(PipelineKey pipeline, UiRect rect, CornerRadii radii, float blurRadiusPx, RgbaColor tint) implements DrawCommand {
		public DrawBackdropBlur {
			Objects.requireNonNull(pipeline, "pipeline");
			Objects.requireNonNull(rect, "rect");
			Objects.requireNonNull(radii, "radii");
			Objects.requireNonNull(tint, "tint");
			if (blurRadiusPx <= 0.0F) {
				throw new IllegalArgumentException("blurRadiusPx must be positive");
			}
		}

		@Override
		public DrawCommand translate(float offsetX, float offsetY) {
			return new DrawBackdropBlur(this.pipeline, translateRect(this.rect, offsetX, offsetY), this.radii, this.blurRadiusPx, this.tint);
		}
	}

	record DrawTexture(PipelineKey pipeline, ResourceId textureId, UiRect rect, TextureRegion region, CornerRadii radii, Gradient4 tint, float edgeSoftnessPx) implements DrawCommand {
		public DrawTexture {
			Objects.requireNonNull(pipeline, "pipeline");
			Objects.requireNonNull(textureId, "textureId");
			Objects.requireNonNull(rect, "rect");
			Objects.requireNonNull(region, "region");
			Objects.requireNonNull(radii, "radii");
			Objects.requireNonNull(tint, "tint");
			if (edgeSoftnessPx < 0.0F) {
				throw new IllegalArgumentException("edgeSoftnessPx must be non-negative");
			}
		}

		@Override
		public DrawCommand translate(float offsetX, float offsetY) {
			return new DrawTexture(this.pipeline, this.textureId, translateRect(this.rect, offsetX, offsetY), this.region, this.radii, this.tint, this.edgeSoftnessPx);
		}
	}

	record DrawText(PipelineKey pipeline, PreparedTextLayout layout, float x, float y, RgbaColor tint) implements DrawCommand {
		public DrawText {
			Objects.requireNonNull(pipeline, "pipeline");
			Objects.requireNonNull(layout, "layout");
			Objects.requireNonNull(tint, "tint");
		}

		@Override
		public DrawCommand translate(float offsetX, float offsetY) {
			return new DrawText(this.pipeline, this.layout, this.x + offsetX, this.y + offsetY, this.tint);
		}
	}

	record PushScissor(UiRect rect) implements DrawCommand {
		public PushScissor {
			Objects.requireNonNull(rect, "rect");
		}

		@Override
		public DrawCommand translate(float offsetX, float offsetY) {
			return new PushScissor(translateRect(this.rect, offsetX, offsetY));
		}
	}

	record PopScissor() implements DrawCommand {
	}

	record PushEffectLayer(UiRect bounds, EffectChain effects) implements DrawCommand {
		public PushEffectLayer {
			Objects.requireNonNull(bounds, "bounds");
			Objects.requireNonNull(effects, "effects");
		}

		@Override
		public DrawCommand translate(float offsetX, float offsetY) {
			return new PushEffectLayer(translateRect(this.bounds, offsetX, offsetY), this.effects);
		}
	}

	record PopEffectLayer() implements DrawCommand {
	}

	private static UiRect translateRect(UiRect rect, float offsetX, float offsetY) {
		return new UiRect(rect.x() + offsetX, rect.y() + offsetY, rect.width(), rect.height());
	}
}
