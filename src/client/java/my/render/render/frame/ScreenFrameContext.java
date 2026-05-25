package my.render.render.frame;

import java.util.Objects;

public record ScreenFrameContext(FrameInfo info, SurfaceMetrics surface, String screenId, boolean pausesGame, float mouseX, float mouseY) implements UiFrameContext {
	public ScreenFrameContext {
		Objects.requireNonNull(info, "info");
		Objects.requireNonNull(surface, "surface");
		Objects.requireNonNull(screenId, "screenId");
		if (screenId.isBlank()) {
			throw new IllegalArgumentException("screenId must not be blank");
		}
	}
}
