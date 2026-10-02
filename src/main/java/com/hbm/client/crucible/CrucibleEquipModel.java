package com.hbm.client.crucible;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * old-hbm {@code ResourceManager.crucible_anim} / {@code crucible_equip}: the 1.12 first-person crucible, a Collada scene
 * of six node-animated meshes (rightArm, leftArm, Hilt, GuardRight, Blade, GuardLeft) played over 1,060 ms, as
 * {@code AnimatedModel.renderAnimated} walked it.
 *
 * <p>The scene is converted once by {@code port-tools/gen_crucible_visuals.py} into {@code crucible_equip.obj} (geometry
 * in node space) and {@code crucible_equip_animation.json} (the hierarchy matrices and all 24 keyframe matrices), the
 * same split as the jetpack glider's {@code JetpackColladaModel}. The keyframe blend is old {@code Transform}: the
 * matrix decomposed into translation, row scale and an LWJGL-read quaternion, translation and scale lerped, rotation
 * slerped, rebuilt by {@code quatToGlMatrix} with the row scale applied.
 */
public final class CrucibleEquipModel {

    public static final Identifier OBJ = Ids.of("hbm", "models/anim/crucible_equip.obj");
    public static final Identifier ANIMATION = Ids.of("hbm", "models/anim/crucible_equip_animation.json");

    final WavefrontObj model;
    private final List<Node> roots;
    private final Map<String, Transform[]> tracks;
    /** {@code loadAnim(1060, ...)}. */
    public final int length;
    /** {@code anim.numKeyFrames}. */
    public final int frameCount;

    private CrucibleEquipModel(WavefrontObj model, List<Node> roots, Map<String, Transform[]> tracks, int length,
                               int frameCount) {
        this.model = model;
        this.roots = roots;
        this.tracks = tracks;
        this.length = length;
        this.frameCount = frameCount;
    }

    private static CrucibleEquipModel instance;

    public static CrucibleEquipModel get() {
        if (instance == null) instance = load();
        return instance;
    }

    /** Dropped on resource reload. */
    public static void reset() {
        instance = null;
    }

    /** A node's draw: called with the pose at that node, before its children (old {@code onRender}/{@code postRender}). */
    @FunctionalInterface
    public interface NodeDraw {
        void draw(String name, PoseStack pose);
    }

    /** The playhead of {@code renderAnimated}: a keyframe pair, the blend between them, and {@code diffN}. */
    public record Playhead(int first, int next, float inter, float diffN) {}

    /**
     * {@code renderAnimated}'s frame arithmetic for {@code elapsed} milliseconds since the wrapper's start (already
     * clamped by the caller's STAY handling): remapped onto {@code 0 .. numKeyFrames - 1}, split into a keyframe pair
     * and fraction, plus {@code diffN = clamp(elapsed / length)}.
     */
    public Playhead playhead(long elapsed) {
        int diff = (int) elapsed;
        float remapped = Math.max(0, Math.min(frameCount - 1, diff / (float) length * (frameCount - 1)));
        float diffN = Math.max(0F, Math.min(1F, diff / (float) length));
        int first = (int) remapped;
        int next = first < frameCount - 1 ? first + 1 : first;
        return new Playhead(first, next, (float) (remapped - Math.floor(remapped)), diffN);
    }

    /** {@code renderWithIndex}: push, the node's keyframe blend (or its scene matrix), draw, children, pop. */
    public void walk(Playhead playhead, PoseStack pose, NodeDraw draw) {
        for (Node root : roots) walk(root, playhead, pose, draw);
    }

    private void walk(Node node, Playhead playhead, PoseStack pose, NodeDraw draw) {
        pose.pushPose();
        Transform[] track = tracks.get(node.name);
        Matrix4f matrix = track == null ? node.base
                : track[playhead.first].interpolate(track[playhead.next], playhead.inter);
        dev.golem.core.render.Poses.mulPose(pose, matrix);
        if (node.geometry) draw.draw(node.name, pose);
        for (Node child : node.children) walk(child, playhead, pose, draw);
        pose.popPose();
    }

    private static CrucibleEquipModel load() {
        try (BufferedReader reader = Minecraft.getInstance().getResourceManager().getResourceOrThrow(ANIMATION)
                .openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            List<Node> roots = nodes(root.getAsJsonArray("nodes"));
            Map<String, Transform[]> tracks = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("transforms").entrySet()) {
                JsonArray frames = entry.getValue().getAsJsonArray();
                Transform[] values = new Transform[frames.size()];
                for (int i = 0; i < values.length; i++) values[i] = new Transform(frames.get(i).getAsJsonArray());
                tracks.put(entry.getKey(), values);
            }
            return new CrucibleEquipModel(WavefrontObj.load(OBJ), roots, tracks, root.get("length").getAsInt(),
                    root.get("frames").getAsInt());
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Failed to load the converted crucible_equip.dae", e);
        }
    }

    private static List<Node> nodes(JsonArray values) {
        List<Node> result = new ArrayList<>();
        for (JsonElement value : values) {
            JsonObject json = value.getAsJsonObject();
            float[] matrix = new float[16];
            for (int i = 0; i < 16; i++) matrix[i] = json.getAsJsonArray("matrix").get(i).getAsFloat();
            boolean geometry = json.has("geometry") && !json.get("geometry").isJsonNull();
            result.add(new Node(json.get("name").getAsString(), geometry, new Matrix4f().set(matrix),
                    nodes(json.getAsJsonArray("children"))));
        }
        return result;
    }

    private record Node(String name, boolean geometry, Matrix4f base, List<Node> children) {}

    /** old {@code animloader.Transform}, as {@code JetpackColladaModel.Transform} ports it. */
    private static final class Transform {
        private final Vector3f translation;
        private final Vector3f scale;
        private final Quaternionf rotation;

        private Transform(JsonArray values) {
            float[] raw = new float[values.size()];
            for (int i = 0; i < raw.length; i++) raw[i] = values.get(i).getAsFloat();
            float scaleX = length(raw, 0);
            float scaleY = length(raw, 4);
            float scaleZ = length(raw, 8);
            float[] normalized = raw.clone();
            for (int i = 0; i < 3; i++) normalized[i] /= scaleX;
            for (int i = 4; i < 7; i++) normalized[i] /= scaleY;
            for (int i = 8; i < 11; i++) normalized[i] /= scaleZ;
            Matrix4f matrix = new Matrix4f().set(normalized);
            translation = new Vector3f(raw[3], raw[7], raw[11]);
            scale = new Vector3f(scaleX, scaleY, scaleZ);
            // LWJGL2's setFromMatrix reads the opposite off-diagonal order from JOML.
            rotation = matrix.transpose().getNormalizedRotation(new Quaternionf());
        }

        private static float length(float[] v, int offset) {
            return (float) Math.sqrt(v[offset] * v[offset] + v[offset + 1] * v[offset + 1] + v[offset + 2] * v[offset + 2]);
        }

        Matrix4f interpolate(Transform other, float amount) {
            Vector3f t = translation.lerp(other.translation, amount, new Vector3f());
            Vector3f s = scale.lerp(other.scale, amount, new Vector3f());
            Quaternionf target = new Quaternionf(other.rotation);
            double dot = rotation.dot(target);
            if (dot < 0D) {
                target.set(-target.x, -target.y, -target.z, -target.w);
                dot = -dot;
            }
            Quaternionf q;
            if (dot > 0.9999999D) {
                // old slerp's near-parallel branch: v0 + t * v1, normalised
                q = new Quaternionf(rotation.x + amount * target.x, rotation.y + amount * target.y,
                        rotation.z + amount * target.z, rotation.w + amount * target.w).normalize();
            } else {
                double theta0 = Math.acos(dot);
                double theta = theta0 * amount;
                float a = (float) (Math.cos(theta) - dot * Math.sin(theta) / Math.sin(theta0));
                float b = (float) (Math.sin(theta) / Math.sin(theta0));
                q = new Quaternionf(a * rotation.x + b * target.x, a * rotation.y + b * target.y,
                        a * rotation.z + b * target.z, a * rotation.w + b * target.w);
            }
            float x = q.x, y = q.y, z = q.z, w = q.w;
            // quatToGlMatrix, then Transform.scale() multiplies rows by the scale
            return new Matrix4f(
                    (1F - 2F * y * y - 2F * z * z) * s.x, (2F * x * y + 2F * z * w) * s.y, (2F * x * z - 2F * y * w) * s.z, 0F,
                    (2F * x * y - 2F * z * w) * s.x, (1F - 2F * x * x - 2F * z * z) * s.y, (2F * y * z + 2F * x * w) * s.z, 0F,
                    (2F * x * z + 2F * y * w) * s.x, (2F * y * z - 2F * x * w) * s.y, (1F - 2F * x * x - 2F * y * y) * s.z, 0F,
                    t.x, t.y, t.z, 1F);
        }
    }
}
