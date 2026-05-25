package my.render.render.shader;

import my.render.render.core.ResourceId;

import java.util.Objects;

public record ShaderProgram(ResourceId vertexShader, ResourceId fragmentShader, String vertexSource, String fragmentSource) {
	public ShaderProgram {
		Objects.requireNonNull(vertexShader, "vertexShader");
		Objects.requireNonNull(fragmentShader, "fragmentShader");
		Objects.requireNonNull(vertexSource, "vertexSource");
		Objects.requireNonNull(fragmentSource, "fragmentSource");
	}
}
