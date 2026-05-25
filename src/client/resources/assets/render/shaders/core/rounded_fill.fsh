#version 150

uniform float uEdgeSoftnessPx;

in vec4 vertexColor;
in vec2 vertexUv;
in vec4 vertexData0;
in vec4 vertexData1;
in vec4 vertexData2;

out vec4 fragColor;

float cornerRadius(vec2 p, vec2 sizePx, vec4 radiiPx) {
	bool left = p.x <= sizePx.x * 0.5;
	bool top = p.y <= sizePx.y * 0.5;
	if (left && top) {
		return radiiPx.x;
	}
	if (!left && top) {
		return radiiPx.y;
	}
	if (!left && !top) {
		return radiiPx.z;
	}
	return radiiPx.w;
}

float sdRoundedRect(vec2 p, vec2 sizePx, vec4 radiiPx) {
	float radius = cornerRadius(p, sizePx, radiiPx);
	vec2 halfSize = sizePx * 0.5;
	vec2 centered = p - halfSize;
	vec2 q = abs(centered) - (halfSize - vec2(radius));
	return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - radius;
}

void main() {
	vec2 localPx = vertexUv * vertexData0.zw;
	float distancePx = sdRoundedRect(localPx, vertexData0.zw, vertexData2);
	float alpha = 1.0 - smoothstep(-uEdgeSoftnessPx, uEdgeSoftnessPx, distancePx);
	fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha);
}
