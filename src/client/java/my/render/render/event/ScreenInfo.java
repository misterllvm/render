package my.render.render.event;

import java.util.Objects;

public record ScreenInfo(String id, boolean pauseScreen) {
	public ScreenInfo {
		Objects.requireNonNull(id, "id");

		if (id.isBlank()) {
			throw new IllegalArgumentException("id must not be blank");
		}
	}
}
