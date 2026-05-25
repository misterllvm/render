package my.render.render.shader;

import my.render.render.pipeline.PipelineKey;
import my.render.render.pipeline.StandardPipelines;

import java.util.List;
import java.util.Objects;

public final class ShaderInterfaces {
	private static final ShaderInterface FILL = new ShaderInterface(
		ShaderInterface.VertexFormat.POSITION_UV_COLOR_QUAD,
		List.of(
			uniform("uViewportSize", ShaderInterface.Type.VEC2, ShaderInterface.Source.ENCODER),
			uniform("uEdgeSoftnessPx", ShaderInterface.Type.FLOAT, ShaderInterface.Source.BATCH_UPLOAD)
		)
	);

	private static final ShaderInterface ROUNDED_FILL = new ShaderInterface(
		ShaderInterface.VertexFormat.POSITION_UV_COLOR_QUAD,
		List.of(
			uniform("uViewportSize", ShaderInterface.Type.VEC2, ShaderInterface.Source.ENCODER),
			uniform("uEdgeSoftnessPx", ShaderInterface.Type.FLOAT, ShaderInterface.Source.BATCH_UPLOAD)
		)
	);

	private static final ShaderInterface BORDER = new ShaderInterface(
		ShaderInterface.VertexFormat.POSITION_UV_COLOR_QUAD,
		List.of(
			uniform("uViewportSize", ShaderInterface.Type.VEC2, ShaderInterface.Source.ENCODER),
			uniform("uEdgeSoftnessPx", ShaderInterface.Type.FLOAT, ShaderInterface.Source.BATCH_UPLOAD)
		)
	);

	private static final ShaderInterface SHADOW = new ShaderInterface(
		ShaderInterface.VertexFormat.POSITION_UV_COLOR_QUAD,
		List.of(uniform("uViewportSize", ShaderInterface.Type.VEC2, ShaderInterface.Source.ENCODER))
	);

	private static final ShaderInterface BACKDROP_BLUR = new ShaderInterface(
		ShaderInterface.VertexFormat.POSITION_UV_COLOR_QUAD,
		List.of(
			uniform("uViewportSize", ShaderInterface.Type.VEC2, ShaderInterface.Source.ENCODER),
			uniform("Sampler0", ShaderInterface.Type.SAMPLER2D, ShaderInterface.Source.TEXTURE_BINDING)
		)
	);

	private static final ShaderInterface TEXTURE = new ShaderInterface(
		ShaderInterface.VertexFormat.POSITION_UV_COLOR_QUAD,
		List.of(
			uniform("uViewportSize", ShaderInterface.Type.VEC2, ShaderInterface.Source.ENCODER),
			uniform("uTextureEdgeSoftnessPx", ShaderInterface.Type.FLOAT, ShaderInterface.Source.BATCH_UPLOAD),
			uniform("Sampler0", ShaderInterface.Type.SAMPLER2D, ShaderInterface.Source.TEXTURE_BINDING)
		)
	);

	private static final ShaderInterface TEXT = new ShaderInterface(
		ShaderInterface.VertexFormat.POSITION_UV_COLOR_QUAD,
		List.of(
			uniform("uViewportSize", ShaderInterface.Type.VEC2, ShaderInterface.Source.ENCODER),
			uniform("uTextEdgeSoftnessPx", ShaderInterface.Type.FLOAT, ShaderInterface.Source.BATCH_UPLOAD),
			uniform("uMsdfSharpness", ShaderInterface.Type.FLOAT, ShaderInterface.Source.BATCH_UPLOAD),
			uniform("Sampler0", ShaderInterface.Type.SAMPLER2D, ShaderInterface.Source.TEXTURE_BINDING)
		)
	);

	private static final ShaderInterface BLUR = new ShaderInterface(
		ShaderInterface.VertexFormat.POSITION_UV_COLOR_QUAD,
		List.of(
			uniform("uViewportSize", ShaderInterface.Type.VEC2, ShaderInterface.Source.ENCODER),
			uniform("uBlurDirection", ShaderInterface.Type.VEC2, ShaderInterface.Source.BATCH_UPLOAD),
			uniform("uTexelSize", ShaderInterface.Type.VEC2, ShaderInterface.Source.BATCH_UPLOAD),
			uniform("uBlurRadiusPx", ShaderInterface.Type.FLOAT, ShaderInterface.Source.BATCH_UPLOAD),
			uniform("Sampler0", ShaderInterface.Type.SAMPLER2D, ShaderInterface.Source.TEXTURE_BINDING)
		)
	);

	private static final ShaderInterface COMPOSITE = new ShaderInterface(
		ShaderInterface.VertexFormat.POSITION_UV_COLOR_QUAD,
		List.of(
			uniform("uViewportSize", ShaderInterface.Type.VEC2, ShaderInterface.Source.ENCODER),
			uniform("uCompositeOpacity", ShaderInterface.Type.FLOAT, ShaderInterface.Source.BATCH_UPLOAD),
			uniform("uCompositeTint", ShaderInterface.Type.VEC4, ShaderInterface.Source.BATCH_UPLOAD),
			uniform("Sampler0", ShaderInterface.Type.SAMPLER2D, ShaderInterface.Source.TEXTURE_BINDING)
		)
	);

	private ShaderInterfaces() {
	}

	public static ShaderInterface forPipeline(PipelineKey key) {
		Objects.requireNonNull(key, "key");
		if (key.equals(StandardPipelines.FILL)) {
			return FILL;
		}
		if (key.equals(StandardPipelines.ROUNDED_FILL)) {
			return ROUNDED_FILL;
		}
		if (key.equals(StandardPipelines.BORDER)) {
			return BORDER;
		}
		if (key.equals(StandardPipelines.SHADOW)) {
			return SHADOW;
		}
		if (key.equals(StandardPipelines.BACKDROP_BLUR)) {
			return BACKDROP_BLUR;
		}
		if (key.equals(StandardPipelines.TEXTURE)) {
			return TEXTURE;
		}
		if (key.equals(StandardPipelines.TEXT)) {
			return TEXT;
		}
		if (key.equals(StandardPipelines.BLUR_DOWN) || key.equals(StandardPipelines.BLUR_UP)) {
			return BLUR;
		}
		if (key.equals(StandardPipelines.COMPOSITE)) {
			return COMPOSITE;
		}
		return new ShaderInterface(
			ShaderInterface.VertexFormat.POSITION_UV_COLOR_QUAD,
			List.of(uniform("uViewportSize", ShaderInterface.Type.VEC2, ShaderInterface.Source.ENCODER))
		);
	}

	private static ShaderInterface.Uniform uniform(String name, ShaderInterface.Type type, ShaderInterface.Source source) {
		return new ShaderInterface.Uniform(name, type, source);
	}
}
