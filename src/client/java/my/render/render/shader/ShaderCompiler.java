package my.render.render.shader;

import org.lwjgl.opengl.GL20C;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ShaderCompiler {
	public RuntimeShaderProgram compile(ShaderProgram shaderProgram, ShaderInterface shaderInterface) {
		Objects.requireNonNull(shaderProgram, "shaderProgram");
		Objects.requireNonNull(shaderInterface, "shaderInterface");

		int vertexShader = this.compileShader(GL20C.GL_VERTEX_SHADER, shaderProgram.vertexSource(), shaderProgram.vertexShader().asString());
		int fragmentShader = this.compileShader(GL20C.GL_FRAGMENT_SHADER, shaderProgram.fragmentSource(), shaderProgram.fragmentShader().asString());
		int program = GL20C.glCreateProgram();

		if (program == 0) {
			GL20C.glDeleteShader(vertexShader);
			GL20C.glDeleteShader(fragmentShader);
			throw new IllegalStateException("Failed to create shader program for " + shaderProgram.vertexShader() + " / " + shaderProgram.fragmentShader());
		}

		try {
			GL20C.glAttachShader(program, vertexShader);
			GL20C.glAttachShader(program, fragmentShader);
			GL20C.glLinkProgram(program);

			if (GL20C.glGetProgrami(program, GL20C.GL_LINK_STATUS) == 0) {
				String log = GL20C.glGetProgramInfoLog(program);
				throw new IllegalStateException("Shader program link failed: " + log);
			}

			Map<String, Integer> uniforms = new LinkedHashMap<>();
			for (ShaderInterface.Uniform uniform : shaderInterface.uniforms()) {
				uniforms.put(uniform.name(), GL20C.glGetUniformLocation(program, uniform.name()));
			}

			return new RuntimeShaderProgram(
				program,
				GL20C.glGetAttribLocation(program, "Position"),
				GL20C.glGetAttribLocation(program, "Color"),
				GL20C.glGetAttribLocation(program, "UV0"),
				GL20C.glGetAttribLocation(program, "Data0"),
				GL20C.glGetAttribLocation(program, "Data1"),
				GL20C.glGetAttribLocation(program, "Data2"),
				GL20C.glGetAttribLocation(program, "Data3"),
				uniforms
			);
		} catch (RuntimeException exception) {
			GL20C.glDeleteProgram(program);
			throw exception;
		} finally {
			if (GL20C.glIsProgram(program)) {
				GL20C.glDetachShader(program, vertexShader);
				GL20C.glDetachShader(program, fragmentShader);
			}
			GL20C.glDeleteShader(vertexShader);
			GL20C.glDeleteShader(fragmentShader);
		}
	}

	private int compileShader(int type, String source, String label) {
		int shader = GL20C.glCreateShader(type);
		if (shader == 0) {
			throw new IllegalStateException("Failed to create shader: " + label);
		}

		GL20C.glShaderSource(shader, Objects.requireNonNull(source, "source"));
		GL20C.glCompileShader(shader);
		if (GL20C.glGetShaderi(shader, GL20C.GL_COMPILE_STATUS) == 0) {
			String log = GL20C.glGetShaderInfoLog(shader);
			GL20C.glDeleteShader(shader);
			throw new IllegalStateException("Shader compile failed for " + label + ": " + log);
		}

		return shader;
	}
}
