package my.render.render.event;

@FunctionalInterface
public interface ResizeListener {
	void resize(ResizeContext context);
}
