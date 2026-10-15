#version 330
// 1.12.2 shaders/gluon_beam.vert, ported. x = UV0.x * len along the beam; the vertex keeps its own billboarded y and z
// and is moved by the noise in the beam's space. TextureMat as in gluon_spiral.vsh. Sampler0 is noise_2.
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

uniform sampler2D Sampler0;

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 texCoord;
out vec4 vertexColor;
out float beamX;

float time;

vec4 getPos(float x, vec2 tex) {
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
    sample_1.rg += sample_3.bg * clamp(pow(tex.s * 2.0, 0.25), 0.0, 1.0) * clamp(pow((1.0 - tex.s) * 2.0, 0.25), 0.0, 1.0);
    return vec4(x, sample_1.r, sample_1.g, 1.0);
}

void main() {
    time = TextureMat[0].w;
    float len = TextureMat[1].w;
    mat3 axes = mat3(TextureMat[0].xyz, TextureMat[1].xyz, TextureMat[2].xyz);

    texCoord = UV0;
    vertexColor = Color;
    beamX = UV0.x * len;
    vec4 moved = getPos(beamX, UV0);
    gl_Position = ProjMat * ModelViewMat * vec4(Position + axes * vec3(0.0, moved.y, moved.z), 1.0);
}
