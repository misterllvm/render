package my.render.render.backend.material;

import my.render.render.pipeline.PipelineKey;
import my.render.render.pipeline.PipelineSpec;

import java.util.Objects;

public record MaterialKey(
	MaterialFamily family,
	PipelineKey pipelineKey,
	int variantId,
	PipelineSpec.BlendMode blendMode,
	PipelineSpec.DepthMode depthMode,
	SamplerMode samplerMode,
	int flags
) {
	public MaterialKey {
		Objects.requireNonNull(family, "family");
		Objects.requireNonNull(pipelineKey, "pipelineKey");
		Objects.requireNonNull(blendMode, "blendMode");
		Objects.requireNonNull(depthMode, "depthMode");
		Objects.requireNonNull(samplerMode, "samplerMode");
		if (variantId < 0) {
			throw new IllegalArgumentException("variantId must be non-negative");
		}
	}
}
