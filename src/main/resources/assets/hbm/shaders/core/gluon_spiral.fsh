#version 330
// 1.12.2 shaders/gluon_spiral.frag: Sampler1 is the texture 1.12 left bound on unit 0, bfg_core_lightning.

uniform sampler2D Sampler1;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    fragColor = texture(Sampler1, texCoord) * vertexColor * vec4(0.6, 0.8, 1.0, 1.0) * 1.5;
}
