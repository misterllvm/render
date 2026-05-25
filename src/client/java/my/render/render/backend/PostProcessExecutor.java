package my.render.render.backend;

import java.util.Objects;

final class PostProcessExecutor {
	private final GpuResourceArena resources;
	private final PipelineBinder pipelineBinder;
	private final GpuCommandEncoder commandEncoder;
	private final PostProcessBatchEncoder batchEncoder;

	PostProcessExecutor(GpuResourceArena resources, PipelineBinder pipelineBinder, GpuCommandEncoder commandEncoder, PostProcessBatchEncoder batchEncoder) {
		this.resources = Objects.requireNonNull(resources, "resources");
		this.pipelineBinder = Objects.requireNonNull(pipelineBinder, "pipelineBinder");
		this.commandEncoder = Objects.requireNonNull(commandEncoder, "commandEncoder");
		this.batchEncoder = Objects.requireNonNull(batchEncoder, "batchEncoder");
	}

	void execute(PostProcessDrawData draw, ExecutionContext context) {
		Objects.requireNonNull(draw, "draw");
		Objects.requireNonNull(context, "context");
		if (draw.executionFamily() != ExecutionFamily.POST_PROCESS) {
			throw new IllegalStateException("Non-post-process draw entered PostProcessExecutor");
		}

		PipelineBinding binding = this.pipelineBinder.bind(draw.pipelineKey());
		QuadBufferUpload upload = this.batchEncoder.encode(draw);
		this.rememberTextures(upload);
		context.stats().recordPipelineUse(draw.pipelineKey());
		context.stats().recordTextureUse(draw.sourceTexture());
		if (this.commandEncoder.execute(binding, upload, context.target(), context.scissor())) {
			context.stats().recordDrawCall();
		} else {
			context.stats().recordSkippedDraw();
		}
	}

	private void rememberTextures(QuadBufferUpload upload) {
		for (QuadBufferUpload.TextureBinding binding : upload.sampledTextures()) {
			if (binding.source() instanceof QuadBufferUpload.TextureSource.ResourceTexture resourceTexture) {
				this.resources.rememberTexture(resourceTexture.id());
			}
		}
	}
}
