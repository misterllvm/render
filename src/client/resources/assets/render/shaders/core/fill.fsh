#version 150

uniform float uEdgeSoftnessPx;

in vec4 vertexColor;
in vec2 vertexUv;
in vec4 vertexData0;

out vec4 fragColor;

float edgeAlpha(vec2 localPx, vec2 sizePx, float softnessPx) {
	if (softnessPx <= 0.0) {
		return 1.0;
	}
	vec2 edge = min(localPx, sizePx - localPx);
	float distanceToEdge = min(edge.x, edge.y);
	return clamp(distanceToEdge / softnessPx, 0.0, 1.0);
}

void main() {
	vec2 localPx = vertexUv * vertexData0.zw;
	float alpha = edgeAlpha(localPx, vertexData0.zw, uEdgeSoftnessPx);
	fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha);
}
