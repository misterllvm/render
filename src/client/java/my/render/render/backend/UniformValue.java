package my.render.render.backend;

import my.render.render.base.RgbaColor;

import java.util.Objects;

sealed interface UniformValue permits UniformValue.FloatValue, UniformValue.Vec2Value, UniformValue.Vec4Value, UniformValue.Sampler2DValue {
	record FloatValue(float value) implements UniformValue {
	}

	record Vec2Value(float x, float y) implements UniformValue {
	}

	record Vec4Value(float x, float y, float z, float w) implements UniformValue {
		static Vec4Value fromColor(RgbaColor color) {
			Objects.requireNonNull(color, "color");
			return new Vec4Value(color.r(), color.g(), color.b(), color.a());
		}
	}

	record Sampler2DValue(int slot) implements UniformValue {
		public Sampler2DValue {
			if (slot < 0) {
				throw new IllegalArgumentException("slot must be non-negative");
			}
		}
	}
}
