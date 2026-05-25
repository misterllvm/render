package my.render.screens;

import my.render.render.base.UiRenderContext;
import my.render.render.event.ScreenInfo;
import my.render.render.frame.ScreenFrameContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Objects;

public abstract class RenderedScreen extends Screen {
	protected RenderedScreen(Component title) {
		super(title);
	}

	public final boolean matches(ScreenInfo screen) {
		Objects.requireNonNull(screen, "screen");
		return this.getClass().getName().equals(screen.id());
	}

	@Override
	public final void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
	}

	@Override
	public final void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
	}

	@Override
	protected final void extractBlurredBackground(GuiGraphicsExtractor context) {
	}

	@Override
	public final void extractTransparentBackground(GuiGraphicsExtractor context) {
	}

	public abstract void renderCustom(ScreenFrameContext frame, UiRenderContext ui);
}
