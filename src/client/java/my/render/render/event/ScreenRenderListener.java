package my.render.render.event;

import my.render.render.base.UiRenderContext;
import my.render.render.frame.ScreenFrameContext;

public interface ScreenRenderListener {
	default boolean supports(ScreenInfo screen) {
		return true;
	}

	void render(ScreenFrameContext frame, UiRenderContext ui);
}
