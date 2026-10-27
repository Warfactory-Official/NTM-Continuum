#version 150
// energy_ball.vsh for the ShaderInstance path (before 26.1): all four vertices sit at the ball's centre and are spread
// in view space. UV0.x is the corner (0..3) plus the radius in blocks as its fraction, UV0.y the time in seconds.

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 quad;
out float time;
out vec4 vertexColor;

const vec2 CORNERS[4] = vec2[](vec2(-1.0, -1.0), vec2(-1.0, 1.0), vec2(1.0, 1.0), vec2(1.0, -1.0));

void main() {
    quad = CORNERS[int(UV0.x)];
    time = UV0.y;
    vertexColor = Color;
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    view.xy += quad * fract(UV0.x);
    gl_Position = ProjMat * view;
}
