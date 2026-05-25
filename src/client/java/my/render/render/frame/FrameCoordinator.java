package my.render.render.frame;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class FrameCoordinator {
	private final AtomicLong nextFrameNumber = new AtomicLong();

	public FrameInfo beginFrame(float partialTick, boolean paused) {
		return new FrameInfo(this.nextFrameNumber.getAndIncrement(), partialTick, paused);
	}

	public SurfaceMetrics surface(int framebufferWidth, int framebufferHeight, int guiWidth, int guiHeight, double guiScale) {
		return new SurfaceMetrics(framebufferWidth, framebufferHeight, guiWidth, guiHeight, guiScale);
	}

	public HudFrameContext hud(FrameInfo info, SurfaceMetrics surface) {
		return new HudFrameContext(info, surface);
	}

	public ScreenFrameContext screen(FrameInfo info, SurfaceMetrics surface, String screenId, boolean pausesGame, float mouseX, float mouseY) {
		return new ScreenFrameContext(info, surface, screenId, pausesGame, mouseX, mouseY);
	}

	public LevelFrameContext level(FrameInfo info, SurfaceMetrics surface, WorldProjection projection) {
		Objects.requireNonNull(projection, "projection");
		return new LevelFrameContext(info, surface, projection);
	}
}
