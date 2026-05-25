package my.render.render.backend;

import my.render.render.pipeline.PipelineSpec;
import my.render.render.shader.RuntimeShaderProgram;
import my.render.render.shader.ShaderInterface;
import my.render.render.shader.ShaderProgram;

import java.util.Objects;

final record PipelineBinding(PipelineSpec spec, ShaderProgram program, RuntimeShaderProgram runtimeProgram, ShaderInterface shaderInterface) {
	PipelineBinding {
		Objects.requireNonNull(spec, "spec");
		Objects.requireNonNull(program, "program");
		Objects.requireNonNull(runtimeProgram, "runtimeProgram");
		Objects.requireNonNull(shaderInterface, "shaderInterface");
	}
}
