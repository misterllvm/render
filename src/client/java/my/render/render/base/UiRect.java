package my.render.render.base;

public record UiRect(float x, float y, float width, float height) {
	public UiRect {
		if (width < 0.0F) {
			throw new IllegalArgumentException("width must be non-negative");
		}
		if (height < 0.0F) {
			throw new IllegalArgumentException("height must be non-negative");
		}
	}

	public float right() {
		return this.x + this.width;
	}

	public float bottom() {
		return this.y + this.height;
	}

	public boolean isEmpty() {
		return this.width <= 0.0F || this.height <= 0.0F;
	}

	public UiRect inset(float amount) {
		float nextWidth = Math.max(0.0F, this.width - amount * 2.0F);
		float nextHeight = Math.max(0.0F, this.height - amount * 2.0F);
		return new UiRect(this.x + amount, this.y + amount, nextWidth, nextHeight);
	}

	public UiRect intersection(UiRect other) {
		float left = Math.max(this.x, other.x);
		float top = Math.max(this.y, other.y);
		float right = Math.min(this.right(), other.right());
		float bottom = Math.min(this.bottom(), other.bottom());
		float width = Math.max(0.0F, right - left);
		float height = Math.max(0.0F, bottom - top);
		return new UiRect(left, top, width, height);
	}
}
