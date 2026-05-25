package my.render.render.frame;

import java.util.Objects;

public record HudFrameContext(FrameInfo info, SurfaceMetrics surface) implements UiFrameContext {
	public HudFrameContext {
		Objects.requireNonNull(info, "info");
		Objects.requireNonNull(surface, "surface");
	}
}
