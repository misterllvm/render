package my.render.render.frame;

public record FrameInfo(long frameNumber, float partialTick, boolean paused) {
	public FrameInfo {
		if (frameNumber < 0L) {
			throw new IllegalArgumentException("frameNumber must be non-negative");
		}
	}
}
