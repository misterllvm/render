package my.render.events.impl;

import my.render.render.base.UiRenderContext;
import my.render.render.frame.HudFrameContext;

import java.util.Objects;

public record HudRenderEvent(HudFrameContext frame, UiRenderContext ui) {
	public HudRenderEvent {
		Objects.requireNonNull(frame, "frame");
		Objects.requireNonNull(ui, "ui");
	}
}
