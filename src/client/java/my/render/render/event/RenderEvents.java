package my.render.render.event;

import my.render.render.base.UiRenderContext;
import my.render.render.frame.HudFrameContext;
import my.render.render.frame.LevelFrameContext;
import my.render.render.frame.ScreenFrameContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RenderEvents {
	private final ListenerList<HudRenderListener> hud = new ListenerList<>();
	private final ListenerList<ScreenRenderListener> screens = new ListenerList<>();
	private final ListenerList<WorldRenderListener> world = new ListenerList<>();
	private final ListenerList<ResizeListener> resize = new ListenerList<>();
	private final ListenerList<UpdateListener> update = new ListenerList<>();

	public void onHud(HudRenderListener listener) {
		this.hud.add(listener);
	}

	public void onScreen(ScreenRenderListener listener) {
		this.screens.add(listener);
	}

	public void onWorld(WorldRenderListener listener) {
		this.world.add(listener);
	}

	public void onResize(ResizeListener listener) {
		this.resize.add(listener);
	}

	public void onUpdate(UpdateListener listener) {
		this.update.add(listener);
	}

	public boolean hasHudListeners() {
		return this.hud.isNotEmpty();
	}

	public boolean hasScreenListeners() {
		return this.screens.isNotEmpty();
	}

	public boolean hasWorldListeners() {
		return this.world.isNotEmpty();
	}

	public void fireHud(HudFrameContext frame, UiRenderContext ui) {
		for (HudRenderListener listener : this.hud.snapshot()) {
			listener.render(frame, ui);
		}
	}

	public boolean supportsScreen(ScreenInfo screen) {
		for (ScreenRenderListener listener : this.screens.snapshot()) {
			if (listener.supports(screen)) {
				return true;
			}
		}
		return false;
	}

	public void fireScreen(ScreenInfo screen, ScreenFrameContext frame, UiRenderContext ui) {
		for (ScreenRenderListener listener : this.screens.snapshot()) {
			if (listener.supports(screen)) {
				listener.render(frame, ui);
			}
		}
	}

	public void fireWorld(LevelFrameContext frame, UiRenderContext ui) {
		for (WorldRenderListener listener : this.world.snapshot()) {
			listener.render(frame, ui);
		}
	}

	public void fireResize(ResizeContext context) {
		for (ResizeListener listener : this.resize.snapshot()) {
			listener.resize(context);
		}
	}

	public void fireUpdate(UpdateContext context) {
		for (UpdateListener listener : this.update.snapshot()) {
			listener.update(context);
		}
	}

	private static final class ListenerList<T> {
		private final List<T> listeners = new ArrayList<>();

		private void add(T listener) {
			this.listeners.add(Objects.requireNonNull(listener, "listener"));
		}

		private boolean isNotEmpty() {
			return !this.listeners.isEmpty();
		}

		private List<T> snapshot() {
			return List.copyOf(this.listeners);
		}
	}
}
