package my.render.screens.hud;

import my.render.events.impl.HudRenderEvent;

@FunctionalInterface
public interface HudElement {
	void render(HudRenderEvent event);
}
