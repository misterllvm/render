package my.render.render.backend;

import my.render.render.backend.material.SamplerMode;

import java.util.Objects;

record TextureSlotBinding(int slot, QuadBufferUpload.TextureSource source, SamplerMode samplerMode) {
	TextureSlotBinding {
		if (slot < 0) {
			throw new IllegalArgumentException("slot must be non-negative");
		}
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(samplerMode, "samplerMode");
	}

	TextureSlotBinding(int slot, QuadBufferUpload.TextureSource source) {
		this(slot, source, SamplerMode.LINEAR_CLAMP);
	}
}
