#version 150

uniform sampler2D Sampler0;
uniform vec2 uBlurDirection;
uniform vec2 uTexelSize;
uniform float uBlurRadiusPx;

in vec4 vertexColor;
in vec2 vertexUv;
in vec4 vertexData2;

out vec4 fragColor;

float gaussian(float x, float sigma) {
	return exp(-(x * x) / (2.0 * sigma * sigma));
}

vec2 clampToValidSource(vec2 uv) {
	vec2 rawMin = min(vertexData2.xy, vertexData2.zw);
	vec2 rawMax = max(vertexData2.xy, vertexData2.zw);
	vec2 inset = min(uTexelSize * 0.5, max((rawMax - rawMin) * 0.5, vec2(0.0)));
	return clamp(uv, rawMin + inset, rawMax - inset);
}

void main() {
	vec2 axis = normalize(uBlurDirection) * uTexelSize * max(uBlurRadiusPx, 1.0);
	float sigma = max(uBlurRadiusPx * 0.5, 1.0);

	vec4 accum = texture(Sampler0, clampToValidSource(vertexUv)) * gaussian(0.0, sigma);
	float weightSum = gaussian(0.0, sigma);

	for (int index = 1; index <= 6; index++) {
		float offset = float(index);
		float weight = gaussian(offset, sigma);
		vec2 delta = axis * offset;
		accum += texture(Sampler0, clampToValidSource(vertexUv + delta)) * weight;
		accum += texture(Sampler0, clampToValidSource(vertexUv - delta)) * weight;
		weightSum += weight * 2.0;
	}

	fragColor = (accum / weightSum) * vertexColor;
}
