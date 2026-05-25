package my.render.render.backend;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

final class RuntimeRenderTarget implements AutoCloseable {
	private static final boolean DEBUG_FBO_CHECKS = Boolean.getBoolean("my.render.debugFboChecks");
	private static final float[] TRANSPARENT_CLEAR = new float[] {0.0F, 0.0F, 0.0F, 0.0F};
	private final int framebufferId;
	private final int colorTextureId;
	private final int logicalWidth;
	private final int logicalHeight;
	private final int physicalWidth;
	private final int physicalHeight;
	private final boolean owned;

	private RuntimeRenderTarget(int framebufferId, int colorTextureId, int logicalWidth, int logicalHeight, int physicalWidth, int physicalHeight, boolean owned) {
		this.framebufferId = framebufferId;
		this.colorTextureId = colorTextureId;
		this.logicalWidth = logicalWidth;
		this.logicalHeight = logicalHeight;
		this.physicalWidth = physicalWidth;
		this.physicalHeight = physicalHeight;
		this.owned = owned;
	}

	static RuntimeRenderTarget main(int logicalWidth, int logicalHeight, int physicalWidth, int physicalHeight) {
		RenderSystem.assertOnRenderThread();
		int framebufferId = GL11C.glGetInteger(GL30C.GL_FRAMEBUFFER_BINDING);
		return new RuntimeRenderTarget(framebufferId, 0, logicalWidth, logicalHeight, physicalWidth, physicalHeight, false);
	}

	static RuntimeRenderTarget offscreen(int logicalWidth, int logicalHeight) {
		RenderSystem.assertOnRenderThread();
		int previousFramebuffer = GL11C.glGetInteger(GL30C.GL_FRAMEBUFFER_BINDING);
		int previousTexture = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);

		int textureId = GL11C.glGenTextures();
		GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, textureId);
		GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR);
		GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR);
		GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE);
		GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE);
		GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_RGBA8, logicalWidth, logicalHeight, 0, GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, 0L);

		int framebufferId = GL30C.glGenFramebuffers();
		GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebufferId);
		GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL11C.GL_TEXTURE_2D, textureId, 0);
		if (GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER) != GL30C.GL_FRAMEBUFFER_COMPLETE) {
			GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, previousFramebuffer);
			GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, previousTexture);
			GL30C.glDeleteFramebuffers(framebufferId);
			GL11C.glDeleteTextures(textureId);
			throw new IllegalStateException("Failed to create offscreen framebuffer");
		}

		GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, previousFramebuffer);
		GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, previousTexture);
		return new RuntimeRenderTarget(framebufferId, textureId, logicalWidth, logicalHeight, logicalWidth, logicalHeight, true);
	}

	int framebufferId() {
		return this.framebufferId;
	}

	int colorTextureId() {
		return this.colorTextureId;
	}

	int logicalWidth() {
		return this.logicalWidth;
	}

	int logicalHeight() {
		return this.logicalHeight;
	}

	int physicalWidth() {
		return this.physicalWidth;
	}

	int physicalHeight() {
		return this.physicalHeight;
	}

	void bindForDraw(GlStateCache stateCache) {
		if (DEBUG_FBO_CHECKS) {
			this.assertComplete("bindForDraw");
		}
		stateCache.bindFramebuffer(this.framebufferId);
		stateCache.viewport(this.physicalWidth, this.physicalHeight);
	}

	void clear() {
		if (!this.owned) {
			return;
		}
		if (DEBUG_FBO_CHECKS) {
			this.assertComplete("clear");
		}

		int previousFramebuffer = GL11C.glGetInteger(GL30C.GL_FRAMEBUFFER_BINDING);
		boolean scissorEnabled = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST);
		try (MemoryStack stack = scissorEnabled ? MemoryStack.stackPush() : null) {
			IntBuffer scissorBox = scissorEnabled ? stack.mallocInt(4) : null;
			if (scissorEnabled) {
				GL11C.glGetIntegerv(GL11C.GL_SCISSOR_BOX, scissorBox);
			}
			try {
				GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, this.framebufferId);
				if (scissorEnabled) {
					GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
				}
				GL30C.glClearBufferfv(GL11C.GL_COLOR, 0, TRANSPARENT_CLEAR);
			} finally {
				GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, previousFramebuffer);
				if (scissorEnabled) {
					GL11C.glEnable(GL11C.GL_SCISSOR_TEST);
					GL11C.glScissor(scissorBox.get(0), scissorBox.get(1), scissorBox.get(2), scissorBox.get(3));
				}
			}
		}
	}

	@Override
	public void close() {
		RenderSystem.assertOnRenderThread();
		if (!this.owned) {
			return;
		}

		GL13C.glActiveTexture(GL13C.GL_TEXTURE0);
		GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, 0);
		GL30C.glDeleteFramebuffers(this.framebufferId);
		GL11C.glDeleteTextures(this.colorTextureId);
	}

	private void assertComplete(String phase) {
		if (!this.owned) {
			return;
		}

		int previousFramebuffer = GL11C.glGetInteger(GL30C.GL_FRAMEBUFFER_BINDING);
		GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, this.framebufferId);
		int status = GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER);
		GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, previousFramebuffer);

		if (status != GL30C.GL_FRAMEBUFFER_COMPLETE) {
			throw new IllegalStateException("Offscreen framebuffer is incomplete during " + phase + ": id=" + this.framebufferId + ", texture=" + this.colorTextureId + ", status=0x" + Integer.toHexString(status));
		}
	}
}
