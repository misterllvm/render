package my.render.render.backend;

import my.render.render.backend.plan.ResolvedDraw;
import my.render.render.backend.plan.TextQuadDrawData;
import my.render.render.base.Gradient4;
import my.render.render.base.RgbaColor;
import my.render.render.base.UiRect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class TextQuadBatchEncoder {
	private static final int FLOATS_PER_VERTEX = 24;
	private final FloatVertexBufferBuilder vertexData = new FloatVertexBufferBuilder();
	private final ArrayList<QuadBufferUpload.Quad> quads = new ArrayList<>();

	QuadBufferUpload encode(List<ResolvedDraw> draws) {
		Objects.requireNonNull(draws, "draws");
		if (draws.isEmpty()) {
			throw new IllegalArgumentException("Text quad batch must not be empty");
		}

		ResolvedDraw first = draws.getFirst();
		TextQuadDrawData firstData = first.compiledData(TextQuadDrawData.class);
		int glyphCount = this.countGlyphs(draws);
		this.vertexData.reset(glyphCount * 4 * FLOATS_PER_VERTEX);
		this.quads.clear();
		this.quads.ensureCapacity(glyphCount);

		for (ResolvedDraw draw : draws) {
			TextQuadDrawData data = draw.compiledData(TextQuadDrawData.class);
			this.appendDraw(data);
		}

		QuadBufferUpload.TextureBinding textureBinding = new QuadBufferUpload.TextureBinding(
			0,
			new QuadBufferUpload.TextureSource.ResourceTexture(firstData.fontTextureId()),
			first.material().key().samplerMode()
		);

		return new QuadBufferUpload(
			this.vertexData.values(),
			this.vertexData.size(),
			FLOATS_PER_VERTEX,
			this.quads,
			List.of(textureBinding),
			new QuadBufferUpload.TextUniforms(firstData.edgeSoftnessPx(), firstData.sharpness())
		);
	}

	private int countGlyphs(List<ResolvedDraw> draws) {
		int glyphCount = 0;
		for (ResolvedDraw draw : draws) {
			glyphCount += draw.compiledData(TextQuadDrawData.class).glyphs().size();
		}
		return glyphCount;
	}

	private void appendDraw(TextQuadDrawData data) {
		Gradient4 tint = Gradient4.solid(data.tint());

		for (TextQuadDrawData.GlyphQuad glyph : data.glyphs()) {
			UiRect rect = glyph.rectPx();
			UiRect uvRect = glyph.uvRect();
			int vertexOffset = this.vertexData.size() / FLOATS_PER_VERTEX;

			this.appendVertex(rect.x(), rect.y(), uvRect.x(), uvRect.y(), tint.topLeft(), data.pxRange());
			this.appendVertex(rect.right(), rect.y(), uvRect.right(), uvRect.y(), tint.topRight(), data.pxRange());
			this.appendVertex(rect.right(), rect.bottom(), uvRect.right(), uvRect.bottom(), tint.bottomRight(), data.pxRange());
			this.appendVertex(rect.x(), rect.bottom(), uvRect.x(), uvRect.bottom(), tint.bottomLeft(), data.pxRange());

			this.quads.add(new QuadBufferUpload.Quad(
				QuadBufferUpload.Kind.TEXT,
				vertexOffset,
				4,
				new QuadBufferUpload.TextPayload(rect, 0, data.tint(), data.pxRange())
			));
		}
	}

	private void appendVertex(float x, float y, float u, float v, RgbaColor color, float pxRange) {
		this.vertexData.add(x);
		this.vertexData.add(y);
		this.vertexData.add(u);
		this.vertexData.add(v);
		this.vertexData.add(color.r());
		this.vertexData.add(color.g());
		this.vertexData.add(color.b());
		this.vertexData.add(color.a());
		this.vertexData.add(0.0F);
		this.vertexData.add(0.0F);
		this.vertexData.add(0.0F);
		this.vertexData.add(0.0F);
		this.vertexData.add(0.0F);
		this.vertexData.add(0.0F);
		this.vertexData.add(0.0F);
		this.vertexData.add(0.0F);
		this.vertexData.add(0.0F);
		this.vertexData.add(0.0F);
		this.vertexData.add(0.0F);
		this.vertexData.add(0.0F);
		this.vertexData.add(pxRange);
		this.vertexData.add(0.0F);
		this.vertexData.add(0.0F);
		this.vertexData.add(0.0F);
	}
}
