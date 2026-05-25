package my.render.render.backend;

import my.render.render.base.UiRect;
import my.render.render.frame.SurfaceMetrics;
import my.render.render.pipeline.PipelineSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class ExecutionTarget {
	private final int targetId;
	private final PipelineSpec.TargetKind targetKind;
	private final int width;
	private final int height;
	private final RuntimeRenderTarget renderTarget;
	private final boolean captureRecordedDraws;
	private final List<RecordedDraw> draws = new ArrayList<>();

	ExecutionTarget(int targetId, PipelineSpec.TargetKind targetKind, int width, int height, RuntimeRenderTarget renderTarget, boolean captureRecordedDraws) {
		this.targetId = targetId;
		this.targetKind = Objects.requireNonNull(targetKind, "targetKind");
		this.width = width;
		this.height = height;
		this.renderTarget = Objects.requireNonNull(renderTarget, "renderTarget");
		this.captureRecordedDraws = captureRecordedDraws;
	}

	ExecutionTarget(int targetId, PipelineSpec.TargetKind targetKind, int width, int height, RuntimeRenderTarget renderTarget) {
		this(targetId, targetKind, width, height, renderTarget, false);
	}

	static ExecutionTarget main(SurfaceMetrics surface) {
		return main(surface, false);
	}

	static ExecutionTarget main(SurfaceMetrics surface, boolean captureRecordedDraws) {
		Objects.requireNonNull(surface, "surface");
		return new ExecutionTarget(
			0,
			PipelineSpec.TargetKind.MAIN_COLOR,
			surface.guiWidth(),
			surface.guiHeight(),
			RuntimeRenderTarget.main(
				surface.guiWidth(),
				surface.guiHeight(),
				surface.framebufferWidth(),
				surface.framebufferHeight()
			),
			captureRecordedDraws
		);
	}

	int targetId() {
		return this.targetId;
	}

	PipelineSpec.TargetKind targetKind() {
		return this.targetKind;
	}

	int width() {
		return this.width;
	}

	int height() {
		return this.height;
	}

	int physicalWidth() {
		return this.renderTarget.physicalWidth();
	}

	int physicalHeight() {
		return this.renderTarget.physicalHeight();
	}

	RuntimeRenderTarget renderTarget() {
		return this.renderTarget;
	}

	boolean captureRecordedDraws() {
		return this.captureRecordedDraws;
	}

	void clear() {
		this.draws.clear();
		this.renderTarget.clear();
	}

	void record(RecordedDraw draw) {
		if (!this.captureRecordedDraws) {
			return;
		}
		this.draws.add(Objects.requireNonNull(draw, "draw"));
	}

	List<RecordedDraw> draws() {
		return List.copyOf(this.draws);
	}

	record RecordedDraw(
		PipelineBinding binding,
		QuadBufferUpload.Kind kind,
		float[] vertexData,
		int floatsPerVertex,
		int quadCount,
		QuadBufferUpload.Payload payload,
		QuadBufferUpload.Uniforms batchUniforms,
		List<UniformBinding> uniforms,
		List<TextureSlotBinding> textures,
		UiRect scissor
	) {
		RecordedDraw(
			PipelineBinding binding,
			QuadBufferUpload.Kind kind,
			float[] vertexData,
			int floatsPerVertex,
			int quadCount,
			QuadBufferUpload.Payload payload,
			QuadBufferUpload.Uniforms batchUniforms,
			List<UniformBinding> uniforms,
			List<TextureSlotBinding> textures,
			UiRect scissor
		) {
			Objects.requireNonNull(binding, "binding");
			Objects.requireNonNull(kind, "kind");
			Objects.requireNonNull(vertexData, "vertexData");
			Objects.requireNonNull(payload, "payload");
			Objects.requireNonNull(batchUniforms, "batchUniforms");
			Objects.requireNonNull(uniforms, "uniforms");
			Objects.requireNonNull(textures, "textures");

			this.binding = binding;
			this.kind = kind;
			this.vertexData = vertexData.clone();
			this.floatsPerVertex = floatsPerVertex;
			this.quadCount = quadCount;
			this.payload = payload;
			this.batchUniforms = batchUniforms;
			this.uniforms = List.copyOf(uniforms);
			this.textures = List.copyOf(textures);
			this.scissor = scissor;
		}
	}
}
