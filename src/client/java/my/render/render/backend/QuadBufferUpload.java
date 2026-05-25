package my.render.render.backend;

import my.render.render.backend.material.SamplerMode;
import my.render.render.base.CornerRadii;
import my.render.render.base.RgbaColor;
import my.render.render.base.UiRect;
import my.render.render.core.ResourceId;

import java.util.List;
import java.util.Objects;

final record QuadBufferUpload(float[] vertexData, int vertexFloatCount, int floatsPerVertex, List<Quad> quads, List<TextureBinding> sampledTextures, Uniforms uniforms) {
	QuadBufferUpload(float[] vertexData, int floatsPerVertex, List<Quad> quads, List<TextureBinding> sampledTextures, Uniforms uniforms) {
		this(vertexData, vertexData.length, floatsPerVertex, quads, sampledTextures, uniforms);
	}

	QuadBufferUpload(float[] vertexData, int vertexFloatCount, int floatsPerVertex, List<Quad> quads, List<TextureBinding> sampledTextures, Uniforms uniforms) {
		Objects.requireNonNull(vertexData, "vertexData");
		Objects.requireNonNull(quads, "quads");
		Objects.requireNonNull(sampledTextures, "sampledTextures");
		Objects.requireNonNull(uniforms, "uniforms");
		if (floatsPerVertex <= 0) {
			throw new IllegalArgumentException("floatsPerVertex must be positive");
		}
		if (vertexFloatCount < 0 || vertexFloatCount > vertexData.length) {
			throw new IllegalArgumentException("vertexFloatCount must be between 0 and vertexData.length");
		}
		if (vertexFloatCount % floatsPerVertex != 0) {
			throw new IllegalArgumentException("vertexFloatCount (" + vertexFloatCount + ") is not aligned to floatsPerVertex (" + floatsPerVertex + ")");
		}
		List<Quad> copiedQuads = List.copyOf(quads);
		List<TextureBinding> copiedTextures = List.copyOf(sampledTextures);
		for (Quad quad : copiedQuads) {
			Objects.requireNonNull(quad, "quad");
		}
		for (TextureBinding binding : copiedTextures) {
			Objects.requireNonNull(binding, "sampledTextures");
		}

		this.vertexData = vertexData;
		this.vertexFloatCount = vertexFloatCount;
		this.floatsPerVertex = floatsPerVertex;
		this.quads = copiedQuads;
		this.sampledTextures = copiedTextures;
		this.uniforms = uniforms;
	}

	int vertexCount() {
		return this.vertexFloatCount / this.floatsPerVertex;
	}

	enum Kind {
		FILL,
		ROUNDED_FILL,
		BORDER,
		SHADOW,
		BACKDROP_BLUR,
		TEXTURE,
		TEXT,
		FULLSCREEN
	}

	record Quad(Kind kind, int vertexOffset, int vertexCount, Payload payload) {
		Quad {
			Objects.requireNonNull(kind, "kind");
			Objects.requireNonNull(payload, "payload");
			if (vertexOffset < 0) {
				throw new IllegalArgumentException("vertexOffset must be non-negative");
			}
			if (vertexCount <= 0) {
				throw new IllegalArgumentException("vertexCount must be positive");
			}
		}
	}

	sealed interface Payload permits AnalyticQuadPayload, TexturePayload, TextPayload, FullscreenPayload, BackdropBlurPayload {
	}

	record AnalyticQuadPayload(
		UiRect shapeRect,
		UiRect geometryRect,
		CornerRadii radii,
		float strokeWidthPx,
		float strokeAlignCode,
		float shadowSoftnessPx
	) implements Payload {
		AnalyticQuadPayload {
			Objects.requireNonNull(shapeRect, "shapeRect");
			Objects.requireNonNull(geometryRect, "geometryRect");
			Objects.requireNonNull(radii, "radii");
		}
	}

	record TexturePayload(UiRect rect, CornerRadii radii, int textureSlot) implements Payload {
		TexturePayload {
			Objects.requireNonNull(rect, "rect");
			Objects.requireNonNull(radii, "radii");
			if (textureSlot < 0) {
				throw new IllegalArgumentException("textureSlot must be non-negative");
			}
		}
	}

	record BackdropBlurPayload(UiRect rect, CornerRadii radii, int textureSlot, float blurRadiusPx) implements Payload {
		BackdropBlurPayload {
			Objects.requireNonNull(rect, "rect");
			Objects.requireNonNull(radii, "radii");
			if (textureSlot < 0) {
				throw new IllegalArgumentException("textureSlot must be non-negative");
			}
			if (blurRadiusPx <= 0.0F) {
				throw new IllegalArgumentException("blurRadiusPx must be positive");
			}
		}
	}

	record TextPayload(UiRect rect, int textureSlot, RgbaColor tint, float pxRange) implements Payload {
		TextPayload {
			Objects.requireNonNull(rect, "rect");
			Objects.requireNonNull(tint, "tint");
			if (textureSlot < 0) {
				throw new IllegalArgumentException("textureSlot must be non-negative");
			}
			if (pxRange <= 0.0F) {
				throw new IllegalArgumentException("pxRange must be positive");
			}
		}
	}

	record FullscreenPayload(UiRect rect, int textureSlot, RgbaColor tint) implements Payload {
		FullscreenPayload {
			Objects.requireNonNull(rect, "rect");
			Objects.requireNonNull(tint, "tint");
			if (textureSlot < 0) {
				throw new IllegalArgumentException("textureSlot must be non-negative");
			}
		}
	}

	sealed interface Uniforms permits AnalyticQuadUniforms, TextureUniforms, TextUniforms, BlurUniforms, CompositeUniforms {
	}

	record AnalyticQuadUniforms(float edgeSoftnessPx) implements Uniforms {
		AnalyticQuadUniforms {
			if (edgeSoftnessPx < 0.0F) {
				throw new IllegalArgumentException("edgeSoftnessPx must be non-negative");
			}
		}
	}

	record TextureUniforms(float edgeSoftnessPx) implements Uniforms {
		static final TextureUniforms DEFAULT = new TextureUniforms(1.0F);

		TextureUniforms {
			if (edgeSoftnessPx < 0.0F) {
				throw new IllegalArgumentException("edgeSoftnessPx must be non-negative");
			}
		}
	}

	record TextUniforms(float edgeSoftnessPx, float sharpness) implements Uniforms {
		TextUniforms {
			if (edgeSoftnessPx <= 0.0F) {
				throw new IllegalArgumentException("edgeSoftnessPx must be positive");
			}
			if (sharpness <= 0.0F) {
				throw new IllegalArgumentException("sharpness must be positive");
			}
		}
	}

	record BlurUniforms(float directionX, float directionY, float texelWidth, float texelHeight, float radiusPx) implements Uniforms {
		BlurUniforms {
			if (directionX == 0.0F && directionY == 0.0F) {
				throw new IllegalArgumentException("blur direction must not be zero");
			}
			if (texelWidth <= 0.0F) {
				throw new IllegalArgumentException("texelWidth must be positive");
			}
			if (texelHeight <= 0.0F) {
				throw new IllegalArgumentException("texelHeight must be positive");
			}
			if (radiusPx <= 0.0F) {
				throw new IllegalArgumentException("radiusPx must be positive");
			}
		}
	}

	record CompositeUniforms(float opacity, RgbaColor tint) implements Uniforms {
		CompositeUniforms {
			if (opacity < 0.0F || opacity > 1.0F) {
				throw new IllegalArgumentException("opacity must be between 0 and 1");
			}
			Objects.requireNonNull(tint, "tint");
		}
	}

	record TextureBinding(int slot, TextureSource source, SamplerMode samplerMode) {
		TextureBinding {
			Objects.requireNonNull(source, "source");
			Objects.requireNonNull(samplerMode, "samplerMode");
		}

		TextureBinding(int slot, TextureSource source) {
			this(slot, source, SamplerMode.LINEAR_CLAMP);
		}
	}

	sealed interface TextureSource permits TextureSource.ResourceTexture, TextureSource.TargetTexture {
		record ResourceTexture(ResourceId id) implements TextureSource {
			public ResourceTexture {
				Objects.requireNonNull(id, "id");
			}
		}

		record TargetTexture(int targetId) implements TextureSource {
			public TargetTexture {
				if (targetId < 0) {
					throw new IllegalArgumentException("targetId must be non-negative");
				}
			}
		}
	}
}
