#version 330
// 1.12.2 shaders/gluon_spiral.vert, ported. The beam's own space is rebuilt from the vertex: x = UV0.x * len along the
// beam, the strip side from UV0.y (0.45 below, 0.55 above). TextureMat (Golem NoiseDisplacedRenderType.params) carries
// the beam: columns 0..2 its axes in render space, column 3 the camera in beam space, and in the w row the time, the
// length, 1 / (subdivisions + 1) and the size. 1.12 bound noise_2 to both noise uniforms, so Sampler0 is noise_2.
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

uniform sampler2D Sampler0;

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 texCoord;
out vec4 vertexColor;

const float subdivXAmount = 1.0 / 16.0;

float time;
float len;

vec4 getPos(float x, vec2 tex, float vz) {
    vec2 texCoord1 = vec2(x / 16.0 - time * 0.7, 0.5 - time * 0.4);
    vec2 texCoord1_1 = vec2(x / 16.0 - time * 0.7, 0.9 - time * 0.05);
    vec2 texCoord2 = vec2(x / 12.0 - time * 0.8, 0.2 - time * 0.6);
    vec2 texCoord3 = vec2(tex.s / 8.0 - time * 0.05, 0.2 - time * 0.3);
    vec4 sample_1 = (textureLod(Sampler0, texCoord1, 0.0) * 2.0 - 1.0)
            * clamp(pow(textureLod(Sampler0, texCoord1_1, 0.0).b, 8.0) * 12.0, 0.0, 0.4);
    vec4 sample_2 = textureLod(Sampler0, texCoord2, 0.0);
    vec4 sample_3 = (smoothstep(0.0, 1.0, textureLod(Sampler0, texCoord3, 0.0)) - 0.5) * 0.4;
    sample_1 += (sample_2 * 2.0 - 1.0) * 0.4 * pow(sample_2.g, 4.0);
    sample_1 *= clamp(tex.x * 20.0, 0.0, 1.0) * clamp(20.0 - tex.x * 20.0, 0.0, 1.0);
    sample_1.rg += vec2(sin(x * 6.0 - time * 20.0) * 4.0 * abs(vz), cos(x * 6.0 - time * 20.0) * 4.0 * abs(vz))
            * clamp(x * 2.0, 0.0, 1.0) * clamp((len - x) * 2.0, 0.0, 1.0);
    sample_1.rg += sample_3.bg * clamp(pow(tex.s * 2.0, 0.25), 0.0, 1.0) * clamp(pow((1.0 - tex.s) * 2.0, 0.25), 0.0, 1.0);
    return vec4(x, sample_1.r, sample_1.g, 1.0);
}

void main() {
    time = TextureMat[0].w;
    len = TextureMat[1].w;
    float subdivUAmount = TextureMat[2].w;
    vec3 playerPos = TextureMat[3].xyz;
    float size = TextureMat[3].w;
    mat3 axes = mat3(TextureMat[0].xyz, TextureMat[1].xyz, TextureMat[2].xyz);

    texCoord = UV0;
    vertexColor = Color;
    float x = UV0.x * len;
    float vz = (UV0.y > 0.5 ? 1.0 : -1.0) * size * 0.025;
    vec3 pos = getPos(x, UV0, vz).xyz;
    vec3 pos2 = getPos(x + subdivXAmount, vec2(UV0.x + subdivUAmount, UV0.y), vz).xyz;
    vec3 across = pos - pos2;
    vec3 side = normalize(cross(pos - playerPos, across)) * (vz > 0.0 ? 1.0 : -1.0) * abs(vz);

    vec3 moved = pos + side - vec3(x, 0.0, vz);
    gl_Position = ProjMat * ModelViewMat * vec4(Position + axes * moved, 1.0);
}
