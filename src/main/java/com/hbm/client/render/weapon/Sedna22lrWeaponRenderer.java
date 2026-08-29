package com.hbm.client.render.weapon;

import com.hbm.client.anim.HbmAnimations;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** OBJ renderers for the Gen-3 AM180, Star F, and Star F akimbo guns. */
public final class Sedna22lrWeaponRenderer extends WeaponRenderer {

    public enum Variant {
        AM180("am180", "am180"),
        STAR_F("star_f", "star_f"),
        STAR_F_AKIMBO("star_f", "star_f_elite");

        private final String model;
        private final String rendererTexture;

        Variant(String model, String rendererTexture) {
            this.model = model;
            this.rendererTexture = rendererTexture;
        }
    }

    private static final double[] IDENTITY = {
            0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 1, 2
    };

    private final Variant variant;
    private final Identifier modelId;
    private final Identifier textureId;
    private WavefrontObj model;
    private RenderType body;

    public Sedna22lrWeaponRenderer(Variant variant) {
        this.variant = variant;
        this.modelId = Ids.of("hbm", "models/weapons/" + variant.model + ".obj");
        this.textureId = Ids.of("hbm", "textures/models/weapons/" + variant.rendererTexture + ".png");
    }

    private WavefrontObj model() {
        if (model == null) {
            model = WavefrontObj.load(modelId);
            body = CompatRenderTypes.entityCutout(textureId);
        }
        return model;
    }

    @Override
    public org.joml.Vector3fc[] extents() {
        WavefrontObj obj = model();
        float[] min = obj.boundsMin();
        float[] max = obj.boundsMax();
        org.joml.Vector3fc[] corners = new org.joml.Vector3fc[8];
        int i = 0;
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            corners[i++] = new org.joml.Vector3f(x == 0 ? min[0] : max[0],
                    y == 0 ? min[1] : max[1], z == 0 ? min[2] : max[2]);
        }
        return corners;
    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : variant == Variant.AM180 ? -0.5F : -0.25F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        if (variant == Variant.AM180) {
            standardAimingTransform(stack, pose, -offset, -offset, offset, 0, -4.1875 / 8D, 0.25);
        } else {
            standardAimingTransform(stack, pose, -1.75F * offset, -1.75F * offset, 2.5F * offset,
                    0, -7.625 / 8D, 1);
        }
    }

    @Override
    public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.33F);
    }

    @Override
    public void renderFirstPerson(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay) {
        if (variant == Variant.AM180) {
            renderAm180(stack, pose, collector, light, overlay);
        } else if (variant == Variant.STAR_F_AKIMBO) {
            for (int side = -1; side <= 1; side += 2) {
                int rail = side == -1 ? 0 : 1;
                pose.pushPose();
                standardAimingTransform(stack, pose, -2F * 0.8F * side, -1.75F * 0.8F, 2.5F * 0.8F,
                        0, -7.625 / 8D, 1);
                renderStarF(stack, pose, collector, light, overlay, rail, side);
                pose.popPose();
            }
        } else {
            renderStarF(stack, pose, collector, light, overlay, 0, 1);
        }
    }

    @Override
    public void renderStatic(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay) {
        if (variant == Variant.AM180) {
            submitAm180Static(pose, collector, light, overlay);
        } else if (variant == Variant.STAR_F_AKIMBO) {
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(225));
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.mulPose(Axis.XP.rotationDegrees(25));
            pose.mulPose(Axis.YP.rotationDegrees(45));
            pose.translate(0.5, 0, 0);
            submitStarStatic(pose, collector, light, overlay);
            pose.popPose();

            pose.translate(0, 0, 5);
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(225));
            pose.mulPose(Axis.YN.rotationDegrees(90));
            pose.mulPose(Axis.XN.rotationDegrees(90));
            pose.mulPose(Axis.XP.rotationDegrees(25));
            pose.mulPose(Axis.YN.rotationDegrees(45));
            pose.translate(-0.5, 0, 0);
            submitStarStatic(pose, collector, light, overlay);
            pose.popPose();
        } else {
            submitStarStatic(pose, collector, light, overlay);
        }
    }

    private void renderAm180(ItemStack stack, PoseStack pose, SubmitSink collector, int light, int overlay) {
        pose.scale(0.1875F, 0.1875F, 0.1875F);
        double[] equip = bus("EQUIP", true, 0);
        double[] recoil = bus("RECOIL", true, 0);
        double[] magazine = bus("MAG", true, 0);
        double[] magTurn = bus("MAGTURN", true, 0);
        double[] magSpin = bus("MAGSPIN", true, 0);
        double[] bolt = bus("BOLT", true, 0);
        double[] turn = bus("TURN", true, 0);

        pose.translate(0, -2, -6);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 2, 6);
        pose.mulPose(Axis.ZP.rotationDegrees((float) turn[2]));
        pose.translate(0, 0, recoil[2]);

        submitPart(collector, pose, body(), model(), "Gun", light, overlay);

        pose.pushPose();
        applyBus(pose, bus("Trigger", true, 0));
        submitPart(collector, pose, body(), model(), "Trigger", light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, bolt[2]);
        applyBus(pose, bolt);
        submitPart(collector, pose, body(), model(), "Bolt", light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(magazine[0], magazine[1], magazine[2]);
        pose.translate(0, 2.0625, 3.75);
        pose.mulPose(Axis.XP.rotationDegrees((float) magTurn[0]));
        pose.mulPose(Axis.ZP.rotationDegrees((float) magTurn[2]));
        pose.translate(0, -2.0625, -3.75);
        pose.translate(0, 2.3125, 1.5);
        pose.mulPose(Axis.XP.rotationDegrees((float) magSpin[0]));
        pose.translate(0, -2.3125, -1.5);
        applyBus(pose, bus("Mag", true, 0));

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        int amount = Minecraft.getInstance().player == null ? 0
                : gun.getPrimaryMagazine(stack, 0).getAmount(stack, Minecraft.getInstance().player.getInventory());
        pose.pushPose();
        pose.translate(0, 0, 1.5);
        pose.mulPose(Axis.YN.rotationDegrees((float) (amount / 59D * 360D)));
        pose.translate(0, 0, -1.5);
        submitPart(collector, pose, body(), model(), "Mag", light, overlay);
        pose.popPose();
        submitPart(collector, pose, body(), model(), "MagPlate", light, overlay);
        pose.popPose();
    }

    private void renderStarF(ItemStack stack, PoseStack pose, SubmitSink collector, int light, int overlay,
            int rail, int side) {
        pose.scale(0.25F, 0.25F, 0.25F);
        double[] equip = bus("EQUIP", true, rail);
        double[] recoil = bus("RECOIL", true, rail);
        double[] hammer = bus("HAMMER", true, rail);
        double[] tilt = bus("TILT", true, rail);
        double[] turn = bus("TURN", true, rail);
        double[] mag = bus("MAG", true, rail);
        double[] bullet = bus("BULLET", true, rail);
        double[] slide = bus("SLIDE", true, rail);

        pose.translate(0, -2, -8);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 2, 8);
        pose.translate(0, 1, -3);
        pose.mulPose(Axis.ZP.rotationDegrees((float) (turn[2] * side)));
        pose.mulPose(Axis.XP.rotationDegrees((float) tilt[0]));
        pose.translate(0, -1, 3);
        pose.translate(0, 0, recoil[2]);

        submitPart(collector, pose, body(), model(), "Gun", light, overlay);

        pose.pushPose();
        pose.translate(0, 1.75, -4.25);
        pose.mulPose(Axis.XP.rotationDegrees((float) (60 * (hammer[0] - 1))));
        pose.translate(0, -1.75, 4.25);
        submitPart(collector, pose, body(), model(), "Hammer", light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, slide[2] * 2.3125);
        submitPart(collector, pose, body(), model(), "Slide", light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(mag[0], mag[1], mag[2]);
        submitPart(collector, pose, body(), model(), "Mag", light, overlay);
        pose.translate(bullet[0], bullet[1], bullet[2]);
        submitPart(collector, pose, body(), model(), "Bullet", light, overlay);
        pose.popPose();
    }

    private void submitAm180Static(PoseStack pose, SubmitSink collector, int light, int overlay) {
        submitPart(collector, pose, body(), model(), "Gun", light, overlay);
        submitPart(collector, pose, body(), model(), "Trigger", light, overlay);
        submitPart(collector, pose, body(), model(), "Bolt", light, overlay);
        submitPart(collector, pose, body(), model(), "Mag", light, overlay);
        submitPart(collector, pose, body(), model(), "MagPlate", light, overlay);
    }

    private void submitStarStatic(PoseStack pose, SubmitSink collector, int light, int overlay) {
        submitPart(collector, pose, body(), model(), "Gun", light, overlay);
        submitPart(collector, pose, body(), model(), "Slide", light, overlay);
        submitPart(collector, pose, body(), model(), "Mag", light, overlay);
        submitPart(collector, pose, body(), model(), "Hammer", light, overlay);
    }

    private RenderType body() {
        model();
        return body;
    }

    private static double[] bus(String name, boolean animated, int rail) {
        return animated ? HbmAnimations.getRelevantTransformation(name, rail) : IDENTITY.clone();
    }

    private static void applyBus(PoseStack pose, double[] transform) {
        pose.translate(transform[0], transform[1], transform[2]);
        for (int i = 0; i < 3; i++) {
            int axis = (int) transform[12 + i];
            float degrees = (float) transform[3 + axis];
            if (axis == 0) pose.mulPose(Axis.XP.rotationDegrees(degrees));
            else if (axis == 1) pose.mulPose(Axis.YP.rotationDegrees(degrees));
            else pose.mulPose(Axis.ZP.rotationDegrees(degrees));
        }
        pose.translate(-transform[9], -transform[10], -transform[11]);
        pose.scale((float) transform[6], (float) transform[7], (float) transform[8]);
    }
}
