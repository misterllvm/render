package my.render.render.backend;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@FunctionalInterface
public interface RenderStateGuard extends AutoCloseable {
	@Override
	void close();

	static RenderStateGuard once(Runnable closer) {
		Objects.requireNonNull(closer, "closer");
		AtomicBoolean closed = new AtomicBoolean();
		return () -> {
			if (closed.compareAndSet(false, true)) {
				closer.run();
			}
		};
	}

	static RenderStateGuard noop() {
		return () -> {
		};
	}
}
