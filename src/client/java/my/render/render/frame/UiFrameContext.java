package my.render.render.frame;

public sealed interface UiFrameContext permits HudFrameContext, ScreenFrameContext, LevelFrameContext {
	FrameInfo info();

	SurfaceMetrics surface();
}
