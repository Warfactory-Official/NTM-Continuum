package com.hbm.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.Ids;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Geometry;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Minimal Wavefront OBJ loader + immediate-mode renderer for the new render pipeline. Ported from the
 * old mod's {@code HFRWavefrontObject} but stripped of the LWJGL VBO/VAO machinery (which has no 26.1
 * equivalent); instead it emits geometry straight into a {@link VertexConsumer}, so it works inside a
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
    private Identifier source = Ids.of("minecraft", "unknown");

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
     * <p>For models drawn in pieces: an armour suit's OBJ holds all eight body parts but a helmet's
     * inventory icon draws one, and framing it by the <em>whole</em> model's bounds would centre the slot on
     * the suit's waist. An empty exclusion set gives the whole-model bounds.
     */
    public float[][] boundsExcluding(Set<String> excluded) {
        float[][] bounds = boundsExcludingExact(excluded);
        return bounds == null ? new float[][] {min, max} : bounds;
    }

    /** Bounding box of non-excluded geometry, or {@code null} when every rendered group was excluded. */
    public @Nullable float[][] boundsExcludingExact(Set<String> excluded) {
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
        return any ? new float[][] {mn, mx} : null;
    }

    /** Bounding box of only the named groups, or {@code null} when none of them contains geometry. */
    public @Nullable float[][] boundsIncluding(Set<String> included) {
        if (included.isEmpty()) return null;

        float[] mn = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] mx = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        boolean any = false;

        for (Group group : groups) {
            if (!included.contains(group.name)) continue;
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
        return any ? new float[][] {mn, mx} : null;
    }

    /** Bounding box of only groups whose material has a resolved render pass. */
    public @Nullable float[][] boundsIncludingMaterials(Set<String> included) {
        if (included.isEmpty()) return null;

        float[] mn = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] mx = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        boolean any = false;

        for (Group group : groups) {
            if (!included.contains(group.material)) continue;
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
        return any ? new float[][] {mn, mx} : null;
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
        WavefrontObj model = new WavefrontObj(pos, uv, norm, groups);
        model.source = location;
        return model;
    }

    /** The distinct material names used by this model, in first-use order (empty string = untextured runs). */
    public List<String> materialNames() {
        List<String> names = new ArrayList<>();
        for (Group g : groups) if (!g.faces.isEmpty() && !names.contains(g.material)) names.add(g.material);
        return names;
    }

    /** Draws every face run using the given material (for multi-texture OBJs, one call per material). */
    public void renderMaterial(String material, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay) {
        for (Group g : groups) if (g.material.equals(material)) drawGroup(g, pose, buffer, light, overlay);
    }

    /**
     * Parses a Wavefront .mtl next to the OBJ and maps material name to texture id (each {@code map_Kd}
     * like {@code hbm:blocks/foo} resolved to {@code hbm:textures/blocks/foo.png}).
     */
    public static Map<String, Identifier> loadMtl(Identifier mtlLocation) {
        Map<String, Identifier> map = new LinkedHashMap<>();
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
        renderExcept(pose, buffer, light, overlay, Set.of());
    }

    /** Draws every part and optionally emits reversed faces for legacy double-sided models. */
    public void renderAll(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, boolean doubleSided) {
        renderExcept(pose, buffer, light, overlay, Set.of(), doubleSided);
    }

    /** Draws every part tinted with an ARGB vertex colour. */
    public void renderAll(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
        for (Group g : groups) drawGroup(g, pose, buffer, light, overlay, color);
    }

    /** Draws every part tinted, with the texture scrolled by {@code (uOffset, vOffset)}; see {@link #renderPart}. */
    public void renderAll(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color,
                          float uOffset, float vOffset) {
        for (Group g : groups) drawGroup(g, pose, buffer, light, overlay, color, false, uOffset, vOffset);
    }

    /** Draws every part except those named in {@code exclude} (e.g. dynamic parts like a fluid overlay). */
    public void renderExcept(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, Set<String> exclude) {
        renderExcept(pose, buffer, light, overlay, exclude, false);
    }

    public void renderExcept(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay,
                             Set<String> exclude, boolean doubleSided) {
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

    /** Draws a named part with only position and vertex colour, for textureless legacy glow parts. */
    public void renderPartColor(String name, PoseStack.Pose pose, VertexConsumer buffer, int color) {
        for (Group g : groups) if (g.name.equals(name)) drawColorGroup(g, pose, buffer, color);
    }

    /** Draws every part with only position and vertex colour, for models used as untextured solids. */
    public void renderAllColor(PoseStack.Pose pose, VertexConsumer buffer, int color) {
        for (Group g : groups) drawColorGroup(g, pose, buffer, color);
    }

    /**
     * Draws a named part with the texture scrolled by {@code (uOffset, vOffset)} and, optionally, both
     * faces emitted.
     *
     * <p>The scroll is baked into the vertices, not set on the render type: 1.12 translated the GL texture
     * matrix, and 26.1's nearest equivalent ({@code energySwirl}) folds the offset into the
     * {@code RenderType}, so an animated offset would mint a new RenderType every frame and vanilla
     * memoizes those forever. The texture wraps, so offsets outside 0..1 tile as before.
     */
    public void renderPart(String name, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay,
                           int color, float uOffset, float vOffset, boolean doubleSided) {
        for (Group g : groups) {
            if (g.name.equals(name)) drawGroup(g, pose, buffer, light, overlay, color, doubleSided, uOffset, vOffset);
        }
    }

    /** This model's parts as {@link Geometry}, the shape a {@code Gem} declaration names. */
    public Geometry part(String name) {
        return parts(name);
    }

    /** Unlike {@link #renderPart}, a name this model does not have throws rather than drawing nothing. */
    public Geometry parts(String... names) {
        return parts(Set.of(names), false);
    }

    /** The same selection, optionally with each triangle emitted a second time wound the other way. */
    public Geometry parts(Set<String> names, boolean doubleSided) {
        Set<String> known = partNames();
        for (String name : names) {
            if (!known.contains(name)) {
                throw new IllegalArgumentException(
                        source + " has no part named " + name + "; it has " + known);
            }
        }
        return new ObjGeometry(Set.copyOf(names), Set.of(), doubleSided);
    }

    public Geometry all() {
        return all(false);
    }

    /**
     * Every part, optionally with each triangle emitted a second time wound the other way. That is
     * reversed geometry, not a no-cull material. The duplicate carries a flipped normal and shades
     * differently, which is what {@code doubleSided()} looked like in 1.12.2.
     */
    public Geometry all(boolean doubleSided) {
        return new ObjGeometry(null, Set.of(), doubleSided);
    }

    /** Every face run wearing one material, for an OBJ whose runs split by {@code usemtl} not {@code o}. */
    public Geometry material(String name) {
        if (!materialNames().contains(name)) {
            throw new IllegalArgumentException(
                    source + " has no material named " + name + "; it has " + materialNames());
        }
        return new ObjGeometry(null, Set.of(), false, name);
    }

    /** Every part except those named: a fluid overlay or a conditional frame drawn separately. */
    public Geometry allExcept(Set<String> exclude) {
        return new ObjGeometry(null, Set.copyOf(exclude), false);
    }

    public Geometry allExcept(Set<String> exclude, boolean doubleSided) {
        return new ObjGeometry(null, Set.copyOf(exclude), doubleSided);
    }

    /** The names of every group this model actually holds, for declarations to be checked against. */
    public Set<String> partNames() {
        Set<String> names = new LinkedHashSet<>();
        for (Group g : groups) if (!g.faces.isEmpty()) names.add(g.name);
        return names;
    }

    private final class ObjGeometry implements Geometry {

        private final @Nullable Set<String> include;
        private final Set<String> exclude;
        private final boolean doubleSided;
        private final @Nullable String material;
        private final String name;
        private float @Nullable [] vertices;
        private int @Nullable [] indices;
        private int vertexCount;
        private float @Nullable [][] bounds;

        private ObjGeometry(@Nullable Set<String> include, Set<String> exclude, boolean doubleSided) {
            this(include, exclude, doubleSided, null);
        }

        private ObjGeometry(@Nullable Set<String> include, Set<String> exclude, boolean doubleSided,
                            @Nullable String material) {
            this.include = include;
            this.exclude = exclude;
            this.doubleSided = doubleSided;
            this.material = material;
            String selection = material != null ? "=" + material
                    : include != null ? "+" + String.join(",", new TreeSet<>(include))
                    : exclude.isEmpty() ? "*" : "-" + String.join(",", new TreeSet<>(exclude));
            this.name = source + "#" + selection + (doubleSided ? "#2" : "");
        }

        private boolean selected(Group group) {
            if (material != null) return group.material.equals(material);
            return include != null ? include.contains(group.name) : !exclude.contains(group.name);
        }

        @Override
        public void draw(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
            for (Group g : groups) {
                if (selected(g)) drawGroup(g, pose, buffer, light, overlay, color, doubleSided);
            }
        }

        @Override
        public float[] vertices() {
            build();
            return vertices;
        }

        @Override
        public int[] indices() {
            build();
            return indices;
        }

        @Override
        public int vertexCount() {
            build();
            return vertexCount;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public float @Nullable [][] bounds() {
            if (bounds == null) bounds = Geometry.super.bounds();
            return bounds;
        }

        private void build() {
            if (vertices != null) return;
            List<Float> data = new ArrayList<>();
            for (Group g : groups) {
                if (!selected(g)) continue;
                for (int[][] face : g.faces) {
                    for (int i = 1; i + 1 < face.length; i++) {
                        append(data, face[0], false);
                        append(data, face[i], false);
                        append(data, face[i + 1], false);
                        if (doubleSided) {
                            append(data, face[i + 1], true);
                            append(data, face[i], true);
                            append(data, face[0], true);
                        }
                    }
                }
            }
            float[] flat = new float[data.size()];
            for (int i = 0; i < flat.length; i++) flat[i] = data.get(i);
            int count = flat.length / 8;
            int[] order = new int[count];
            for (int i = 0; i < count; i++) order[i] = i;
            vertexCount = count;
            indices = order;
            vertices = flat;
        }

        private void append(List<Float> data, int[] ref, boolean reverseNormal) {
            float[] p = positions[ref[0]];
            float u = ref[1] >= 0 ? uvs[ref[1]][0] : 0F;
            float v = ref[1] >= 0 ? uvs[ref[1]][1] : 0F;
            float nx = 0F, ny = 1F, nz = 0F;
            if (ref[2] >= 0) {
                float[] nrm = normals[ref[2]];
                nx = nrm[0]; ny = nrm[1]; nz = nrm[2];
            }
            if (reverseNormal) { nx = -nx; ny = -ny; nz = -nz; }
            data.add(p[0]); data.add(p[1]); data.add(p[2]);
            data.add(u); data.add(v);
            data.add(nx); data.add(ny); data.add(nz);
        }
    }

    private void drawGroup(Group g, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay) {
        drawGroup(g, pose, buffer, light, overlay, -1, false);
    }

    private void drawGroup(Group g, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
        drawGroup(g, pose, buffer, light, overlay, color, false);
    }

    private void drawGroup(Group g, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay,
                           int color, boolean doubleSided) {
        drawGroup(g, pose, buffer, light, overlay, color, doubleSided, 0F, 0F);
    }

    private void drawGroup(Group g, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay,
                            int color, boolean doubleSided, float uOffset, float vOffset) {
        for (int[][] face : g.faces) {
            // Fan-triangulate; emit each triangle as a quad (4th vertex duplicates the 3rd) for QUADS mode.
            for (int i = 1; i + 1 < face.length; i++) {
                emit(face[0], pose, buffer, light, overlay, color, false, uOffset, vOffset);
                emit(face[i], pose, buffer, light, overlay, color, false, uOffset, vOffset);
                emit(face[i + 1], pose, buffer, light, overlay, color, false, uOffset, vOffset);
                emit(face[i + 1], pose, buffer, light, overlay, color, false, uOffset, vOffset);
                if (doubleSided) {
                    emit(face[i + 1], pose, buffer, light, overlay, color, true, uOffset, vOffset);
                    emit(face[i + 1], pose, buffer, light, overlay, color, true, uOffset, vOffset);
                    emit(face[i], pose, buffer, light, overlay, color, true, uOffset, vOffset);
                    emit(face[0], pose, buffer, light, overlay, color, true, uOffset, vOffset);
                }
            }
        }
    }

    private void drawColorGroup(Group g, PoseStack.Pose pose, VertexConsumer buffer, int color) {
        for (int[][] face : g.faces) {
            for (int i = 1; i + 1 < face.length; i++) {
                emitColor(face[0], pose, buffer, color);
                emitColor(face[i], pose, buffer, color);
                emitColor(face[i + 1], pose, buffer, color);
                emitColor(face[i + 1], pose, buffer, color);
            }
        }
    }

    private void emitColor(int[] ref, PoseStack.Pose pose, VertexConsumer buffer, int color) {
        float[] p = positions[ref[0]];
        Vertexes.color(buffer, pose, p[0], p[1], p[2], color);
    }

    private void emit(int[] ref, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
        emit(ref, pose, buffer, light, overlay, color, false);
    }

    private void emit(int[] ref, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color,
                      boolean reverseNormal) {
        emit(ref, pose, buffer, light, overlay, color, reverseNormal, 0F, 0F);
    }

    private void emit(int[] ref, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color,
                      boolean reverseNormal, float uOffset, float vOffset) {
        float[] p = positions[ref[0]];
        float u = (ref[1] >= 0 ? uvs[ref[1]][0] : 0F) + uOffset;
        float v = (ref[1] >= 0 ? uvs[ref[1]][1] : 0F) + vOffset;
        float nx = 0F, ny = 1F, nz = 0F;
        if (ref[2] >= 0) {
            float[] nrm = normals[ref[2]];
            nx = nrm[0]; ny = nrm[1]; nz = nrm[2];
        }
        if (reverseNormal) { nx = -nx; ny = -ny; nz = -nz; }
        Vertexes.vertex(buffer, pose, p[0], p[1], p[2], color, u, v, overlay, light, nx, ny, nz);
    }
}
