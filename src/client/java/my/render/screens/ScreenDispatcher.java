package my.render.screens;

import my.render.events.api.EventBus;
import my.render.events.impl.ScreenRenderEvent;
import net.minecraft.client.Minecraft;

import java.util.Objects;

public final class ScreenDispatcher {
	public ScreenDispatcher(EventBus eventBus) {
		Objects.requireNonNull(eventBus, "eventBus");
		eventBus.subscribe(ScreenRenderEvent.class, this::render);
	}

	private void render(ScreenRenderEvent event) {
		if (Minecraft.getInstance().screen instanceof RenderedScreen renderedScreen && renderedScreen.matches(event.screen())) {
			renderedScreen.renderCustom(event.frame(), event.ui());
		}
	}
}
