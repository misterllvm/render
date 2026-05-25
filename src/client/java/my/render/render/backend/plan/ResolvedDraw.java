package my.render.render.backend.plan;

import my.render.render.backend.DrawCommand;
import my.render.render.backend.material.MaterialDescriptor;
import my.render.render.base.UiRect;
import my.render.render.pipeline.PipelineKey;
import my.render.render.pipeline.PipelineSpec;

import java.util.Objects;

public record ResolvedDraw(
	DrawCommand source,
	PipelineKey pipelineKey,
	PipelineSpec pipelineSpec,
	MaterialDescriptor material,
	CompiledDrawData compiledData,
	int passId,
	int orderIndex,
	int scissorId,
	UiRect scissorRect,
	UiRect passBounds,
	int dependencyId
) {
	public ResolvedDraw {
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(pipelineKey, "pipelineKey");
		Objects.requireNonNull(pipelineSpec, "pipelineSpec");
		Objects.requireNonNull(material, "material");
		Objects.requireNonNull(compiledData, "compiledData");
		Objects.requireNonNull(passBounds, "passBounds");
		if (passId < 0) {
			throw new IllegalArgumentException("passId must be non-negative");
		}
		if (orderIndex < 0) {
			throw new IllegalArgumentException("orderIndex must be non-negative");
		}
		if (scissorId < 0) {
			throw new IllegalArgumentException("scissorId must be non-negative");
		}
		if (dependencyId < 0) {
			throw new IllegalArgumentException("dependencyId must be non-negative");
		}
	}

	public int estimatedQuadCount() {
		return this.material.estimatedQuadCount();
	}

	public <T extends CompiledDrawData> T compiledData(Class<T> type) {
		Objects.requireNonNull(type, "type");
		return type.cast(this.compiledData);
	}
}
