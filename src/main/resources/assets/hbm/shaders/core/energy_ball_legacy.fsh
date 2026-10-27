#version 150
// A white core, a shell of noise turning around it and thin arcs where two noise layers cross, tinted by the vertex
// colour and drawn SRC_ALPHA / ONE.

uniform sampler2D Sampler0;

in vec2 quad;
in float time;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float d = length(quad);
    if (d >= 1.0) discard;
    float angle = atan(quad.y, quad.x) / 6.2831853;
    float swirl = texture(Sampler0, vec2(angle + time * 0.15 + d * 0.3, d * 0.6 - time * 0.4)).r;
    float churn = texture(Sampler0, quad * 0.35 + vec2(time * 0.07, -time * 0.05)).g;
    float arcs = 1.0 - smoothstep(0.0, 0.06, abs(swirl - churn));
    float core = smoothstep(0.45, 0.0, d);
    float shell = smoothstep(1.0, 0.55, d);
    float glow = pow(1.0 - d, 1.5);
    vec3 color = vertexColor.rgb * (glow * 0.9 + swirl * churn * shell * 1.6 + arcs * shell * 1.2) + vec3(core * 1.3);
    fragColor = vec4(min(color, 1.0), clamp(max(max(color.r, color.g), color.b), 0.0, 1.0) * vertexColor.a);
}
