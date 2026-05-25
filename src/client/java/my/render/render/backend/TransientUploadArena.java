package my.render.render.backend;

import my.render.render.shader.RuntimeShaderProgram;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

final class TransientUploadArena implements AutoCloseable {
	private static final int INITIAL_VERTEX_BYTES = 256 * 1024;
	private static final int FLOAT_BYTES = Float.BYTES;

	private int vertexArrayId;
	private int vertexBufferId;
	private int elementBufferId;
	private int vertexCapacityBytes;
	private int indexedQuadCapacity;
	private int frameOffsetBytes;
	private ByteBuffer stagingBuffer;
	private FloatBuffer stagingFloatBuffer;

	void beginFrame() {
		this.frameOffsetBytes = 0;
		if (this.vertexBufferId != 0 && this.vertexCapacityBytes > 0) {
			GL30C.glBindVertexArray(this.vertexArrayId);
			GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.vertexBufferId);
			GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, this.vertexCapacityBytes, GL15C.GL_STREAM_DRAW);
			GL15C.glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, this.elementBufferId);
		}
	}

	GeometryUpload upload(RuntimeShaderProgram program, float[] vertexData, int vertexFloatCount, int floatsPerVertex, int quadCount) {
		if (floatsPerVertex <= 0) {
			throw new IllegalArgumentException("floatsPerVertex must be positive");
		}
		if (vertexFloatCount < 0 || vertexFloatCount > vertexData.length || vertexFloatCount % floatsPerVertex != 0) {
			throw new IllegalArgumentException("vertexFloatCount is not valid for the supplied vertex buffer");
		}
		this.ensureHandles();

		int byteSize = vertexFloatCount * FLOAT_BYTES;
		this.ensureVertexCapacity(this.frameOffsetBytes + byteSize);
		this.ensureStagingCapacity(byteSize);
		this.ensureQuadIndices(quadCount);

		GL30C.glBindVertexArray(this.vertexArrayId);
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.vertexBufferId);
		GL15C.glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, this.elementBufferId);

		this.stagingBuffer.clear();
		this.stagingFloatBuffer.clear();
		this.stagingFloatBuffer.put(vertexData, 0, vertexFloatCount);
		this.stagingBuffer.limit(byteSize);
		GL15C.glBufferSubData(GL15C.GL_ARRAY_BUFFER, this.frameOffsetBytes, this.stagingBuffer);

		int stride = floatsPerVertex * FLOAT_BYTES;
		long baseOffset = this.frameOffsetBytes;
		this.configureAttribute(program.positionLocation(), 2, stride, baseOffset);
		this.configureAttribute(program.uv0Location(), 2, stride, baseOffset + 2L * FLOAT_BYTES);
		this.configureAttribute(program.colorLocation(), 4, stride, baseOffset + 4L * FLOAT_BYTES);
		this.configureAttribute(program.data0Location(), 4, stride, baseOffset + 8L * FLOAT_BYTES);
		this.configureAttribute(program.data1Location(), 4, stride, baseOffset + 12L * FLOAT_BYTES);
		this.configureAttribute(program.data2Location(), 4, stride, baseOffset + 16L * FLOAT_BYTES);
		this.configureAttribute(program.data3Location(), 4, stride, baseOffset + 20L * FLOAT_BYTES);

		this.frameOffsetBytes += align(byteSize, 256);
		return new GeometryUpload(vertexFloatCount / floatsPerVertex, quadCount * 6);
	}

	@Override
	public void close() {
		if (this.stagingBuffer != null) {
			MemoryUtil.memFree(this.stagingBuffer);
			this.stagingBuffer = null;
			this.stagingFloatBuffer = null;
		}
		if (this.elementBufferId != 0) {
			GL15C.glDeleteBuffers(this.elementBufferId);
			this.elementBufferId = 0;
			this.indexedQuadCapacity = 0;
		}
		if (this.vertexBufferId != 0) {
			GL15C.glDeleteBuffers(this.vertexBufferId);
			this.vertexBufferId = 0;
		}
		if (this.vertexArrayId != 0) {
			GL30C.glDeleteVertexArrays(this.vertexArrayId);
			this.vertexArrayId = 0;
		}
		this.vertexCapacityBytes = 0;
		this.frameOffsetBytes = 0;
	}

	private void ensureHandles() {
		if (this.vertexArrayId == 0) {
			this.vertexArrayId = GL30C.glGenVertexArrays();
		}
		if (this.vertexBufferId == 0) {
			this.vertexBufferId = GL15C.glGenBuffers();
			this.vertexCapacityBytes = INITIAL_VERTEX_BYTES;
			GL30C.glBindVertexArray(this.vertexArrayId);
			GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.vertexBufferId);
			GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, this.vertexCapacityBytes, GL15C.GL_STREAM_DRAW);
		}
		if (this.elementBufferId == 0) {
			this.elementBufferId = GL15C.glGenBuffers();
		}
	}

	private void ensureVertexCapacity(int requiredBytes) {
		if (requiredBytes <= this.vertexCapacityBytes) {
			return;
		}
		int newCapacity = this.vertexCapacityBytes;
		while (newCapacity < requiredBytes) {
			newCapacity *= 2;
		}
		this.vertexCapacityBytes = newCapacity;
		GL30C.glBindVertexArray(this.vertexArrayId);
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.vertexBufferId);
		GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, this.vertexCapacityBytes, GL15C.GL_STREAM_DRAW);
	}

	private void ensureStagingCapacity(int requiredBytes) {
		if (this.stagingBuffer != null && this.stagingBuffer.capacity() >= requiredBytes) {
			return;
		}
		if (this.stagingBuffer != null) {
			MemoryUtil.memFree(this.stagingBuffer);
		}
		this.stagingBuffer = MemoryUtil.memAlloc(align(requiredBytes, 256));
		this.stagingFloatBuffer = this.stagingBuffer.asFloatBuffer();
	}

	private void configureAttribute(int location, int size, int stride, long offset) {
		if (location < 0) {
			return;
		}

		GL20C.glEnableVertexAttribArray(location);
		GL20C.glVertexAttribPointer(location, size, GL11C.GL_FLOAT, false, stride, offset);
	}

	private void ensureQuadIndices(int quadCount) {
		if (quadCount <= 0 || quadCount <= this.indexedQuadCapacity) {
			return;
		}
		if (quadCount > 0x4000_0000) {
			throw new IllegalArgumentException("Quad count too large: " + quadCount);
		}

		IntBuffer indexBuffer = MemoryUtil.memAllocInt(quadCount * 6);
		try {
			for (int quad = 0; quad < quadCount; quad++) {
				int baseVertex = quad * 4;
				indexBuffer.put(baseVertex);
				indexBuffer.put(baseVertex + 1);
				indexBuffer.put(baseVertex + 2);
				indexBuffer.put(baseVertex);
				indexBuffer.put(baseVertex + 2);
				indexBuffer.put(baseVertex + 3);
			}
			indexBuffer.flip();
			GL15C.glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, this.elementBufferId);
			GL15C.glBufferData(GL15C.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15C.GL_STATIC_DRAW);
			this.indexedQuadCapacity = quadCount;
		} finally {
			MemoryUtil.memFree(indexBuffer);
		}
	}

	private static int align(int value, int alignment) {
		int remainder = value % alignment;
		return remainder == 0 ? value : value + (alignment - remainder);
	}

	record GeometryUpload(int vertexCount, int indexCount) {
	}
}
