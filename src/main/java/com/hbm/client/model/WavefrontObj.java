package com.hbm.client.model;

import dev.golem.core.Ids;
import dev.golem.core.render.Vertexes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal Wavefront OBJ loader + immediate-mode renderer for the new render pipeline. Ported from the
 * old mod's {@code HFRWavefrontObject} but stripped of the LWJGL VBO/VAO machinery (which has no 26.1
 * equivalent) — instead it emits geometry straight into a {@link VertexConsumer}, so it works inside a
 * {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer}'s submit pass.
 *
 * <p>Geometry is grouped by OBJ {@code o}/{@code g} objects so a renderer can draw the whole model
 * ({@link #renderAll}) or a single named part ({@link #renderPart}). Faces may be tris, quads or
 * n-gons; they are fan-triangulated and each triangle is emitted as a degenerate quad (4th vertex =
 * 3rd) because entity render types draw in QUADS mode. UV V is flipped ({@code 1 - v}) to match the
 * old loader.
 */
public final class WavefrontObj {

    /** One OBJ object/group: a name and its faces (each face = vertex refs of [posIdx, uvIdx, normalIdx]).
     * A {@code usemtl} directive starts a new group with the same name but a new {@link #material}, so
     * multi-texture models can be drawn one material at a time ({@link #renderMaterial}). */
    private static final class Group {
        final String name;
        final String material;
        final List<int[][]> faces = new ArrayList<>();
        Group(String name, String material) { this.name = name; this.material = material; }
    }

    private final float[][] positions;
    private final float[][] uvs;
    private final float[][] normals;
    private final List<Group> groups;
    private final float[] min;
    private final float[] max;

    private WavefrontObj(List<float[]> pos, List<float[]> uv, List<float[]> norm, List<Group> groups) {
        this.positions = pos.toArray(new float[0][]);
        this.uvs = uv.toArray(new float[0][]);
        this.normals = norm.toArray(new float[0][]);
        this.groups = groups;
        float[] mn = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] mx = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (float[] p : this.positions) {
            for (int i = 0; i < 3; i++) {
                mn[i] = Math.min(mn[i], p[i]);
                mx[i] = Math.max(mx[i], p[i]);
            }
        }
        this.min = mn;
        this.max = mx;
    }

    /** Model-space minimum corner [x,y,z]. */
    public float[] boundsMin() { return min; }
    /** Model-space maximum corner [x,y,z]. */
    public float[] boundsMax() { return max; }

    /**
     * The bounding box of everything except the named parts, as {@code {min, max}}.
     *
     * <p>Needed when a model is drawn in pieces: an armour suit's OBJ holds all eight body parts, but a
     * helmet's inventory icon draws one of them. Framing that icon by the <em>whole</em> model's bounds
     * centres the slot on the suit's waist and leaves the helmet somewhere above the frame. An empty
     * exclusion set gives the whole-model bounds, so callers can pass one unconditionally.
     */
    public float[][] boundsExcluding(java.util.Set<String> excluded) {
        if (excluded.isEmpty()) return new float[][] {min, max};

        float[] mn = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] mx = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        boolean any = false;

        for (Group group : groups) {
            if (excluded.contains(group.name)) continue;
            for (int[][] face : group.faces) {
                for (int[] vertex : face) {
                    float[] p = positions[vertex[0]];
                    any = true;
                    for (int i = 0; i < 3; i++) {
                        mn[i] = Math.min(mn[i], p[i]);
                        mx[i] = Math.max(mx[i], p[i]);
                    }
                }
            }
        }
        return any ? new float[][] {mn, mx} : new float[][] {min, max};
    }

    /** Loads and parses an OBJ from the client resource pack at {@code location}. */
    public static WavefrontObj load(Identifier location) {
        List<float[]> pos = new ArrayList<>();
        List<float[]> uv = new ArrayList<>();
        List<float[]> norm = new ArrayList<>();
        List<Group> groups = new ArrayList<>();
        Group current = new Group("default", "");
        groups.add(current);

        try (InputStream in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(location).open();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                String[] t = line.split("\\s+");
                switch (t[0]) {
                    case "v" -> pos.add(new float[]{Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])});
                    case "vt" -> uv.add(new float[]{Float.parseFloat(t[1]), 1.0F - Float.parseFloat(t[2])});
                    case "vn" -> norm.add(new float[]{Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])});
                    case "o", "g" -> {
                        // Some exporters emit a leading empty default group; reuse it instead of leaving it dangling.
                        if (current.faces.isEmpty() && groups.size() == 1) {
                            groups.set(0, current = new Group(t.length > 1 ? t[1] : "default", current.material));
                        } else {
                            groups.add(current = new Group(t.length > 1 ? t[1] : "default", current.material));
                        }
                    }
                    case "usemtl" -> {
                        // Same part name, new material run (so multi-texture models can render per material).
                        String mtl = t.length > 1 ? t[1] : "";
                        if (current.faces.isEmpty()) {
                            Group replacement = new Group(current.name, mtl);
                            groups.set(groups.indexOf(current), replacement);
                            current = replacement;
                        } else if (!current.material.equals(mtl)) {
                            groups.add(current = new Group(current.name, mtl));
                        }
                    }
                    case "f" -> {
                        int n = t.length - 1;
                        int[][] face = new int[n][];
                        for (int i = 0; i < n; i++) face[i] = parseRef(t[i + 1]);
                        current.faces.add(face);
                    }
                    default -> { /* ignore mtllib/s etc. */ }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load OBJ model " + location, e);
        }
        return new WavefrontObj(pos, uv, norm, groups);
    }

    /** The distinct material names used by this model, in first-use order (empty string = untextured runs). */
    public List<String> materialNames() {
        List<String> names = new ArrayList<>();
        for (Group g : groups) if (!g.faces.isEmpty() && !names.contains(g.material)) names.add(g.material);
        return names;
    }

    /** Draws every face run using the given material (for multi-texture OBJs — one call per material). */
    public void renderMaterial(String material, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay) {
        for (Group g : groups) if (g.material.equals(material)) drawGroup(g, pose, buffer, light, overlay);
    }

    /**
     * Parses a Wavefront .mtl next to the OBJ and maps material name → texture id (each {@code map_Kd}
     * like {@code hbm:blocks/foo} resolved to {@code hbm:textures/blocks/foo.png}).
     */
    public static java.util.Map<String, Identifier> loadMtl(Identifier mtlLocation) {
        java.util.Map<String, Identifier> map = new java.util.LinkedHashMap<>();
        try (InputStream in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(mtlLocation).open();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            String current = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                String[] t = line.split("\\s+");
                if (t[0].equals("newmtl") && t.length > 1) {
                    current = t[1];
                } else if (t[0].equals("map_Kd") && t.length > 1 && current != null) {
                    Identifier tex = Ids.parse(t[1]);
                    map.put(current, Ids.of(tex.getNamespace(), "textures/" + tex.getPath() + ".png"));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load MTL " + mtlLocation, e);
        }
        return map;
    }

    /** Parses an OBJ face vertex ref "v", "v/vt", "v//vn" or "v/vt/vn" into 0-based [pos, uv, normal] (-1 = absent). */
    private static int[] parseRef(String s) {
        String[] p = s.split("/", -1);
        int vi = Integer.parseInt(p[0]) - 1;
        int ti = p.length > 1 && !p[1].isEmpty() ? Integer.parseInt(p[1]) - 1 : -1;
        int ni = p.length > 2 && !p[2].isEmpty() ? Integer.parseInt(p[2]) - 1 : -1;
        return new int[]{vi, ti, ni};
    }

    /** Draws every part of the model. */
    public void renderAll(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay) {
        renderExcept(pose, buffer, light, overlay, java.util.Set.of());
    }

    /** Draws every part and optionally emits reversed faces for legacy double-sided models. */
    public void renderAll(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, boolean doubleSided) {
        renderExcept(pose, buffer, light, overlay, java.util.Set.of(), doubleSided);
    }

    /** Draws every part except those named in {@code exclude} (e.g. dynamic parts like a fluid overlay). */
    public void renderExcept(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, java.util.Set<String> exclude) {
        renderExcept(pose, buffer, light, overlay, exclude, false);
    }

    public void renderExcept(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay,
                             java.util.Set<String> exclude, boolean doubleSided) {
        for (Group g : groups) if (!exclude.contains(g.name)) drawGroup(g, pose, buffer, light, overlay, -1, doubleSided);
    }

    /** Draws a single named part (no-op if the part doesn't exist). */
    public void renderPart(String name, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay) {
        renderPart(name, pose, buffer, light, overlay, -1);
    }

    /** Draws a single named part with a vertex colour (e.g. an overlay tinted by a fluid colour). */
    public void renderPart(String name, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
        for (Group g : groups) if (g.name.equals(name)) drawGroup(g, pose, buffer, light, overlay, color);
    }

    private void drawGroup(Group g, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay) {
        drawGroup(g, pose, buffer, light, overlay, -1, false);
    }

    private void drawGroup(Group g, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
        drawGroup(g, pose, buffer, light, overlay, color, false);
    }

    private void drawGroup(Group g, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay,
                           int color, boolean doubleSided) {
        for (int[][] face : g.faces) {
            // Fan-triangulate; emit each triangle as a quad (4th vertex duplicates the 3rd) for QUADS mode.
            for (int i = 1; i + 1 < face.length; i++) {
                emit(face[0], pose, buffer, light, overlay, color);
                emit(face[i], pose, buffer, light, overlay, color);
                emit(face[i + 1], pose, buffer, light, overlay, color);
                emit(face[i + 1], pose, buffer, light, overlay, color);
                if (doubleSided) {
                    emit(face[i + 1], pose, buffer, light, overlay, color, true);
                    emit(face[i + 1], pose, buffer, light, overlay, color, true);
                    emit(face[i], pose, buffer, light, overlay, color, true);
                    emit(face[0], pose, buffer, light, overlay, color, true);
                }
            }
        }
    }

    private void emit(int[] ref, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
        emit(ref, pose, buffer, light, overlay, color, false);
    }

    private void emit(int[] ref, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color,
                      boolean reverseNormal) {
        float[] p = positions[ref[0]];
        float u = ref[1] >= 0 ? uvs[ref[1]][0] : 0F;
        float v = ref[1] >= 0 ? uvs[ref[1]][1] : 0F;
        float nx = 0F, ny = 1F, nz = 0F;
        if (ref[2] >= 0) {
            float[] nrm = normals[ref[2]];
            nx = nrm[0]; ny = nrm[1]; nz = nrm[2];
        }
        if (reverseNormal) { nx = -nx; ny = -ny; nz = -nz; }
        Vertexes.vertex(buffer, pose, p[0], p[1], p[2], color, u, v, overlay, light, nx, ny, nz);
    }
}
