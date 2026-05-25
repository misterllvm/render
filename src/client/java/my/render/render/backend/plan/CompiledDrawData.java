package my.render.render.backend.plan;

public sealed interface CompiledDrawData permits AnalyticQuadDrawData, SampledQuadDrawData, TextQuadDrawData, BackdropBlurDrawData {
}
