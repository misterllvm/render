package my.render.render.integration;

import my.render.Best;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class FabricBridge {
	private static final AtomicReference<RenderController> ACTIVE = new AtomicReference<>();
	private static final Identifier HUD_LAYER_ID = Identifier.fromNamespaceAndPath(Best.MOD_ID, "renderer/hud");

	private FabricBridge() {
	}

	public static void initialize(RenderController controller) {
		Objects.requireNonNull(controller, "controller");

		if (!ACTIVE.compareAndSet(null, controller)) {
			throw new IllegalStateException("Render bridge is already initialized");
		}

		HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, HUD_LAYER_ID, (graphics, deltaTracker) -> controller.captureHud(deltaTracker));
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
			ScreenEvents.afterExtract(screen).register((current, graphics, mouseX, mouseY, tickProgress) ->
				controller.captureScreen(current, mouseX, mouseY, tickProgress))
		);
		LevelRenderEvents.END_MAIN.register(controller::renderLevel);
		ClientTickEvents.END_CLIENT_TICK.register(client -> controller.update());
		InvalidateRenderStateCallback.EVENT.register(() -> runOnRenderThread(controller::reload));
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> runOnRenderThread(FabricBridge::shutdown));
	}

	public static void flushQueuedUi() {
		RenderController controller = ACTIVE.get();
		if (controller != null) {
			controller.flushQueuedUi();
		}
	}

	public static void shutdown() {
		RenderController controller = ACTIVE.getAndSet(null);
		if (controller != null) {
			controller.close();
		}
	}

	private static void runOnRenderThread(Runnable action) {
		Objects.requireNonNull(action, "action");
		if (RenderSystem.isOnRenderThread()) {
			action.run();
			return;
		}

		RenderSystem.queueFencedTask(action);
	}
}
