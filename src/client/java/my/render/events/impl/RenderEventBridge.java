package my.render.events.impl;

import my.render.events.api.EventBus;
import my.render.render.base.UiRenderContext;
import my.render.render.event.*;
import my.render.render.event.*;
import my.render.render.frame.HudFrameContext;
import my.render.render.frame.LevelFrameContext;
import my.render.render.frame.ScreenFrameContext;
import my.render.render.integration.RenderController;
import my.render.screens.RenderedScreen;
import net.minecraft.client.Minecraft;

import java.util.Objects;

public final class RenderEventBridge implements HudRenderListener, ScreenRenderListener, WorldRenderListener, ResizeListener, UpdateListener {
	private final EventBus eventBus;
	private ScreenInfo currentScreen;

	public RenderEventBridge(EventBus eventBus) {
		this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
	}

	public void install(RenderController renderController) {
		Objects.requireNonNull(renderController, "renderController");
		renderController.onHud(this);
		renderController.onScreen(this);
		renderController.onWorld(this);
		renderController.onResize(this);
		renderController.onUpdate(this);
	}

	@Override
	public void render(HudFrameContext frame, UiRenderContext ui) {
		if (this.eventBus.hasSubscribers(HudRenderEvent.class)) {
			this.eventBus.post(new HudRenderEvent(frame, ui));
		}
	}

	@Override
	public boolean supports(ScreenInfo screen) {
		if (!this.eventBus.hasSubscribers(ScreenRenderEvent.class)) {
			this.currentScreen = null;
			return false;
		}
		if (!(Minecraft.getInstance().screen instanceof RenderedScreen renderedScreen) || !renderedScreen.matches(screen)) {
			this.currentScreen = null;
			return false;
		}
		this.currentScreen = Objects.requireNonNull(screen, "screen");
		return true;
	}

	@Override
	public void render(ScreenFrameContext frame, UiRenderContext ui) {
		if (this.currentScreen != null && this.eventBus.hasSubscribers(ScreenRenderEvent.class)) {
			this.eventBus.post(new ScreenRenderEvent(this.currentScreen, frame, ui));
		}
	}

	@Override
	public void render(LevelFrameContext frame, UiRenderContext ui) {
		if (this.eventBus.hasSubscribers(WorldRenderEvent.class)) {
			this.eventBus.post(new WorldRenderEvent(frame, ui));
		}
	}

	@Override
	public void resize(ResizeContext context) {
		if (this.eventBus.hasSubscribers(ResizeEvent.class)) {
			this.eventBus.post(new ResizeEvent(context));
		}
	}

	@Override
	public void update(UpdateContext context) {
		if (this.eventBus.hasSubscribers(TickEvent.class)) {
			this.eventBus.post(new TickEvent(context));
		}
	}
}
