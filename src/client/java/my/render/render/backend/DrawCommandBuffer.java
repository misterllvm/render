package my.render.render.backend;

import java.util.*;

public final class DrawCommandBuffer {
	private final List<DrawCommand> commands = new ArrayList<>();

	public void add(DrawCommand command) {
		this.commands.add(Objects.requireNonNull(command, "command"));
	}

	public void addAll(Collection<? extends DrawCommand> commands) {
		Objects.requireNonNull(commands, "commands");
		for (DrawCommand command : commands) {
			this.add(command);
		}
	}

	public int size() {
		return this.commands.size();
	}

	public boolean isEmpty() {
		return this.commands.isEmpty();
	}

	public List<DrawCommand> view() {
		return Collections.unmodifiableList(this.commands);
	}

	public List<DrawCommand> snapshot() {
		return List.copyOf(this.commands);
	}

	public void clear() {
		this.commands.clear();
	}
}
