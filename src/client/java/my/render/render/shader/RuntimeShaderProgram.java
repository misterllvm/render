package my.render.render.shader;

import org.lwjgl.opengl.GL20C;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RuntimeShaderProgram implements AutoCloseable {
	private final int programId;
	private final int positionLocation;
	private final int colorLocation;
	private final int uv0Location;
	private final int data0Location;
	private final int data1Location;
	private final int data2Location;
	private final int data3Location;
	private final Map<String, Integer> uniformLocations;

	public RuntimeShaderProgram(
		int programId,
		int positionLocation,
		int colorLocation,
		int uv0Location,
		int data0Location,
		int data1Location,
		int data2Location,
		int data3Location,
		Map<String, Integer> uniformLocations
	) {
		if (programId <= 0) {
			throw new IllegalArgumentException("programId must be positive");
		}
		Objects.requireNonNull(uniformLocations, "uniformLocations");

		this.programId = programId;
		this.positionLocation = positionLocation;
		this.colorLocation = colorLocation;
		this.uv0Location = uv0Location;
		this.data0Location = data0Location;
		this.data1Location = data1Location;
		this.data2Location = data2Location;
		this.data3Location = data3Location;
		this.uniformLocations = Map.copyOf(new LinkedHashMap<>(uniformLocations));
	}

	public int programId() {
		return this.programId;
	}

	public int positionLocation() {
		return this.positionLocation;
	}

	public int colorLocation() {
		return this.colorLocation;
	}

	public int uv0Location() {
		return this.uv0Location;
	}

	public int data0Location() {
		return this.data0Location;
	}

	public int data1Location() {
		return this.data1Location;
	}

	public int data2Location() {
		return this.data2Location;
	}

	public int data3Location() {
		return this.data3Location;
	}

	public int uniformLocation(String name) {
		Objects.requireNonNull(name, "name");
		return this.uniformLocations.getOrDefault(name, -1);
	}

	@Override
	public void close() {
		GL20C.glDeleteProgram(this.programId);
	}
}
