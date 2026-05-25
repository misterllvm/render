package my.render.render.effect;

import my.render.render.base.RgbaColor;
import my.render.render.core.ResourceId;

import java.util.Objects;

public sealed interface EffectSpec permits EffectSpec.Blur, EffectSpec.Tint, EffectSpec.Custom {
	ResourceId type();

	record Blur(float radiusPx, int passes) implements EffectSpec {
		public static final ResourceId TYPE = ResourceId.of("render", "effects/blur");

		public Blur {
			if (radiusPx < 0.0F) {
				throw new IllegalArgumentException("radiusPx must be non-negative");
			}
			if (passes < 1) {
				throw new IllegalArgumentException("passes must be positive");
			}
		}

		@Override
		public ResourceId type() {
			return TYPE;
		}
	}

	record Tint(RgbaColor color) implements EffectSpec {
		public static final ResourceId TYPE = ResourceId.of("render", "effects/tint");

		public Tint {
			Objects.requireNonNull(color, "color");
		}

		@Override
		public ResourceId type() {
			return TYPE;
		}
	}

	record Custom(ResourceId type) implements EffectSpec {
		public Custom {
			Objects.requireNonNull(type, "type");
		}
	}
}
