package my.render.events.api;

import java.util.*;

public final class EventBus {
	private final Map<Class<?>, List<RegisteredListener<?>>> listeners = new LinkedHashMap<>();

	public synchronized <E> Subscription subscribe(Class<E> eventType, EventListener<? super E> listener) {
		Objects.requireNonNull(eventType, "eventType");
		Objects.requireNonNull(listener, "listener");
		RegisteredListener<E> registered = new RegisteredListener<>(listener);
		this.listeners.computeIfAbsent(eventType, key -> new ArrayList<>()).add(registered);
		return () -> this.unsubscribe(eventType, registered);
	}

	public synchronized boolean hasSubscribers(Class<?> eventType) {
		Objects.requireNonNull(eventType, "eventType");
		List<RegisteredListener<?>> registered = this.listeners.get(eventType);
		return registered != null && !registered.isEmpty();
	}

	public <E> void post(E event) {
		Objects.requireNonNull(event, "event");
		List<RegisteredListener<?>> snapshot;
		synchronized (this) {
			List<RegisteredListener<?>> registered = this.listeners.get(event.getClass());
			snapshot = registered == null ? List.of() : List.copyOf(registered);
		}
		for (RegisteredListener<?> listener : snapshot) {
			@SuppressWarnings("unchecked")
			RegisteredListener<E> typedListener = (RegisteredListener<E>) listener;
			typedListener.listener().onEvent(event);
		}
	}

	private synchronized <E> void unsubscribe(Class<E> eventType, RegisteredListener<E> listener) {
		List<RegisteredListener<?>> registered = this.listeners.get(eventType);
		if (registered == null) {
			return;
		}
		registered.remove(listener);
		if (registered.isEmpty()) {
			this.listeners.remove(eventType);
		}
	}

	private record RegisteredListener<E>(EventListener<? super E> listener) {
		private RegisteredListener {
			Objects.requireNonNull(listener, "listener");
		}
	}
}
