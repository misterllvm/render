package my.render.render.event;

@FunctionalInterface
public interface UpdateListener {
	void update(UpdateContext context);
}
