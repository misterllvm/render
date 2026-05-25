package my.render.render.base;

public record TextureRegion(int u, int v, int width, int height, int textureWidth, int textureHeight) {
	public TextureRegion {
		if (width <= 0) {
			throw new IllegalArgumentException("width must be positive");
		}
		if (height <= 0) {
			throw new IllegalArgumentException("height must be positive");
		}
		if (textureWidth <= 0) {
			throw new IllegalArgumentException("textureWidth must be positive");
		}
		if (textureHeight <= 0) {
			throw new IllegalArgumentException("textureHeight must be positive");
		}
	}

	public static TextureRegion full(int textureWidth, int textureHeight) {
		return new TextureRegion(0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
	}
}
