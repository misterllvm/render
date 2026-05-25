package my.render.render.backend;

import my.render.render.base.UiRect;
import my.render.render.pipeline.PipelineSpec;
import my.render.render.shader.RuntimeShaderProgram;
import org.lwjgl.opengl.GL11C;

import java.util.Objects;

final class RuntimeDrawExecutor implements AutoCloseable {
	private final TextureBinder textureBinder;
	private final UniformUploader uniformUploader = new UniformUploader();
	private final TransientUploadArena uploadArena = new TransientUploadArena();
	private final GlStateCache stateCache = new GlStateCache();

	RuntimeDrawExecutor(GpuResourceArena resources) {
		Objects.requireNonNull(resources, "resources");
		this.textureBinder = new TextureBinder(resources, new ResourceTextureLoader(), this.stateCache);
	}

	void beginFrame() {
		this.stateCache.reset();
		this.uploadArena.beginFrame();
	}

	boolean submit(ExecutionTarget target, PipelineBinding binding, QuadBufferUpload.Kind kind, float[] vertexData, int vertexFloatCount, int floatsPerVertex, int quadCount, java.util.List<UniformBinding> uniforms, java.util.List<TextureSlotBinding> textures, UiRect scissor) {
		Objects.requireNonNull(target, "target");
		Objects.requireNonNull(binding, "binding");
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(vertexData, "vertexData");
		Objects.requireNonNull(uniforms, "uniforms");
		Objects.requireNonNull(textures, "textures");
		if (scissor != null && scissor.isEmpty()) {
			return false;
		}
		target.renderTarget().bindForDraw(this.stateCache);
		this.configureState(binding.spec());
		this.applyScissor(target, scissor);

		RuntimeShaderProgram program = binding.runtimeProgram();
		this.stateCache.useProgram(program.programId());
		this.textureBinder.bind(textures);
		this.uniformUploader.upload(program, uniforms);
		TransientUploadArena.GeometryUpload geometry = this.uploadArena.upload(program, vertexData, vertexFloatCount, floatsPerVertex, quadCount);
		if (binding.spec().topology() == PipelineSpec.Topology.QUADS) {
			GL11C.glDrawElements(GL11C.GL_TRIANGLES, geometry.indexCount(), GL11C.GL_UNSIGNED_INT, 0L);
			return true;
		}
		GL11C.glDrawArrays(this.drawMode(binding.spec().topology()), 0, geometry.vertexCount());
		return true;
	}

	@Override
	public void close() {
		this.uploadArena.close();
	}

	private int drawMode(PipelineSpec.Topology topology) {
		return switch (Objects.requireNonNull(topology, "topology")) {
			case QUADS, TRIANGLES -> GL11C.GL_TRIANGLES;
			case LINES -> GL11C.GL_LINES;
		};
	}

	private void configureState(PipelineSpec spec) {
		this.stateCache.disableCull();
		this.stateCache.blend(spec.blendMode());
		this.stateCache.depth(spec.depthMode());
	}

	private void applyScissor(ExecutionTarget target, UiRect scissor) {
		if (scissor == null) {
			this.stateCache.disableScissor();
			return;
		}

		float scaleX = target.physicalWidth() / (float) Math.max(target.width(), 1);
		float scaleY = target.physicalHeight() / (float) Math.max(target.height(), 1);
		int x = Math.max(0, Math.round(scissor.x() * scaleX));
		int y = Math.max(0, Math.round((target.height() - scissor.bottom()) * scaleY));
		int width = scissor.isEmpty() ? 0 : Math.max(1, Math.round(scissor.width() * scaleX));
		int height = scissor.isEmpty() ? 0 : Math.max(1, Math.round(scissor.height() * scaleY));

		this.stateCache.scissor(x, y, width, height);
	}
}
