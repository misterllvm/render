package my.render.render.backend;

import my.render.render.base.CornerRadii;
import my.render.render.base.UiRect;
import my.render.render.shader.ShaderInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class UniformUploadResolver {
	List<TextureSlotBinding> resolveTextures(QuadBufferUpload upload, QuadBufferUpload.Payload payload) {
		Objects.requireNonNull(upload, "upload");
		Objects.requireNonNull(payload, "payload");

		Integer slot = this.textureSlot(payload);
		if (slot == null) {
			return List.of();
		}

		for (QuadBufferUpload.TextureBinding textureBinding : upload.sampledTextures()) {
			if (textureBinding.slot() == slot.intValue()) {
				return List.of(new TextureSlotBinding(textureBinding.slot(), textureBinding.source(), textureBinding.samplerMode()));
			}
		}

		throw new IllegalStateException("No texture binding found for slot " + slot);
	}

	List<UniformBinding> resolveUniforms(PipelineBinding binding, QuadBufferUpload upload, QuadBufferUpload.Payload payload, ExecutionTarget target, List<TextureSlotBinding> textures) {
		Objects.requireNonNull(binding, "binding");
		Objects.requireNonNull(upload, "upload");
		Objects.requireNonNull(payload, "payload");
		Objects.requireNonNull(target, "target");
		Objects.requireNonNull(textures, "textures");

		List<UniformBinding> resolved = new ArrayList<>();
		for (ShaderInterface.Uniform uniform : binding.shaderInterface().uniforms()) {
			UniformValue value = this.resolveUniformValue(uniform, upload.uniforms(), payload, target, textures);
			resolved.add(new UniformBinding(uniform.name(), uniform.type(), value));
		}

		return resolved;
	}

	private UniformValue resolveUniformValue(ShaderInterface.Uniform uniform, QuadBufferUpload.Uniforms batchUniforms, QuadBufferUpload.Payload payload, ExecutionTarget target, List<TextureSlotBinding> textures) {
		return switch (uniform.source()) {
			case ENCODER -> this.encoderValue(uniform.name(), target);
			case BATCH_UPLOAD -> this.batchValue(uniform.name(), batchUniforms);
			case QUAD_PAYLOAD -> this.payloadValue(uniform.name(), payload);
			case TEXTURE_BINDING -> this.textureValue(uniform.name(), textures);
		};
	}

	private UniformValue encoderValue(String name, ExecutionTarget target) {
		if ("uViewportSize".equals(name)) {
			return new UniformValue.Vec2Value((float) target.width(), (float) target.height());
		}

		throw new IllegalArgumentException("Unsupported encoder uniform: " + name);
	}

	private UniformValue batchValue(String name, QuadBufferUpload.Uniforms uniforms) {
		if (uniforms instanceof QuadBufferUpload.AnalyticQuadUniforms analyticQuadUniforms) {
			if ("uEdgeSoftnessPx".equals(name)) {
				return new UniformValue.FloatValue(analyticQuadUniforms.edgeSoftnessPx());
			}
		}

		if (uniforms instanceof QuadBufferUpload.TextUniforms textUniforms) {
			if ("uTextEdgeSoftnessPx".equals(name)) {
				return new UniformValue.FloatValue(textUniforms.edgeSoftnessPx());
			}
			if ("uMsdfSharpness".equals(name)) {
				return new UniformValue.FloatValue(textUniforms.sharpness());
			}
		}

		if (uniforms instanceof QuadBufferUpload.TextureUniforms textureUniforms) {
			if ("uTextureEdgeSoftnessPx".equals(name)) {
				return new UniformValue.FloatValue(textureUniforms.edgeSoftnessPx());
			}
		}

		if (uniforms instanceof QuadBufferUpload.BlurUniforms blurUniforms) {
			if ("uBlurDirection".equals(name)) {
				return new UniformValue.Vec2Value(blurUniforms.directionX(), blurUniforms.directionY());
			}
			if ("uTexelSize".equals(name)) {
				return new UniformValue.Vec2Value(blurUniforms.texelWidth(), blurUniforms.texelHeight());
			}
			if ("uBlurRadiusPx".equals(name)) {
				return new UniformValue.FloatValue(blurUniforms.radiusPx());
			}
		}

		if (uniforms instanceof QuadBufferUpload.CompositeUniforms compositeUniforms) {
			if ("uCompositeOpacity".equals(name)) {
				return new UniformValue.FloatValue(compositeUniforms.opacity());
			}
			if ("uCompositeTint".equals(name)) {
				return UniformValue.Vec4Value.fromColor(compositeUniforms.tint());
			}
		}

		throw new IllegalArgumentException("Unsupported batch uniform '" + name + "' for " + uniforms.getClass().getSimpleName());
	}

	private UniformValue payloadValue(String name, QuadBufferUpload.Payload payload) {
		if (payload instanceof QuadBufferUpload.AnalyticQuadPayload analyticPayload) {
			if ("uRectPx".equals(name) || "uShapeRectPx".equals(name)) {
				return this.rectValue(analyticPayload.shapeRect());
			}
			if ("uGeometryRectPx".equals(name)) {
				return this.rectValue(analyticPayload.geometryRect());
			}
			if ("uCornerRadiiPx".equals(name)) {
				return this.radiiValue(analyticPayload.radii());
			}
			if ("uStrokeWidthPx".equals(name)) {
				return new UniformValue.FloatValue(analyticPayload.strokeWidthPx());
			}
			if ("uStrokeAlign".equals(name)) {
				return new UniformValue.FloatValue(analyticPayload.strokeAlignCode());
			}
			if ("uShadowSoftnessPx".equals(name)) {
				return new UniformValue.FloatValue(analyticPayload.shadowSoftnessPx());
			}
		}

		if (payload instanceof QuadBufferUpload.TextPayload textPayload) {
			if ("uMsdfPxRange".equals(name)) {
				return new UniformValue.FloatValue(textPayload.pxRange());
			}
		}

		if (payload instanceof QuadBufferUpload.BackdropBlurPayload backdropBlurPayload) {
			if ("uRectPx".equals(name)) {
				return this.rectValue(backdropBlurPayload.rect());
			}
			if ("uCornerRadiiPx".equals(name)) {
				return this.radiiValue(backdropBlurPayload.radii());
			}
			if ("uBlurRadiusPx".equals(name)) {
				return new UniformValue.FloatValue(backdropBlurPayload.blurRadiusPx());
			}
		}

		throw new IllegalArgumentException("Unsupported payload uniform '" + name + "' for " + payload.getClass().getSimpleName());
	}

	private UniformValue textureValue(String name, List<TextureSlotBinding> textures) {
		if (!"Sampler0".equals(name)) {
			throw new IllegalArgumentException("Unsupported texture uniform: " + name);
		}
		if (textures.isEmpty()) {
			throw new IllegalStateException("Sampler0 requested but no texture bindings are available");
		}
		return new UniformValue.Sampler2DValue(textures.getFirst().slot());
	}

	private Integer textureSlot(QuadBufferUpload.Payload payload) {
		if (payload instanceof QuadBufferUpload.TexturePayload texturePayload) {
			return texturePayload.textureSlot();
		}
		if (payload instanceof QuadBufferUpload.TextPayload textPayload) {
			return textPayload.textureSlot();
		}
		if (payload instanceof QuadBufferUpload.FullscreenPayload fullscreenPayload) {
			return fullscreenPayload.textureSlot();
		}
		if (payload instanceof QuadBufferUpload.BackdropBlurPayload backdropBlurPayload) {
			return backdropBlurPayload.textureSlot();
		}
		return null;
	}

	private UniformValue.Vec4Value rectValue(UiRect rect) {
		Objects.requireNonNull(rect, "rect");
		return new UniformValue.Vec4Value(rect.x(), rect.y(), rect.width(), rect.height());
	}

	private UniformValue.Vec4Value radiiValue(CornerRadii radii) {
		Objects.requireNonNull(radii, "radii");
		return new UniformValue.Vec4Value(radii.topLeft(), radii.topRight(), radii.bottomRight(), radii.bottomLeft());
	}
}
