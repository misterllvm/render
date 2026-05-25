package my.render.render.backend.plan;

import my.render.render.base.CornerRadii;
import my.render.render.base.Gradient4;
import my.render.render.base.UiRect;
import my.render.render.core.ResourceId;

import java.util.Objects;

public record SampledQuadDrawData(
	ResourceId textureId,
	UiRect rectPx,
	UiRect uvRect,
	CornerRadii radiiPx,
	Gradient4 tint,
	float edgeSoftnessPx
) implements CompiledDrawData {
	public SampledQuadDrawData {
		Objects.requireNonNull(textureId, "textureId");
		Objects.requireNonNull(rectPx, "rectPx");
		Objects.requireNonNull(uvRect, "uvRect");
		Objects.requireNonNull(radiiPx, "radiiPx");
		Objects.requireNonNull(tint, "tint");
		if (edgeSoftnessPx < 0.0F) {
			throw new IllegalArgumentException("edgeSoftnessPx must be non-negative");
		}
	}
}
