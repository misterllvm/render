package my.render.render.backend;

import my.render.render.core.ResourceId;

import java.util.List;
import java.util.Objects;

final class TextureBinder {
	private final GpuResourceArena resources;
	private final ResourceTextureLoader textureLoader;
	private final GlStateCache stateCache;

	TextureBinder(GpuResourceArena resources, ResourceTextureLoader textureLoader, GlStateCache stateCache) {
		this.resources = Objects.requireNonNull(resources, "resources");
		this.textureLoader = Objects.requireNonNull(textureLoader, "textureLoader");
		this.stateCache = Objects.requireNonNull(stateCache, "stateCache");
	}

	void bind(List<TextureSlotBinding> textures) {
		Objects.requireNonNull(textures, "textures");

		for (TextureSlotBinding texture : textures) {
			this.stateCache.activeTexture(texture.slot());
			int handle = this.resolveTextureHandle(texture.source());
			this.stateCache.bindTexture2D(texture.slot(), handle);
		}

		this.stateCache.activeTexture(0);
	}

	private int resolveTextureHandle(QuadBufferUpload.TextureSource source) {
		return switch (Objects.requireNonNull(source, "source")) {
			case QuadBufferUpload.TextureSource.ResourceTexture resourceTexture -> this.resolveResourceTexture(resourceTexture.id());
			case QuadBufferUpload.TextureSource.TargetTexture targetTexture -> this.resources.resolveOffscreenTexture(targetTexture.targetId());
		};
	}

	private int resolveResourceTexture(ResourceId textureId) {
		Integer existing = this.resources.textureHandle(textureId);
		if (existing != null) {
			return existing.intValue();
		}

		int handle = this.textureLoader.load(textureId);
		this.resources.registerTextureHandle(textureId, handle);
		return handle;
	}
}
