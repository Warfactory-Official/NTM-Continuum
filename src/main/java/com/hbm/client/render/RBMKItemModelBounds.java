package com.hbm.client.render;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Set;
import java.util.function.Consumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

final class RBMKItemModelBounds {

    private RBMKItemModelBounds() {}

    static void applyColumnTransform(PoseStack pose, float scale, float yaw, float lift) {
        pose.translate(.5F, .5F + lift, .5F);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.scale(scale, scale, scale);
        pose.translate(-.5F, 0F, -.5F);
    }

    static void emitBox(PoseStack pose, float minX, float minY, float minZ,
                        float maxX, float maxY, float maxZ, Consumer<Vector3fc> output) {
        emit(pose.last().pose(), new float[]{minX, minY, minZ}, new float[]{maxX, maxY, maxZ}, output);
    }

    static void emitPart(PoseStack pose, WavefrontObj model, String part, Consumer<Vector3fc> output) {
        float[][] bounds = model.boundsIncluding(Set.of(part));
        if (bounds != null) emit(pose.last().pose(), bounds[0], bounds[1], output);
    }

    private static void emit(Matrix4f matrix, float[] min, float[] max, Consumer<Vector3fc> output) {
        for (int xi = 0; xi < 2; xi++) for (int yi = 0; yi < 2; yi++) for (int zi = 0; zi < 2; zi++) {
            Vector3f point = new Vector3f(xi == 0 ? min[0] : max[0],
                    yi == 0 ? min[1] : max[1], zi == 0 ? min[2] : max[2]);
            matrix.transformPosition(point);
            output.accept(point);
        }
    }
}
