package my.render.render.backend;

import my.render.render.core.ResourceId;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL21C;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

final class ResourceTextureLoader {
	int load(ResourceId textureId) {
		Objects.requireNonNull(textureId, "textureId");
		String classpathLocation = "assets/" + textureId.namespace() + "/" + textureId.path();
		byte[] bytes;
		int previousTexture = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
		int previousPixelUnpackBuffer = GL11C.glGetInteger(GL21C.GL_PIXEL_UNPACK_BUFFER_BINDING);
		int previousUnpackAlignment = GL11C.glGetInteger(GL11C.GL_UNPACK_ALIGNMENT);
		int previousUnpackRowLength = GL11C.glGetInteger(GL12C.GL_UNPACK_ROW_LENGTH);
		int previousUnpackSkipPixels = GL11C.glGetInteger(GL12C.GL_UNPACK_SKIP_PIXELS);
		int previousUnpackSkipRows = GL11C.glGetInteger(GL12C.GL_UNPACK_SKIP_ROWS);

		try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream(classpathLocation)) {
			if (stream == null) {
				throw new IllegalStateException("Missing texture resource: " + textureId.asString());
			}
			bytes = stream.readAllBytes();
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to read texture resource: " + textureId.asString(), exception);
		}

		ByteBuffer encoded = MemoryUtil.memAlloc(bytes.length);
		encoded.put(bytes).flip();

		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer width = stack.mallocInt(1);
			IntBuffer height = stack.mallocInt(1);
			IntBuffer channels = stack.mallocInt(1);
			ByteBuffer pixels = STBImage.stbi_load_from_memory(encoded, width, height, channels, 4);
			if (pixels == null) {
				throw new IllegalStateException("Failed to decode texture resource: " + textureId.asString() + " (" + STBImage.stbi_failure_reason() + ")");
			}

			int textureHandle = GL11C.glGenTextures();
			try {
				GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, textureHandle);
				GL15C.glBindBuffer(GL21C.GL_PIXEL_UNPACK_BUFFER, 0);
				GL11C.glPixelStorei(GL11C.GL_UNPACK_ALIGNMENT, 1);
				GL11C.glPixelStorei(GL12C.GL_UNPACK_ROW_LENGTH, 0);
				GL11C.glPixelStorei(GL12C.GL_UNPACK_SKIP_PIXELS, 0);
				GL11C.glPixelStorei(GL12C.GL_UNPACK_SKIP_ROWS, 0);
				GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR);
				GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR);
				GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL12C.GL_TEXTURE_BASE_LEVEL, 0);
				GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL12C.GL_TEXTURE_MAX_LEVEL, 0);
				GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE);
				GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE);
				GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_RGBA8, width.get(0), height.get(0), 0, GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, pixels);
				return textureHandle;
			} catch (Throwable throwable) {
				GL11C.glDeleteTextures(textureHandle);
				throw throwable;
			} finally {
				GL15C.glBindBuffer(GL21C.GL_PIXEL_UNPACK_BUFFER, previousPixelUnpackBuffer);
				GL11C.glPixelStorei(GL11C.GL_UNPACK_ALIGNMENT, previousUnpackAlignment);
				GL11C.glPixelStorei(GL12C.GL_UNPACK_ROW_LENGTH, previousUnpackRowLength);
				GL11C.glPixelStorei(GL12C.GL_UNPACK_SKIP_PIXELS, previousUnpackSkipPixels);
				GL11C.glPixelStorei(GL12C.GL_UNPACK_SKIP_ROWS, previousUnpackSkipRows);
				GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, previousTexture);
				STBImage.stbi_image_free(pixels);
			}
		} finally {
			MemoryUtil.memFree(encoded);
		}
	}
}
