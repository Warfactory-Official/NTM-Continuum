#version 330
#moj_import <minecraft:globals.glsl>
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
in vec2 texCoord0;
in vec4 distortionParams;
flat in float distortionTime;
out vec4 fragColor;
void main() {
    vec4 mask = texture(Sampler1, texCoord0);
    float time = distortionTime;
    vec2 offset = texture(Sampler2, texCoord0 * 0.5 + time * 0.2).rg * 2.0 - 1.0;
    offset *= texture(Sampler2, texCoord0 * 0.3 - time * 0.15).gb * 2.0 - 1.0;
    offset *= distortionParams.r * smoothstep(0.0, 1.0, mask.a) * 0.1;
    vec2 screenUv = gl_FragCoord.xy / ScreenSize;
    fragColor = texture(Sampler0, screenUv + offset);
}
