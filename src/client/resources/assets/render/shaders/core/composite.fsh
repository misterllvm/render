#version 150

uniform sampler2D Sampler0;
uniform vec4 uCompositeTint;
uniform float uCompositeOpacity;

in vec4 vertexColor;
in vec2 vertexUv;
in vec4 vertexData2;

out vec4 fragColor;

vec2 clampToValidSource(vec2 uv) {
	vec2 rawMin = min(vertexData2.xy, vertexData2.zw);
	vec2 rawMax = max(vertexData2.xy, vertexData2.zw);
	vec2 texelSize = 1.0 / vec2(textureSize(Sampler0, 0));
	vec2 inset = min(texelSize * 0.5, max((rawMax - rawMin) * 0.5, vec2(0.0)));
	return clamp(uv, rawMin + inset, rawMax - inset);
}

void main() {
	vec4 sampled = texture(Sampler0, clampToValidSource(vertexUv));
	vec4 composed = sampled * uCompositeTint * vertexColor;
	fragColor = vec4(composed.rgb, composed.a * uCompositeOpacity);
}
