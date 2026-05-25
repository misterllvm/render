package my.render;

import my.render.events.api.EventBus;
import my.render.events.impl.RenderEventBridge;
import my.render.managers.HudManager;
import my.render.render.integration.RenderController;
import my.render.screens.ScreenDispatcher;

import java.util.Objects;

public final class ClientRuntime {
	private final RenderController renderController;
	private final EventBus eventBus;
	private final HudManager hudManager;
	private final RenderEventBridge eventBridge;
	@SuppressWarnings("unused")
	private final ScreenDispatcher screenDispatcher;

	public ClientRuntime(RenderController renderController) {
		this.renderController = Objects.requireNonNull(renderController, "renderController");
		this.eventBus = new EventBus();
		this.hudManager = new HudManager();
		this.eventBridge = new RenderEventBridge(this.eventBus);
		this.screenDispatcher = new ScreenDispatcher(this.eventBus);

		this.eventBridge.install(renderController);
		this.hudManager.bind(this.eventBus);
	}

	public RenderController renderController() {
		return this.renderController;
	}

	public EventBus eventBus() {
		return this.eventBus;
	}


	public HudManager hudManager() {
		return this.hudManager;
	}
}
