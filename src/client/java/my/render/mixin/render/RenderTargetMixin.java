package my.render.mixin.render;

import my.render.render.integration.FabricBridge;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderTarget.class)
public class RenderTargetMixin {
	@Inject(method = "blitToScreen", at = @At("RETURN"))
	private void flushQueuedRendererUiAfterMainPresent(CallbackInfo ci) {
		RenderTarget self = (RenderTarget) (Object) this;
		RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
		if (self == mainTarget) {
			FabricBridge.flushQueuedUi();
		}
	}
}
