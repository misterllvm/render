package my.render.render.frame;

import java.util.Objects;

public record LevelFrameContext(FrameInfo info, SurfaceMetrics surface, WorldProjection projection) implements UiFrameContext {
	public LevelFrameContext {
		Objects.requireNonNull(info, "info");
		Objects.requireNonNull(surface, "surface");
		Objects.requireNonNull(projection, "projection");
	}
}
