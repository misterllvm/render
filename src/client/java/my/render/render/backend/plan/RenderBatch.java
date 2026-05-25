package my.render.render.backend.plan;

import my.render.render.backend.ExecutionFamily;

import java.util.List;
import java.util.Objects;

public record RenderBatch(List<ResolvedDraw> draws) {
	public RenderBatch {
		draws = List.copyOf(draws);
		if (draws.isEmpty()) {
			throw new IllegalArgumentException("RenderBatch must not be empty");
		}
		for (ResolvedDraw draw : draws) {
			Objects.requireNonNull(draw, "draw");
		}
	}

	public ResolvedDraw firstDraw() {
		return this.draws.getFirst();
	}

	public ExecutionFamily executionFamily() {
		return this.firstDraw().material().executionFamily();
	}

	public boolean dedicated() {
		return this.firstDraw().material().dedicated();
	}
}
