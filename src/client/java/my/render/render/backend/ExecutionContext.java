package my.render.render.backend;

import my.render.render.backend.diagnostics.SubmissionStats;
import my.render.render.base.UiRect;

import java.util.Objects;

record ExecutionContext(
	ExecutionTarget target,
	UiRect scissor,
	SubmissionStats stats
) {
	ExecutionContext {
		Objects.requireNonNull(target, "target");
		Objects.requireNonNull(stats, "stats");
	}
}
