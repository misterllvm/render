package my.render.render.shader;

import java.util.List;
import java.util.Objects;

public record ShaderInterface(VertexFormat vertexFormat, List<Uniform> uniforms) {
	public ShaderInterface(VertexFormat vertexFormat, List<Uniform> uniforms) {
		Objects.requireNonNull(vertexFormat, "vertexFormat");
		Objects.requireNonNull(uniforms, "uniforms");
		List<Uniform> copiedUniforms = List.copyOf(uniforms);
		for (Uniform uniform : copiedUniforms) {
			Objects.requireNonNull(uniform, "uniform");
		}

		this.vertexFormat = vertexFormat;
		this.uniforms = copiedUniforms;
	}

	public enum VertexFormat {
		POSITION_UV_COLOR_QUAD
	}

	public record Uniform(String name, Type type, Source source) {
		public Uniform {
			Objects.requireNonNull(name, "name");
			Objects.requireNonNull(type, "type");
			Objects.requireNonNull(source, "source");
			if (name.isBlank()) {
				throw new IllegalArgumentException("name must not be blank");
			}
		}
	}

	public enum Type {
		FLOAT,
		VEC2,
		VEC4,
		SAMPLER2D
	}

	public enum Source {
		ENCODER,
		BATCH_UPLOAD,
		QUAD_PAYLOAD,
		TEXTURE_BINDING
	}
}
