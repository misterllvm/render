package my.render.render.backend.plan;

import my.render.render.base.CornerRadii;
import my.render.render.base.Gradient4;
import my.render.render.base.UiRect;

import java.util.Objects;

public record AnalyticQuadDrawData(
	AnalyticQuadVariant variant,
	UiRect shapeRectPx,
	UiRect geometryRectPx,
	CornerRadii radiiPx,
	Gradient4 gradient,
	float strokeWidthPx,
	float strokeAlignCode,
	float shadowSoftnessPx
) implements CompiledDrawData {
	public AnalyticQuadDrawData {
		Objects.requireNonNull(variant, "variant");
		Objects.requireNonNull(shapeRectPx, "shapeRectPx");
		Objects.requireNonNull(geometryRectPx, "geometryRectPx");
		Objects.requireNonNull(radiiPx, "radiiPx");
		Objects.requireNonNull(gradient, "gradient");
	}

	public static AnalyticQuadDrawData fill(UiRect rect, Gradient4 gradient) {
		return new AnalyticQuadDrawData(
			AnalyticQuadVariant.FILL,
			rect,
			rect,
			CornerRadii.NONE,
			gradient,
			0.0F,
			0.0F,
			0.0F
		);
	}

	public static AnalyticQuadDrawData roundedFill(UiRect rect, CornerRadii radii, Gradient4 gradient) {
		return new AnalyticQuadDrawData(
			AnalyticQuadVariant.ROUNDED_FILL,
			rect,
			rect,
			radii,
			gradient,
			0.0F,
			0.0F,
			0.0F
		);
	}

	public static AnalyticQuadDrawData border(UiRect shapeRect, UiRect geometryRect, CornerRadii radii, Gradient4 gradient, float strokeWidthPx, float strokeAlignCode) {
		return new AnalyticQuadDrawData(
			AnalyticQuadVariant.BORDER,
			shapeRect,
			geometryRect,
			radii,
			gradient,
			strokeWidthPx,
			strokeAlignCode,
			0.0F
		);
	}

	public static AnalyticQuadDrawData shadow(UiRect shapeRect, UiRect geometryRect, CornerRadii radii, Gradient4 gradient, float shadowSoftnessPx) {
		return new AnalyticQuadDrawData(
			AnalyticQuadVariant.SHADOW,
			shapeRect,
			geometryRect,
			radii,
			gradient,
			0.0F,
			0.0F,
			shadowSoftnessPx
		);
	}
}
