#version 150

uniform sampler2D Sampler0;
uniform float uTextEdgeSoftnessPx;
uniform float uMsdfSharpness;

in vec4 vertexColor;
in vec2 vertexUv;
in vec4 vertexData3;

out vec4 fragColor;

float median3(vec3 sampleValue) {
	return max(min(sampleValue.r, sampleValue.g), min(max(sampleValue.r, sampleValue.g), sampleValue.b));
}

float screenPxRange() {
	vec2 unitRange = vec2(vertexData3.x) / vec2(textureSize(Sampler0, 0));
	vec2 screenTexel = max(vec2(1.0) / fwidth(vertexUv), vec2(1.0));
	return max(0.5 * dot(unitRange, screenTexel), 1.0) * uMsdfSharpness;
}

void main() {
	vec3 msdf = texture(Sampler0, vertexUv).rgb;
	float sd = median3(msdf);
	float pxRange = max(screenPxRange(), 0.0001);
	float softness = max(uTextEdgeSoftnessPx / max(pxRange, 0.0001), 0.0001);
	float alpha = smoothstep(0.5 - softness, 0.5 + softness, sd);
	float fringeSuppression = clamp((1.75 - pxRange) / 1.25, 0.0, 1.0);
	float cutoff = 0.10 * fringeSuppression;
	alpha = clamp((alpha - cutoff) / max(1.0 - cutoff, 0.0001), 0.0, 1.0);
	fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha);
}
