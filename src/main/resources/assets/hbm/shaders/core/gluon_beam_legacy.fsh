#version 150
// 1.12.2 shaders/gluon_beam.frag: the first 0.4 blocks pinch the texture together towards the muzzle. Its noise-offset
// sample was overwritten before use, so only the plain sample is kept. beamX is 1.12's beam_length * texCoord.x.

uniform sampler2D Sampler1;

in vec2 texCoord;
in vec4 vertexColor;
in float beamX;

out vec4 fragColor;

float remap(float num, float min1, float max1, float min2, float max2) {
    return ((num - min1) / (max1 - min1)) * (max2 - min2) + min2;
}

void main() {
    vec2 coord = texCoord;
    float r = clamp(remap(beamX, 0.0, 0.4, 0.0, 1.0), 0.01, 1.0);
    coord.y = clamp((coord.y - 0.5) / r + 0.5, 0.0, 1.0);
    fragColor = texture(Sampler1, coord) * vertexColor * vec4(0.6, 0.85, 2.0, 1.0) * 1.5;
}
