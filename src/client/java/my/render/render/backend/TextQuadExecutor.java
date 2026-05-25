package my.render.render.backend;

import my.render.render.backend.plan.ResolvedDraw;
import my.render.render.backend.plan.TextQuadDrawData;

import java.util.List;
import java.util.Objects;

final class TextQuadExecutor {
	private final GpuResourceArena resources;
	private final PipelineBinder pipelineBinder;
	private final GpuCommandEncoder commandEncoder;
	private final TextQuadBatchEncoder batchEncoder;

	TextQuadExecutor(GpuResourceArena resources, PipelineBinder pipelineBinder, GpuCommandEncoder commandEncoder, TextQuadBatchEncoder batchEncoder) {
		this.resources = Objects.requireNonNull(resources, "resources");
		this.pipelineBinder = Objects.requireNonNull(pipelineBinder, "pipelineBinder");
		this.commandEncoder = Objects.requireNonNull(commandEncoder, "commandEncoder");
		this.batchEncoder = Objects.requireNonNull(batchEncoder, "batchEncoder");
	}

	void execute(List<ResolvedDraw> draws, ExecutionContext context) {
		Objects.requireNonNull(draws, "draws");
		Objects.requireNonNull(context, "context");

		ResolvedDraw first = this.validateBatch(draws);
		QuadBufferUpload upload = this.batchEncoder.encode(draws);
		this.rememberTextures(upload);
		PipelineBinding binding = this.pipelineBinder.bind(first.pipelineSpec());
		context.stats().recordPipelineUse(first.pipelineKey());
		context.stats().recordTextureUse(first.compiledData(TextQuadDrawData.class).fontTextureId());
		if (this.commandEncoder.execute(binding, upload, context.target(), context.scissor())) {
			context.stats().recordDrawCall();
		} else {
			context.stats().recordSkippedDraw();
		}
	}

	private ResolvedDraw validateBatch(List<ResolvedDraw> draws) {
		if (draws.isEmpty()) {
			throw new IllegalArgumentException("Text quad batch must not be empty");
		}

		ResolvedDraw first = draws.getFirst();
		TextQuadDrawData firstData = first.compiledData(TextQuadDrawData.class);
		for (ResolvedDraw draw : draws) {
			if (draw.material().executionFamily() != ExecutionFamily.TEXT_QUAD) {
				throw new IllegalStateException("Non-text draw entered TextQuadExecutor");
			}
			TextQuadDrawData data = draw.compiledData(TextQuadDrawData.class);
			if (!draw.pipelineKey().equals(first.pipelineKey())) {
				throw new IllegalStateException("Text batch mixes pipeline variants");
			}
			if (!data.fontTextureId().equals(firstData.fontTextureId())) {
				throw new IllegalStateException("Text batch mixes font atlases");
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
