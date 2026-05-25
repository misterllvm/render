package my.render.render.backend;

final class FloatVertexBufferBuilder {
	private float[] values = new float[1024];
	private int size;

	void reset(int requiredCapacity) {
		this.size = 0;
		this.ensureCapacity(requiredCapacity);
	}

	int size() {
		return this.size;
	}

	float[] values() {
		return this.values;
	}

	void add(float value) {
		this.ensureCapacity(this.size + 1);
		this.values[this.size++] = value;
	}

	private void ensureCapacity(int targetSize) {
		if (targetSize <= this.values.length) {
			return;
		}
		int newCapacity = this.values.length;
		while (newCapacity < targetSize) {
			newCapacity *= 2;
		}
		float[] expanded = new float[newCapacity];
		System.arraycopy(this.values, 0, expanded, 0, this.size);
		this.values = expanded;
	}
}
