package my.render.render.frame;

import java.util.Optional;

@FunctionalInterface
public interface WorldProjection {
	Optional<ProjectedPoint> project(double x, double y, double z);

	static WorldProjection unavailable() {
		return (x, y, z) -> Optional.empty();
	}

	record ProjectedPoint(float x, float y, float depth) {
	}
}
