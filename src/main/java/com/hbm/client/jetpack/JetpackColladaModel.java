package com.hbm.client.jetpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
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

/** Runtime for the generated jetpack Collada scene and its exact local transform tracks. */
public final class JetpackColladaModel {

    public static final Identifier OBJ = Ids.of("hbm", "models/anim/jetpack.obj");
    public static final Identifier ANIMATION = Ids.of("hbm", "models/anim/jetpack_animation.json");
    public static final Identifier TEXTURE = Ids.of("hbm", "textures/armor/jetpack_anim.png");

    private final WavefrontObj model;
    private final RenderType renderType;
    private final List<Node> roots;
    private final Map<String, Transform[]> animated;
    private final int length;
    private final int frameCount;

    private JetpackColladaModel(WavefrontObj model, List<Node> roots, Map<String, Transform[]> animated,
            int length, int frameCount) {
        this.model = model;
        this.renderType = CompatRenderTypes.entityCutoutCull(TEXTURE);
        this.roots = roots;
        this.animated = animated;
        this.length = length;
        this.frameCount = frameCount;
    }

    public static JetpackColladaModel load() {
        try (BufferedReader reader = Minecraft.getInstance().getResourceManager()
                .getResourceOrThrow(ANIMATION).openAsReader()) {
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
            return new JetpackColladaModel(WavefrontObj.load(OBJ), roots, animated, length, frameCount);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Failed to load generated jetpack Collada conversion", e);
        }
    }

    /** Renders the 1,000 ms animation at the supplied wall-clock position. */
    public void submit(PoseStack pose, SubmitSink sink, int light, long elapsedMillis, boolean reverse) {
        int elapsed = (int) Math.max(0L, Math.min(length, elapsedMillis));
        float framePosition = (reverse ? length - elapsed : elapsed) / (float) length * (frameCount - 1);
        int first = Math.max(0, Math.min(frameCount - 1, (int) framePosition));
        int next = Math.min(frameCount - 1, first + 1);
        float interpolation = framePosition - first;
        visitNodes(roots, pose, first, next, interpolation, (part, transformed) ->
                sink.submitCustomGeometry(transformed, renderType,
                        (drawPose, buffer) -> model.renderPart(part, drawPose, buffer, light, OverlayTexture.NO_OVERLAY)));
    }

    /** Bounds of the actual open pose, not the untransformed node-local OBJ or an invented cube. */
    public Vector3f[] openBounds() {
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
        visitNodes(roots, new PoseStack(), frameCount - 1, frameCount - 1, 0F, (part, transformed) -> {
            float[][] bounds = model.boundsIncluding(java.util.Set.of(part));
            if (bounds == null) return;
            for (int i = 0; i < 8; i++) {
                Vector3f point = new Vector3f(bounds[i & 1][0], bounds[(i >> 1) & 1][1],
                        bounds[(i >> 2) & 1][2]).mulPosition(transformed.last().pose());
                min.min(point);
                max.max(point);
            }
        });
        return new Vector3f[] {min, max};
    }

    private void visitNodes(List<Node> nodes, PoseStack pose,
            int first, int next, float interpolation, java.util.function.BiConsumer<String, PoseStack> visitor) {
        for (Node node : nodes) {
            pose.pushPose();
            Transform[] frames = animated.get(node.name);
            if (frames == null) dev.golem.core.render.Poses.mulPose(pose, node.base);
            else frames[first].interpolate(frames[next], interpolation).apply(pose);
            if (node.geometry != null) visitor.accept(node.name, pose);
            visitNodes(node.children, pose, first, next, interpolation, visitor);
            pose.popPose();
        }
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

        private Transform(Vector3f translation, Vector3f scale, Quaternionf rotation) {
            this.translation = translation;
            this.scale = scale;
            this.rotation = rotation;
        }

        private Transform interpolate(Transform other, float amount) {
            Vector3f position = new Vector3f(translation).lerp(other.translation, amount);
            Vector3f size = new Vector3f(scale).lerp(other.scale, amount);
            Quaternionf target = new Quaternionf(other.rotation);
            double dot = rotation.dot(target);
            if (dot < 0D) {
                target.set(-target.x, -target.y, -target.z, -target.w);
                dot = -dot;
            }
            Quaternionf quat;
            if (dot > 0.9999999D) {
                quat = new Quaternionf(rotation.x + amount * (target.x - rotation.x),
                        rotation.y + amount * (target.y - rotation.y),
                        rotation.z + amount * (target.z - rotation.z),
                        rotation.w + amount * (target.w - rotation.w)).normalize();
            } else {
                double theta0 = Math.acos(dot), theta = theta0 * amount;
                float a = (float) (Math.cos(theta) - dot * Math.sin(theta) / Math.sin(theta0));
                float b = (float) (Math.sin(theta) / Math.sin(theta0));
                quat = new Quaternionf(a * rotation.x + b * target.x, a * rotation.y + b * target.y,
                        a * rotation.z + b * target.z, a * rotation.w + b * target.w);
            }
            return new Transform(position, size, quat);
        }

        private void apply(PoseStack pose) {
            // The old reflected tracks can yield non-unit quaternions. JOML's unit-quaternion
            // fast path collapses those wings; preserve quatToGlMatrix's 1-2*y*y-2*z*z form
            // and Transform.scale's row scaling rather than silently normalizing the track.
            float x = rotation.x, y = rotation.y, z = rotation.z, w = rotation.w;
            float sx = scale.x, sy = scale.y, sz = scale.z;
            dev.golem.core.render.Poses.mulPose(pose, new Matrix4f(
                    (1F - 2F*y*y - 2F*z*z)*sx, (2F*x*y + 2F*z*w)*sy, (2F*x*z - 2F*y*w)*sz, 0F,
                    (2F*x*y - 2F*z*w)*sx, (1F - 2F*x*x - 2F*z*z)*sy, (2F*y*z + 2F*x*w)*sz, 0F,
                    (2F*x*z + 2F*y*w)*sx, (2F*y*z - 2F*x*w)*sy, (1F - 2F*x*x - 2F*y*y)*sz, 0F,
                    translation.x, translation.y, translation.z, 1F));
        }
    }
}
