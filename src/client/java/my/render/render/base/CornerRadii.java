package my.render.render.base;

public record CornerRadii(float topLeft, float topRight, float bottomRight, float bottomLeft) {
	public static final CornerRadii NONE = new CornerRadii(0.0F, 0.0F, 0.0F, 0.0F);

	public CornerRadii {
		validate(topLeft);
		validate(topRight);
		validate(bottomRight);
		validate(bottomLeft);
	}

	public static CornerRadii uniform(float radius) {
		return new CornerRadii(radius, radius, radius, radius);
	}

	private static void validate(float value) {
		if (value < 0.0F) {
			throw new IllegalArgumentException("corner radius must be non-negative");
		}
	}
}
