package my.render.render.event;

import my.render.render.base.UiRenderContext;
import my.render.render.frame.LevelFrameContext;

@FunctionalInterface
public interface WorldRenderListener {
	void render(LevelFrameContext frame, UiRenderContext ui);
}
