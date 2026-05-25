#version 150

uniform vec2 uViewportSize;

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec4 Data0;
in vec4 Data1;
in vec4 Data2;
in vec4 Data3;

out vec4 vertexColor;
out vec2 vertexUv;
out vec4 vertexData0;
out vec4 vertexData1;
out vec4 vertexData2;
out vec4 vertexData3;

void main() {
	vec2 ndc = (Position.xy / uViewportSize) * 2.0 - 1.0;
	gl_Position = vec4(ndc.x, -ndc.y, 0.0, 1.0);
	vertexColor = Color;
	vertexUv = UV0;
	vertexData0 = Data0;
	vertexData1 = Data1;
	vertexData2 = Data2;
	vertexData3 = Data3;
}
