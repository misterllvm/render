package my.render.render.pipeline;

import my.render.render.backend.material.MaterialFamily;
import my.render.render.backend.material.MaterialFlags;
import my.render.render.backend.material.SamplerMode;
import my.render.render.core.ResourceId;

import java.util.Objects;

public record PipelineSpec(
	PipelineKey key,
	ResourceId vertexShader,
	ResourceId fragmentShader,
	Topology topology,
	BlendMode blendMode,
	DepthMode depthMode,
	TargetKind targetKind,
	MaterialFamily materialFamily,
	int variantId,
	SamplerMode samplerMode,
	int defaultMaterialFlags
) {
	public PipelineSpec {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(vertexShader, "vertexShader");
		Objects.requireNonNull(fragmentShader, "fragmentShader");
		Objects.requireNonNull(topology, "topology");
		Objects.requireNonNull(blendMode, "blendMode");
		Objects.requireNonNull(depthMode, "depthMode");
		Objects.requireNonNull(targetKind, "targetKind");
		Objects.requireNonNull(materialFamily, "materialFamily");
		Objects.requireNonNull(samplerMode, "samplerMode");
		if (variantId < 0) {
			throw new IllegalArgumentException("variantId must be non-negative");
		}
	}

	public PipelineSpec(PipelineKey key, ResourceId vertexShader, ResourceId fragmentShader, Topology topology, BlendMode blendMode, DepthMode depthMode, TargetKind targetKind) {
		this(
			key,
			vertexShader,
			fragmentShader,
			topology,
			blendMode,
			depthMode,
			targetKind,
			MaterialFamily.CUSTOM,
			0,
			SamplerMode.LINEAR_CLAMP,
			MaterialFlags.NONE
		);
	}

	public enum Topology {
		QUADS,
		TRIANGLES,
		LINES
	}

	public enum BlendMode {
		OPAQUE,
		ALPHA,
		ADDITIVE
	}

	public enum DepthMode {
		NONE,
		TEST,
		TEST_WRITE
	}

	public enum TargetKind {
		MAIN_COLOR,
		OFFSCREEN_COLOR
	}
}
