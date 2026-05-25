package my.render.render.backend.plan;

import my.render.render.backend.ExecutionFamily;
import my.render.render.backend.plan.SubmissionCompiler.*;
import my.render.render.base.UiRect;
import my.render.render.effect.EffectChain;
import my.render.render.frame.UiFrameContext;
import my.render.render.pipeline.PipelineSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BatchPlanner {
	public PlannedSubmission plan(CompiledSubmission submission) {
		Objects.requireNonNull(submission, "submission");
		return new PlannedSubmission(submission.frame(), this.planPass(submission.rootPass()));
	}

	private PlannedPass planPass(CompiledPass pass) {
		List<PlannedStep> plannedSteps = new ArrayList<>();
		List<ResolvedDraw> openBatch = new ArrayList<>();

		for (CompiledStep step : pass.steps()) {
			if (step instanceof DrawStep drawStep) {
				ResolvedDraw draw = drawStep.draw();
				if (this.canMerge(openBatch, draw)) {
					openBatch.add(draw);
				} else {
					this.flush(openBatch, plannedSteps);
					openBatch.add(draw);
				}
				continue;
			}

			if (step instanceof ScissorMarker scissorMarker) {
				this.flush(openBatch, plannedSteps);
				plannedSteps.add(new ScissorStep(scissorMarker.push(), scissorMarker.scissorId(), scissorMarker.rect()));
				continue;
			}

			if (step instanceof ChildPassStep childPassStep) {
				this.flush(openBatch, plannedSteps);
				plannedSteps.add(new ChildStep(this.planPass(childPassStep.child())));
			}
		}

		this.flush(openBatch, plannedSteps);
		return new PlannedPass(pass.passId(), pass.targetKind(), pass.bounds(), pass.effects(), pass.initialScissor(), plannedSteps);
	}

	private void flush(List<ResolvedDraw> openBatch, List<PlannedStep> plannedSteps) {
		if (openBatch.isEmpty()) {
			return;
		}
		UiRect scissor = openBatch.getFirst().scissorRect();
		plannedSteps.add(new BatchStep(new RenderBatch(openBatch), scissor));
		openBatch.clear();
	}

	private boolean canMerge(List<ResolvedDraw> openBatch, ResolvedDraw next) {
		if (openBatch.isEmpty()) {
			return true;
		}

		ResolvedDraw first = openBatch.getFirst();
		if (first.material().executionFamily() == ExecutionFamily.POST_PROCESS) {
			return false;
		}
		if (first.material().executionFamily() != next.material().executionFamily()) {
			return false;
		}
		if (!first.material().batchable() || !next.material().batchable()) {
			return false;
		}
		if (first.material().dedicated() || next.material().dedicated()) {
			return false;
		}
		return first.pipelineKey().equals(next.pipelineKey())
			&& first.material().batchUniformKey() == next.material().batchUniformKey()
			&& first.material().textureStateKey() == next.material().textureStateKey()
			&& first.scissorId() == next.scissorId()
			&& first.dependencyId() == next.dependencyId();
	}

	public record PlannedSubmission(UiFrameContext frame, PlannedPass rootPass) {
		public PlannedSubmission {
			Objects.requireNonNull(frame, "frame");
			Objects.requireNonNull(rootPass, "rootPass");
		}
	}

	public record PlannedPass(int passId, PipelineSpec.TargetKind targetKind, UiRect bounds, EffectChain effects, UiRect initialScissor, List<PlannedStep> steps) {
		public PlannedPass {
			Objects.requireNonNull(targetKind, "targetKind");
			Objects.requireNonNull(bounds, "bounds");
			Objects.requireNonNull(effects, "effects");
			steps = List.copyOf(steps);
		}
	}

	public sealed interface PlannedStep permits BatchStep, ScissorStep, ChildStep {
	}

	public record BatchStep(RenderBatch batch, UiRect scissor) implements PlannedStep {
		public BatchStep {
			Objects.requireNonNull(batch, "batch");
		}
	}

	public record ScissorStep(boolean push, int scissorId, UiRect rect) implements PlannedStep {
		public ScissorStep {
			if (scissorId < 0) {
				throw new IllegalArgumentException("scissorId must be non-negative");
			}
		}
	}

	public record ChildStep(PlannedPass child) implements PlannedStep {
		public ChildStep {
			Objects.requireNonNull(child, "child");
		}
	}
}
