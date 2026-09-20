package com.hbm.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runtime for a converted Collada scene and its exact local transform tracks — the transition seal's
 * {@code seal.dae}, converted to {@code seal.obj} + {@code seal_animation.json} the same way the
 * jetpack glider was ({@code com.hbm.client.jetpack.JetpackColladaModel}, of which this is the
 * door-owned copy with the resources as parameters). The old {@code AnimatedModel} interpolated a
 * decomposed transform per node between the two keyframes around the playhead; so does this.
 */
public final class DoorColladaModel {

    private final Surface surface;
    private final WavefrontObj model;
    private final Map<String, Transform[]> animated;
    private final int length;
    private final int frameCount;

    private final Gem.Model<Pose> gem;
    private final Map<Gem.Part<Pose>, float[][]> boxes = new IdentityHashMap<>();
    private final Pose pose = new Pose();

    /** The playhead: a pair of frames and how far between them, resolved once so every node agrees. */
    public static final class Pose {
        int first;
        int next;
        float interpolation;
    }

    private DoorColladaModel(WavefrontObj model, Surface surface, List<Node> roots, Map<String, Transform[]> animated,
            int length, int frameCount) {
        this.model = model;
        this.surface = surface;
        this.animated = animated;
        this.length = length;
        this.frameCount = frameCount;
        Gem.Model<Pose> declaration = Gem.model();
        for (Node node : roots) declare(node, null, declaration);
        this.gem = declaration.seal();
    }

    /**
     * Turns one scene node into a part; a node with no geometry becomes a joint.
     *
     * <p>A node's track is resolved here rather than per frame, and an animated one is given the scratch
     * its blend writes into: this channel runs once per node on every frame the glider is on screen, and
     * a blend that allocates its own vectors, quaternion and matrix is a hundred objects a frame.
     */
    private void declare(Node node, Gem.Part<Pose> parent, Gem.Model<Pose> declaration) {
        Gem.Part<Pose> part;
        if (node.geometry == null) {
            part = parent == null ? declaration.joint() : parent.joint();
        } else {
            part = parent == null
                    ? declaration.part(model.part(node.name), surface)
                    : parent.child(model.part(node.name), surface);
            boxes.put(part, model.boundsIncluding(Set.of(node.name)));
        }
        Transform[] track = animated.get(node.name);
        if (track == null) {
            part.pose(state -> node.base);
        } else {
            Blend blend = new Blend();
            part.pose(state -> track[state.first].interpolate(track[state.next], state.interpolation, blend));
        }
        for (Node child : node.children) declare(child, part, declaration);
    }

    private Pose seek(long elapsedMillis, boolean reverse) {
        int elapsed = (int) Math.max(0L, Math.min(length, elapsedMillis));
        float framePosition = (reverse ? length - elapsed : elapsed) / (float) length * (frameCount - 1);
        pose.first = Math.max(0, Math.min(frameCount - 1, (int) framePosition));
        pose.next = Math.min(frameCount - 1, pose.first + 1);
        pose.interpolation = framePosition - pose.first;
        return pose;
    }

    public static DoorColladaModel load(Identifier obj, Identifier animation, Identifier texture) {
        try (BufferedReader reader = Minecraft.getInstance().getResourceManager()
                .getResourceOrThrow(animation).openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            List<Node> roots = nodes(root.getAsJsonArray("nodes"));
            Map<String, Transform[]> animated = new HashMap<>();
            JsonObject tracks = root.getAsJsonObject("transforms");
            if (tracks != null) {
                for (Map.Entry<String, JsonElement> entry : tracks.entrySet()) {
                    JsonArray frames = entry.getValue().getAsJsonArray();
                    Transform[] values = new Transform[frames.size()];
                    for (int i = 0; i < values.length; i++)
                        values[i] = new Transform(frames.get(i).getAsJsonArray());
                    animated.put(entry.getKey(), values);
                }
            }
            int length = root.get("length").getAsInt();
            int frameCount = root.get("frames").getAsInt();
            return new DoorColladaModel(WavefrontObj.load(obj), Surface.cutoutCull(texture.toString()),
                    roots, animated, length, frameCount);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Failed to load converted Collada door " + obj, e);
        }
    }

    /**
     * Renders the animation at the supplied position in milliseconds, clamped to its length so the
     * ends hold (the old {@code EndType.STAY}); {@code reverse} plays it from the open pose back.
     */
    public void submit(PoseStack poseStack, SubmitSink sink, int light, long elapsedMillis, boolean reverse) {
        Gem.replay(gem, seek(elapsedMillis, reverse), poseStack, sink, light);
    }

    /** Bounds of the actual open pose, not the untransformed node-local OBJ or an invented cube. */
    public Vector3f[] openBounds() {
        Pose open = seek(length, false);
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
        for (Gem.Part<Pose> root : gem.roots()) corners(root, new Matrix4f(), open, min, max);
        return new Vector3f[] {min, max};
    }

    private void corners(Gem.Part<Pose> part, Matrix4f parent, Pose state,
            Vector3f min, Vector3f max) {
        Matrix4f here = new Matrix4f(parent);
        part.applyTo(here, state);
        float[][] bounds = boxes.get(part);
        if (bounds != null) {
            for (int i = 0; i < 8; i++) {
                Vector3f point = new Vector3f(bounds[i & 1][0], bounds[(i >> 1) & 1][1],
                        bounds[(i >> 2) & 1][2]).mulPosition(here);
                min.min(point);
                max.max(point);
            }
        }
        for (Gem.Part<Pose> child : part.children()) corners(child, here, state, min, max);
    }

    private static List<Node> nodes(JsonArray values) {
        List<Node> result = new ArrayList<>();
        for (JsonElement value : values) result.add(node(value.getAsJsonObject()));
        return result;
    }

    private static Node node(JsonObject json) {
        List<Node> children = nodes(json.getAsJsonArray("children"));
        String geometry = json.has("geometry") && !json.get("geometry").isJsonNull()
                ? json.get("geometry").getAsString() : null;
        float[] matrix = new float[16];
        for (int i = 0; i < matrix.length; i++) matrix[i] = json.getAsJsonArray("matrix").get(i).getAsFloat();
        return new Node(json.get("name").getAsString(), geometry, new Matrix4f().set(matrix), children);
    }

    private static final class Node {
        private final String name;
        private final String geometry;
        private final Matrix4f base;
        private final List<Node> children;

        private Node(String name, String geometry, Matrix4f base, List<Node> children) {
            this.name = name;
            this.geometry = geometry;
            this.base = base;
            this.children = children;
        }
    }

    /** Matrix decomposition and interpolation matching old {@code Transform.interpolateAndApply}. */
    private static final class Transform {
        private final Vector3f translation;
        private final Vector3f scale;
        private final Quaternionf rotation;

        /**
         * Collada scene matrices were transposed by the old loader, but animation output was not. The
         * latter is consequently decomposed with translation in indices 3/7/11, exactly like the old
         * {@code Transform} class, before its quaternion is written back in OpenGL column order.
         */
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

        private static float length(float[] values, int offset) {
            return (float) Math.sqrt(values[offset] * values[offset] + values[offset + 1] * values[offset + 1]
                    + values[offset + 2] * values[offset + 2]);
        }

        private Matrix4f interpolate(Transform other, float amount, Blend into) {
            translation.lerp(other.translation, amount, into.translation);
            scale.lerp(other.scale, amount, into.scale);
            Quaternionf target = into.target.set(other.rotation);
            double dot = rotation.dot(target);
            if (dot < 0D) {
                target.set(-target.x, -target.y, -target.z, -target.w);
                dot = -dot;
            }
            Quaternionf quat = into.rotation;
            if (dot > 0.9999999D) {
                quat.set(rotation.x + amount * (target.x - rotation.x),
                        rotation.y + amount * (target.y - rotation.y),
                        rotation.z + amount * (target.z - rotation.z),
                        rotation.w + amount * (target.w - rotation.w)).normalize();
            } else {
                double theta0 = Math.acos(dot), theta = theta0 * amount;
                float a = (float) (Math.cos(theta) - dot * Math.sin(theta) / Math.sin(theta0));
                float b = (float) (Math.sin(theta) / Math.sin(theta0));
                quat.set(a * rotation.x + b * target.x, a * rotation.y + b * target.y,
                        a * rotation.z + b * target.z, a * rotation.w + b * target.w);
            }
            return matrix(into.matrix, quat, into.scale, into.translation);
        }

        /**
         * The old reflected tracks can yield non-unit quaternions. JOML's unit-quaternion fast path
         * collapses those wings; this preserves {@code quatToGlMatrix}'s {@code 1-2*y*y-2*z*z} form and
         * {@code Transform.scale}'s row scaling rather than silently normalizing the track.
         */
        private static Matrix4f matrix(Matrix4f out, Quaternionf rotation, Vector3f scale,
                Vector3f translation) {
            float x = rotation.x, y = rotation.y, z = rotation.z, w = rotation.w;
            float sx = scale.x, sy = scale.y, sz = scale.z;
            return out.set(
                    (1F - 2F*y*y - 2F*z*z)*sx, (2F*x*y + 2F*z*w)*sy, (2F*x*z - 2F*y*w)*sz, 0F,
                    (2F*x*y - 2F*z*w)*sx, (1F - 2F*x*x - 2F*z*z)*sy, (2F*y*z + 2F*x*w)*sz, 0F,
                    (2F*x*z + 2F*y*w)*sx, (2F*y*z - 2F*x*w)*sy, (1F - 2F*x*x - 2F*y*y)*sz, 0F,
                    translation.x, translation.y, translation.z, 1F);
        }
    }

    /** One animated node's blend scratch. The walk consumes each matrix before moving on, so reusing
     * it costs nothing and keeps a frame's transform work off the heap. */
    private static final class Blend {
        private final Vector3f translation = new Vector3f();
        private final Vector3f scale = new Vector3f();
        private final Quaternionf rotation = new Quaternionf();
        private final Quaternionf target = new Quaternionf();
        private final Matrix4f matrix = new Matrix4f();
    }
}
