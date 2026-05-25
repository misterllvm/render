package my.render.render.event;

import my.render.render.frame.SurfaceMetrics;

import java.util.Objects;

public record ResizeContext(SurfaceMetrics previousSurface, SurfaceMetrics currentSurface) {
	public ResizeContext {
		Objects.requireNonNull(previousSurface, "previousSurface");
		Objects.requireNonNull(currentSurface, "currentSurface");
	}
}
