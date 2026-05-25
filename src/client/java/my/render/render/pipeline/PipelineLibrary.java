package my.render.render.pipeline;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PipelineLibrary {
	private final Map<PipelineKey, PipelineSpec> specs = new LinkedHashMap<>();

	public void register(PipelineSpec spec) {
		Objects.requireNonNull(spec, "spec");
		PipelineSpec existing = this.specs.putIfAbsent(spec.key(), spec);
		if (existing != null && !existing.equals(spec)) {
			throw new IllegalStateException("Pipeline key already registered with a different spec: " + spec.key().value());
		}
	}

	public void registerAll(Collection<PipelineSpec> specs) {
		Objects.requireNonNull(specs, "specs");
		for (PipelineSpec spec : specs) {
			this.register(spec);
		}
	}

	public PipelineSpec resolve(PipelineKey key) {
		Objects.requireNonNull(key, "key");
		PipelineSpec spec = this.specs.get(key);
		if (spec == null) {
			throw new IllegalArgumentException("Unknown pipeline: " + key.value());
		}
		return spec;
	}

	public boolean contains(PipelineKey key) {
		return this.specs.containsKey(Objects.requireNonNull(key, "key"));
	}

	public Collection<PipelineSpec> all() {
		return java.util.List.copyOf(this.specs.values());
	}
}
