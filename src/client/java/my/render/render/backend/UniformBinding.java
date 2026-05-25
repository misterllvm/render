package my.render.render.backend;

import my.render.render.shader.ShaderInterface;

import java.util.Objects;

record UniformBinding(String name, ShaderInterface.Type type, UniformValue value) {
	UniformBinding {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(value, "value");
		if (name.isBlank()) {
			throw new IllegalArgumentException("name must not be blank");
		}
	}
}
