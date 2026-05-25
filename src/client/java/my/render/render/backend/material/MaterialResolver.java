package my.render.render.backend.material;

import my.render.render.backend.DrawCommand;
import my.render.render.backend.ExecutionFamily;
import my.render.render.pipeline.PipelineSpec;

import java.util.Objects;

public final class MaterialResolver {
	public MaterialDescriptor resolve(DrawCommand command, PipelineSpec spec) {
		Objects.requireNonNull(command, "command");
		Objects.requireNonNull(spec, "spec");

		ExecutionFamily executionFamily = this.executionFamily(command);
		MaterialKey key = new MaterialKey(
			spec.materialFamily(),
			spec.key(),
			spec.variantId(),
			spec.blendMode(),
			spec.depthMode(),
			spec.samplerMode(),
			this.flagsFor(command, spec)
		);
		return new MaterialDescriptor(
			key,
			executionFamily,
			this.batchUniformKey(command, spec),
			this.textureStateKey(command, spec),
			this.estimatedQuadCount(command),
			this.batchable(command, spec, executionFamily),
			this.dedicated(command, spec, executionFamily)
		);
	}

	private ExecutionFamily executionFamily(DrawCommand command) {
		if (command instanceof DrawCommand.FillRect
			|| command instanceof DrawCommand.FillRoundedRect
			|| command instanceof DrawCommand.DrawBorder
			|| command instanceof DrawCommand.DrawShadow) {
			return ExecutionFamily.ANALYTIC_QUAD;
		}
		if (command instanceof DrawCommand.DrawTexture) {
			return ExecutionFamily.SAMPLED_QUAD;
		}
		if (command instanceof DrawCommand.DrawText) {
			return ExecutionFamily.TEXT_QUAD;
		}
		if (command instanceof DrawCommand.DrawBackdropBlur) {
			return ExecutionFamily.BACKDROP_BLUR;
		}
		throw new IllegalArgumentException("Unsupported render command for material resolution: " + command.getClass().getSimpleName());
	}

	private int flagsFor(DrawCommand command, PipelineSpec spec) {
		int flags = spec.defaultMaterialFlags();

		if (command instanceof DrawCommand.DrawText) {
			flags |= MaterialFlags.TEXT | MaterialFlags.MSDF | MaterialFlags.SAMPLED;
		}
		if (command instanceof DrawCommand.DrawTexture) {
			flags |= MaterialFlags.SAMPLED;
		}
		if (command instanceof DrawCommand.DrawBackdropBlur) {
			flags |= MaterialFlags.SAMPLED | MaterialFlags.OFFSCREEN_INPUT | MaterialFlags.EFFECT_STAGE;
		}

		return flags;
	}

	private int batchUniformKey(DrawCommand command, PipelineSpec spec) {
		if (command instanceof DrawCommand.DrawTexture drawTexture) {
			return Float.floatToIntBits(drawTexture.edgeSoftnessPx());
		}
		if (command instanceof DrawCommand.DrawText) {
			// TODO: replace with real text material parameters once text config stops being hardcoded in the executor.
			return Objects.hash(0.72F, 1.12F);
		}
		if (command instanceof DrawCommand.FillRect) {
			return 0;
		}
		if (command instanceof DrawCommand.FillRoundedRect || command instanceof DrawCommand.DrawBorder) {
			return 1;
		}
		if (command instanceof DrawCommand.DrawShadow) {
			return 2;
		}
		if (command instanceof DrawCommand.DrawBackdropBlur) {
			return 3;
		}
		return Objects.hash(spec.key(), spec.variantId());
	}

	private int textureStateKey(DrawCommand command, PipelineSpec spec) {
		if (command instanceof DrawCommand.DrawTexture drawTexture) {
			return Objects.hash(drawTexture.textureId(), spec.samplerMode());
		}
		if (command instanceof DrawCommand.DrawText drawText) {
			return Objects.hash(drawText.layout().font(), spec.samplerMode());
		}
		if (command instanceof DrawCommand.DrawBackdropBlur) {
			// TODO: route real offscreen dependency ids once backdrop capture is compiled explicitly.
			return -1;
		}
		return 0;
	}

	private boolean batchable(DrawCommand command, PipelineSpec spec, ExecutionFamily executionFamily) {
		if (executionFamily == ExecutionFamily.ANALYTIC_QUAD
			|| executionFamily == ExecutionFamily.SAMPLED_QUAD
			|| executionFamily == ExecutionFamily.TEXT_QUAD) {
			return true;
		}
		return false;
	}

	private boolean dedicated(DrawCommand command, PipelineSpec spec, ExecutionFamily executionFamily) {
		if (executionFamily == ExecutionFamily.ANALYTIC_QUAD
			|| executionFamily == ExecutionFamily.SAMPLED_QUAD
			|| executionFamily == ExecutionFamily.TEXT_QUAD) {
			return false;
		}
		return command instanceof DrawCommand.DrawBackdropBlur;
	}

	private int estimatedQuadCount(DrawCommand command) {
		if (command instanceof DrawCommand.DrawText drawText) {
			return drawText.layout().glyphs().size();
		}
		if (command.pipeline() != null) {
			return 1;
		}
		return 0;
	}
}
