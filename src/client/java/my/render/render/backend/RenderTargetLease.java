package my.render.render.backend;

import my.render.render.effect.EffectChain;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RenderTargetLease implements AutoCloseable {
	private final int targetId;
	private final int width;
	private final int height;
	private final EffectChain effects;
	private final ExecutionTarget target;
	private final Runnable releaser;
	private final AtomicBoolean closed = new AtomicBoolean();

	RenderTargetLease(int targetId, int width, int height, EffectChain effects, ExecutionTarget target, Runnable releaser) {
		this.targetId = targetId;
		this.width = width;
		this.height = height;
		this.effects = Objects.requireNonNull(effects, "effects");
		this.target = Objects.requireNonNull(target, "target");
		this.releaser = Objects.requireNonNull(releaser, "releaser");
	}

	public int targetId() {
		return this.targetId;
	}

	public int width() {
		return this.width;
	}

	public int height() {
		return this.height;
	}

	public EffectChain effects() {
		return this.effects;
	}

	ExecutionTarget target() {
		return this.target;
	}

	public boolean isClosed() {
		return this.closed.get();
	}

	@Override
	public void close() {
		if (!this.closed.compareAndSet(false, true)) {
			return;
		}
		boolean released = false;
		try {
			this.releaser.run();
			released = true;
		} finally {
			if (!released) {
				this.closed.set(false);
			}
		}
	}
}
