package my.render.render.font;

import my.render.render.base.TextureRegion;
import my.render.render.base.UiRect;
import my.render.render.core.ResourceId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MsdfFont {
	private static final int FALLBACK_CODE_POINT = '?';
	private static final int SPACE_CODE_POINT = ' ';
	private final ResourceId atlas;
	private final Metrics metrics;
	private final Map<Integer, GlyphMetrics> glyphs;
	private final Map<Long, Float> kernings;
	private final GlyphMetrics fallbackGlyph;
	private final GlyphMetrics spaceGlyph;

	private MsdfFont(ResourceId atlas, Metrics metrics, Map<Integer, GlyphMetrics> glyphs, Map<Long, Float> kernings) {
		this.atlas = Objects.requireNonNull(atlas, "atlas");
		this.metrics = Objects.requireNonNull(metrics, "metrics");
		this.glyphs = Map.copyOf(glyphs);
		this.kernings = Map.copyOf(kernings);
		this.fallbackGlyph = this.glyphs.get(FALLBACK_CODE_POINT);
		this.spaceGlyph = this.glyphs.get(SPACE_CODE_POINT);
	}

	public static MsdfFont load(ResourceId atlas, ResourceId metricsResource) {
		Objects.requireNonNull(atlas, "atlas");
		JsonObject root = readMetrics(Objects.requireNonNull(metricsResource, "metricsResource"));
		JsonObject info = root.getAsJsonObject("info");
		JsonObject common = root.getAsJsonObject("common");
		JsonObject distanceField = root.getAsJsonObject("distanceField");
		JsonArray chars = root.getAsJsonArray("chars");
		JsonArray pages = root.getAsJsonArray("pages");
		if (info == null || common == null || distanceField == null || chars == null || pages == null) {
			throw new IllegalStateException("Incomplete MSDF font metrics for " + metricsResource.asString());
		}
		if (common.get("pages").getAsInt() != 1 || pages.size() != 1) {
			throw new IllegalStateException("Only single-page MSDF atlases are supported: " + metricsResource.asString());
		}
		if (!"msdf".equalsIgnoreCase(distanceField.get("fieldType").getAsString())) {
			throw new IllegalStateException("Expected MSDF distance field in " + metricsResource.asString());
		}

		int atlasWidth = common.get("scaleW").getAsInt();
		int atlasHeight = common.get("scaleH").getAsInt();
		Map<Integer, GlyphMetrics> glyphs = new LinkedHashMap<>();
		for (JsonElement element : chars) {
			JsonObject glyph = element.getAsJsonObject();
			if (glyph.get("page").getAsInt() != 0) {
				continue;
			}
			int codePoint = glyph.get("id").getAsInt();
			int x = glyph.get("x").getAsInt();
			int y = glyph.get("y").getAsInt();
			int width = glyph.get("width").getAsInt();
			int height = glyph.get("height").getAsInt();
			TextureRegion region = width > 0 && height > 0
				? new TextureRegion(x, y, width, height, atlasWidth, atlasHeight)
				: new TextureRegion(0, 0, 1, 1, atlasWidth, atlasHeight);
			glyphs.put(
				codePoint,
				new GlyphMetrics(
					codePoint,
					region,
					width,
					height,
					glyph.get("xoffset").getAsFloat(),
					glyph.get("yoffset").getAsFloat(),
					glyph.get("xadvance").getAsFloat()
				)
			);
		}

		Map<Long, Float> kernings = new LinkedHashMap<>();
		JsonArray kerningArray = root.getAsJsonArray("kernings");
		if (kerningArray != null) {
			for (JsonElement element : kerningArray) {
				JsonObject kerning = element.getAsJsonObject();
				kernings.put(
					kerningKey(kerning.get("first").getAsInt(), kerning.get("second").getAsInt()),
					kerning.get("amount").getAsFloat()
				);
			}
		}

		return new MsdfFont(
			atlas,
			new Metrics(info.get("size").getAsFloat(), common.get("lineHeight").getAsFloat(), distanceField.get("distanceRange").getAsFloat()),
			glyphs,
			kernings
		);
	}

	public PreparedTextLayout layout(String text, float fontSize) {
		Objects.requireNonNull(text, "text");
		if (fontSize <= 0.0F) {
			throw new IllegalArgumentException("fontSize must be positive");
		}

		float scale = fontSize / this.metrics.sourceFontSize();
		float lineHeight = this.metrics.lineHeight() * scale;
		List<PreparedTextLayout.Glyph> glyphs = new ArrayList<>();
		List<PreparedTextLayout.Line> lines = new ArrayList<>();
		StringBuilder currentLine = new StringBuilder();
		float cursorX = 0.0F;
		float cursorY = 0.0F;
		float maxWidth = 0.0F;
		int lineCount = 1;
		int previousCodePoint = -1;

		for (int index = 0; index < text.length();) {
			int codePoint = text.codePointAt(index);
			index += Character.charCount(codePoint);

			if (codePoint == '\r') {
				continue;
			}
			if (codePoint == '\n') {
				lines.add(new PreparedTextLayout.Line(currentLine.toString(), cursorX));
				maxWidth = Math.max(maxWidth, cursorX);
				currentLine.setLength(0);
				cursorX = 0.0F;
				cursorY += lineHeight;
				lineCount++;
				previousCodePoint = -1;
				continue;
			}

			GlyphMetrics glyph = this.resolveGlyph(codePoint);
			if (previousCodePoint >= 0) {
				cursorX += this.kerning(previousCodePoint, glyph.codePoint()) * scale;
			}

			if (glyph.isVisible()) {
				UiRect planeBounds = new UiRect(
					cursorX + glyph.xOffset() * scale,
					cursorY + glyph.yOffset() * scale,
					glyph.width() * scale,
					glyph.height() * scale
				);
				glyphs.add(new PreparedTextLayout.Glyph(planeBounds, glyph.region(), glyph.xAdvance() * scale));
			}

			cursorX += glyph.xAdvance() * scale;
			currentLine.appendCodePoint(codePoint);
			previousCodePoint = glyph.codePoint();
		}

		lines.add(new PreparedTextLayout.Line(currentLine.toString(), cursorX));
		maxWidth = Math.max(maxWidth, cursorX);
		return new PreparedTextLayout(this.atlas, fontSize, maxWidth, maxWidth, lineCount * lineHeight, this.metrics.pxRange(), glyphs, lines);
	}

	private GlyphMetrics resolveGlyph(int codePoint) {
		GlyphMetrics glyph = this.glyphs.get(codePoint);
		if (glyph != null) {
			return glyph;
		}
		if (Character.isWhitespace(codePoint) && this.spaceGlyph != null) {
			return this.spaceGlyph;
		}
		if (this.fallbackGlyph != null) {
			return this.fallbackGlyph;
		}
		throw new IllegalArgumentException("Missing MSDF glyph U+" + Integer.toHexString(codePoint).toUpperCase());
	}

	private float kerning(int first, int second) {
		return this.kernings.getOrDefault(kerningKey(first, second), 0.0F);
	}

	private static long kerningKey(int first, int second) {
		return ((long) first << 32) | (second & 0xFFFFFFFFL);
	}

	private static JsonObject readMetrics(ResourceId resource) {
		String classpath = "assets/" + resource.namespace() + "/" + resource.path();
		try (InputStream stream = MsdfFont.class.getClassLoader().getResourceAsStream(classpath)) {
			if (stream == null) {
				throw new IllegalStateException("Missing MSDF font metrics: " + resource.asString());
			}
			try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				return JsonParser.parseReader(reader).getAsJsonObject();
			}
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to read MSDF font metrics: " + resource.asString(), exception);
		}
	}

	private record Metrics(float sourceFontSize, float lineHeight, float pxRange) {
	}

	private record GlyphMetrics(int codePoint, TextureRegion region, float width, float height, float xOffset, float yOffset, float xAdvance) {
		private boolean isVisible() {
			return this.width > 0.0F && this.height > 0.0F;
		}
	}
}
