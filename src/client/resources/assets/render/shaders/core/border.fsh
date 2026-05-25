#version 150

uniform float uEdgeSoftnessPx;

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

vec2 innerOrigin(float strokeAlign, float strokeWidthPx) {
	if (strokeAlign < 0.5) {
		return vec2(strokeWidthPx);
	}
	return vec2(strokeWidthPx);
}

vec2 innerSize(float strokeAlign, vec2 shapeSizePx, float strokeWidthPx) {
	if (strokeAlign < 0.5) {
		return max(shapeSizePx - vec2(strokeWidthPx * 2.0), vec2(0.0));
	}
	if (strokeAlign < 1.5) {
		return max(shapeSizePx - vec2(strokeWidthPx), vec2(0.0));
	}
	return shapeSizePx;
}

vec4 innerRadii(float strokeAlign, vec4 radiiPx, float strokeWidthPx) {
	if (strokeAlign < 0.5) {
		return max(radiiPx - vec4(strokeWidthPx), vec4(0.0));
	}
	if (strokeAlign < 1.5) {
		return max(radiiPx - vec4(strokeWidthPx * 0.5), vec4(0.0));
	}
	return radiiPx;
}

float outerExpansion(float strokeAlign, float strokeWidthPx) {
	if (strokeAlign < 0.5) {
		return 0.0;
	}
	if (strokeAlign < 1.5) {
		return strokeWidthPx * 0.5;
	}
	return strokeWidthPx;
}

void main() {
	vec2 geometryLocalPx = vertexUv * vertexData1.zw;
	float outerDistancePx = sdRoundedRect(
		geometryLocalPx,
		vertexData1.zw,
		vertexData2 + vec4(outerExpansion(vertexData3.y, vertexData3.x))
	);

	vec2 innerOriginPx = innerOrigin(vertexData3.y, vertexData3.x);
	vec2 innerLocalPx = geometryLocalPx - innerOriginPx;
	vec2 innerSizePx = innerSize(vertexData3.y, vertexData0.zw, vertexData3.x);
	vec4 innerCornerRadiiPx = innerRadii(vertexData3.y, vertexData2, vertexData3.x);
	float innerDistancePx = sdRoundedRect(innerLocalPx, innerSizePx, innerCornerRadiiPx);

	float outerMask = 1.0 - smoothstep(-uEdgeSoftnessPx, uEdgeSoftnessPx, outerDistancePx);
	float innerMask = 1.0 - smoothstep(-uEdgeSoftnessPx, uEdgeSoftnessPx, innerDistancePx);
	float strokeMask = clamp(outerMask - innerMask, 0.0, 1.0);

	fragColor = vec4(vertexColor.rgb, vertexColor.a * strokeMask);
}
