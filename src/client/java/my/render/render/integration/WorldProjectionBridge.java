package my.render.render.integration;

import my.render.render.frame.SurfaceMetrics;
import my.render.render.frame.WorldProjection;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.Optional;

final class WorldProjectionBridge implements WorldProjection {
	private final LevelRenderContext context;
	private final SurfaceMetrics surface;

	WorldProjectionBridge(LevelRenderContext context, SurfaceMetrics surface) {
		this.context = Objects.requireNonNull(context, "context");
		this.surface = Objects.requireNonNull(surface, "surface");
	}

	@Override
	public Optional<ProjectedPoint> project(double x, double y, double z) {
		Vec3 projected = this.context.gameRenderer().projectPointToScreen(new Vec3(x, y, z));
		float projectedX = (float) projected.x;
		float projectedY = (float) projected.y;
		float projectedDepth = (float) projected.z;

		if (!Float.isFinite(projectedX) || !Float.isFinite(projectedY) || !Float.isFinite(projectedDepth)) {
			return Optional.empty();
		}
		if (projectedDepth <= 0.0F) {
			return Optional.empty();
		}

		float guiX = projectedX * this.surface.guiWidth() / (float) Math.max(1, this.surface.framebufferWidth());
		float guiY = projectedY * this.surface.guiHeight() / (float) Math.max(1, this.surface.framebufferHeight());

		if (guiX < -64.0F || guiX > this.surface.guiWidth() + 64.0F || guiY < -64.0F || guiY > this.surface.guiHeight() + 64.0F) {
			return Optional.empty();
		}

		return Optional.of(new ProjectedPoint(guiX, guiY, projectedDepth));
	}
}
