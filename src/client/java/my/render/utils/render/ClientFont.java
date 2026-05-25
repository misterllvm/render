package my.render.utils.render;

import my.render.Best;
import my.render.render.core.ResourceId;
import my.render.render.font.MsdfFont;
import my.render.render.font.PreparedTextLayout;

import java.util.Objects;

public final class ClientFont {
	public static final ResourceId ATLAS = ResourceId.of(Best.MOD_ID, "ui/font.png");
	private static final ResourceId METRICS = ResourceId.of(Best.MOD_ID, "ui/font.json");
	private static final MsdfFont FONT = MsdfFont.load(ATLAS, METRICS);

	private ClientFont() {
	}

	public static PreparedTextLayout layout(String text, float fontSize) {
		Objects.requireNonNull(text, "text");
		return FONT.layout(text, fontSize);
	}
}
