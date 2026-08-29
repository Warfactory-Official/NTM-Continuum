package com.hbm.client.render.weapon;

import com.hbm.client.anim.HbmAnimations;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** OBJ renderers for the Gen-3 Greasegun, LAG, Uzi, and Uzi akimbo guns. */
public final class Sedna9mmWeaponRenderer extends WeaponRenderer {

    public enum Variant {
        GREASEGUN("greasegun", "greasegun"),
        LAG("mike_hawk", "lag"),
        UZI("uzi", "uzi"),
        UZI_AKIMBO("uzi", "uzi");

        private final String model;
        private final String texture;

        Variant(String model, String texture) {
            this.model = model;
            this.texture = texture;
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

    public Sedna9mmWeaponRenderer(Variant variant) {
        this.variant = variant;
        this.modelId = Ids.of("hbm", "models/weapons/" + variant.model + ".obj");
        this.textureId = Ids.of("hbm", "textures/models/weapons/" + variant.texture + ".png");
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
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : variant == Variant.GREASEGUN ? -0.5F : -0.25F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        switch (variant) {
            case GREASEGUN -> standardAimingTransform(stack, pose,
                    -1.5F * offset, -1F * offset, 1.75F * offset,
                    0, -2.625 / 8D, 1.125);
            case LAG -> standardAimingTransform(stack, pose,
                    -1.5F * offset, -1F * offset, 1.5F * offset,
                    0, -3.375 / 8D, 0.5);
            case UZI -> standardAimingTransform(stack, pose,
                    -1.75F * offset, -1.5F * offset, 2.5F * offset,
                    0, -4.375 / 8D, 1);
            case UZI_AKIMBO -> { }
        }
    }

    @Override
    public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.33F);
    }

    @Override
    public void renderFirstPerson(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay) {
        switch (variant) {
            case GREASEGUN -> renderGreasegun(stack, pose, collector, light, overlay);
            case LAG -> renderLag(stack, pose, collector, light, overlay);
            case UZI -> {
                pose.pushPose();
                renderUzi(stack, pose, collector, light, overlay, 0, 1);
                pose.popPose();
            }
            case UZI_AKIMBO -> {
                float offset = 0.8F;
                for (int side = -1; side <= 1; side += 2) {
                    int rail = side == -1 ? 0 : 1;
                    pose.pushPose();
                    standardAimingTransform(stack, pose, -2.25F * offset * side,
                            -1.5F * offset, 2.5F * offset, 0, -4.375 / 8D, 1);
                    renderUzi(stack, pose, collector, light, overlay, rail, side);
                    pose.popPose();
                }
            }
        }
    }

    @Override
    public void renderStatic(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay) {
        switch (variant) {
            case GREASEGUN -> {
                submitPart(collector, pose, body(), model(), "Gun", light, overlay);
                submitPart(collector, pose, body(), model(), "Stock", light, overlay);
                submitPart(collector, pose, body(), model(), "Magazine", light, overlay);
                submitPart(collector, pose, body(), model(), "Handle", light, overlay);
                submitPart(collector, pose, body(), model(), "Flap", light, overlay);
                submitPart(collector, pose, body(), model(), "Bullet", light, overlay);
            }
            case LAG -> {
                submitPart(collector, pose, body(), model(), "Grip", light, overlay);
                submitPart(collector, pose, body(), model(), "Slide", light, overlay);
                submitPart(collector, pose, body(), model(), "Hammer", light, overlay);
            }
            case UZI -> submitUziStatic(pose, collector, light, overlay, "Gun");
            case UZI_AKIMBO -> submitUziAkimboStatic(pose, collector, light, overlay);
        }
    }

    private void renderGreasegun(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay) {
        pose.scale(0.375F, 0.375F, 0.375F);
        double[] equip = bus("EQUIP");
        double[] stock = bus("STOCK");
        double[] recoil = bus("RECOIL");
        double[] flap = bus("FLAP");
        double[] lift = bus("LIFT");
        double[] handle = bus("HANDLE");
        double[] mag = bus("MAG");
        double[] turn = bus("TURN");
        double[] bullet = bus("BULLET");

        pose.translate(0, -3, -3);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 3, 3);
        pose.translate(0, -3, -3);
        pose.mulPose(Axis.XP.rotationDegrees((float) lift[0]));
        pose.translate(0, 3, 3);
        if (ItemGunBaseNT.aimingProgress < 1F) pose.mulPose(Axis.ZP.rotationDegrees((float) turn[2]));
        pose.translate(0, 0, recoil[2]);
        submitPart(collector, pose, body(), model(), "Gun", light, overlay);

        pose.pushPose();
        pose.translate(0, 0, -4 - stock[2]);
        submitPart(collector, pose, body(), model(), "Stock", light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(mag[0], mag[1], mag[2]);
        submitPart(collector, pose, body(), model(), "Magazine", light, overlay);
        if (bullet[0] != 1) submitPart(collector, pose, body(), model(), "Bullet", light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, -1.4375, -0.125);
        pose.mulPose(Axis.XP.rotationDegrees((float) handle[0]));
        pose.translate(0, 1.4375, 0.125);
        submitPart(collector, pose, body(), model(), "Handle", light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0.53125, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) flap[2]));
        pose.translate(0, -0.5125, 0);
        submitPart(collector, pose, body(), model(), "Flap", light, overlay);
        pose.popPose();
    }

    private void renderLag(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay) {
        pose.scale(0.25F, 0.25F, 0.25F);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        double[] equip = bus("EQUIP");
        double[] addTrans = bus("ADD_TRANS");
        double[] addRot = bus("ADD_ROT");

        pose.translate(4, -4, 0);
        pose.mulPose(Axis.ZN.rotationDegrees((float) equip[0]));
        pose.translate(-4, 4, 0);
        pose.translate(addTrans[0], addTrans[1], addTrans[2]);
        pose.mulPose(Axis.ZP.rotationDegrees((float) addRot[2]));
        pose.mulPose(Axis.YP.rotationDegrees((float) addRot[1]));

        pose.pushPose();
        applyBus(pose, bus("Grip"));
        submitPart(collector, pose, body(), model(), "Grip", light, overlay);

        pose.pushPose();
        applyBus(pose, bus("Slide"));
        submitPart(collector, pose, body(), model(), "Slide", light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(3.125, 0.125, 0);
        pose.mulPose(Axis.ZN.rotationDegrees(25));
        pose.translate(-3.125, -0.125, 0);
        applyBus(pose, bus("Hammer"));
        submitPart(collector, pose, body(), model(), "Hammer", light, overlay);
        pose.popPose();

        if (((ItemGunBaseNT) stack.getItem()).getPrimaryMagazine(stack, 0).getAmount(stack, null) > 0) {
            pose.pushPose();
            applyBus(pose, bus("Bullet"));
            submitPart(collector, pose, body(), model(), "Bullet", light, overlay);
            pose.popPose();
        }

        pose.pushPose();
        applyBus(pose, bus("Magazine"));
        submitPart(collector, pose, body(), model(), "Magazine", light, overlay);
        pose.popPose();
        pose.popPose();
    }

    private void renderUzi(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay, int rail, int side) {
        pose.scale(0.25F, 0.25F, 0.25F);
        double[] equip = bus("EQUIP", rail);
        double[] stockFront = bus("STOCKFRONT", rail);
        double[] stockBack = bus("STOCKBACK", rail);
        double[] recoil = bus("RECOIL", rail);
        double[] lift = bus("LIFT", rail);
        double[] mag = bus("MAG", rail);
        double[] bullet = bus("BULLET", rail);
        double[] slide = bus("SLIDE", rail);
        double[] yeet = bus("YEET", rail);
        double[] speen = bus("SPEEN", rail);

        pose.translate(yeet[0], yeet[1], yeet[2]);
        pose.mulPose((side < 0 ? Axis.ZN : Axis.ZP).rotationDegrees((float) speen[0]));
        pose.translate(0, -2, -4);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 2, 4);
        pose.translate(0, 0, -6);
        pose.mulPose(Axis.XP.rotationDegrees((float) lift[0]));
        pose.translate(0, 0, 6);
        pose.translate(0, 0, recoil[2]);

        submitPart(collector, pose, body(), model(), side < 0 && variant == Variant.UZI_AKIMBO ? "GunMirror" : "Gun",
                light, overlay);

        pose.pushPose();
        pose.translate(0, 0.3125D, -5.75);
        pose.mulPose(Axis.XP.rotationDegrees((float) (180 - stockFront[0])));
        pose.translate(0, -0.3125D, 5.75);
        submitPart(collector, pose, body(), model(), "StockFront", light, overlay);
        pose.translate(0, -0.3125D, -3);
        pose.mulPose(Axis.XP.rotationDegrees((float) (-200 - stockBack[0])));
        pose.translate(0, 0.3125D, 3);
        submitPart(collector, pose, body(), model(), "StockBack", light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, slide[2]);
        submitPart(collector, pose, body(), model(), "Slide", light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(mag[0], mag[1], mag[2]);
        submitPart(collector, pose, body(), model(), "Magazine", light, overlay);
        if (bullet[0] == 1) submitPart(collector, pose, body(), model(), "Bullet", light, overlay);
        pose.popPose();
    }

    private void submitUziStatic(PoseStack pose, SubmitSink collector, int light, int overlay, String gunPart) {
        submitPart(collector, pose, body(), model(), gunPart, light, overlay);
        submitPart(collector, pose, body(), model(), "StockBack", light, overlay);
        submitPart(collector, pose, body(), model(), "StockFront", light, overlay);
        submitPart(collector, pose, body(), model(), "Slide", light, overlay);
        submitPart(collector, pose, body(), model(), "Magazine", light, overlay);
    }

    private void submitUziAkimboStatic(PoseStack pose, SubmitSink collector, int light, int overlay) {
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(225));
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees(25));
        pose.mulPose(Axis.YP.rotationDegrees(45));
        pose.translate(0.5, 0, 0);
        submitUziStatic(pose, collector, light, overlay, "Gun");
        pose.popPose();

        pose.translate(0, 0, 5);
        pose.pushPose();
        pose.mulPose(Axis.ZN.rotationDegrees(225));
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XN.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees(25));
        pose.mulPose(Axis.YN.rotationDegrees(45));
        pose.translate(-0.5, 0, 0);
        submitUziStatic(pose, collector, light, overlay, "GunMirror");
        pose.popPose();
    }

    private RenderType body() {
        model();
        return body;
    }

    private static double[] bus(String name) {
        return HbmAnimations.getRelevantTransformation(name);
    }

    private static double[] bus(String name, int rail) {
        return HbmAnimations.getRelevantTransformation(name, rail);
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
