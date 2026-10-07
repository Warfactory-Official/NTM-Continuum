#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;
out vec3 rayPos;
out vec2 lensPos;
flat out vec3 holeCentre;
flat out vec3 discNormal;
flat out vec3 discTangent;
flat out float holeSize;
flat out float lensRange;
flat out float fieldTime;
flat out int fieldType;

// BlackHoleRenderer.Lens.submitField: UV0 = the corner's offset from the hole along the view's right/up axes, in
// blocks (exact, so both triangles of the quad rebuild the same centre); Color.rgb = the field's 1.7 X axis and Color.a = the field type; Normal = the field's normal;
// UV1 = (size * 1024, the world-bending range in shadow radii * 256); UV2 = (tick + partial) * 64 as low/high 16 bits.
float unsignedShort(int value) {
    return value < 0 ? float(value) + 65536.0 : float(value);
}

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    rayPos = Position;
    holeSize = unsignedShort(UV1.x) / 1024.0;
    lensPos = UV0 / holeSize;
    lensRange = unsignedShort(UV1.y) / 256.0;
    fieldTime = (unsignedShort(UV2.x) + unsignedShort(UV2.y) * 65536.0) / 64.0;
    fieldType = int(Color.a * 255.0 + 0.5);
    discTangent = normalize(Color.rgb * 2.0 - 1.0);
    discNormal = normalize(Normal);
    // the view's right and up axes in position space are the model-view rotation's first two rows
    vec3 right = normalize(vec3(ModelViewMat[0][0], ModelViewMat[1][0], ModelViewMat[2][0]));
    vec3 up = normalize(vec3(ModelViewMat[0][1], ModelViewMat[1][1], ModelViewMat[2][1]));
    holeCentre = Position - (UV0.x * right + UV0.y * up);
}
