package my.render.render.event;

import my.render.render.base.UiRenderContext;
import my.render.render.frame.HudFrameContext;

@FunctionalInterface
public interface HudRenderListener {
	void render(HudFrameContext frame, UiRenderContext ui);
}
