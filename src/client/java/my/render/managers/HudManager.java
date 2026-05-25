package my.render.managers;

import my.render.events.api.EventBus;
import my.render.events.impl.HudRenderEvent;
import my.render.screens.hud.HudElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class HudManager {
	private final List<HudElement> overlays = new ArrayList<>();

	public void bind(EventBus eventBus) {
		Objects.requireNonNull(eventBus, "eventBus");
		eventBus.subscribe(HudRenderEvent.class, this::render);
	}

	public void register(HudElement overlay) {
		this.overlays.add(Objects.requireNonNull(overlay, "overlay"));
	}

	private void render(HudRenderEvent event) {
		for (HudElement overlay : List.copyOf(this.overlays)) {
			overlay.render(event);
		}
	}
}
