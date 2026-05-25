package my.render.events.impl;

import my.render.render.event.ResizeContext;

import java.util.Objects;

public record ResizeEvent(ResizeContext context) {
	public ResizeEvent {
		Objects.requireNonNull(context, "context");
	}
}
