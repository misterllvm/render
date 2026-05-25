package my.render.render.integration;

import my.render.render.backend.DrawCommandBuffer;
import my.render.render.base.UiRenderContext;
import my.render.render.core.RenderRuntime;
import my.render.render.event.*;
import my.render.render.frame.*;
import com.mojang.blaze3d.platform.Window;
import my.render.render.event.*;
import my.render.render.frame.*;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RenderController implements AutoCloseable {
	private final RenderRuntime runtime;
	private final RenderEvents events = new RenderEvents();
	private final List<QueuedUiSubmission<HudFrameContext>> queuedHud = new ArrayList<>();
	private final List<QueuedUiSubmission<ScreenFrameContext>> queuedScreens = new ArrayList<>();
	private final AtomicBoolean closed = new AtomicBoolean();
	private long tickNumber;
	private SurfaceMetrics lastSurface;

	public RenderController(RenderRuntime runtime) {
		this.runtime = Objects.requireNonNull(runtime, "runtime");
	}

	public RenderRuntime runtime() {
		return this.runtime;
	}

	public RenderEvents events() {
		return this.events;
	}

	public void onHud(HudRenderListener listener) {
		this.events.onHud(listener);
	}

	public void onScreen(ScreenRenderListener listener) {
		this.events.onScreen(listener);
	}

	public void onWorld(WorldRenderListener listener) {
		this.events.onWorld(listener);
	}

	public void onResize(ResizeListener listener) {
		this.events.onResize(listener);
	}

	public void onUpdate(UpdateListener listener) {
		this.events.onUpdate(listener);
	}

	public void captureHud(DeltaTracker deltaTracker) {
		Objects.requireNonNull(deltaTracker, "deltaTracker");
		if (this.isClosed() || !this.events.hasHudListeners()) {
			return;
		}

		FrameCoordinator frames = this.runtime.frames();
		SurfaceMetrics surface = this.surface();
		Minecraft client = Minecraft.getInstance();
		FrameInfo info = frames.beginFrame(deltaTracker.getGameTimeDeltaPartialTick(false), client.isPaused());
		HudFrameContext frame = frames.hud(info, surface);
		DrawCommandBuffer commands = this.runtime.backend().newCommandBuffer();
		UiRenderContext ui = this.runtime.backend().beginUi(frame, commands);
		this.events.fireHud(frame, ui);

		if (!commands.isEmpty()) {
			this.queuedHud.add(new QueuedUiSubmission<>(frame, commands));
		}
	}

	public void captureScreen(Screen screen, int mouseX, int mouseY, float tickProgress) {
		Objects.requireNonNull(screen, "screen");
		if (this.isClosed() || !this.events.hasScreenListeners()) {
			return;
		}

		ScreenInfo screenInfo = new ScreenInfo(screen.getClass().getName(), screen.isPauseScreen());
		if (!this.events.supportsScreen(screenInfo)) {
			return;
		}

		FrameCoordinator frames = this.runtime.frames();
		SurfaceMetrics surface = this.surface();
		Minecraft client = Minecraft.getInstance();
		FrameInfo info = frames.beginFrame(tickProgress, client.isPaused());
		ScreenFrameContext frame = frames.screen(info, surface, screenInfo.id(), screenInfo.pauseScreen(), mouseX, mouseY);
		DrawCommandBuffer commands = this.runtime.backend().newCommandBuffer();
		UiRenderContext ui = this.runtime.backend().beginUi(frame, commands);
		this.events.fireScreen(screenInfo, frame, ui);

		if (!commands.isEmpty()) {
			this.queuedScreens.add(new QueuedUiSubmission<>(frame, commands));
		}
	}

	public void flushQueuedUi() {
		if (this.isClosed()) {
			return;
		}

		this.flushQueue(this.queuedScreens);
		this.flushQueue(this.queuedHud);
	}

	public void renderLevel(LevelRenderContext context) {
		Objects.requireNonNull(context, "context");
		if (this.isClosed() || !this.events.hasWorldListeners()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		FrameCoordinator frames = this.runtime.frames();
		SurfaceMetrics surface = this.surface();
		FrameInfo info = frames.beginFrame(client.getDeltaTracker().getGameTimeDeltaPartialTick(false), client.isPaused());
		LevelFrameContext frame = frames.level(info, surface, new WorldProjectionBridge(context, surface));
		DrawCommandBuffer commands = this.runtime.backend().newCommandBuffer();
		UiRenderContext ui = this.runtime.backend().beginUi(frame, commands);
		this.events.fireWorld(frame, ui);

		if (!commands.isEmpty()) {
			this.runtime.backend().submit(frame, commands);
		}
	}

	public void update() {
		if (this.isClosed()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		SurfaceMetrics currentSurface = this.surface();
		if (this.lastSurface != null && this.surfaceChanged(this.lastSurface, currentSurface)) {
			this.events.fireResize(new ResizeContext(this.lastSurface, currentSurface));
		}
		this.lastSurface = currentSurface;
		this.events.fireUpdate(new UpdateContext(
			this.tickNumber++,
			client.getDeltaTracker().getGameTimeDeltaPartialTick(false),
			client.isPaused(),
			currentSurface
		));
	}

	public void reload() {
		if (this.isClosed()) {
			return;
		}

		this.queuedHud.clear();
		this.queuedScreens.clear();
		this.lastSurface = null;
		this.runtime.reload();
	}

	@Override
	public void close() {
		if (this.closed.compareAndSet(false, true)) {
			this.queuedHud.clear();
			this.queuedScreens.clear();
			this.runtime.close();
		}
	}

	private boolean isClosed() {
		return this.closed.get();
	}

	private SurfaceMetrics surface() {
		Window window = Minecraft.getInstance().getWindow();
		int framebufferWidth = Math.max(1, window.getWidth());
		int framebufferHeight = Math.max(1, window.getHeight());
		int guiWidth = Math.max(1, window.getGuiScaledWidth());
		int guiHeight = Math.max(1, window.getGuiScaledHeight());
		double guiScale = Math.max(window.getGuiScale(), 1.0D);
		return this.runtime.frames().surface(
			framebufferWidth,
			framebufferHeight,
			guiWidth,
			guiHeight,
			guiScale
		);
	}

	private boolean surfaceChanged(SurfaceMetrics previousSurface, SurfaceMetrics currentSurface) {
		return previousSurface.framebufferWidth() != currentSurface.framebufferWidth()
			|| previousSurface.framebufferHeight() != currentSurface.framebufferHeight()
			|| previousSurface.guiWidth() != currentSurface.guiWidth()
			|| previousSurface.guiHeight() != currentSurface.guiHeight()
			|| previousSurface.guiScale() != currentSurface.guiScale();
	}

	private <T extends UiFrameContext> void flushQueue(List<QueuedUiSubmission<T>> queue) {
		List<QueuedUiSubmission<T>> pending = List.copyOf(queue);
		queue.clear();

		for (QueuedUiSubmission<T> submission : pending) {
			this.runtime.backend().submit(submission.frame(), submission.commands());
		}
	}

	private record QueuedUiSubmission<T extends UiFrameContext>(T frame, DrawCommandBuffer commands) {
		private QueuedUiSubmission {
			Objects.requireNonNull(frame, "frame");
			Objects.requireNonNull(commands, "commands");
		}
	}
}
