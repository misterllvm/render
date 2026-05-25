package my.render.render.backend;

import my.render.render.core.ResourceId;
import my.render.render.pipeline.PipelineLibrary;
import my.render.render.pipeline.PipelineSpec;
import org.lwjgl.opengl.GL11C;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class GpuResourceArena implements AutoCloseable {
	private final Set<ResourceId> shaders = new LinkedHashSet<>();
	private final Set<ResourceId> textures = new LinkedHashSet<>();
	private final Set<Integer> offscreenTargets = new LinkedHashSet<>();
	private final Map<ResourceId, Integer> textureHandles = new LinkedHashMap<>();
	private final Map<Integer, RuntimeRenderTarget> offscreenRenderTargets = new LinkedHashMap<>();
	private final AtomicLong generation = new AtomicLong();
	private final AtomicBoolean closed = new AtomicBoolean();

	public void rememberShader(ResourceId shaderPath) {
		this.ensureOpen();
		Objects.requireNonNull(shaderPath, "shaderPath");
		this.shaders.add(shaderPath);
	}

	public void rememberTexture(ResourceId texturePath) {
		this.ensureOpen();
		Objects.requireNonNull(texturePath, "texturePath");
		this.textures.add(texturePath);
	}

	public void rememberAllPipelines(PipelineLibrary pipelines) {
		this.ensureOpen();
		Objects.requireNonNull(pipelines, "pipelines");
		for (PipelineSpec spec : pipelines.all()) {
			this.rememberShader(spec.vertexShader());
			this.rememberShader(spec.fragmentShader());
		}
	}

	public void rememberOffscreenTarget(int targetId) {
		this.ensureOpen();
		this.offscreenTargets.add(targetId);
	}

	public void registerTextureHandle(ResourceId texturePath, int textureHandle) {
		this.ensureOpen();
		Objects.requireNonNull(texturePath, "texturePath");
		if (textureHandle <= 0) {
			throw new IllegalArgumentException("textureHandle must be positive");
		}
		this.textures.add(texturePath);
		this.textureHandles.put(texturePath, textureHandle);
	}

	public Integer textureHandle(ResourceId texturePath) {
		this.ensureOpen();
		return this.textureHandles.get(Objects.requireNonNull(texturePath, "texturePath"));
	}

	public void registerOffscreenTarget(int targetId, RuntimeRenderTarget renderTarget) {
		this.ensureOpen();
		Objects.requireNonNull(renderTarget, "renderTarget");
		this.offscreenTargets.add(targetId);
		this.offscreenRenderTargets.put(targetId, renderTarget);
	}

	public int resolveOffscreenTexture(int targetId) {
		this.ensureOpen();
		RuntimeRenderTarget renderTarget = this.offscreenRenderTargets.get(targetId);
		if (renderTarget == null) {
			throw new IllegalArgumentException("Unknown offscreen target texture: " + targetId);
		}
		return renderTarget.colorTextureId();
	}

	public long generation() {
		return this.generation.get();
	}

	public Set<ResourceId> shaders() {
		return Set.copyOf(this.shaders);
	}

	public Set<ResourceId> textures() {
		return Set.copyOf(this.textures);
	}

	public void reloadAll() {
		this.ensureOpen();
		this.releaseTextureHandles();
		this.releaseOffscreenTargets();
		this.offscreenTargets.clear();
		this.generation.incrementAndGet();
	}

	public void trimTransientTargets() {
		this.ensureOpen();
		this.releaseOffscreenTargets();
		this.offscreenTargets.clear();
	}

	@Override
	public void close() {
		if (this.closed.compareAndSet(false, true)) {
			this.releaseTextureHandles();
			this.releaseOffscreenTargets();
			this.shaders.clear();
			this.textures.clear();
			this.offscreenTargets.clear();
		}
	}

	private void ensureOpen() {
		if (this.closed.get()) {
			throw new IllegalStateException("GPU resource arena is closed");
		}
	}

	private void releaseTextureHandles() {
		for (Integer textureHandle : this.textureHandles.values()) {
			GL11C.glDeleteTextures(textureHandle.intValue());
		}
		this.textureHandles.clear();
	}

	private void releaseOffscreenTargets() {
		for (RuntimeRenderTarget renderTarget : this.offscreenRenderTargets.values()) {
			renderTarget.close();
		}
		this.offscreenRenderTargets.clear();
	}
}
