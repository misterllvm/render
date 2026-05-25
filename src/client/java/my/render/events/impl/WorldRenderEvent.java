package my.render.events.impl;

import my.render.render.base.UiRenderContext;
import my.render.render.frame.LevelFrameContext;

import java.util.Objects;

public record WorldRenderEvent(LevelFrameContext frame, UiRenderContext ui) {
	public WorldRenderEvent {
		Objects.requireNonNull(frame, "frame");
		Objects.requireNonNull(ui, "ui");
	}
}
