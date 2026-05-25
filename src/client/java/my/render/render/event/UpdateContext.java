package my.render.render.event;

import my.render.render.frame.SurfaceMetrics;

import java.util.Objects;

public record UpdateContext(long tickNumber, float partialTick, boolean paused, SurfaceMetrics surface) {
	public UpdateContext {
		Objects.requireNonNull(surface, "surface");
	}
}
