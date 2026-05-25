package my.render.render.backend;

import my.render.render.base.UiRect;

import java.util.Arrays;
import java.util.Objects;

final class GpuCommandEncoder implements AutoCloseable {
	private final UniformUploadResolver uniformResolver = new UniformUploadResolver();
	private final RuntimeDrawExecutor drawExecutor;

	GpuCommandEncoder(GpuResourceArena resources) {
		this.drawExecutor = new RuntimeDrawExecutor(Objects.requireNonNull(resources, "resources"));
	}

	void beginFrame() {
		this.drawExecutor.beginFrame();
	}

	boolean execute(PipelineBinding binding, QuadBufferUpload upload, ExecutionTarget target, UiRect scissor) {
		Objects.requireNonNull(binding, "binding");
		Objects.requireNonNull(upload, "upload");
		Objects.requireNonNull(target, "target");

		if (scissor != null && scissor.isEmpty()) {
			return false;
		}
		if (upload.vertexCount() == 0 || upload.quads().isEmpty()) {
			return false;
		}

		QuadBufferUpload.Quad primaryQuad = upload.quads().getFirst();
		if (upload.sampledTextures().size() > 1) {
			throw new IllegalStateException("A single batch draw cannot bind multiple sampled textures");
		}

		var textures = this.uniformResolver.resolveTextures(upload, primaryQuad.payload());
		var uniforms = this.uniformResolver.resolveUniforms(binding, upload, primaryQuad.payload(), target, textures);
		if (target.captureRecordedDraws()) {
			target.record(new ExecutionTarget.RecordedDraw(
				binding,
				primaryQuad.kind(),
				Arrays.copyOf(upload.vertexData(), upload.vertexFloatCount()),
				upload.floatsPerVertex(),
				upload.quads().size(),
				primaryQuad.payload(),
				upload.uniforms(),
				uniforms,
				textures,
				scissor
			));
		}
		return this.drawExecutor.submit(
			target,
			binding,
			primaryQuad.kind(),
			upload.vertexData(),
			upload.vertexFloatCount(),
			upload.floatsPerVertex(),
			upload.quads().size(),
			uniforms,
			textures,
			scissor
		);
	}

	@Override
	public void close() {
		this.drawExecutor.close();
	}
}
