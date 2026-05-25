package my.render.render.backend;

import my.render.render.backend.plan.BackdropBlurDrawData;
import my.render.render.base.CornerRadii;
import my.render.render.base.Gradient4;
import my.render.render.base.RgbaColor;
import my.render.render.base.UiRect;

import java.util.ArrayList;
import java.util.Objects;

final class BackdropBlurBatchEncoder {
	private static final int FLOATS_PER_VERTEX = 24;
	private static final UiRect UNIT_RECT = new UiRect(0.0F, 0.0F, 1.0F, 1.0F);
	private final float[] vertexDataScratch = new float[4 * FLOATS_PER_VERTEX];
	private final ArrayList<QuadBufferUpload.Quad> quads = new ArrayList<>(1);
	private final ArrayList<QuadBufferUpload.TextureBinding> textures = new ArrayList<>(1);

	QuadBufferUpload encode(BackdropBlurDrawData draw, QuadBufferUpload.TextureSource textureSource) {
		return this.encode(draw, textureSource, UNIT_RECT);
	}

	QuadBufferUpload encode(BackdropBlurDrawData draw, QuadBufferUpload.TextureSource textureSource, UiRect sampleUvRect) {
		Objects.requireNonNull(draw, "draw");
		Objects.requireNonNull(textureSource, "textureSource");
		Objects.requireNonNull(sampleUvRect, "sampleUvRect");

		UiRect rect = draw.rectPx();
		Gradient4 tint = Gradient4.solid(draw.tint());
		QuadBufferUpload.BackdropBlurPayload payload = new QuadBufferUpload.BackdropBlurPayload(rect, draw.radiiPx(), 0, draw.blurRadiusPx());
		float[] vertexData = this.vertexDataScratch;
		float leftU = clamp01(sampleUvRect.x());
		float rightU = clamp01(sampleUvRect.right());
		float topV = TextureUvOrientation.mapV(textureSource, sampleUvRect.y());
		float bottomV = TextureUvOrientation.mapV(textureSource, sampleUvRect.bottom());
		topV = clamp01(topV);
		bottomV = clamp01(bottomV);

		this.appendVertex(vertexData, 0, rect.x(), rect.y(), UNIT_RECT.x(), UNIT_RECT.y(), tint.topLeft(), draw, leftU, topV, rightU, bottomV);
		this.appendVertex(vertexData, 1, rect.right(), rect.y(), UNIT_RECT.right(), UNIT_RECT.y(), tint.topRight(), draw, leftU, topV, rightU, bottomV);
		this.appendVertex(vertexData, 2, rect.right(), rect.bottom(), UNIT_RECT.right(), UNIT_RECT.bottom(), tint.bottomRight(), draw, leftU, topV, rightU, bottomV);
		this.appendVertex(vertexData, 3, rect.x(), rect.bottom(), UNIT_RECT.x(), UNIT_RECT.bottom(), tint.bottomLeft(), draw, leftU, topV, rightU, bottomV);

		this.quads.clear();
		this.quads.add(new QuadBufferUpload.Quad(QuadBufferUpload.Kind.BACKDROP_BLUR, 0, 4, payload));
		this.textures.clear();
		this.textures.add(new QuadBufferUpload.TextureBinding(0, textureSource));
		return new QuadBufferUpload(vertexData, FLOATS_PER_VERTEX, this.quads, this.textures, QuadBufferUpload.TextureUniforms.DEFAULT);
	}

	private static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	private void appendVertex(float[] vertexData, int vertexIndex, float x, float y, float u, float v, RgbaColor color, BackdropBlurDrawData draw, float leftU, float topV, float rightU, float bottomV) {
		int base = vertexIndex * FLOATS_PER_VERTEX;
		UiRect rect = draw.rectPx();
		CornerRadii radii = draw.radiiPx();
		vertexData[base] = x;
		vertexData[base + 1] = y;
		vertexData[base + 2] = u;
		vertexData[base + 3] = v;
		vertexData[base + 4] = color.r();
		vertexData[base + 5] = color.g();
		vertexData[base + 6] = color.b();
		vertexData[base + 7] = color.a();
		vertexData[base + 8] = rect.x();
		vertexData[base + 9] = rect.y();
		vertexData[base + 10] = rect.width();
		vertexData[base + 11] = rect.height();
		vertexData[base + 12] = radii.topLeft();
		vertexData[base + 13] = radii.topRight();
		vertexData[base + 14] = radii.bottomRight();
		vertexData[base + 15] = radii.bottomLeft();
		vertexData[base + 16] = leftU;
		vertexData[base + 17] = topV;
		vertexData[base + 18] = rightU;
		vertexData[base + 19] = bottomV;
		vertexData[base + 20] = draw.blurRadiusPx();
		vertexData[base + 21] = 0.0F;
		vertexData[base + 22] = 0.0F;
		vertexData[base + 23] = 0.0F;
	}
}
