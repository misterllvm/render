package my.render.render.frame;

public record SurfaceMetrics(int framebufferWidth, int framebufferHeight, int guiWidth, int guiHeight, double guiScale) {
	public SurfaceMetrics {
		if (framebufferWidth <= 0) {
			throw new IllegalArgumentException("framebufferWidth must be positive");
		}
		if (framebufferHeight <= 0) {
			throw new IllegalArgumentException("framebufferHeight must be positive");
		}
		if (guiWidth <= 0) {
			throw new IllegalArgumentException("guiWidth must be positive");
		}
		if (guiHeight <= 0) {
			throw new IllegalArgumentException("guiHeight must be positive");
		}
		if (guiScale <= 0.0D) {
			throw new IllegalArgumentException("guiScale must be positive");
		}
	}

	public double framebufferScaleX() {
		return (double) this.framebufferWidth / (double) this.guiWidth;
	}

	public double framebufferScaleY() {
		return (double) this.framebufferHeight / (double) this.guiHeight;
	}
}
