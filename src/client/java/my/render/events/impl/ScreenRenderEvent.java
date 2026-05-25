package my.render.events.impl;

import my.render.render.base.UiRenderContext;
import my.render.render.event.ScreenInfo;
import my.render.render.frame.ScreenFrameContext;

import java.util.Objects;

public record ScreenRenderEvent(ScreenInfo screen, ScreenFrameContext frame, UiRenderContext ui) {
	public ScreenRenderEvent {
		Objects.requireNonNull(screen, "screen");
		Objects.requireNonNull(frame, "frame");
		Objects.requireNonNull(ui, "ui");
	}
}
