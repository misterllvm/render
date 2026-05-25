package my.render.render.backend.diagnostics;

import my.render.render.frame.UiFrameContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RendererDiagnostics {
	private static final int DEFAULT_HISTORY_LIMIT = 120;
	private final Object lock = new Object();
	private final int historyLimit;
	private final ArrayDeque<SubmissionStats> history = new ArrayDeque<>();
	private boolean enabled = true;
	private boolean captureRecordedDraws;
	private SubmissionStats lastCompleted;

	public RendererDiagnostics() {
		this(DEFAULT_HISTORY_LIMIT);
	}

	public RendererDiagnostics(int historyLimit) {
		if (historyLimit <= 0) {
			throw new IllegalArgumentException("historyLimit must be positive");
		}
		this.historyLimit = historyLimit;
	}

	public boolean enabled() {
		synchronized (this.lock) {
			return this.enabled;
		}
	}

	public void setEnabled(boolean enabled) {
		synchronized (this.lock) {
			this.enabled = enabled;
		}
	}

	public boolean captureRecordedDraws() {
		synchronized (this.lock) {
			return this.enabled && this.captureRecordedDraws;
		}
	}

	public void setCaptureRecordedDraws(boolean captureRecordedDraws) {
		synchronized (this.lock) {
			this.captureRecordedDraws = captureRecordedDraws;
		}
	}

	public SubmissionStats beginSubmission(UiFrameContext frame, int rawCommandCount) {
		Objects.requireNonNull(frame, "frame");
		return SubmissionStats.begin(frame, rawCommandCount);
	}

	public void finishSubmission(SubmissionStats stats) {
		Objects.requireNonNull(stats, "stats");
		stats.finishSubmission();

		synchronized (this.lock) {
			if (!this.enabled) {
				return;
			}
			SubmissionStats snapshot = stats.snapshot();
			this.lastCompleted = snapshot;
			this.history.addLast(snapshot);
			while (this.history.size() > this.historyLimit) {
				this.history.removeFirst();
			}
		}
	}

	public SubmissionStats lastCompleted() {
		synchronized (this.lock) {
			return this.lastCompleted;
		}
	}

	public List<SubmissionStats> historySnapshot() {
		synchronized (this.lock) {
			return List.copyOf(new ArrayList<>(this.history));
		}
	}
}
