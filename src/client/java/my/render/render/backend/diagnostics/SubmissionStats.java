package my.render.render.backend.diagnostics;

import my.render.render.frame.UiFrameContext;

import java.util.Objects;

public final class SubmissionStats {
	private final String frameContextName;
	private final int surfaceWidth;
	private final int surfaceHeight;
	private final int rawCommandCount;
	private final long submissionStartedNanos;
	private long submissionFinishedNanos;
	private int resolvedDrawCount;
	private int compiledPassCount;
	private int drawCallCount;
	private int skippedDrawCount;
	private int recordedDrawCount;
	private int textureSwitchCount;
	private int pipelineSwitchCount;
	private int offscreenPassCount;
	private Object lastPipelineMarker;
	private Object lastTextureMarker;
	private long shadowCompileStartedNanos;
	private long shadowCompileFinishedNanos;
	private boolean shadowCompileAttempted;
	private boolean shadowCompileSucceeded;
	private String shadowCompileFailure;

	private SubmissionStats(String frameContextName, int surfaceWidth, int surfaceHeight, int rawCommandCount, long submissionStartedNanos) {
		this.frameContextName = Objects.requireNonNull(frameContextName, "frameContextName");
		this.surfaceWidth = surfaceWidth;
		this.surfaceHeight = surfaceHeight;
		this.rawCommandCount = rawCommandCount;
		this.submissionStartedNanos = submissionStartedNanos;
	}

	public static SubmissionStats begin(UiFrameContext frame, int rawCommandCount) {
		Objects.requireNonNull(frame, "frame");
		return new SubmissionStats(
			frame.getClass().getSimpleName(),
			frame.surface().guiWidth(),
			frame.surface().guiHeight(),
			rawCommandCount,
			System.nanoTime()
		);
	}

	public String frameContextName() {
		return this.frameContextName;
	}

	public int surfaceWidth() {
		return this.surfaceWidth;
	}

	public int surfaceHeight() {
		return this.surfaceHeight;
	}

	public int rawCommandCount() {
		return this.rawCommandCount;
	}

	public int resolvedDrawCount() {
		return this.resolvedDrawCount;
	}

	public int compiledPassCount() {
		return this.compiledPassCount;
	}

	public int drawCallCount() {
		return this.drawCallCount;
	}

	public int skippedDrawCount() {
		return this.skippedDrawCount;
	}

	public int recordedDrawCount() {
		return this.recordedDrawCount;
	}

	public int textureSwitchCount() {
		return this.textureSwitchCount;
	}

	public int pipelineSwitchCount() {
		return this.pipelineSwitchCount;
	}

	public int offscreenPassCount() {
		return this.offscreenPassCount;
	}

	public boolean shadowCompileAttempted() {
		return this.shadowCompileAttempted;
	}

	public boolean shadowCompileSucceeded() {
		return this.shadowCompileSucceeded;
	}

	public String shadowCompileFailure() {
		return this.shadowCompileFailure;
	}

	public long shadowCompileNanos() {
		if (!this.shadowCompileAttempted || this.shadowCompileFinishedNanos <= this.shadowCompileStartedNanos) {
			return 0L;
		}
		return this.shadowCompileFinishedNanos - this.shadowCompileStartedNanos;
	}

	public long totalSubmissionNanos() {
		if (this.submissionFinishedNanos <= this.submissionStartedNanos) {
			return 0L;
		}
		return this.submissionFinishedNanos - this.submissionStartedNanos;
	}

	public double totalSubmissionMillis() {
		return this.totalSubmissionNanos() / 1_000_000.0D;
	}

	public void beginShadowCompile() {
		this.shadowCompileAttempted = true;
		this.shadowCompileSucceeded = false;
		this.shadowCompileFailure = null;
		this.shadowCompileStartedNanos = System.nanoTime();
		this.shadowCompileFinishedNanos = 0L;
	}

	public void finishShadowCompile(int resolvedDrawCount, int compiledPassCount) {
		this.shadowCompileSucceeded = true;
		this.shadowCompileFinishedNanos = System.nanoTime();
		this.resolvedDrawCount = Math.max(this.resolvedDrawCount, resolvedDrawCount);
		this.compiledPassCount = Math.max(this.compiledPassCount, compiledPassCount);
	}

	public void failShadowCompile(Throwable throwable) {
		Objects.requireNonNull(throwable, "throwable");
		this.shadowCompileSucceeded = false;
		this.shadowCompileFinishedNanos = System.nanoTime();
		this.shadowCompileFailure = throwable.getClass().getSimpleName() + ": " + Objects.toString(throwable.getMessage(), "<no message>");
	}

	public void recordDrawCall() {
		this.drawCallCount++;
	}

	public void recordSkippedDraw() {
		this.skippedDrawCount++;
	}

	public void recordRecordedDraw() {
		this.recordedDrawCount++;
	}

	public void recordTextureSwitch() {
		this.textureSwitchCount++;
	}

	public void recordPipelineSwitch() {
		this.pipelineSwitchCount++;
	}

	public void recordOffscreenPass() {
		this.offscreenPassCount++;
	}

	public void recordPipelineUse(Object marker) {
		if (marker == null) {
			return;
		}
		if (this.lastPipelineMarker != null && !this.lastPipelineMarker.equals(marker)) {
			this.pipelineSwitchCount++;
		}
		this.lastPipelineMarker = marker;
	}

	public void recordTextureUse(Object marker) {
		if (marker == null) {
			return;
		}
		if (this.lastTextureMarker != null && !this.lastTextureMarker.equals(marker)) {
			this.textureSwitchCount++;
		}
		this.lastTextureMarker = marker;
	}

	public void finishSubmission() {
		this.submissionFinishedNanos = System.nanoTime();
	}

	public SubmissionStats snapshot() {
		SubmissionStats snapshot = new SubmissionStats(
			this.frameContextName,
			this.surfaceWidth,
			this.surfaceHeight,
			this.rawCommandCount,
			this.submissionStartedNanos
		);
		snapshot.submissionFinishedNanos = this.submissionFinishedNanos;
		snapshot.resolvedDrawCount = this.resolvedDrawCount;
		snapshot.compiledPassCount = this.compiledPassCount;
		snapshot.drawCallCount = this.drawCallCount;
		snapshot.skippedDrawCount = this.skippedDrawCount;
		snapshot.recordedDrawCount = this.recordedDrawCount;
		snapshot.textureSwitchCount = this.textureSwitchCount;
		snapshot.pipelineSwitchCount = this.pipelineSwitchCount;
		snapshot.offscreenPassCount = this.offscreenPassCount;
		snapshot.shadowCompileStartedNanos = this.shadowCompileStartedNanos;
		snapshot.shadowCompileFinishedNanos = this.shadowCompileFinishedNanos;
		snapshot.shadowCompileAttempted = this.shadowCompileAttempted;
		snapshot.shadowCompileSucceeded = this.shadowCompileSucceeded;
		snapshot.shadowCompileFailure = this.shadowCompileFailure;
		return snapshot;
	}
}
