package my.render.render.backend;

import my.render.render.backend.material.SamplerMode;
import my.render.render.base.RgbaColor;
import my.render.render.base.UiRect;
import my.render.render.pipeline.PipelineKey;

import java.util.Objects;

record PostProcessDrawData(
	PipelineKey pipelineKey,
	QuadBufferUpload.TextureSource sourceTexture,
	SamplerMode samplerMode,
	UiRect destinationRect,
	UiRect uvRect,
	RgbaColor tint,
	QuadBufferUpload.Uniforms uniforms
) {
	PostProcessDrawData {
		Objects.requireNonNull(pipelineKey, "pipelineKey");
		Objects.requireNonNull(sourceTexture, "sourceTexture");
		Objects.requireNonNull(samplerMode, "samplerMode");
		Objects.requireNonNull(destinationRect, "destinationRect");
		Objects.requireNonNull(uvRect, "uvRect");
		Objects.requireNonNull(tint, "tint");
		Objects.requireNonNull(uniforms, "uniforms");
	}

	ExecutionFamily executionFamily() {
		return ExecutionFamily.POST_PROCESS;
	}
}
