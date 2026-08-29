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

/** OBJ renderers for the Gen-3 Henry, heavy revolver, and Hangman .44 guns. */
public final class Sedna44WeaponRenderer extends WeaponRenderer {

    public enum Variant {
        HENRY("henry", "henry", false),
        HENRY_LINCOLN("henry", "henry_lincoln", false),
        HEAVY_REVOLVER("lilmac", "heavy_revolver", false),
        LILMAC("lilmac", "lilmac", true),
        PROTEGE("lilmac", "protege", false),
        HANGMAN("hangman", "hangman", false);

        private final String model;
        private final String texture;
        private final boolean scoped;

        Variant(String model, String texture, boolean scoped) {
            this.model = model;
            this.texture = texture;
            this.scoped = scoped;
        }
    }

    private static final double[] IDENTITY = {
            0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 1, 2
    };

    private final Variant variant;
    private final Identifier modelId;
    private final Identifier textureId;
    private final Identifier scopeId = Ids.of("hbm", "textures/misc/scope_44.png");
    private WavefrontObj model;
    private RenderType body;
    private RenderType scope;

    public Sedna44WeaponRenderer(Variant variant) {
        this.variant = variant;
        this.modelId = Ids.of("hbm", "models/weapons/" + variant.model + ".obj");
        this.textureId = Ids.of("hbm", "textures/models/weapons/" + variant.texture + ".png");
    }

    private WavefrontObj model() {
        if (model == null) {
            model = WavefrontObj.load(modelId);
            body = CompatRenderTypes.entityCutout(textureId);
            if (variant.scoped) scope = CompatRenderTypes.entityCutout(scopeId);
        }
        return model;
    }

    private RenderType body() {
        model();
        return body;
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
        if (variant == Variant.HENRY || variant == Variant.HENRY_LINCOLN || variant == Variant.HANGMAN) {
            return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F;
        }
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        float offset = 0.8F;
        if (variant == Variant.HENRY || variant == Variant.HENRY_LINCOLN) {
            pose.translate(0, 0, 0.875);
            standardAimingTransform(stack, pose, -1.25F * offset, -1F * offset, 1.75F * offset,
                    0, -5 / 8D, 1);
            float aiming = ItemGunBaseNT.prevAimingProgress
                    + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
            pose.mulPose(Axis.XP.rotationDegrees(-2.5F * aiming));
        } else if (variant == Variant.HANGMAN) {
            pose.translate(0, 0, 0.875);
            standardAimingTransform(stack, pose, -1.5F * offset, -0.875F * offset, 1.75F * offset,
                    0, -1.5 / 8D, 1.25);
        } else {
            pose.translate(0, 0, 1);
            standardAimingTransform(stack, pose, -1.0F * offset, -0.75F * offset, 1F * offset,
                    0, variant.scoped ? -4.75 / 8D : -3.875 / 8D, variant.scoped ? -0.25 : 0);
        }
    }

    @Override
    public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, variant.scoped ? 0.66F : 0.33F);
    }

    @Override
    public void renderFirstPerson(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay) {
        switch (variant) {
            case HENRY, HENRY_LINCOLN -> renderHenry(stack, pose, collector, light, overlay);
            case HEAVY_REVOLVER, LILMAC, PROTEGE -> renderHeavyRevolver(stack, pose, collector, light, overlay);
            case HANGMAN -> renderHangman(stack, pose, collector, light, overlay);
        }
    }

    @Override
    public void renderStatic(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay) {
        switch (variant) {
            case HENRY, HENRY_LINCOLN -> {
                submitPart(collector, pose, body(), model(), "Gun", light, overlay);
                submitPart(collector, pose, body(), model(), "Sight", light, overlay);
                submitPart(collector, pose, body(), model(), "Hammer", light, overlay);
                submitPart(collector, pose, body(), model(), "Lever", light, overlay);
                submitPart(collector, pose, body(), model(), "Front", light, overlay);
                submitPart(collector, pose, body(), model(), "Bullet", light, overlay);
            }
            case HEAVY_REVOLVER, LILMAC, PROTEGE -> {
                submitPart(collector, pose, body(), model(), "Gun", light, overlay);
                submitPart(collector, pose, body(), model(), "Pivot", light, overlay);
                submitPart(collector, pose, body(), model(), "Cylinder", light, overlay);
                submitPart(collector, pose, body(), model(), "Bullets", light, overlay);
                submitPart(collector, pose, body(), model(), "Hammer", light, overlay);
                if (variant.scoped) submitPart(collector, pose, scope(), model(), "Scope", light, overlay);
            }
            case HANGMAN -> {
                submitPart(collector, pose, body(), model(), "Rifle", light, overlay);
                submitPart(collector, pose, body(), model(), "Internals", light, overlay);
                submitPart(collector, pose, body(), model(), "Lid", light, overlay);
                submitPart(collector, pose, body(), model(), "Magazine", light, overlay);
                submitPart(collector, pose, body(), model(), "Bullets", light, overlay);
            }
        }
    }

    private RenderType scope() {
        model();
        return scope;
    }

    private void renderHenry(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay) {
        pose.scale(0.375F, 0.375F, 0.375F);
        double[] equip = bus("EQUIP");
        double[] sight = bus("SIGHT");
        double[] recoil = bus("RECOIL");
        double[] hammer = bus("HAMMER");
        double[] lever = bus("LEVER");
        double[] turn = bus("TURN");
        double[] lift = bus("LIFT");
        double[] twist = bus("TWIST");
        double[] bullet = bus("BULLET");
        double[] yeet = bus("YEET");
        double[] roll = bus("ROLL");

        pose.translate(recoil[0] * 2, recoil[1], recoil[2]);
        pose.mulPose(Axis.XP.rotationDegrees((float) (recoil[2] * 5)));
        pose.mulPose(Axis.ZP.rotationDegrees((float) turn[2]));
        pose.translate(yeet[0], yeet[1], yeet[2]);

        pose.translate(0, 1, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) roll[2]));
        pose.translate(0, -1, 0);

        pose.translate(0, -4, 4);
        pose.mulPose(Axis.XP.rotationDegrees((float) lift[0]));
        pose.translate(0, 4, -4);

        pose.translate(0, 2, -4);
        pose.mulPose(Axis.XN.rotationDegrees((float) equip[0]));
        pose.translate(0, -2, 4);

        submitPart(collector, pose, body(), model(), "Gun", light, overlay);

        pose.pushPose();
        pose.translate(0, 1.25, -0.1875);
        pose.mulPose(Axis.XP.rotationDegrees((float) sight[0]));
        pose.translate(0, -1.25, 0.1875);
        submitPart(collector, pose, body(), model(), "Sight", light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0.625, -3);
        pose.mulPose(Axis.XP.rotationDegrees((float) (-30 + hammer[0])));
        pose.translate(0, -0.625, 3);
        submitPart(collector, pose, body(), model(), "Hammer", light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0.25, -2.3125);
        pose.mulPose(Axis.XP.rotationDegrees((float) lever[0]));
        pose.translate(0, -0.25, 2.3125);
        submitPart(collector, pose, body(), model(), "Lever", light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 1, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) twist[2]));
        pose.translate(0, -1, 0);
        submitPart(collector, pose, body(), model(), "Front", light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(bullet[0], bullet[1], bullet[2] - 1);
        submitPart(collector, pose, body(), model(), "Bullet", light, overlay);
        pose.popPose();
    }

    private void renderHeavyRevolver(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay) {
        pose.scale(0.125F, 0.125F, 0.125F);
        pose.mulPose(Axis.YP.rotationDegrees(90));

        double[] equipSpin = bus("ROTATE");
        double[] spin = bus("SPIN");
        double[] recoil = bus("RECOIL");
        double[] reloadLift = bus("RELOAD_LIFT");
        double[] reloadJolt = bus("RELOAD_JOLT");
        double[] reloadTilt = bus("RELAOD_TILT");
        double[] cylinderFlip = bus("RELOAD_CYLINDER");
        double[] reloadBullets = bus("RELOAD_BULLETS");

        pose.mulPose(Axis.ZP.rotationDegrees((float) spin[0]));
        pose.translate(6, -3, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) equipSpin[0]));
        pose.translate(-6, 3, 0);

        standardAimingTransform(stack, pose, 0, 0, recoil[2], -recoil[2], 0, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) (recoil[2] * 10)));
        pose.mulPose(Axis.ZP.rotationDegrees((float) reloadLift[0]));
        pose.translate(reloadJolt[0], 0, 0);
        pose.mulPose(Axis.XP.rotationDegrees((float) reloadTilt[0]));

        submitPart(collector, pose, body(), model(), "Gun", light, overlay);

        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees((float) cylinderFlip[0]));
        submitPart(collector, pose, body(), model(), "Pivot", light, overlay);
        pose.translate(0, 1.75, 0);
        pose.mulPose(Axis.XP.rotationDegrees((float) (bus("DRUM")[2] * -60)));
        pose.translate(0, -1.75, 0);
        submitPart(collector, pose, body(), model(), "Cylinder", light, overlay);
        pose.translate(reloadBullets[0], reloadBullets[1], reloadBullets[2]);
        if (bus("RELOAD_BULLETS_CON")[0] != 1) {
            submitPart(collector, pose, body(), model(), "Bullets", light, overlay);
        }
        // Casings are deliberately omitted until the shared spent-casing system is ported.
        pose.popPose();

        pose.pushPose();
        pose.translate(4, 1.25, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) (-30 + 30 * bus("HAMMER")[2])));
        pose.translate(-4, -1.25, 0);
        submitPart(collector, pose, body(), model(), "Hammer", light, overlay);
        pose.popPose();

        if (variant.scoped) submitPart(collector, pose, scope(), model(), "Scope", light, overlay);
    }

    private void renderHangman(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay) {
        float offset = 0.8F;
        double[] equip = bus("EQUIP");
        double[] recoil = bus("RECOIL");
        double[] roll = bus("ROLL");
        double[] turn = bus("TURN");
        double[] smack = bus("SMACK");
        double[] lid = bus("LID");
        double[] mag = bus("MAG");
        double[] bullets = bus("BULLETS");

        pose.translate(1.5F * offset, 0, -1);
        pose.mulPose(Axis.YP.rotationDegrees((float) turn[1]));
        pose.translate(-1.5F * offset, 0, 1);
        pose.mulPose(Axis.ZP.rotationDegrees((float) roll[2]));
        pose.translate(smack[0], smack[1], smack[2]);

        pose.scale(0.125F, 0.125F, 0.125F);
        pose.translate(0, -4, -10);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 4, 10);
        pose.translate(0, 0, recoil[2]);

        submitPart(collector, pose, body(), model(), "Rifle", light, overlay);
        submitPart(collector, pose, body(), model(), "Internals", light, overlay);

        pose.pushPose();
        pose.translate(-2.1875, -1.75, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) lid[2]));
        pose.translate(2.1875, 1.75, 0);
        submitPart(collector, pose, body(), model(), "Lid", light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(mag[0], mag[1], mag[2]);
        submitPart(collector, pose, body(), model(), "Magazine", light, overlay);
        if (bullets[0] == 0) submitPart(collector, pose, body(), model(), "Bullets", light, overlay);
        pose.popPose();
    }

    private static double[] bus(String name) {
        return HbmAnimations.getRelevantTransformation(name);
    }
}
