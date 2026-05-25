package my.render.render.pipeline;

import java.util.Objects;

public record PipelineKey(String value) {
	public PipelineKey {
		Objects.requireNonNull(value, "value");
		if (value.isBlank()) {
			throw new IllegalArgumentException("value must not be blank");
		}
	}

	public static PipelineKey of(String value) {
		return new PipelineKey(value);
	}
}
