#version 150

in vec4 vertexColor;
in vec2 vertexUv;
in vec4 vertexData0;
in vec4 vertexData1;
in vec4 vertexData2;
in vec4 vertexData3;

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
	vec2 screenPx = vertexData1.xy + vertexUv * vertexData1.zw;
	vec2 localPx = screenPx - vertexData0.xy;
	float distancePx = sdRoundedRect(localPx, vertexData0.zw, vertexData2);
	float outsideDistancePx = max(distancePx, 0.0);
	float shadowSoftnessPx = max(vertexData3.z, 0.0);
	float sigma = max(shadowSoftnessPx * 0.78, 0.75);
	float gaussian = exp(-(outsideDistancePx * outsideDistancePx) / (2.0 * sigma * sigma));
	float borderMask = smoothstep(-shadowSoftnessPx * 0.5, 1.0, distancePx);
	float alpha = gaussian * borderMask;

	if (alpha <= 0.001) {
		discard;
	}

	fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha);
}
