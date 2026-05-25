package my.render.render.backend;

import my.render.render.base.UiRect;
import my.render.render.effect.EffectChain;
import my.render.render.pipeline.PipelineSpec;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class OffscreenPassManager implements AutoCloseable {
	private final GpuResourceArena resources;
	private final AtomicInteger nextTargetId = new AtomicInteger(1);
	private final Map<TargetKey, ArrayDeque<TargetSlot>> available = new HashMap<>();
	private final AtomicBoolean closed = new AtomicBoolean();

	public OffscreenPassManager(GpuResourceArena resources) {
		this.resources = Objects.requireNonNull(resources, "resources");
	}

	public RenderTargetLease acquire(UiRect bounds, EffectChain effects) {
		this.ensureOpen();
		Objects.requireNonNull(bounds, "bounds");
		Objects.requireNonNull(effects, "effects");
		RenderSystem.assertOnRenderThread();

		int requestedWidth = Math.max(1, (int) Math.ceil(bounds.width()));
		int requestedHeight = Math.max(1, (int) Math.ceil(bounds.height()));
		int width = bucketDimension(requestedWidth);
		int height = bucketDimension(requestedHeight);
		TargetKey key = new TargetKey(width, height);
		ArrayDeque<TargetSlot> pool = this.available.computeIfAbsent(key, ignored -> new ArrayDeque<>());
		TargetSlot slot = pool.pollFirst();
		if (slot == null) {
			int targetId = this.nextTargetId.getAndIncrement();
			RuntimeRenderTarget renderTarget = RuntimeRenderTarget.offscreen(width, height);
			this.resources.registerOffscreenTarget(targetId, renderTarget);
			slot = new TargetSlot(new ExecutionTarget(targetId, PipelineSpec.TargetKind.OFFSCREEN_COLOR, width, height, renderTarget));
		}

		slot.target().clear();
		this.resources.rememberOffscreenTarget(slot.id());
		TargetSlot leased = slot;
		return new RenderTargetLease(leased.id(), leased.width(), leased.height(), effects, leased.target(), () -> this.recycle(leased));
	}

	public void release(RenderTargetLease lease) {
		Objects.requireNonNull(lease, "lease").close();
	}

	public void trim() {
		this.available.clear();
		this.resources.trimTransientTargets();
	}

	@Override
	public void close() {
		if (this.closed.compareAndSet(false, true)) {
			this.available.clear();
			this.resources.trimTransientTargets();
		}
	}

	private void recycle(TargetSlot slot) {
		if (this.closed.get()) {
			return;
		}

		TargetKey key = new TargetKey(slot.width(), slot.height());
		this.available.computeIfAbsent(key, ignored -> new ArrayDeque<>()).addFirst(slot);
	}

	private void ensureOpen() {
		if (this.closed.get()) {
			throw new IllegalStateException("Offscreen pass manager is closed");
		}
	}

	private static int bucketDimension(int size) {
		int alignment;
		if (size <= 256) {
			alignment = 16;
		} else if (size <= 1024) {
			alignment = 64;
		} else {
			alignment = 128;
		}
		int remainder = size % alignment;
		return remainder == 0 ? size : size + (alignment - remainder);
	}

	private record TargetKey(int width, int height) {
	}

	private record TargetSlot(ExecutionTarget target) {
		private int id() {
			return this.target.targetId();
		}

		private int width() {
			return this.target.width();
		}

		private int height() {
			return this.target.height();
		}
	}
}
