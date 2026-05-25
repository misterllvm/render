package my.render.render.effect;

import java.util.List;
import java.util.Objects;

public record EffectChain(List<EffectSpec> stages) {
	public EffectChain(List<EffectSpec> stages) {
		Objects.requireNonNull(stages, "stages");
		List<EffectSpec> copied = List.copyOf(stages);
		for (EffectSpec stage : copied) {
			Objects.requireNonNull(stage, "stage");
		}
		this.stages = copied;
	}

	public static EffectChain of(EffectSpec... stages) {
		return new EffectChain(List.of(stages));
	}

	public boolean isEmpty() {
		return this.stages.isEmpty();
	}
}
