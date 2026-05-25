package my.render.render.backend.material;

public final class MaterialFlags {
	public static final int NONE = 0;
	public static final int ANALYTIC_SHAPE = 1 << 0;
	public static final int SAMPLED = 1 << 1;
	public static final int TEXT = 1 << 2;
	public static final int MSDF = 1 << 3;
	public static final int OFFSCREEN_INPUT = 1 << 4;
	public static final int OFFSCREEN_OUTPUT = 1 << 5;
	public static final int EFFECT_STAGE = 1 << 7;
	public static final int SHADOW = 1 << 8;
	public static final int BORDER = 1 << 9;

	private MaterialFlags() {
	}

	public static int of(int... flags) {
		int combined = NONE;
		for (int flag : flags) {
			combined |= flag;
		}
		return combined;
	}

	public static boolean has(int flags, int flag) {
		return (flags & flag) == flag;
	}
}
