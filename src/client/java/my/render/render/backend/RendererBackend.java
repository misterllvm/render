package my.render.render.backend;

import my.render.render.backend.diagnostics.RendererDiagnostics;
import my.render.render.backend.diagnostics.SubmissionStats;
import my.render.render.backend.material.MaterialResolver;
import my.render.render.backend.plan.BatchPlanner;
import my.render.render.backend.plan.BatchPlanner.*;
import my.render.render.backend.plan.SubmissionCompiler;
import my.render.render.backend.plan.SubmissionCompiler.CompiledSubmission;
import my.render.render.base.UiRect;
import my.render.render.base.UiRenderContext;
import my.render.render.frame.UiFrameContext;
import my.render.render.pipeline.PipelineLibrary;
import my.render.render.pipeline.PipelineSpec;
import my.render.render.shader.ShaderSourceLoader;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RendererBackend implements AutoCloseable {
	private final GpuResourceArena resources;
	private final PipelineLibrary pipelines;
	private final OffscreenPassManager offscreenPasses;
	private final PipelineBinder pipelineBinder;
	private final GpuCommandEncoder commandEncoder;
	private final RendererDiagnostics diagnostics;
	private final MaterialResolver materialResolver;
	private final SubmissionCompiler submissionCompiler;
	private final BatchPlanner batchPlanner;
	private final AnalyticQuadBatchEncoder analyticQuadBatchEncoder;
	private final AnalyticQuadExecutor analyticQuadExecutor;
	private final SampledQuadBatchEncoder sampledQuadBatchEncoder;
	private final SampledQuadExecutor sampledQuadExecutor;
	private final TextQuadBatchEncoder textQuadBatchEncoder;
	private final TextQuadExecutor textQuadExecutor;
	private final PostProcessBatchEncoder postProcessBatchEncoder;
	private final PostProcessExecutor postProcessExecutor;
	private final BackdropBlurBatchEncoder backdropBlurBatchEncoder;
	private final BackdropBlurExecutor backdropBlurExecutor;
	private final MaterialExecutionRouter executionRouter;
	private final EffectChainExecutor effectChainExecutor;
	private final AtomicBoolean closed = new AtomicBoolean();

	public RendererBackend(GpuResourceArena resources, PipelineLibrary pipelines, OffscreenPassManager offscreenPasses) {
		this.resources = Objects.requireNonNull(resources, "resources");
		this.pipelines = Objects.requireNonNull(pipelines, "pipelines");
		this.offscreenPasses = Objects.requireNonNull(offscreenPasses, "offscreenPasses");
		this.pipelineBinder = new PipelineBinder(resources, pipelines, new ShaderSourceLoader());
		this.commandEncoder = new GpuCommandEncoder(resources);
		this.diagnostics = new RendererDiagnostics();
		this.materialResolver = new MaterialResolver();
		this.submissionCompiler = new SubmissionCompiler(this.pipelines, this.materialResolver);
		this.batchPlanner = new BatchPlanner();
		this.analyticQuadBatchEncoder = new AnalyticQuadBatchEncoder();
		this.analyticQuadExecutor = new AnalyticQuadExecutor(this.pipelineBinder, this.commandEncoder, this.analyticQuadBatchEncoder);
		this.sampledQuadBatchEncoder = new SampledQuadBatchEncoder();
		this.sampledQuadExecutor = new SampledQuadExecutor(this.resources, this.pipelineBinder, this.commandEncoder, this.sampledQuadBatchEncoder);
		this.textQuadBatchEncoder = new TextQuadBatchEncoder();
		this.textQuadExecutor = new TextQuadExecutor(this.resources, this.pipelineBinder, this.commandEncoder, this.textQuadBatchEncoder);
		this.postProcessBatchEncoder = new PostProcessBatchEncoder();
		this.postProcessExecutor = new PostProcessExecutor(this.resources, this.pipelineBinder, this.commandEncoder, this.postProcessBatchEncoder);
		this.backdropBlurBatchEncoder = new BackdropBlurBatchEncoder();
		this.backdropBlurExecutor = new BackdropBlurExecutor(this.pipelineBinder, this.commandEncoder, this.backdropBlurBatchEncoder, this.offscreenPasses);
		this.executionRouter = new MaterialExecutionRouter(this.analyticQuadExecutor, this.sampledQuadExecutor, this.textQuadExecutor, this.postProcessExecutor, this.backdropBlurExecutor);
		this.effectChainExecutor = new EffectChainExecutor(this.offscreenPasses, this.executionRouter);
		this.backdropBlurExecutor.bindEffectChainExecutor(this.effectChainExecutor);
	}

	public GpuResourceArena resources() {
		return this.resources;
	}

	public PipelineLibrary pipelines() {
		return this.pipelines;
	}

	public OffscreenPassManager offscreenPasses() {
		return this.offscreenPasses;
	}

	public RendererDiagnostics diagnostics() {
		return this.diagnostics;
	}

	public DrawCommandBuffer newCommandBuffer() {
		this.ensureOpen();
		return new DrawCommandBuffer();
	}

	public UiRenderContext beginUi(UiFrameContext frame, DrawCommandBuffer commands) {
		this.ensureOpen();
		Objects.requireNonNull(frame, "frame");
		Objects.requireNonNull(commands, "commands");
		return new UiRenderContext(frame.surface(), commands);
	}

	public void submit(UiFrameContext frame, DrawCommandBuffer commands) {
		this.ensureOpen();
		Objects.requireNonNull(frame, "frame");
		Objects.requireNonNull(commands, "commands");
		RenderSystem.assertOnRenderThread();

		List<DrawCommand> commandView = commands.view();
		SubmissionStats stats = this.diagnostics.beginSubmission(frame, commandView.size());

		try {
			CompiledSubmission compiled = this.submissionCompiler.compile(frame, commandView, stats);
			PlannedSubmission planned = this.batchPlanner.plan(compiled);
			try (GlStateSnapshot ignored = GlStateSnapshot.capture()) {
				this.commandEncoder.beginFrame();
				this.executePlannedSubmission(planned, stats);
			}
		} finally {
			this.diagnostics.finishSubmission(stats);
		}
	}

	@Override
	public void close() {
		if (this.closed.compareAndSet(false, true)) {
			this.commandEncoder.close();
			this.pipelineBinder.close();
			this.offscreenPasses.close();
			this.resources.close();
		}
	}

	private void executePlannedSubmission(PlannedSubmission submission, SubmissionStats stats) {
		ExecutionTarget mainTarget = ExecutionTarget.main(submission.frame().surface(), this.diagnostics.captureRecordedDraws());
		this.executePlannedPass(submission.rootPass(), mainTarget, submission.rootPass().bounds(), null, stats);
	}

	private void executePlannedPass(PlannedPass pass, ExecutionTarget destinationTarget, UiRect destinationBounds, UiRect destinationScissor, SubmissionStats stats) {
		if (pass.targetKind() == PipelineSpec.TargetKind.MAIN_COLOR) {
			this.executePlannedPassSteps(pass, destinationTarget, destinationBounds, stats);
			return;
		}
		if (this.skipEmptyScissor(destinationScissor, stats)) {
			return;
		}

		stats.recordOffscreenPass();
		try (RenderTargetLease lease = this.offscreenPasses.acquire(pass.bounds(), pass.effects())) {
			ExecutionTarget offscreenTarget = lease.target();
			this.executePlannedPassSteps(pass, offscreenTarget, pass.bounds(), stats);
			this.effectChainExecutor.execute(pass.effects(), offscreenTarget, pass.bounds(), destinationTarget, destinationBounds, destinationScissor, stats);
		}
	}

	private void executePlannedPassSteps(PlannedPass pass, ExecutionTarget target, UiRect targetBounds, SubmissionStats stats) {
		ArrayDeque<UiRect> scissorStack = new ArrayDeque<>();

		for (PlannedStep step : pass.steps()) {
			if (step instanceof BatchStep batchStep) {
				UiRect scissor = batchStep.scissor();
				if (this.skipEmptyScissor(scissor, stats)) {
					continue;
				}
				this.executionRouter.executeBatch(batchStep.batch(), new ExecutionContext(target, scissor, stats));
				continue;
			}
			if (step instanceof ScissorStep scissorStep) {
				this.applyPlannedScissor(scissorStack, pass.initialScissor(), scissorStep);
				continue;
			}
			if (step instanceof ChildStep childStep) {
				this.executePlannedPass(childStep.child(), target, targetBounds, this.effectiveScissor(scissorStack, pass.initialScissor()), stats);
			}
		}
	}

	private void applyPlannedScissor(ArrayDeque<UiRect> scissorStack, UiRect initialScissor, ScissorStep step) {
		if (step.push()) {
			UiRect next = step.rect();
			if (next == null) {
				throw new IllegalStateException("Planned scissor push is missing a scissor rect");
			}
			UiRect active = this.effectiveScissor(scissorStack, initialScissor);
			if (active != null) {
				next = active.intersection(next);
			}
			scissorStack.push(next);
			return;
		}

		if (scissorStack.isEmpty()) {
			throw new IllegalStateException("Attempted to pop an empty scissor stack during planned execution");
		}
		scissorStack.pop();
	}

	private UiRect effectiveScissor(ArrayDeque<UiRect> scissorStack, UiRect initialScissor) {
		return scissorStack.isEmpty() ? initialScissor : scissorStack.peek();
	}

	private boolean skipEmptyScissor(UiRect scissor, SubmissionStats stats) {
		if (scissor == null || !scissor.isEmpty()) {
			return false;
		}
		stats.recordSkippedDraw();
		return true;
	}

	private void ensureOpen() {
		if (this.closed.get()) {
			throw new IllegalStateException("Renderer backend is closed");
		}
	}
}
