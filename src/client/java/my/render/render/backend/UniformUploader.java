package my.render.render.backend;

import my.render.render.shader.RuntimeShaderProgram;
import org.lwjgl.opengl.GL20C;

import java.util.List;
import java.util.Objects;

final class UniformUploader {
	void upload(RuntimeShaderProgram program, List<UniformBinding> uniforms) {
		Objects.requireNonNull(program, "program");
		Objects.requireNonNull(uniforms, "uniforms");

		for (UniformBinding uniform : uniforms) {
			int location = program.uniformLocation(uniform.name());
			if (location < 0) {
				continue;
			}

			switch (uniform.value()) {
				case UniformValue.FloatValue floatValue -> GL20C.glUniform1f(location, floatValue.value());
				case UniformValue.Vec2Value vec2Value -> GL20C.glUniform2f(location, vec2Value.x(), vec2Value.y());
				case UniformValue.Vec4Value vec4Value -> GL20C.glUniform4f(location, vec4Value.x(), vec4Value.y(), vec4Value.z(), vec4Value.w());
				case UniformValue.Sampler2DValue sampler2DValue -> GL20C.glUniform1i(location, sampler2DValue.slot());
			}
		}
	}
}
