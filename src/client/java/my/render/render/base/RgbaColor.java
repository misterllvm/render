package my.render.render.base;

public record RgbaColor(float r, float g, float b, float a) {
	public static final RgbaColor WHITE = new RgbaColor(1.0F, 1.0F, 1.0F, 1.0F);
	public static final RgbaColor TRANSPARENT = new RgbaColor(0.0F, 0.0F, 0.0F, 0.0F);

	public RgbaColor {
		validateChannel("r", r);
		validateChannel("g", g);
		validateChannel("b", b);
		validateChannel("a", a);
	}

	public static RgbaColor of(float r, float g, float b, float a) {
		return new RgbaColor(r, g, b, a);
	}

	public static RgbaColor fromArgb(int argb) {
		float alpha = (float) ((argb >>> 24) & 0xFF) / 255.0F;
		float red = (float) ((argb >>> 16) & 0xFF) / 255.0F;
		float green = (float) ((argb >>> 8) & 0xFF) / 255.0F;
		float blue = (float) (argb & 0xFF) / 255.0F;
		return new RgbaColor(red, green, blue, alpha);
	}

	public int toArgb() {
		int alpha = clampByte(Math.round(this.a * 255.0F));
		int red = clampByte(Math.round(this.r * 255.0F));
		int green = clampByte(Math.round(this.g * 255.0F));
		int blue = clampByte(Math.round(this.b * 255.0F));
		return alpha << 24 | red << 16 | green << 8 | blue;
	}

	public RgbaColor withAlpha(float alpha) {
		return new RgbaColor(this.r, this.g, this.b, alpha);
	}

	private static void validateChannel(String name, float value) {
		if (value < 0.0F || value > 1.0F) {
			throw new IllegalArgumentException(name + " must be between 0 and 1");
		}
	}

	private static int clampByte(int value) {
		return Math.max(0, Math.min(255, value));
	}
}
