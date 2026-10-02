#version 150
// old-hbm shaders/crucible_lightning.vert for the ShaderInstance path (before 26.1): LocalMat maps the object-space
// offset to the posed space of the submitted vertices; UV0 = (tex, time).

uniform sampler2D Sampler0;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 LocalMat;

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec4 vertexColor;

void main() {
    vertexColor = Color;
    float time = UV0.y;
    vec3 noise_a = textureLod(Sampler0, UV0.xx + vec2(time * 0.001, time * -0.02), 0.0).xyz;
    vec3 noise_b = textureLod(Sampler0, UV0.xx + vec2(time * 0.01, time * 0.015), 0.0).xyz;
    vec3 offset = (noise_a.xyz * noise_b.zxy - vec3(0.5)) * 0.1;
    gl_Position = ProjMat * ModelViewMat * vec4(Position + (LocalMat * vec4(offset, 0.0)).xyz, 1.0);
}
