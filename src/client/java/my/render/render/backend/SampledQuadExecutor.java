package my.render.render.backend;

import my.render.render.backend.plan.ResolvedDraw;
import my.render.render.backend.plan.SampledQuadDrawData;

import java.util.List;
import java.util.Objects;

final class SampledQuadExecutor {
	private final GpuResourceArena resources;
	private final PipelineBinder pipelineBinder;
	private final GpuCommandEncoder commandEncoder;
	private final SampledQuadBatchEncoder batchEncoder;

	SampledQuadExecutor(GpuResourceArena resources, PipelineBinder pipelineBinder, GpuCommandEncoder commandEncoder, SampledQuadBatchEncoder batchEncoder) {
		this.resources = Objects.requireNonNull(resources, "resources");
		this.pipelineBinder = Objects.requireNonNull(pipelineBinder, "pipelineBinder");
		this.commandEncoder = Objects.requireNonNull(commandEncoder, "commandEncoder");
		this.batchEncoder = Objects.requireNonNull(batchEncoder, "batchEncoder");
	}

	void execute(List<ResolvedDraw> draws, ExecutionContext context) {
		Objects.requireNonNull(draws, "draws");
		Objects.requireNonNull(context, "context");

		ResolvedDraw first = this.validateBatch(draws);
		PipelineBinding binding = this.pipelineBinder.bind(first.pipelineSpec());
		QuadBufferUpload upload = this.batchEncoder.encode(draws);
		this.rememberTextures(upload);
		context.stats().recordPipelineUse(first.pipelineKey());
		context.stats().recordTextureUse(first.compiledData(SampledQuadDrawData.class).textureId());
		if (this.commandEncoder.execute(binding, upload, context.target(), context.scissor())) {
			context.stats().recordDrawCall();
		} else {
			context.stats().recordSkippedDraw();
		}
	}

	private ResolvedDraw validateBatch(List<ResolvedDraw> draws) {
		if (draws.isEmpty()) {
			throw new IllegalArgumentException("Sampled quad batch must not be empty");
		}

		ResolvedDraw first = draws.getFirst();
		SampledQuadDrawData firstData = first.compiledData(SampledQuadDrawData.class);
		for (ResolvedDraw draw : draws) {
			if (draw.material().executionFamily() != ExecutionFamily.SAMPLED_QUAD) {
				throw new IllegalStateException("Non-sampled draw entered SampledQuadExecutor");
			}
			SampledQuadDrawData data = draw.compiledData(SampledQuadDrawData.class);
			if (!draw.pipelineKey().equals(first.pipelineKey())) {
				throw new IllegalStateException("Sampled batch mixes pipeline variants");
			}
			if (!data.textureId().equals(firstData.textureId())) {
				throw new IllegalStateException("Sampled batch mixes textures");
			}
		}
		return first;
	}

	private void rememberTextures(QuadBufferUpload upload) {
		for (QuadBufferUpload.TextureBinding binding : upload.sampledTextures()) {
			if (binding.source() instanceof QuadBufferUpload.TextureSource.ResourceTexture resourceTexture) {
				this.resources.rememberTexture(resourceTexture.id());
			}
		}
	}
}
