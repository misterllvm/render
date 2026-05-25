package my.render.render.backend;

import my.render.render.base.RgbaColor;
import my.render.render.base.UiRect;

import java.util.ArrayList;
import java.util.Objects;

final class PostProcessBatchEncoder {
	private static final int FLOATS_PER_VERTEX = 24;
	private final float[] vertexDataScratch = new float[4 * FLOATS_PER_VERTEX];
	private final ArrayList<QuadBufferUpload.Quad> quads = new ArrayList<>(1);
	private final ArrayList<QuadBufferUpload.TextureBinding> textures = new ArrayList<>(1);

	QuadBufferUpload encode(PostProcessDrawData draw) {
		Objects.requireNonNull(draw, "draw");

		UiRect rect = draw.destinationRect();
		UiRect uvRect = draw.uvRect();
		RgbaColor tint = draw.tint();
		float topV = clamp01(TextureUvOrientation.mapV(draw.sourceTexture(), uvRect.y()));
		float bottomV = clamp01(TextureUvOrientation.mapV(draw.sourceTexture(), uvRect.bottom()));
		float leftU = clamp01(uvRect.x());
		float rightU = clamp01(uvRect.right());

		float[] vertexData = this.vertexDataScratch;
		this.writeVertex(vertexData, 0, rect.x(), rect.y(), leftU, topV, tint, leftU, topV, rightU, bottomV);
		this.writeVertex(vertexData, 1, rect.right(), rect.y(), rightU, topV, tint, leftU, topV, rightU, bottomV);
		this.writeVertex(vertexData, 2, rect.right(), rect.bottom(), rightU, bottomV, tint, leftU, topV, rightU, bottomV);
		this.writeVertex(vertexData, 3, rect.x(), rect.bottom(), leftU, bottomV, tint, leftU, topV, rightU, bottomV);

		QuadBufferUpload.FullscreenPayload payload = new QuadBufferUpload.FullscreenPayload(rect, 0, tint);
		QuadBufferUpload.TextureBinding texture = new QuadBufferUpload.TextureBinding(0, draw.sourceTexture(), draw.samplerMode());
		QuadBufferUpload.Quad quad = new QuadBufferUpload.Quad(QuadBufferUpload.Kind.FULLSCREEN, 0, 4, payload);
		this.quads.clear();
		this.quads.add(quad);
		this.textures.clear();
		this.textures.add(texture);
		return new QuadBufferUpload(vertexData, FLOATS_PER_VERTEX, this.quads, this.textures, draw.uniforms());
	}

	private void writeVertex(float[] vertexData, int vertexIndex, float x, float y, float u, float v, RgbaColor tint, float leftU, float topV, float rightU, float bottomV) {
		int base = vertexIndex * FLOATS_PER_VERTEX;
		vertexData[base] = x;
		vertexData[base + 1] = y;
		vertexData[base + 2] = u;
		vertexData[base + 3] = v;
		vertexData[base + 4] = tint.r();
		vertexData[base + 5] = tint.g();
		vertexData[base + 6] = tint.b();
		vertexData[base + 7] = tint.a();
		vertexData[base + 8] = 0.0F;
		vertexData[base + 9] = 0.0F;
		vertexData[base + 10] = 0.0F;
		vertexData[base + 11] = 0.0F;
		vertexData[base + 12] = 0.0F;
		vertexData[base + 13] = 0.0F;
		vertexData[base + 14] = 0.0F;
		vertexData[base + 15] = 0.0F;
		vertexData[base + 16] = leftU;
		vertexData[base + 17] = topV;
		vertexData[base + 18] = rightU;
		vertexData[base + 19] = bottomV;
		vertexData[base + 20] = 0.0F;
		vertexData[base + 21] = 0.0F;
		vertexData[base + 22] = 0.0F;
		vertexData[base + 23] = 0.0F;
	}

	private static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}
}
