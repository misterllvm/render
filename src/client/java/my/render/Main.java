package my.render;

import my.render.render.integration.RenderController;
import my.render.screens.hud.Showcase;

import java.util.Objects;

public final class Main {
	private Main() {
	}

	public static ClientRuntime initialize(RenderController renderController) {
		Objects.requireNonNull(renderController, "renderController");
		ClientRuntime runtime = new ClientRuntime(renderController);
		runtime.hudManager().register(new Showcase());
		return runtime;
	}
}
