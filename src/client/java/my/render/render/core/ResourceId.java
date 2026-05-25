package my.render.render.core;

import java.util.Objects;

public record ResourceId(String namespace, String path) {
	public ResourceId {
		Objects.requireNonNull(namespace, "namespace");
		Objects.requireNonNull(path, "path");

		if (namespace.isBlank()) {
			throw new IllegalArgumentException("namespace must not be blank");
		}
		if (path.isBlank()) {
			throw new IllegalArgumentException("path must not be blank");
		}
	}

	public static ResourceId of(String namespace, String path) {
		return new ResourceId(namespace, path);
	}

	public static ResourceId parse(String value) {
		Objects.requireNonNull(value, "value");
		int separator = value.indexOf(':');
		if (separator <= 0 || separator == value.length() - 1) {
			throw new IllegalArgumentException("Resource id must be in namespace:path form");
		}
		return new ResourceId(value.substring(0, separator), value.substring(separator + 1));
	}

	public String asString() {
		return this.namespace + ":" + this.path;
	}

	@Override
	public String toString() {
		return this.asString();
	}
}
