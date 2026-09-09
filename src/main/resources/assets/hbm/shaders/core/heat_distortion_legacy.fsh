#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform float time;
uniform vec2 windowSize;
in vec2 texCoord;
in vec4 distortionParams;
out vec4 fragColor;
void main() {
    vec4 mask = texture(Sampler1, texCoord);
    float particleTime = time + distortionParams.g * 20.0;
    vec2 offset = texture(Sampler2, texCoord * 0.5 + vec2(particleTime * 0.2)).rg * 2.0 - 1.0;
    offset *= texture(Sampler2, texCoord * 0.3 - vec2(particleTime * 0.15)).gb * 2.0 - 1.0;
    offset *= distortionParams.r * smoothstep(0.0, 1.0, mask.a) * 0.1;
    vec2 screenUv = gl_FragCoord.xy / windowSize;
    fragColor = texture(Sampler0, screenUv + offset);
}
