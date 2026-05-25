package my.render.render.font;

import my.render.render.base.TextureRegion;
import my.render.render.base.UiRect;
import my.render.render.core.ResourceId;

import java.util.List;
import java.util.Objects;

public record PreparedTextLayout(ResourceId font, float fontSize, float maxWidth, float width, float height, float pxRange, List<Glyph> glyphs, List<Line> lines) {
	public PreparedTextLayout(ResourceId font, float fontSize, float maxWidth, float width, float height, float pxRange, List<Glyph> glyphs, List<Line> lines) {
		Objects.requireNonNull(font, "font");
		Objects.requireNonNull(glyphs, "glyphs");
		Objects.requireNonNull(lines, "lines");
		if (fontSize <= 0.0F) {
			throw new IllegalArgumentException("fontSize must be positive");
		}
		if (maxWidth < 0.0F) {
			throw new IllegalArgumentException("maxWidth must be non-negative");
		}
		if (width < 0.0F) {
			throw new IllegalArgumentException("width must be non-negative");
		}
		if (height < 0.0F) {
			throw new IllegalArgumentException("height must be non-negative");
		}
		if (pxRange <= 0.0F) {
			throw new IllegalArgumentException("pxRange must be positive");
		}
		List<Glyph> copiedGlyphs = List.copyOf(glyphs);
		for (Glyph glyph : copiedGlyphs) {
			Objects.requireNonNull(glyph, "glyph");
		}
		List<Line> copiedLines = List.copyOf(lines);
		for (Line line : copiedLines) {
			Objects.requireNonNull(line, "line");
		}

		this.font = font;
		this.fontSize = fontSize;
		this.maxWidth = maxWidth;
		this.width = width;
		this.height = height;
		this.pxRange = pxRange;
		this.glyphs = copiedGlyphs;
		this.lines = copiedLines;
	}

	public static PreparedTextLayout singleLine(ResourceId font, float fontSize, float width, float height, String text) {
		Objects.requireNonNull(text, "text");
		return new PreparedTextLayout(
			font,
			fontSize,
			0.0F,
			width,
			height,
			4.0F,
			List.of(new Glyph(new UiRect(0.0F, 0.0F, width, height), TextureRegion.full(1, 1), width)),
			List.of(new Line(text, width))
		);
	}

	public record Glyph(UiRect planeBounds, TextureRegion atlasRegion, float advancePx) {
		public Glyph {
			Objects.requireNonNull(planeBounds, "planeBounds");
			Objects.requireNonNull(atlasRegion, "atlasRegion");
			if (advancePx < 0.0F) {
				throw new IllegalArgumentException("advancePx must be non-negative");
			}
		}
	}

	public record Line(String text, float width) {
		public Line {
			Objects.requireNonNull(text, "text");
			if (width < 0.0F) {
				throw new IllegalArgumentException("width must be non-negative");
			}
		}
	}
}
