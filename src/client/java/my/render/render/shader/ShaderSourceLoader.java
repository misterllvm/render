package my.render.render.shader;

import my.render.render.core.ResourceId;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class ShaderSourceLoader {
	public String load(ResourceId shaderId) {
		Objects.requireNonNull(shaderId, "shaderId");
		String classpathLocation = "assets/" + shaderId.namespace() + "/" + shaderId.path();

		try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream(classpathLocation)) {
			if (stream == null) {
				throw new IllegalStateException("Missing shader resource: " + shaderId.asString());
			}
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to load shader resource: " + shaderId.asString(), exception);
		}
	}
}
