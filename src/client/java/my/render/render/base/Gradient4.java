package my.render.render.base;

import java.util.Objects;

public record Gradient4(RgbaColor topLeft, RgbaColor topRight, RgbaColor bottomRight, RgbaColor bottomLeft) {
	public Gradient4 {
		Objects.requireNonNull(topLeft, "topLeft");
		Objects.requireNonNull(topRight, "topRight");
		Objects.requireNonNull(bottomRight, "bottomRight");
		Objects.requireNonNull(bottomLeft, "bottomLeft");
	}

	public static Gradient4 solid(RgbaColor color) {
		Objects.requireNonNull(color, "color");
		return new Gradient4(color, color, color, color);
	}
}
