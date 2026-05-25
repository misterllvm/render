package my.render.render.backend;

import my.render.render.backend.plan.ResolvedDraw;
import my.render.render.backend.plan.SampledQuadDrawData;
import my.render.render.base.Gradient4;
import my.render.render.base.RgbaColor;
import my.render.render.base.UiRect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class SampledQuadBatchEncoder {
	private static final int FLOATS_PER_VERTEX = 24;
	private static final UiRect UNIT_RECT = new UiRect(0.0F, 0.0F, 1.0F, 1.0F);
	private final FloatVertexBufferBuilder vertexData = new FloatVertexBufferBuilder();
	private final ArrayList<QuadBufferUpload.Quad> quads = new ArrayList<>();

	QuadBufferUpload encode(List<ResolvedDraw> draws) {
		Objects.requireNonNull(draws, "draws");
		if (draws.isEmpty()) {
			throw new IllegalArgumentException("Sampled quad batch must not be empty");
		}

		ResolvedDraw first = draws.getFirst();
		SampledQuadDrawData firstData = first.compiledData(SampledQuadDrawData.class);
		this.vertexData.reset(draws.size() * 4 * FLOATS_PER_VERTEX);
		this.quads.clear();
		this.quads.ensureCapacity(draws.size());

		for (ResolvedDraw draw : draws) {
			SampledQuadDrawData data = draw.compiledData(SampledQuadDrawData.class);
			this.appendDraw(data);
		}

		QuadBufferUpload.TextureBinding textureBinding = new QuadBufferUpload.TextureBinding(
			0,
			new QuadBufferUpload.TextureSource.ResourceTexture(firstData.textureId()),
			first.material().key().samplerMode()
		);

		return new QuadBufferUpload(
			this.vertexData.values(),
			this.vertexData.size(),
			FLOATS_PER_VERTEX,
			this.quads,
			List.of(textureBinding),
			new QuadBufferUpload.TextureUniforms(firstData.edgeSoftnessPx())
		);
	}

	private void appendDraw(SampledQuadDrawData data) {
		QuadBufferUpload.TexturePayload payload = new QuadBufferUpload.TexturePayload(data.rectPx(), data.radiiPx(), 0);
		UiRect rect = data.rectPx();
		UiRect uvRect = data.uvRect();
		Gradient4 tint = data.tint();
		int vertexOffset = this.vertexData.size() / FLOATS_PER_VERTEX;

		this.appendVertex(rect.x(), rect.y(), uvRect.x(), uvRect.y(), tint.topLeft(), data);
		this.appendVertex(rect.right(), rect.y(), uvRect.right(), uvRect.y(), tint.topRight(), data);
		this.appendVertex(rect.right(), rect.bottom(), uvRect.right(), uvRect.bottom(), tint.bottomRight(), data);
		this.appendVertex(rect.x(), rect.bottom(), uvRect.x(), uvRect.bottom(), tint.bottomLeft(), data);

		this.quads.add(new QuadBufferUpload.Quad(QuadBufferUpload.Kind.TEXTURE, vertexOffset, 4, payload));
	}

	private void appendVertex(float x, float y, float u, float v, RgbaColor color, SampledQuadDrawData data) {
		this.vertexData.add(x);
		this.vertexData.add(y);
		this.vertexData.add(u);
		this.vertexData.add(v);
		this.vertexData.add(color.r());
		this.vertexData.add(color.g());
		this.vertexData.add(color.b());
		this.vertexData.add(color.a());
		this.vertexData.add(data.rectPx().x());
		this.vertexData.add(data.rectPx().y());
		this.vertexData.add(data.rectPx().width());
		this.vertexData.add(data.rectPx().height());
		this.vertexData.add(data.radiiPx().topLeft());
		this.vertexData.add(data.radiiPx().topRight());
		this.vertexData.add(data.radiiPx().bottomRight());
		this.vertexData.add(data.radiiPx().bottomLeft());
		this.vertexData.add(data.uvRect().x());
		this.vertexData.add(data.uvRect().y());
		this.vertexData.add(data.uvRect().right());
		this.vertexData.add(data.uvRect().bottom());
		this.vertexData.add(0.0F);
		this.vertexData.add(0.0F);
		this.vertexData.add(0.0F);
		this.vertexData.add(0.0F);
	}
}
