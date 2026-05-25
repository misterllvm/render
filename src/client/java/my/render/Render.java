package my.render;

import my.render.render.core.RenderBootstrap;
import my.render.render.integration.FabricBridge;
import my.render.render.integration.RenderController;
import my.render.utils.GameUptime;
import net.fabricmc.api.ClientModInitializer;

public class Render implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		RenderController controller = new RenderController(RenderBootstrap.initialize());
		Main.initialize(controller);
		GameUptime.markStart();
		FabricBridge.initialize(controller);
	}
}
