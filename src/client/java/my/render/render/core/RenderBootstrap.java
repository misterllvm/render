package my.render.render.core;

import my.render.render.backend.GpuResourceArena;
import my.render.render.backend.OffscreenPassManager;
import my.render.render.backend.RendererBackend;
import my.render.render.frame.FrameCoordinator;
import my.render.render.pipeline.PipelineLibrary;
import my.render.render.pipeline.StandardPipelines;

public final class RenderBootstrap {
	private RenderBootstrap() {
	}

	public static RenderRuntime initialize() {
		GpuResourceArena resources = new GpuResourceArena();
		PipelineLibrary pipelines = new PipelineLibrary();
		StandardPipelines.registerDefaults(pipelines);
		resources.rememberAllPipelines(pipelines);

		OffscreenPassManager offscreenPasses = new OffscreenPassManager(resources);
		RendererBackend backend = new RendererBackend(resources, pipelines, offscreenPasses);
		FrameCoordinator frames = new FrameCoordinator();
		return new RenderRuntime(backend, frames);
	}
}
