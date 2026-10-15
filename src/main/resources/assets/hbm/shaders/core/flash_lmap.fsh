#version 330
// 1.12.2 shaders/flash_lmap.frag over vanilla's entity vertex stage (Golem EntityShaderRenderType): the lit,
// lightmapped colour plus the glow map in Sampler3, raised to 1.3, less 0.1 and tinted the muzzle flash's orange.
// The 1.12 program applied no fog.
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler3;

in vec4 vertexColor;
in vec4 lightMapColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
    color *= vertexColor * ColorModulator;
    color *= lightMapColor;
    vec4 fl = texture(Sampler3, texCoord0);
    fragColor = vec4(color.rgb + (pow(fl.rgb, vec3(1.3)) - 0.1) * vec3(1.0, 0.72, 0.46), color.a);
}
