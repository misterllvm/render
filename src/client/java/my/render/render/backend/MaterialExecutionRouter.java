package my.render.render.backend;

import my.render.render.backend.plan.RenderBatch;

import java.util.Objects;

final class MaterialExecutionRouter {
	private final AnalyticQuadExecutor analyticQuadExecutor;
	private final SampledQuadExecutor sampledQuadExecutor;
	private final TextQuadExecutor textQuadExecutor;
	private final PostProcessExecutor postProcessExecutor;
	private final BackdropBlurExecutor backdropBlurExecutor;

	MaterialExecutionRouter(AnalyticQuadExecutor analyticQuadExecutor, SampledQuadExecutor sampledQuadExecutor, TextQuadExecutor textQuadExecutor, PostProcessExecutor postProcessExecutor, BackdropBlurExecutor backdropBlurExecutor) {
		this.analyticQuadExecutor = Objects.requireNonNull(analyticQuadExecutor, "analyticQuadExecutor");
		this.sampledQuadExecutor = Objects.requireNonNull(sampledQuadExecutor, "sampledQuadExecutor");
		this.textQuadExecutor = Objects.requireNonNull(textQuadExecutor, "textQuadExecutor");
		this.postProcessExecutor = Objects.requireNonNull(postProcessExecutor, "postProcessExecutor");
		this.backdropBlurExecutor = Objects.requireNonNull(backdropBlurExecutor, "backdropBlurExecutor");
	}

	void executeBatch(RenderBatch batch, ExecutionContext context) {
		Objects.requireNonNull(batch, "batch");
		Objects.requireNonNull(context, "context");
		var first = batch.firstDraw();
		switch (first.material().executionFamily()) {
			case ANALYTIC_QUAD -> this.analyticQuadExecutor.execute(batch.draws(), context);
			case SAMPLED_QUAD -> this.sampledQuadExecutor.execute(batch.draws(), context);
			case TEXT_QUAD -> this.textQuadExecutor.execute(batch.draws(), context);
			case BACKDROP_BLUR -> this.backdropBlurExecutor.execute(batch, context);
			case POST_PROCESS -> throw new IllegalStateException("POST_PROCESS draw entered regular batch router path");
		}
	}

	void executePostProcess(PostProcessDrawData draw, ExecutionContext context) {
		Objects.requireNonNull(draw, "draw");
		Objects.requireNonNull(context, "context");
		if (draw.executionFamily() != ExecutionFamily.POST_PROCESS) {
			throw new IllegalStateException(draw.executionFamily() + " draw entered post-process router path");
		}
		this.postProcessExecutor.execute(draw, context);
	}
}
