package com.hbm.client.model;

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

public final class WavefrontObj {

    private static final class Group {
        final String name;
        final List<int[][]> faces = new ArrayList<>();
        Group(String name) { this.name = name; }
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

    public float[] boundsMin() { return min; }

    public float[] boundsMax() { return max; }

    public static WavefrontObj load(Identifier location) {
        List<float[]> pos = new ArrayList<>();
        List<float[]> uv = new ArrayList<>();
        List<float[]> norm = new ArrayList<>();
        List<Group> groups = new ArrayList<>();
        Group current = new Group("default");
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

                        if (current.faces.isEmpty() && groups.size() == 1) {
                            groups.set(0, current = new Group(t.length > 1 ? t[1] : "default"));
                        } else {
                            groups.add(current = new Group(t.length > 1 ? t[1] : "default"));
                        }
                    }
                    case "f" -> {
                        int n = t.length - 1;
                        int[][] face = new int[n][];
                        for (int i = 0; i < n; i++) face[i] = parseRef(t[i + 1]);
                        current.faces.add(face);
                    }
                    default -> {  }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load OBJ model " + location, e);
        }
        return new WavefrontObj(pos, uv, norm, groups);
    }

    private static int[] parseRef(String s) {
        String[] p = s.split("/", -1);
        int vi = Integer.parseInt(p[0]) - 1;
        int ti = p.length > 1 && !p[1].isEmpty() ? Integer.parseInt(p[1]) - 1 : -1;
        int ni = p.length > 2 && !p[2].isEmpty() ? Integer.parseInt(p[2]) - 1 : -1;
        return new int[]{vi, ti, ni};
    }

    public void renderAll(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay) {
        renderExcept(pose, buffer, light, overlay, java.util.Set.of());
    }

    public void renderExcept(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, java.util.Set<String> exclude) {
        for (Group g : groups) if (!exclude.contains(g.name)) drawGroup(g, pose, buffer, light, overlay);
    }

    public void renderPart(String name, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay) {
        renderPart(name, pose, buffer, light, overlay, -1);
    }

    public void renderPart(String name, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
        for (Group g : groups) if (g.name.equals(name)) drawGroup(g, pose, buffer, light, overlay, color);
    }

    private void drawGroup(Group g, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay) {
        drawGroup(g, pose, buffer, light, overlay, -1);
    }

    private void drawGroup(Group g, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
        for (int[][] face : g.faces) {

            for (int i = 1; i + 1 < face.length; i++) {
                emit(face[0], pose, buffer, light, overlay, color);
                emit(face[i], pose, buffer, light, overlay, color);
                emit(face[i + 1], pose, buffer, light, overlay, color);
                emit(face[i + 1], pose, buffer, light, overlay, color);
            }
        }
    }

    private void emit(int[] ref, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
        float[] p = positions[ref[0]];
        float u = ref[1] >= 0 ? uvs[ref[1]][0] : 0F;
        float v = ref[1] >= 0 ? uvs[ref[1]][1] : 0F;
        float nx = 0F, ny = 1F, nz = 0F;
        if (ref[2] >= 0) {
            float[] nrm = normals[ref[2]];
            nx = nrm[0]; ny = nrm[1]; nz = nrm[2];
        }
        buffer.addVertex(pose, p[0], p[1], p[2])
                .setColor(color)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
