package my.render.render.backend.plan;

import my.render.render.base.RgbaColor;
import my.render.render.base.UiRect;
import my.render.render.core.ResourceId;

import java.util.List;
import java.util.Objects;

public record TextQuadDrawData(
	ResourceId fontTextureId,
	float pxRange,
	float edgeSoftnessPx,
	float sharpness,
	RgbaColor tint,
	List<GlyphQuad> glyphs
) implements CompiledDrawData {
	public static final float DEFAULT_EDGE_SOFTNESS_PX = 0.72F;
	public static final float DEFAULT_SHARPNESS = 1.12F;

	public TextQuadDrawData(ResourceId fontTextureId, float pxRange, float edgeSoftnessPx, float sharpness, RgbaColor tint, List<GlyphQuad> glyphs) {
		Objects.requireNonNull(fontTextureId, "fontTextureId");
		Objects.requireNonNull(tint, "tint");
		Objects.requireNonNull(glyphs, "glyphs");
		if (pxRange <= 0.0F) {
			throw new IllegalArgumentException("pxRange must be positive");
		}
		if (edgeSoftnessPx <= 0.0F) {
			throw new IllegalArgumentException("edgeSoftnessPx must be positive");
		}
		if (sharpness <= 0.0F) {
			throw new IllegalArgumentException("sharpness must be positive");
		}

		List<GlyphQuad> copiedGlyphs = List.copyOf(glyphs);
		for (GlyphQuad glyph : copiedGlyphs) {
			Objects.requireNonNull(glyph, "glyph");
		}

		this.fontTextureId = fontTextureId;
		this.pxRange = pxRange;
		this.edgeSoftnessPx = edgeSoftnessPx;
		this.sharpness = sharpness;
		this.tint = tint;
		this.glyphs = copiedGlyphs;
	}

	public record GlyphQuad(UiRect rectPx, UiRect uvRect) {
		public GlyphQuad {
			Objects.requireNonNull(rectPx, "rectPx");
			Objects.requireNonNull(uvRect, "uvRect");
		}
	}
}
