package my.render.events.impl;

import my.render.render.event.UpdateContext;

import java.util.Objects;

public record TickEvent(UpdateContext context) {
	public TickEvent {
		Objects.requireNonNull(context, "context");
	}
}
