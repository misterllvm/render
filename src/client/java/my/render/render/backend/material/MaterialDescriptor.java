package my.render.render.backend.material;

import my.render.render.backend.ExecutionFamily;

import java.util.Objects;

public record MaterialDescriptor(
	MaterialKey key,
	ExecutionFamily executionFamily,
	int batchUniformKey,
	int textureStateKey,
	int estimatedQuadCount,
	boolean batchable,
	boolean dedicated
) {
	public MaterialDescriptor {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(executionFamily, "executionFamily");
		if (estimatedQuadCount < 0) {
			throw new IllegalArgumentException("estimatedQuadCount must be non-negative");
		}
	}
}
