package my.render.events.api;

@FunctionalInterface
public interface EventListener<E> {
	void onEvent(E event);
}
