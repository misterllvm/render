package my.render.utils.math;

public final class MathUtil {
	private MathUtil() {
	}

	public static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	public static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	public static float clamp01(float value) {
		return clamp(value, 0.0F, 1.0F);
	}

	public static float lerp(float start, float end, float progress) {
		return start + (end - start) * progress;
	}
}
