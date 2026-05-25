package my.render.render.backend;

import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

final class GlStateSnapshot implements AutoCloseable {
	private final int framebufferId;
	private final int programId;
	private final int vertexArrayId;
	private final int arrayBufferId;
	private final int activeTexture;
	private final int[] textureBindings;
	private final int[] samplerBindings;
	private final boolean blendEnabled;
	private final int blendSrcRgb;
	private final int blendDstRgb;
	private final int blendSrcAlpha;
	private final int blendDstAlpha;
	private final boolean depthTestEnabled;
	private final boolean depthMask;
	private final boolean scissorEnabled;
	private final boolean cullEnabled;
	private final int[] viewport;
	private final int[] scissorBox;

	private GlStateSnapshot() {
		this.framebufferId = GL11C.glGetInteger(GL30C.GL_FRAMEBUFFER_BINDING);
		this.programId = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
		this.vertexArrayId = GL11C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING);
		this.arrayBufferId = GL11C.glGetInteger(GL15C.GL_ARRAY_BUFFER_BINDING);
		this.activeTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
		this.textureBindings = this.captureTextureBindings();
		this.samplerBindings = this.captureSamplerBindings();
		this.blendEnabled = GL11C.glIsEnabled(GL11C.GL_BLEND);
		this.blendSrcRgb = GL11C.glGetInteger(GL14C.GL_BLEND_SRC_RGB);
		this.blendDstRgb = GL11C.glGetInteger(GL14C.GL_BLEND_DST_RGB);
		this.blendSrcAlpha = GL11C.glGetInteger(GL14C.GL_BLEND_SRC_ALPHA);
		this.blendDstAlpha = GL11C.glGetInteger(GL14C.GL_BLEND_DST_ALPHA);
		this.depthTestEnabled = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST);
		this.depthMask = GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK);
		this.scissorEnabled = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST);
		this.cullEnabled = GL11C.glIsEnabled(GL11C.GL_CULL_FACE);

		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer viewportBuffer = stack.mallocInt(4);
			IntBuffer scissorBuffer = stack.mallocInt(4);
			GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewportBuffer);
			GL11C.glGetIntegerv(GL11C.GL_SCISSOR_BOX, scissorBuffer);
			this.viewport = new int[] {viewportBuffer.get(0), viewportBuffer.get(1), viewportBuffer.get(2), viewportBuffer.get(3)};
			this.scissorBox = new int[] {scissorBuffer.get(0), scissorBuffer.get(1), scissorBuffer.get(2), scissorBuffer.get(3)};
		}
	}

	static GlStateSnapshot capture() {
		return new GlStateSnapshot();
	}

	@Override
	public void close() {
		GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, this.framebufferId);
		GL11C.glViewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
		GL20C.glUseProgram(this.programId);
		GL30C.glBindVertexArray(this.vertexArrayId);
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.arrayBufferId);

		if (this.blendEnabled) {
			GL11C.glEnable(GL11C.GL_BLEND);
		} else {
			GL11C.glDisable(GL11C.GL_BLEND);
		}
		GL14C.glBlendFuncSeparate(this.blendSrcRgb, this.blendDstRgb, this.blendSrcAlpha, this.blendDstAlpha);

		if (this.depthTestEnabled) {
			GL11C.glEnable(GL11C.GL_DEPTH_TEST);
		} else {
			GL11C.glDisable(GL11C.GL_DEPTH_TEST);
		}
		GL11C.glDepthMask(this.depthMask);

		if (this.scissorEnabled) {
			GL11C.glEnable(GL11C.GL_SCISSOR_TEST);
			GL11C.glScissor(this.scissorBox[0], this.scissorBox[1], this.scissorBox[2], this.scissorBox[3]);
		} else {
			GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
		}

		if (this.cullEnabled) {
			GL11C.glEnable(GL11C.GL_CULL_FACE);
		} else {
			GL11C.glDisable(GL11C.GL_CULL_FACE);
		}

		for (int slot = 0; slot < this.textureBindings.length; slot++) {
			GL13C.glActiveTexture(GL13C.GL_TEXTURE0 + slot);
			GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, this.textureBindings[slot]);
			GL33C.glBindSampler(slot, this.samplerBindings[slot]);
		}

		GL13C.glActiveTexture(this.activeTexture);
	}

	private int[] captureTextureBindings() {
		int maxTrackedSlots = Math.max(1, Math.min(8, GL11C.glGetInteger(GL20C.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS)));
		int[] bindings = new int[maxTrackedSlots];

		for (int slot = 0; slot < maxTrackedSlots; slot++) {
			GL13C.glActiveTexture(GL13C.GL_TEXTURE0 + slot);
			bindings[slot] = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
		}

		GL13C.glActiveTexture(this.activeTexture);
		return bindings;
	}

	private int[] captureSamplerBindings() {
		int maxTrackedSlots = Math.max(1, Math.min(8, GL11C.glGetInteger(GL20C.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS)));
		int[] bindings = new int[maxTrackedSlots];

		for (int slot = 0; slot < maxTrackedSlots; slot++) {
			bindings[slot] = GL33C.glGetIntegeri(GL33C.GL_SAMPLER_BINDING, slot);
		}

		return bindings;
	}
}
