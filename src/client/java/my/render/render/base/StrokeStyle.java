package my.render.render.base;

import java.util.Objects;

public record StrokeStyle(float widthPx, RgbaColor color, Align align) {
	public StrokeStyle {
		if (widthPx < 0.0F) {
			throw new IllegalArgumentException("widthPx must be non-negative");
		}
		Objects.requireNonNull(color, "color");
		Objects.requireNonNull(align, "align");
	}

	public enum Align {
		INSIDE,
		CENTER,
		OUTSIDE
	}
}
