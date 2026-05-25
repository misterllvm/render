package my.render.render.backend;

import my.render.render.backend.plan.AnalyticQuadDrawData;
import my.render.render.backend.plan.ResolvedDraw;

import java.util.List;
import java.util.Objects;

final class AnalyticQuadExecutor {
	private final PipelineBinder pipelineBinder;
	private final GpuCommandEncoder commandEncoder;
	private final AnalyticQuadBatchEncoder batchEncoder;

	AnalyticQuadExecutor(PipelineBinder pipelineBinder, GpuCommandEncoder commandEncoder, AnalyticQuadBatchEncoder batchEncoder) {
		this.pipelineBinder = Objects.requireNonNull(pipelineBinder, "pipelineBinder");
		this.commandEncoder = Objects.requireNonNull(commandEncoder, "commandEncoder");
		this.batchEncoder = Objects.requireNonNull(batchEncoder, "batchEncoder");
	}

	void execute(List<ResolvedDraw> draws, ExecutionContext context) {
		Objects.requireNonNull(draws, "draws");
		Objects.requireNonNull(context, "context");

		ResolvedDraw first = this.validateBatch(draws);
		PipelineBinding binding = this.pipelineBinder.bind(first.pipelineSpec());
		QuadBufferUpload upload = this.batchEncoder.encode(draws);
		context.stats().recordPipelineUse(first.pipelineKey());
		if (this.commandEncoder.execute(binding, upload, context.target(), context.scissor())) {
			context.stats().recordDrawCall();
		} else {
			context.stats().recordSkippedDraw();
		}
	}

	private ResolvedDraw validateBatch(List<ResolvedDraw> draws) {
		if (draws.isEmpty()) {
			throw new IllegalArgumentException("Analytic quad batch must not be empty");
		}

		ResolvedDraw first = draws.getFirst();
		for (ResolvedDraw draw : draws) {
			if (draw.material().executionFamily() != ExecutionFamily.ANALYTIC_QUAD) {
				throw new IllegalStateException("Non-analytic draw entered AnalyticQuadExecutor");
			}
			if (!(draw.compiledData() instanceof AnalyticQuadDrawData)) {
				throw new IllegalStateException("Analytic draw is missing compiled analytic payload");
			}
			if (!draw.pipelineKey().equals(first.pipelineKey())) {
				throw new IllegalStateException("Analytic batch mixes pipeline variants");
			}
		}
		return first;
	}
}
