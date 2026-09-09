package com.hbm.client.jetpack;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.WorldEffects;
import dev.golem.core.render.Vertexes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The jetpack's 20-tick dynamic ribbon. The insertion threshold and width are the old values: 0.1 and
 * 0.025 respectively. The ribbon is submitted as one ordered geometry pass rather than as a line, so
 * it keeps the old smooth taper and does not become a one-pixel debug line at distance.
 */
public final class JetpackTrail {

    private static final Identifier FRESNEL = dev.golem.core.Ids.of("hbm", "textures/models/bfg/fresnel_ms.png");
    private static final float WIDTH = 0.025F;
    private static final float END_FADE = 0.15F;

    private final List<Vec3> points = new ArrayList<>();
    private final List<Integer> ages = new ArrayList<>();

    public void tryAddNewPos(Vec3 position) {
        if (points.isEmpty()) {
            points.add(position);
            ages.add(0);
        } else if (points.get(points.size() - 1).distanceTo(position) > 0.1D) {
            points.add(position);
            ages.add(0);
        } else {
            points.set(points.size() - 1, position);
            ages.set(ages.size() - 1, 0);
        }
    }

    public boolean tick() {
        Iterator<Vec3> iterator = points.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            iterator.next();
            int age = ages.get(index) + 1;
            if (age > 20) {
                iterator.remove();
                ages.remove(index);
            } else {
                ages.set(index++, age);
            }
        }
        return !points.isEmpty();
    }

    public boolean isEmpty() {
        return points.size() < 2;
    }

    public void submit(PoseStack pose, CameraInfo camera) {
        if (points.size() < 2) return;
        org.joml.Matrix4f viewToLocal = new org.joml.Matrix4f(pose.last().pose()).invert();
        List<Vec3> localPoints = new ArrayList<>(points.size());
        for (Vec3 point : points) localPoints.add(worldToLocal(point, camera, viewToLocal));
        org.joml.Vector3f viewer = new org.joml.Vector3f(0F, 0F, 0F).mulPosition(viewToLocal);
        Vec3 viewerPosition = new Vec3(viewer.x(), viewer.y(), viewer.z());
        org.joml.Matrix4f localToView = new org.joml.Matrix4f(pose.last().pose());
        pose.pushPose();
        WorldEffects.submit(pose, CompatRenderTypes.plasmaGlow(FRESNEL), 3,
                (drawPose, buffer) -> drawRibbon(drawPose, buffer, localPoints, viewerPosition, localToView));
        pose.popPose();
    }

    private static Vec3 worldToLocal(Vec3 point, CameraInfo camera, org.joml.Matrix4f viewToLocal) {
        Vec3 relative = point.subtract(camera.pos);
        org.joml.Vector3f view = camera.worldToRender(new org.joml.Vector3f((float) relative.x, (float) relative.y, (float) relative.z));
        org.joml.Vector3f local = view.mulPosition(viewToLocal);
        return new Vec3(local.x(), local.y(), local.z());
    }

    private void drawRibbon(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer,
            List<Vec3> localPoints, Vec3 viewerPosition, org.joml.Matrix4f localToView) {
        if (localPoints.size() < 2) return;
        Vec3 first = localPoints.get(0);
        Vec3 cross = cross(localPoints.get(1).subtract(first), viewerPosition.subtract(first), localToView);
        vertex(buffer, pose, first.add(cross), 0F, 1F, fade(0F));
        vertex(buffer, pose, first.subtract(cross), 0F, 0F, fade(0F));
        for (int i = 1; i < localPoints.size(); i++) {
            Vec3 previous = localPoints.get(i - 1);
            Vec3 current = localPoints.get(i);
            Vec3 next = i < localPoints.size() - 1 ? localPoints.get(i + 1) : current;
            Vec3 tangent = next.subtract(previous);
            float normalized = i / (float) (localPoints.size() - 1);
            cross = cross(tangent, viewerPosition.subtract(previous), localToView);
            float middle = (i - 0.5F) / (localPoints.size() - 1);
            vertex(buffer, pose, previous.add(current.subtract(previous).scale(0.5D)), middle, 0.5F, fade(middle));
            vertex(buffer, pose, current.add(cross), normalized, 1F, fade(normalized));
            vertex(buffer, pose, current.subtract(cross), normalized, 0F, fade(normalized));
        }
    }

    private static Vec3 cross(Vec3 tangent, Vec3 toViewer, org.joml.Matrix4f localToView) {
        Vec3 direction = tangent.cross(toViewer).normalize();
        org.joml.Vector3f transformed = localToView.transformDirection(
                new org.joml.Vector3f((float) direction.x, (float) direction.y, (float) direction.z));
        float length = transformed.length();
        return direction.scale(length == 0F ? 0F : WIDTH / length);
    }

    private static float fade(float t) {
        return remap01(t, 0F, END_FADE) * remap01(t, 1F, 1F - END_FADE) * 0.7F;
    }

    private static float remap01(float value, float from, float to) {
        if (from == to) return value >= from ? 1F : 0F;
        return clamp((value - from) / (to - from), 0F, 1F);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
            Vec3 position, float u, float v, float alpha) {
        Vertexes.vertex(buffer, pose, (float) position.x, (float) position.y, (float) position.z,
                0.7F, 0.7F, 0.7F, alpha, u, v, 0, 0xF000F0, 0F, 1F, 0F);
    }
}
