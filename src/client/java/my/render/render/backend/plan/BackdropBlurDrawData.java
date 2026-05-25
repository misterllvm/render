package my.render.render.backend.plan;

import my.render.render.base.CornerRadii;
import my.render.render.base.RgbaColor;
import my.render.render.base.UiRect;

import java.util.Objects;

public record BackdropBlurDrawData(
	UiRect rectPx,
	CornerRadii radiiPx,
	float blurRadiusPx,
	RgbaColor tint
) implements CompiledDrawData {
	public BackdropBlurDrawData {
		Objects.requireNonNull(rectPx, "rectPx");
		Objects.requireNonNull(radiiPx, "radiiPx");
		Objects.requireNonNull(tint, "tint");
		if (blurRadiusPx <= 0.0F) {
			throw new IllegalArgumentException("blurRadiusPx must be positive");
		}
	}
}
