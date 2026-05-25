package my.render.render.backend;

import my.render.render.pipeline.PipelineSpec;
import org.lwjgl.opengl.*;

import java.util.Arrays;

final class GlStateCache {
	private static final int TRACKED_TEXTURE_SLOTS = 16;
	private final int[] texture2D = new int[TRACKED_TEXTURE_SLOTS];
	private final boolean[] samplerZeroBound = new boolean[TRACKED_TEXTURE_SLOTS];
	private int activeTextureSlot = -1;
	private int framebuffer = -1;
	private int viewportWidth = -1;
	private int viewportHeight = -1;
	private int program = -1;
	private PipelineSpec.BlendMode blendMode;
	private PipelineSpec.DepthMode depthMode;
	private boolean cullDisabled;
	private boolean scissorKnown;
	private boolean scissorEnabled;
	private int scissorX;
	private int scissorY;
	private int scissorWidth;
	private int scissorHeight;

	GlStateCache() {
		this.reset();
	}

	void reset() {
		Arrays.fill(this.texture2D, -1);
		Arrays.fill(this.samplerZeroBound, false);
		this.activeTextureSlot = -1;
		this.framebuffer = -1;
		this.viewportWidth = -1;
		this.viewportHeight = -1;
		this.program = -1;
		this.blendMode = null;
		this.depthMode = null;
		this.cullDisabled = false;
		this.scissorKnown = false;
	}

	void bindFramebuffer(int framebuffer) {
		if (this.framebuffer == framebuffer) {
			return;
		}
		GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer);
		this.framebuffer = framebuffer;
	}

	void viewport(int width, int height) {
		if (this.viewportWidth == width && this.viewportHeight == height) {
			return;
		}
		GL11C.glViewport(0, 0, width, height);
		this.viewportWidth = width;
		this.viewportHeight = height;
	}

	void useProgram(int program) {
		if (this.program == program) {
			return;
		}
		GL20C.glUseProgram(program);
		this.program = program;
	}

	void disableCull() {
		if (this.cullDisabled) {
			return;
		}
		GL11C.glDisable(GL11C.GL_CULL_FACE);
		this.cullDisabled = true;
	}

	void blend(PipelineSpec.BlendMode mode) {
		if (this.blendMode == mode) {
			return;
		}
		switch (mode) {
			case OPAQUE -> GL11C.glDisable(GL11C.GL_BLEND);
			case ALPHA -> {
				GL11C.glEnable(GL11C.GL_BLEND);
				GL14C.glBlendFuncSeparate(GL11C.GL_SRC_ALPHA, GL11C.GL_ONE_MINUS_SRC_ALPHA, GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_ALPHA);
			}
			case ADDITIVE -> {
				GL11C.glEnable(GL11C.GL_BLEND);
				GL11C.glBlendFunc(GL11C.GL_SRC_ALPHA, GL11C.GL_ONE);
			}
		}
		this.blendMode = mode;
	}

	void depth(PipelineSpec.DepthMode mode) {
		if (this.depthMode == mode) {
			return;
		}
		switch (mode) {
			case NONE -> {
				GL11C.glDisable(GL11C.GL_DEPTH_TEST);
				GL11C.glDepthMask(false);
			}
			case TEST -> {
				GL11C.glEnable(GL11C.GL_DEPTH_TEST);
				GL11C.glDepthMask(false);
			}
			case TEST_WRITE -> {
				GL11C.glEnable(GL11C.GL_DEPTH_TEST);
				GL11C.glDepthMask(true);
			}
		}
		this.depthMode = mode;
	}

	void disableScissor() {
		if (this.scissorKnown && !this.scissorEnabled) {
			return;
		}
		GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
		this.scissorKnown = true;
		this.scissorEnabled = false;
	}

	void scissor(int x, int y, int width, int height) {
		if (this.scissorKnown && this.scissorEnabled && this.scissorX == x && this.scissorY == y && this.scissorWidth == width && this.scissorHeight == height) {
			return;
		}
		if (!this.scissorKnown || !this.scissorEnabled) {
			GL11C.glEnable(GL11C.GL_SCISSOR_TEST);
		}
		GL11C.glScissor(x, y, width, height);
		this.scissorKnown = true;
		this.scissorEnabled = true;
		this.scissorX = x;
		this.scissorY = y;
		this.scissorWidth = width;
		this.scissorHeight = height;
	}

	void bindTexture2D(int slot, int handle) {
		this.activeTexture(slot);
		this.bindSamplerZero(slot);
		if (slot < 0 || slot >= this.texture2D.length) {
			GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, handle);
			return;
		}
		if (this.texture2D[slot] == handle) {
			return;
		}
		GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, handle);
		this.texture2D[slot] = handle;
	}

	void activeTexture(int slot) {
		if (this.activeTextureSlot == slot) {
			return;
		}
		GL13C.glActiveTexture(GL13C.GL_TEXTURE0 + slot);
		this.activeTextureSlot = slot;
	}

	private void bindSamplerZero(int slot) {
		if (slot >= 0 && slot < this.samplerZeroBound.length && this.samplerZeroBound[slot]) {
			return;
		}
		GL33C.glBindSampler(slot, 0);
		if (slot >= 0 && slot < this.samplerZeroBound.length) {
			this.samplerZeroBound[slot] = true;
		}
	}
}
