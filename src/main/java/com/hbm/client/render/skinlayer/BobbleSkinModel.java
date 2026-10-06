package com.hbm.client.render.skinlayer;

import com.hbm.blocks.generic.BobbleFigure;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * old-hbm {@code BobbleSkinModel}: a bobblehead figure built from a player's real skin, each body part a base box and a
 * slightly larger overlay box from {@link SkinBoxVoxelizer}, the head half again as large as the body. Drawn
 * double-sided and blended (old-hbm: culling off, {@code SRC_ALPHA, ONE_MINUS_SRC_ALPHA}, alpha test {@code > 0}),
 * lit by the parts' normals; the head bobs on the wall clock as {@code renderGuy}'s does.
 */
public final class BobbleSkinModel {

    private static final float BODY_SCALE = 0.0625f;
    private static final float HEAD_SCALE = 0.09375f;

    private static final float HEAD_OVERLAY_XZ = 1.08f;
    private static final float HEAD_OVERLAY_Y = 1.035f;
    private static final float BODY_OVERLAY_XZ = 1.05f;
    private static final float BODY_OVERLAY_Y = 1.035f;
    private static final float LIMB_OVERLAY_XZ = 1.15f;
    private static final float LIMB_OVERLAY_Y = 1.035f;

    private static final float LIMB_PIVOT_Y = 12f;
    private static final float BODY_PIVOT_Y = 12f;
    private static final float HEAD_PIVOT_Y = 0f;

    /** The grey stand-in while a skin loads or if it never does: base layer only. */
    public static final int[] GRAY_SKIN = grayPixels();
    private static BobbleSkinModel grayModel;

    private final SkinBoxVoxelizer headBase, bodyBase, rightArmBase, leftArmBase, rightLegBase, leftLegBase;
    private final SkinBoxVoxelizer headOverlay, bodyOverlay, rightArmOverlay, leftArmOverlay, rightLegOverlay, leftLegOverlay;

    /** {@code skin}: 64x64 {@code 0xAARRGGBB}, already through vanilla's legacy-skin processing. */
    public BobbleSkinModel(int[] skin) {
        headBase = SkinBoxVoxelizer.create(skin, 8, 8, 8, 0, 0);
        bodyBase = SkinBoxVoxelizer.create(skin, 8, 12, 4, 16, 16);
        rightArmBase = SkinBoxVoxelizer.create(skin, 4, 12, 4, 40, 16);
        leftArmBase = SkinBoxVoxelizer.create(skin, 4, 12, 4, 32, 48);
        rightLegBase = SkinBoxVoxelizer.create(skin, 4, 12, 4, 0, 16);
        leftLegBase = SkinBoxVoxelizer.create(skin, 4, 12, 4, 16, 48);
        headOverlay = SkinBoxVoxelizer.create(skin, 8, 8, 8, 32, 0);
        bodyOverlay = SkinBoxVoxelizer.create(skin, 8, 12, 4, 16, 32);
        rightArmOverlay = SkinBoxVoxelizer.create(skin, 4, 12, 4, 40, 32);
        leftArmOverlay = SkinBoxVoxelizer.create(skin, 4, 12, 4, 48, 48);
        rightLegOverlay = SkinBoxVoxelizer.create(skin, 4, 12, 4, 0, 32);
        leftLegOverlay = SkinBoxVoxelizer.create(skin, 4, 12, 4, 0, 48);
    }

    public static BobbleSkinModel gray() {
        if (grayModel == null) grayModel = new BobbleSkinModel(GRAY_SKIN);
        return grayModel;
    }

    private static int[] grayPixels() {
        int[] img = new int[64 * 64];
        int gray = 0xFFA0A0A0;
        fillRect(img, 0, 0, 32, 16, gray);    // head base
        fillRect(img, 0, 16, 16, 16, gray);   // right leg
        fillRect(img, 16, 16, 24, 16, gray);  // body
        fillRect(img, 40, 16, 16, 16, gray);  // right arm
        fillRect(img, 16, 48, 16, 16, gray);  // left leg
        fillRect(img, 32, 48, 16, 16, gray);  // left arm
        return img;
    }

    private static void fillRect(int[] img, int x, int y, int w, int h, int argb) {
        for (int j = 0; j < h; j++)
            for (int i = 0; i < w; i++)
                img[(y + j) * 64 + x + i] = argb;
    }

    private static void renderPartPair(PoseStack pose, SubmitSink sink, RenderType type, int light,
            SkinBoxVoxelizer base, SkinBoxVoxelizer overlay, float scale, float scaleXZ, float scaleY, float pivotY) {
        pose.pushPose();
        pose.scale(scale, scale, scale);
        if (!base.isEmpty()) sink.submitCustomGeometry(pose, type, (p, buffer) -> base.emit(p, buffer, light));
        if (!overlay.isEmpty()) {
            pose.translate(0, pivotY, 0);
            pose.scale(scaleXZ, scaleY, scaleXZ);
            pose.translate(0, -pivotY, 0);
            sink.submitCustomGeometry(pose, type, (p, buffer) -> overlay.emit(p, buffer, light));
        }
        pose.popPose();
    }

    private static void rotate(PoseStack pose, double[] rot) {
        pose.mulPose(Axis.XP.rotationDegrees((float) rot[0]));
        pose.mulPose(Axis.YP.rotationDegrees((float) rot[1]));
        pose.mulPose(Axis.ZP.rotationDegrees((float) rot[2]));
    }

    /** old-hbm {@code render(time, rotLeftArm, rotRightArm, rotLeftLeg, rotRightLeg, rotBody, rotHead)}. */
    public void render(PoseStack pose, SubmitSink sink, Identifier texture, long time, BobbleFigure.Pose figure, int light) {
        RenderType type = CompatRenderTypes.entityTranslucent(texture);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees((float) figure.body() - 90.0F));

        // LEFT LEG
        pose.pushPose();
        pose.translate(0.125, 1, 0);
        rotate(pose, figure.leftLeg());
        pose.translate(0, -0.75, 0);
        renderPartPair(pose, sink, type, light, leftLegBase, leftLegOverlay, BODY_SCALE, LIMB_OVERLAY_XZ, LIMB_OVERLAY_Y, LIMB_PIVOT_Y);
        pose.popPose();

        // RIGHT LEG
        pose.pushPose();
        pose.translate(-0.125, 1, 0);
        rotate(pose, figure.rightLeg());
        pose.translate(0, -0.75, 0);
        renderPartPair(pose, sink, type, light, rightLegBase, rightLegOverlay, BODY_SCALE, LIMB_OVERLAY_XZ, LIMB_OVERLAY_Y, LIMB_PIVOT_Y);
        pose.popPose();

        // LEFT ARM
        pose.pushPose();
        pose.translate(0.25, 1.625, 0);
        rotate(pose, figure.leftArm());
        pose.translate(0.125, -0.625, 0);
        renderPartPair(pose, sink, type, light, leftArmBase, leftArmOverlay, BODY_SCALE, LIMB_OVERLAY_XZ, LIMB_OVERLAY_Y, LIMB_PIVOT_Y);
        pose.popPose();

        // RIGHT ARM
        pose.pushPose();
        pose.translate(-0.25, 1.625, 0);
        rotate(pose, figure.rightArm());
        pose.translate(-0.125, -0.625, 0);
        renderPartPair(pose, sink, type, light, rightArmBase, rightArmOverlay, BODY_SCALE, LIMB_OVERLAY_XZ, LIMB_OVERLAY_Y, LIMB_PIVOT_Y);
        pose.popPose();

        // BODY
        pose.pushPose();
        pose.translate(0, 1.0, 0);
        renderPartPair(pose, sink, type, light, bodyBase, bodyOverlay, BODY_SCALE, BODY_OVERLAY_XZ, BODY_OVERLAY_Y, BODY_PIVOT_Y);
        pose.popPose();

        // HEAD
        double speed = 0.005;
        double amplitude = 1;
        pose.pushPose();
        pose.translate(0, 1.75, 0);
        pose.mulPose(Axis.XP.rotationDegrees((float) (Math.sin(time * speed) * amplitude)));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (Math.sin(time * speed + Math.PI * 0.5) * amplitude)));
        rotate(pose, figure.head());
        renderPartPair(pose, sink, type, light, headBase, headOverlay, HEAD_SCALE, HEAD_OVERLAY_XZ, HEAD_OVERLAY_Y, HEAD_PIVOT_Y);
        pose.popPose();

        pose.popPose();
    }
}
