package my.render.render.pipeline;

import my.render.render.backend.material.MaterialFamily;
import my.render.render.backend.material.MaterialFlags;
import my.render.render.backend.material.SamplerMode;
import my.render.render.core.ResourceId;

public final class StandardPipelines {
	public static final PipelineKey FILL = PipelineKey.of("fill");
	public static final PipelineKey ROUNDED_FILL = PipelineKey.of("rounded_fill");
	public static final PipelineKey BORDER = PipelineKey.of("border");
	public static final PipelineKey SHADOW = PipelineKey.of("shadow");
	public static final PipelineKey BACKDROP_BLUR = PipelineKey.of("backdrop_blur");
	public static final PipelineKey TEXTURE = PipelineKey.of("texture");
	public static final PipelineKey TEXT = PipelineKey.of("text");
	public static final PipelineKey BLUR_DOWN = PipelineKey.of("blur_down");
	public static final PipelineKey BLUR_UP = PipelineKey.of("blur_up");
	public static final PipelineKey COMPOSITE = PipelineKey.of("composite");

	@Deprecated public static final PipelineKey UI_FILL = FILL;
	@Deprecated public static final PipelineKey UI_ROUNDED_FILL = ROUNDED_FILL;
	@Deprecated public static final PipelineKey UI_BORDER = BORDER;
	@Deprecated public static final PipelineKey UI_SHADOW = SHADOW;
	@Deprecated public static final PipelineKey UI_BACKDROP_BLUR = BACKDROP_BLUR;
	@Deprecated public static final PipelineKey UI_TEXTURE = TEXTURE;
	@Deprecated public static final PipelineKey TEXT_MSDF = TEXT;
	@Deprecated public static final PipelineKey EFFECT_BLUR_DOWNSAMPLE = BLUR_DOWN;
	@Deprecated public static final PipelineKey EFFECT_BLUR_UPSAMPLE = BLUR_UP;
	@Deprecated public static final PipelineKey EFFECT_COMPOSITE = COMPOSITE;

	private StandardPipelines() {
	}

	public static void registerDefaults(PipelineLibrary pipelines) {
		ResourceId quadVertex = shader("quad.vsh");
		pipelines.register(new PipelineSpec(FILL, quadVertex, shader("fill.fsh"), PipelineSpec.Topology.QUADS, PipelineSpec.BlendMode.ALPHA, PipelineSpec.DepthMode.NONE, PipelineSpec.TargetKind.MAIN_COLOR, MaterialFamily.SHAPE_FILL, 0, SamplerMode.LINEAR_CLAMP, MaterialFlags.of(MaterialFlags.ANALYTIC_SHAPE)));
		pipelines.register(new PipelineSpec(ROUNDED_FILL, quadVertex, shader("rounded_fill.fsh"), PipelineSpec.Topology.QUADS, PipelineSpec.BlendMode.ALPHA, PipelineSpec.DepthMode.NONE, PipelineSpec.TargetKind.MAIN_COLOR, MaterialFamily.SHAPE_FILL, 1, SamplerMode.LINEAR_CLAMP, MaterialFlags.of(MaterialFlags.ANALYTIC_SHAPE)));
		pipelines.register(new PipelineSpec(BORDER, quadVertex, shader("border.fsh"), PipelineSpec.Topology.QUADS, PipelineSpec.BlendMode.ALPHA, PipelineSpec.DepthMode.NONE, PipelineSpec.TargetKind.MAIN_COLOR, MaterialFamily.SHAPE_BORDER, 0, SamplerMode.LINEAR_CLAMP, MaterialFlags.of(MaterialFlags.ANALYTIC_SHAPE, MaterialFlags.BORDER)));
		pipelines.register(new PipelineSpec(SHADOW, quadVertex, shader("shadow.fsh"), PipelineSpec.Topology.QUADS, PipelineSpec.BlendMode.ALPHA, PipelineSpec.DepthMode.NONE, PipelineSpec.TargetKind.MAIN_COLOR, MaterialFamily.SHAPE_SHADOW, 0, SamplerMode.LINEAR_CLAMP, MaterialFlags.of(MaterialFlags.ANALYTIC_SHAPE, MaterialFlags.SHADOW)));
		pipelines.register(new PipelineSpec(BACKDROP_BLUR, quadVertex, shader("backdrop_blur.fsh"), PipelineSpec.Topology.QUADS, PipelineSpec.BlendMode.ALPHA, PipelineSpec.DepthMode.NONE, PipelineSpec.TargetKind.MAIN_COLOR, MaterialFamily.BACKDROP, 0, SamplerMode.LINEAR_CLAMP, MaterialFlags.of(MaterialFlags.SAMPLED, MaterialFlags.OFFSCREEN_INPUT, MaterialFlags.EFFECT_STAGE)));
		pipelines.register(new PipelineSpec(TEXTURE, quadVertex, shader("texture.fsh"), PipelineSpec.Topology.QUADS, PipelineSpec.BlendMode.ALPHA, PipelineSpec.DepthMode.NONE, PipelineSpec.TargetKind.MAIN_COLOR, MaterialFamily.TEXTURED_QUAD, 0, SamplerMode.LINEAR_CLAMP, MaterialFlags.of(MaterialFlags.SAMPLED)));
		pipelines.register(new PipelineSpec(TEXT, quadVertex, shader("text.fsh"), PipelineSpec.Topology.QUADS, PipelineSpec.BlendMode.ALPHA, PipelineSpec.DepthMode.NONE, PipelineSpec.TargetKind.MAIN_COLOR, MaterialFamily.MSDF_TEXT, 0, SamplerMode.LINEAR_CLAMP, MaterialFlags.of(MaterialFlags.SAMPLED, MaterialFlags.TEXT, MaterialFlags.MSDF)));
		pipelines.register(new PipelineSpec(BLUR_DOWN, quadVertex, shader("blur_down.fsh"), PipelineSpec.Topology.QUADS, PipelineSpec.BlendMode.OPAQUE, PipelineSpec.DepthMode.NONE, PipelineSpec.TargetKind.OFFSCREEN_COLOR, MaterialFamily.POST_PROCESS, 0, SamplerMode.LINEAR_CLAMP, MaterialFlags.of(MaterialFlags.SAMPLED, MaterialFlags.OFFSCREEN_INPUT, MaterialFlags.OFFSCREEN_OUTPUT, MaterialFlags.EFFECT_STAGE)));
		pipelines.register(new PipelineSpec(BLUR_UP, quadVertex, shader("blur_up.fsh"), PipelineSpec.Topology.QUADS, PipelineSpec.BlendMode.OPAQUE, PipelineSpec.DepthMode.NONE, PipelineSpec.TargetKind.OFFSCREEN_COLOR, MaterialFamily.POST_PROCESS, 1, SamplerMode.LINEAR_CLAMP, MaterialFlags.of(MaterialFlags.SAMPLED, MaterialFlags.OFFSCREEN_INPUT, MaterialFlags.OFFSCREEN_OUTPUT, MaterialFlags.EFFECT_STAGE)));
		pipelines.register(new PipelineSpec(COMPOSITE, quadVertex, shader("composite.fsh"), PipelineSpec.Topology.QUADS, PipelineSpec.BlendMode.ALPHA, PipelineSpec.DepthMode.NONE, PipelineSpec.TargetKind.MAIN_COLOR, MaterialFamily.COMPOSITE, 0, SamplerMode.LINEAR_CLAMP, MaterialFlags.of(MaterialFlags.SAMPLED, MaterialFlags.OFFSCREEN_INPUT, MaterialFlags.EFFECT_STAGE)));
	}

	private static ResourceId shader(String fileName) {
		return ResourceId.of("render", "shaders/core/" + fileName);
	}
}
