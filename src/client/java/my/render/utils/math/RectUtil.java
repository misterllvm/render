package my.render.utils.math;

import my.render.render.base.UiRect;

import java.util.Objects;

public final class RectUtil {
	private RectUtil() {
	}

	public static UiRect offset(UiRect rect, float dx, float dy) {
		Objects.requireNonNull(rect, "rect");
		return new UiRect(rect.x() + dx, rect.y() + dy, rect.width(), rect.height());
	}

	public static UiRect expand(UiRect rect, float amount) {
		Objects.requireNonNull(rect, "rect");
		return new UiRect(rect.x() - amount, rect.y() - amount, Math.max(0.0F, rect.width() + amount * 2.0F), Math.max(0.0F, rect.height() + amount * 2.0F));
	}
}
