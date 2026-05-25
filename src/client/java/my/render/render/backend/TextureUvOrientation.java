package my.render.render.backend;

final class TextureUvOrientation {
	private TextureUvOrientation() {
	}

	static float mapV(QuadBufferUpload.TextureSource source, float v) {
		if (source instanceof QuadBufferUpload.TextureSource.TargetTexture) {
			return 1.0F - v;
		}
		return v;
	}
}
