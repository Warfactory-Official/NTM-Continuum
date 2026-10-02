#version 150
// old-hbm shaders/crucible_lightning.frag: the vertex colour, nothing else.

in vec4 vertexColor;

out vec4 fragColor;

void main() {
    fragColor = vertexColor;
}
