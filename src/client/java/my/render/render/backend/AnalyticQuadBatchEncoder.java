package my.render.render.backend;

import my.render.render.backend.plan.AnalyticQuadDrawData;
import my.render.render.backend.plan.AnalyticQuadVariant;
import my.render.render.backend.plan.ResolvedDraw;
import my.render.render.base.Gradient4;
import my.render.render.base.RgbaColor;
import my.render.render.base.UiRect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class AnalyticQuadBatchEncoder {
	private static final int FLOATS_PER_VERTEX = 24;
	private static final UiRect UNIT_RECT = new UiRect(0.0F, 0.0F, 1.0F, 1.0F);
	private static final QuadBufferUpload.AnalyticQuadUniforms HARD_EDGE_UNIFORMS = new QuadBufferUpload.AnalyticQuadUniforms(0.0F);
	private static final QuadBufferUpload.AnalyticQuadUniforms SOFT_EDGE_UNIFORMS = new QuadBufferUpload.AnalyticQuadUniforms(1.0F);
	private final FloatVertexBufferBuilder vertexData = new FloatVertexBufferBuilder();
	private final ArrayList<QuadBufferUpload.Quad> quads = new ArrayList<>();

	QuadBufferUpload encode(List<ResolvedDraw> draws) {
		Objects.requireNonNull(draws, "draws");
		if (draws.isEmpty()) {
			throw new IllegalArgumentException("Analytic draw batch must not be empty");
		}

		this.vertexData.reset(draws.size() * 4 * FLOATS_PER_VERTEX);
		this.quads.clear();
		this.quads.ensureCapacity(draws.size());

		for (ResolvedDraw draw : draws) {
			AnalyticQuadDrawData data = draw.compiledData(AnalyticQuadDrawData.class);
			this.appendDraw(data);
		}

		AnalyticQuadVariant variant = draws.getFirst().compiledData(AnalyticQuadDrawData.class).variant();
		return new QuadBufferUpload(
			this.vertexData.values(),
			this.vertexData.size(),
			FLOATS_PER_VERTEX,
			this.quads,
			List.of(),
			this.batchUniforms(variant)
		);
	}

	private void appendDraw(AnalyticQuadDrawData data) {
		QuadBufferUpload.AnalyticQuadPayload payload = new QuadBufferUpload.AnalyticQuadPayload(
			data.shapeRectPx(),
			data.geometryRectPx(),
			data.radiiPx(),
			data.strokeWidthPx(),
			data.strokeAlignCode(),
			data.shadowSoftnessPx()
		);

		UiRect geometry = data.geometryRectPx();
		Gradient4 gradient = data.gradient();
		int vertexOffset = this.vertexData.size() / FLOATS_PER_VERTEX;

		this.appendVertex(geometry.x(), geometry.y(), UNIT_RECT.x(), UNIT_RECT.y(), gradient.topLeft(), payload);
		this.appendVertex(geometry.right(), geometry.y(), UNIT_RECT.right(), UNIT_RECT.y(), gradient.topRight(), payload);
		this.appendVertex(geometry.right(), geometry.bottom(), UNIT_RECT.right(), UNIT_RECT.bottom(), gradient.bottomRight(), payload);
		this.appendVertex(geometry.x(), geometry.bottom(), UNIT_RECT.x(), UNIT_RECT.bottom(), gradient.bottomLeft(), payload);

		this.quads.add(new QuadBufferUpload.Quad(this.kind(data.variant()), vertexOffset, 4, payload));
	}

	private QuadBufferUpload.Kind kind(AnalyticQuadVariant variant) {
		return switch (variant) {
			case FILL -> QuadBufferUpload.Kind.FILL;
			case ROUNDED_FILL -> QuadBufferUpload.Kind.ROUNDED_FILL;
			case BORDER -> QuadBufferUpload.Kind.BORDER;
			case SHADOW -> QuadBufferUpload.Kind.SHADOW;
		};
	}

	private QuadBufferUpload.AnalyticQuadUniforms batchUniforms(AnalyticQuadVariant variant) {
		return switch (variant) {
			case FILL -> HARD_EDGE_UNIFORMS;
			case ROUNDED_FILL, BORDER, SHADOW -> SOFT_EDGE_UNIFORMS;
		};
	}

	private void appendVertex(float x, float y, float u, float v, RgbaColor color, QuadBufferUpload.AnalyticQuadPayload payload) {
		this.vertexData.add(x);
		this.vertexData.add(y);
		this.vertexData.add(u);
		this.vertexData.add(v);
		this.vertexData.add(color.r());
		this.vertexData.add(color.g());
		this.vertexData.add(color.b());
		this.vertexData.add(color.a());
		this.vertexData.add(payload.shapeRect().x());
		this.vertexData.add(payload.shapeRect().y());
		this.vertexData.add(payload.shapeRect().width());
		this.vertexData.add(payload.shapeRect().height());
		this.vertexData.add(payload.geometryRect().x());
		this.vertexData.add(payload.geometryRect().y());
		this.vertexData.add(payload.geometryRect().width());
		this.vertexData.add(payload.geometryRect().height());
		this.vertexData.add(payload.radii().topLeft());
		this.vertexData.add(payload.radii().topRight());
		this.vertexData.add(payload.radii().bottomRight());
		this.vertexData.add(payload.radii().bottomLeft());
		this.vertexData.add(payload.strokeWidthPx());
		this.vertexData.add(payload.strokeAlignCode());
		this.vertexData.add(payload.shadowSoftnessPx());
		this.vertexData.add(0.0F);
	}
}
