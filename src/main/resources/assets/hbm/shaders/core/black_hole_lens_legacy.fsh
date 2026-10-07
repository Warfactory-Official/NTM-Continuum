#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;   // Core SceneWarp: the scene under the world's translucent layer (water, glass)
uniform sampler2D Sampler3;   // its alpha: how much that layer covers each pixel
uniform sampler2D Sampler4;   // the world depth including that layer
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
in vec3 rayPos;
in vec2 lensPos;
flat in vec3 holeCentre;
flat in vec3 discNormal;
flat in vec3 discTangent;
flat in float holeSize;
flat in float lensRange;
flat in float fieldTime;
flat in int fieldType;
out vec4 fragColor;

// Port-only black hole (1.7 has no lensing): every pixel of the impostor quad follows its light path through the
// Schwarzschild geometry (the photon orbit equation u'' = 3Mu^2 - u, integrated in the orbit's own plane).
// Each crossing of the field's plane shows the 1.7 accretion field at that point: the 15 counter-rotating disc
// rings of RenderBlackHole.renderDisc (or RenderQuasar), or the two swirl rings of renderSwirl, with their
// colours, textures and blending; each crossing of renderJets' two cones adds their glow. Everything is
// composited in the order the light meets it, so the hole and the disc hide what is behind them. A path that falls in is the shadow; one that escapes shows the scene copy
// where it lands, assuming the background is LENS_BACKGROUND blocks behind the hole. The primary image, the
// arcs over and under the shadow and the thin higher-order rings all come from the paths themselves.
// Inputs (BlackHoleRenderer.Lens.submitField): all in the vertex's position space, camera at the origin.

const float LENS_BACKGROUND = 32.0;   // blocks; must match BlackHoleRenderer.LENS_BACKGROUND
const float SQRT27 = 5.196152;        // the shadow radius b_c is 3*sqrt(3) M, and the 1.7 sphere radius (size)
const float START = 60.0;             // in M: rays are straight lines beyond this (and the field reaches 32 M)
const int MAX_STEPS = 360;
const int MAX_EVENTS = 8;
// renderJets: two additive cones along the field's normal, radius 0.5 at length 10 (sizes), alpha 0.35 at the
// apex fading to 0 at the rim; a cone point satisfies |x|^2 = (1 + (0.5 / 10)^2) * (x . n)^2
const float JET_SLOPE2 = 1.0025;
const float JET_LENGTH = 10.0 * SQRT27;  // in M

// The fitted Schwarzschild deflection in x = b_c / b (the same fit as the Java side).
float deflection(float b) {
    float x = min(SQRT27 / b, 0.99999);
    return x * (-0.2661 + x * (0.1192 - 0.2262 * x)) - 0.9965 * log(1.0 - x);
}

vec3 ringColour(int i) {
    float k = float(i);
    if (fieldType == 1) {
        // 1.7 RenderQuasar.setColorFromIteration
        float gb = (k / 15.0) * (k / 15.0);
        return vec3(1.0, gb, gb);
    }
    // 1.7 RenderBlackHole.setColorFromIteration, clamped like Tessellator.setColorRGBA_F
    if (i < 5) return vec3(1.0, 0.125 + k * 0.1, 0.0);
    if (i == 5) return vec3(1.0, 1.0, 0.0);
    float j = k - 6.0;
    return clamp(vec3(1.0 - j / 9.0, 1.0 - j / 9.0, j / 5.0), 0.0, 1.0);
}

vec4 fieldTexture(float uvRadius, float angle) {
    return textureLod(Sampler1, vec2(0.5) + uvRadius * vec2(cos(angle), sin(angle)), 0.0);
}

// One crossing of the field plane at radius rho (in sizes) and angle theta, drawn over dst like 1.7 drew it:
// per ring, a translucent pass (SRC_ALPHA, ONE_MINUS_SRC_ALPHA) and then the glow (SRC_ALPHA, ONE).
vec3 field(vec3 dst, float rho, float theta) {
    if (fieldType >= 2) {
        // renderSwirl: rotates -5 degrees per tick; UV radius rho / 12 on both rings
        vec3 colour = fieldType == 3 ? vec3(0.9098, 0.2235, 0.0510) : vec3(0.2196, 0.5961, 0.7020);
        float glow = fieldType == 3 ? 0.25 : 0.75;
        vec4 tex = fieldTexture(rho / 12.0, theta - radians(mod(fieldTime * 5.0, 360.0)));
        if (rho >= 0.9 && rho <= 3.0) {
            float t = (rho - 0.9) / 2.1;
            vec4 src = tex * vec4(colour * t, 1.0);
            dst = src.rgb * src.a + dst * (1.0 - src.a);
            vec4 add = tex * vec4(vec3(t), mix(1.0, glow, t));
            dst += add.rgb * add.a;
        } else if (rho > 3.0 && rho <= 6.0) {
            float t = (rho - 3.0) / 3.0;
            vec4 src = tex * vec4(colour, 1.0 - t);
            dst = src.rgb * src.a + dst * (1.0 - src.a);
            vec4 add = tex * vec4(mix(vec3(1.0), colour, t), glow * (1.0 - t));
            dst += add.rgb * add.a;
        }
        return dst;
    }
    for (int i = 0; i < 15; i++) {
        float inner = 3.0 - float(i) * 0.175;
        if (rho < inner || rho > 2.0 * inner) continue;
        float t = (rho - inner) / inner;
        // ring i turns -(tick + partial) * (i + 1)^1.25 degrees about the disc normal
        float spin = radians(mod(fieldTime * pow(float(i + 1), 1.25), 360.0));
        vec4 tex = fieldTexture(0.25 + 0.25 * t, theta - spin);
        vec3 colour = ringColour(i);
        vec4 src = tex * vec4(colour, 1.0 - t);
        dst = src.rgb * src.a + dst * (1.0 - src.a);
        vec4 add = tex * vec4(mix(vec3(1.0), colour, t), 0.75 * (1.0 - t));
        dst += add.rgb * add.a;
    }
    return dst;
}

// The background at a point behind the hole. Where water or glass lies in front of the hole there, that surface is
// not behind it: the scene under the translucent layer is the background, and main() lays the layer over the result.
// (The impostor faces the camera, so its own depth is the hole's.)
vec3 sceneAt(vec3 point) {
    vec4 clip = ProjMat * ModelViewMat * vec4(point, 1.0);
    vec2 uv = clip.w <= 0.05 ? vec2(0.5) : clamp(clip.xy / clip.w * 0.5 + 0.5, 0.0, 1.0);
    if (texture(Sampler4, uv).r < gl_FragCoord.z) return texture(Sampler2, uv).rgb;
    return texture(Sampler0, uv).rgb;
}

vec4 blackHole() {
    float mass = holeSize / SQRT27;                 // M in blocks
    vec3 d = normalize(rayPos);
    vec3 o = -holeCentre / mass;                    // camera relative to the hole, in M
    float r0 = length(o);
    vec3 e1 = o / r0;
    float toward = dot(d, e1);                      // < 0 when the ray heads towards the hole
    vec3 side = d - toward * e1;
    float b = length(side) * r0;                    // impact parameter, in M
    if (b < 1e-4) return vec4(0.0, 0.0, 0.0, 1.0);
    vec3 e2 = side / length(side);
    float c = asin(clamp(b / r0, 0.0, 1.0));        // the straight line is r = b / sin(phi + c)
    if (toward > 0.0) c = 3.14159265 - c;

    // the bending fades out by lensRange, so the lensed world has no seam against the unbent one
    float edge = length(lensPos);
    float fade = 1.0 - smoothstep(0.6 * lensRange, lensRange, edge);

    float alpha;                                    // total deflection
    bool captured = false;
    // what the light meets on its way, nearest first: kind 0 = field crossing (rho, theta), 1 = jet (glow, -)
    int kind[MAX_EVENTS];
    float eventA[MAX_EVENTS];
    float eventB[MAX_EVENTS];
    int events = 0;
    bool jets = fieldType != 2;                     // 1.7: a plain vortex has no jets
    float limit = fieldType >= 2 ? 0.9 : 0.55;      // the field's inner radius, in sizes
    float a1 = dot(e1, discNormal), a2 = dot(e2, discNormal);
    vec3 t2 = cross(discTangent, discNormal);

    if (b >= START || (r0 > START && toward >= 0.0)) {
        alpha = deflection(b);
    } else {
        float phi, u, w;
        if (r0 > START) {
            phi = asin(b / START) - c;
            u = sin(phi + c) / b;
            w = cos(phi + c) / b;
        } else {
            phi = 0.0;
            u = 1.0 / r0;
            w = -u * toward / (b / r0);
        }
        float f = cos(phi) * a1 + sin(phi) * a2;
        bool escaped = false;
        for (int step = 0; step < MAX_STEPS; step++) {
            float h = u > 0.08 ? 0.03 : 0.08;
            // RK4 on u' = w, w' = 3u^2 - u
            float k1u = w, k1w = 3.0 * u * u - u;
            float u2 = u + 0.5 * h * k1u, w2 = w + 0.5 * h * k1w;
            float k2u = w2, k2w = 3.0 * u2 * u2 - u2;
            float u3 = u + 0.5 * h * k2u, w3 = w + 0.5 * h * k2w;
            float k3u = w3, k3w = 3.0 * u3 * u3 - u3;
            float u4 = u + h * k3u, w4 = w + h * k3w;
            float k4u = w4, k4w = 3.0 * u4 * u4 - u4;
            float nu = u + h / 6.0 * (k1u + 2.0 * k2u + 2.0 * k3u + k4u);
            float nw = w + h / 6.0 * (k1w + 2.0 * k2w + 2.0 * k3w + k4w);
            float nphi = phi + h;
            float nf = cos(nphi) * a1 + sin(nphi) * a2;
            // this step's events, by their fraction s along the step (2 = none)
            float sField = 2.0, fieldRho = 0.0, fieldTheta = 0.0;
            if ((f < 0.0) != (nf < 0.0)) {
                float s = f / (f - nf);
                float cu = mix(u, nu, s);
                float cphi = mix(phi, nphi, s);
                if (cu > 0.0 && cu < 0.5) {
                    float r = 1.0 / cu;
                    vec3 p = r * (cos(cphi) * e1 + sin(cphi) * e2);
                    float rhoHere = r / SQRT27;
                    if (rhoHere >= limit && rhoHere <= 6.0) {
                        sField = s;
                        fieldRho = rhoHere;
                        fieldTheta = atan(dot(p, t2), dot(p, discTangent));
                    }
                }
            }
            float sJet0 = 2.0, sJet1 = 2.0, jet0 = 0.0, jet1 = 0.0;
            if (jets && nu > 1e-4 && max(u, nu) > 1.0 / (1.1 * JET_LENGTH)) {
                // the step as a straight chord, against the double cone
                vec3 p0 = (cos(phi) * e1 + sin(phi) * e2) / u;
                vec3 dv = (cos(nphi) * e1 + sin(nphi) * e2) / nu - p0;
                float an = dot(p0, discNormal), dn = dot(dv, discNormal);
                float qa = dot(dv, dv) - JET_SLOPE2 * dn * dn;
                float qb = 2.0 * (dot(p0, dv) - JET_SLOPE2 * an * dn);
                float qc = dot(p0, p0) - JET_SLOPE2 * an * an;
                float root = qb * qb - 4.0 * qa * qc;
                if (abs(qa) > 1e-8 && root >= 0.0) {
                    float q = sqrt(root);
                    float s0 = (-qb - q) / (2.0 * qa), s1 = (-qb + q) / (2.0 * qa);
                    if (s0 > s1) { float swap = s0; s0 = s1; s1 = swap; }
                    float h0 = abs(an + s0 * dn), h1 = abs(an + s1 * dn);
                    if (s0 >= 0.0 && s0 < 1.0 && h0 <= JET_LENGTH) { sJet0 = s0; jet0 = 0.35 * (1.0 - h0 / JET_LENGTH); }
                    if (s1 >= 0.0 && s1 < 1.0 && h1 <= JET_LENGTH) { sJet1 = s1; jet1 = 0.35 * (1.0 - h1 / JET_LENGTH); }
                }
            }
            for (int e = 0; e < 3; e++) {
                float nearest = min(sField, min(sJet0, sJet1));
                if (nearest >= 2.0 || events >= MAX_EVENTS) break;
                if (nearest == sField) {
                    kind[events] = 0; eventA[events] = fieldRho; eventB[events] = fieldTheta; sField = 2.0;
                } else if (nearest == sJet0) {
                    kind[events] = 1; eventA[events] = jet0; eventB[events] = 0.0; sJet0 = 2.0;
                } else {
                    kind[events] = 1; eventA[events] = jet1; eventB[events] = 0.0; sJet1 = 2.0;
                }
                events++;
            }
            u = nu; w = nw; phi = nphi; f = nf;
            if (u >= 0.5) { captured = true; break; }
            if (u <= 1.0 / START && w < 0.0) { escaped = true; break; }
        }
        if (!escaped) captured = true;
        // the rest of the way out is straight: it reaches infinity where u = 0
        alpha = phi + 3.14159265 - atan(u, w) - (3.14159265 - c);
    }

    vec3 colour;
    if (captured) {
        colour = vec3(0.0);
    } else {
        // the escaping line: the incoming one turned by alpha in the orbit plane (direction and closest point)
        float bend = alpha * fade;
        vec3 inDir = d;
        vec3 inNear = -toward * r0 * d + o;         // closest point of the straight incoming line, in M
        vec3 axisN = normalize(cross(e1, e2));
        vec3 outDir = inDir * cos(bend) + cross(axisN, inDir) * sin(bend);
        vec3 outNear = inNear * cos(bend) + cross(axisN, inNear) * sin(bend);
        vec3 axis = normalize(holeCentre);
        float along = dot(outDir, axis);
        float reach = along > 0.05 ? (LENS_BACKGROUND - dot(outNear * mass, axis)) / along : LENS_BACKGROUND;
        colour = sceneAt(holeCentre + outNear * mass + outDir * reach);
    }
    // back to front: the farthest event is drawn first
    for (int i = MAX_EVENTS - 1; i >= 0; i--) {
        if (i >= events) continue;
        if (kind[i] == 0) colour = field(colour, eventA[i], eventB[i]);
        else colour += vec3(eventA[i]);             // the jets' SRC_ALPHA, ONE glow, white
    }
    return vec4(colour, 1.0);
}

void main() {
    vec4 hole = blackHole();
    // Core SceneWarp: seen through water or glass, the layer in front goes back over the hole (scene = layer over
    // what was under it, so layer over the hole = scene + (hole - under) * transmission)
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    if (texelFetch(Sampler4, pixel, 0).r < gl_FragCoord.z) {
        float through = 1.0 - texelFetch(Sampler3, pixel, 0).a;
        hole.rgb = texelFetch(Sampler0, pixel, 0).rgb + (hole.rgb - texelFetch(Sampler2, pixel, 0).rgb) * through;
    }
    fragColor = hole;
}
