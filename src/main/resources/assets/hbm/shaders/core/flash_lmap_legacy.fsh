#version 150
// The ShaderInstance path (before 26.1) of flash_lmap.fsh, over vanilla's rendertype_entity_cutout vertex stage.

uniform sampler2D Sampler0;
uniform sampler2D Sampler3;
uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec4 lightMapColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) {
        discard;
    }
    color *= vertexColor * ColorModulator;
    color *= lightMapColor;
    vec4 fl = texture(Sampler3, texCoord0);
    fragColor = vec4(color.rgb + (pow(fl.rgb, vec3(1.3)) - 0.1) * vec3(1.0, 0.72, 0.46), color.a);
}
