package my.render.render.backend;

import my.render.render.pipeline.PipelineKey;
import my.render.render.pipeline.PipelineLibrary;
import my.render.render.pipeline.PipelineSpec;
import my.render.render.shader.*;
import my.render.render.shader.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class PipelineBinder implements AutoCloseable {
	private final GpuResourceArena resources;
	private final PipelineLibrary pipelines;
	private final ShaderSourceLoader shaderSources;
	private final ShaderCompiler shaderCompiler;
	private final Map<PipelineKey, PipelineBinding> bindings = new LinkedHashMap<>();
	private long boundGeneration = -1L;

	PipelineBinder(GpuResourceArena resources, PipelineLibrary pipelines, ShaderSourceLoader shaderSources) {
		this.resources = Objects.requireNonNull(resources, "resources");
		this.pipelines = Objects.requireNonNull(pipelines, "pipelines");
		this.shaderSources = Objects.requireNonNull(shaderSources, "shaderSources");
		this.shaderCompiler = new ShaderCompiler();
	}

	PipelineBinding bind(PipelineKey key) {
		Objects.requireNonNull(key, "key");
		return this.bind(this.pipelines.resolve(key));
	}

	PipelineBinding bind(PipelineSpec spec) {
		Objects.requireNonNull(spec, "spec");
		this.refreshIfNeeded();
		return this.bindings.computeIfAbsent(spec.key(), ignored -> this.createBinding(spec));
	}

	private PipelineBinding createBinding(PipelineSpec spec) {
		this.resources.rememberShader(spec.vertexShader());
		this.resources.rememberShader(spec.fragmentShader());
		ShaderInterface shaderInterface = ShaderInterfaces.forPipeline(spec.key());
		ShaderProgram shaderProgram = new ShaderProgram(
			spec.vertexShader(),
			spec.fragmentShader(),
			this.shaderSources.load(spec.vertexShader()),
			this.shaderSources.load(spec.fragmentShader())
		);
		return new PipelineBinding(
			spec,
			shaderProgram,
			this.shaderCompiler.compile(shaderProgram, shaderInterface),
			shaderInterface
		);
	}

	private void refreshIfNeeded() {
		long generation = this.resources.generation();
		if (generation != this.boundGeneration) {
			this.releaseBindings();
			this.boundGeneration = generation;
		}
	}

	@Override
	public void close() {
		this.releaseBindings();
	}

	private void releaseBindings() {
		for (PipelineBinding binding : this.bindings.values()) {
			binding.runtimeProgram().close();
		}
		this.bindings.clear();
	}
}
