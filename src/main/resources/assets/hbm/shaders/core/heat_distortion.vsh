#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
in vec3 Position;
in vec2 UV0;
in vec4 Color;
in ivec2 UV1;
out vec2 texCoord0;
out vec4 distortionParams;
flat out float distortionTime;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord0 = UV0;
    distortionParams = Color;
    float low = float(UV1.x);
    float high = float(UV1.y);
    if (low < 0.0) low += 65536.0;
    if (high < 0.0) high += 65536.0;
    distortionTime = (low + high * 65536.0) / 4294967295.0 * 16384.0;
}
