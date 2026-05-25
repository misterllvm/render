package my.render.render.core;

import my.render.render.backend.GpuResourceArena;
import my.render.render.backend.OffscreenPassManager;
import my.render.render.backend.RendererBackend;
import my.render.render.frame.FrameCoordinator;
import my.render.render.pipeline.PipelineLibrary;

import java.util.concurrent.atomic.AtomicBoolean;

public final class RenderRuntime implements AutoCloseable {
	private final RendererBackend backend;
	private final FrameCoordinator frames;
	private final AtomicBoolean closed = new AtomicBoolean();

	RenderRuntime(RendererBackend backend, FrameCoordinator frames) {
		this.backend = backend;
		this.frames = frames;
	}

	public RendererBackend backend() {
		return this.backend;
	}

	public FrameCoordinator frames() {
		return this.frames;
	}

	public PipelineLibrary pipelines() {
		return this.backend.pipelines();
	}

	public GpuResourceArena resources() {
		return this.backend.resources();
	}

	public OffscreenPassManager offscreenPasses() {
		return this.backend.offscreenPasses();
	}

	public void reload() {
		this.ensureOpen();
		this.resources().reloadAll();
		this.offscreenPasses().trim();
	}

	@Override
	public void close() {
		if (this.closed.compareAndSet(false, true)) {
			this.backend.close();
		}
	}

	private void ensureOpen() {
		if (this.closed.get()) {
			throw new IllegalStateException("Renderer runtime is closed");
		}
	}
}
