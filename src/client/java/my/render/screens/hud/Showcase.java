package my.render.screens.hud;

import my.render.events.impl.HudRenderEvent;
import my.render.render.base.*;
import my.render.render.pipeline.StandardPipelines;
import my.render.utils.math.RectUtil;
import my.render.utils.render.ClientFont;

public final class Showcase implements HudElement {

	@Override
	public void render(HudRenderEvent event) {
		UiRenderContext ui = event.ui();
		UiRect showcase = new UiRect(18.0F, 18.0F, 388.0F, 164.0F);

		//sphere
		UiRect orb1 = new UiRect(showcase.x() + 22.0F, showcase.y() + 68.0F, 24.0F, 24.0F);
		CornerRadii radii1 = CornerRadii.uniform(orb1.width() * 0.5F);
		RgbaColor core1 = RgbaColor.of(0.38F, 0.84F, 0.98F, 0.95F);
		ui.drawShadow(orb1, radii1, 16.0F, RgbaColor.of(0.16F, 0.56F, 1.0F, 0.34F));
		ui.fillRoundedRect(StandardPipelines.ROUNDED_FILL, orb1, radii1, new Gradient4(
				core1, core1.withAlpha(core1.a() * 0.96F), core1.withAlpha(core1.a() * 0.72F), core1.withAlpha(core1.a() * 0.82F)
		));

		//gradient
		UiRect gradientChip = new UiRect(showcase.x() + 136.0F, showcase.y() + 62.0F, 98.0F, 34.0F);
		ui.drawShadow(RectUtil.offset(gradientChip, 0.0F, 6.0F), CornerRadii.uniform(16.0F), 14.0F, RgbaColor.of(0.00F, 0.00F, 0.00F, 0.24F));
		ui.fillRoundedRect(StandardPipelines.ROUNDED_FILL, gradientChip, CornerRadii.uniform(16.0F), new Gradient4(
				RgbaColor.of(0.25F, 0.63F, 1.0F, 0.96F),
				RgbaColor.of(0.52F, 0.46F, 1.0F, 0.96F),
				RgbaColor.of(0.18F, 0.20F, 0.38F, 0.96F),
				RgbaColor.of(0.18F, 0.46F, 0.82F, 0.96F)
		));
		ui.drawBorder(gradientChip, CornerRadii.uniform(16.0F), new StrokeStyle(1.0F, RgbaColor.of(1.0F, 1.0F, 1.0F, 0.12F), StrokeStyle.Align.INSIDE));
		ui.drawText(ClientFont.layout("Gradient", 10.0F), gradientChip.x() + 18.0F, gradientChip.y() + 11.0F, RgbaColor.WHITE);

		//border
		UiRect borderChip = new UiRect(showcase.x() + 244.0F, showcase.y() + 62.0F, 58.0F, 34.0F);
		ui.fillRoundedRect(borderChip, CornerRadii.uniform(17.0F), RgbaColor.of(0.08F, 0.10F, 0.14F, 0.92F));
		ui.drawBorder(borderChip, CornerRadii.uniform(17.0F), new StrokeStyle(2.0F, RgbaColor.of(0.40F, 0.84F, 0.98F, 0.92F), StrokeStyle.Align.INSIDE));

		//shadow
		UiRect shadowChip = new UiRect(showcase.x() + 314.0F, showcase.y() + 62.0F, 52.0F, 34.0F);
		ui.drawShadow(shadowChip, CornerRadii.uniform(17.0F), 16.0F, RgbaColor.of(0.42F, 0.84F, 1.0F, 0.26F));
		ui.fillRoundedRect(shadowChip, CornerRadii.uniform(17.0F), RgbaColor.of(0.08F, 0.10F, 0.14F, 0.76F));

		ui.drawText(ClientFont.layout("Shapes", 10.0F), showcase.x() + 136.0F, showcase.y() + 104.0F, RgbaColor.of(0.70F, 0.75F, 0.82F, 1.0F));

		//blur
		UiRect softBlur = new UiRect(showcase.x() + 136.0F, showcase.y() + 116.0F, 108.0F, 26.0F);
		UiRect denseBlur = new UiRect(showcase.x() + 256.0F, showcase.y() + 116.0F, 110.0F, 26.0F);

		ui.drawBackdropBlur(softBlur, CornerRadii.uniform(13.0F), 6.0F, RgbaColor.of(0.10F, 0.14F, 0.20F, 0.18F));
		ui.drawBorder(softBlur, CornerRadii.uniform(13.0F), new StrokeStyle(1.0F, RgbaColor.of(1.0F, 1.0F, 1.0F, 0.08F), StrokeStyle.Align.INSIDE));
		ui.drawText(ClientFont.layout("Blur x1", 10.0F), softBlur.x() + 14.0F, softBlur.y() + 8.0F, RgbaColor.WHITE);

		ui.drawBackdropBlur(denseBlur, CornerRadii.uniform(13.0F), 12.0F, RgbaColor.of(0.08F, 0.12F, 0.18F, 0.22F));
		ui.drawBorder(denseBlur, CornerRadii.uniform(13.0F), new StrokeStyle(1.0F, RgbaColor.of(0.40F, 0.84F, 0.98F, 0.16F), StrokeStyle.Align.INSIDE));
		ui.drawText(ClientFont.layout("Blur x2", 10.0F), denseBlur.x() + 14.0F, denseBlur.y() + 8.0F, RgbaColor.WHITE);

		//dock
		UiRect dock = new UiRect(18.0F, event.frame().surface().guiHeight() - 76.0F, 276.0F, 58.0F);
		CornerRadii dockRadii = CornerRadii.uniform(18.0F);
		ui.drawBackdropBlur(dock, dockRadii, 8.0F, RgbaColor.of(0.09F, 0.12F, 0.17F, 0.16F));
	}
}