#version 330
// old-hbm shaders/crucible_lightning.vert, ported: the per-particle time rides UV0.y, the sample coordinate is UV0.x
// (the old "tex"), and the object-space offset is mapped to render space by TextureMat (Golem NoiseDisplacedRenderType).
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

uniform sampler2D Sampler0;

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
    gl_Position = ProjMat * ModelViewMat * vec4(Position + (TextureMat * vec4(offset, 0.0)).xyz, 1.0);
}
