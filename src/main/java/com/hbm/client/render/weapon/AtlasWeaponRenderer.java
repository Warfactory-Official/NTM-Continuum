package com.hbm.client.render.weapon;

import com.hbm.client.anim.HbmAnimations;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * The break-action revolver, a direct port of 1.12.2 {@code ItemRenderAtlas}.
 *
 * <p>The part tree, its pivots and every constant are upstream's. Two things are worth knowing when
 * reading them: the model is authored at 8× (hence the 0.125 scale), and several joints read a bus's
 * <b>Z component as an angle</b> rather than a translation — {@code RECOIL} drives both the shove back
 * and the muzzle rise that way. That is upstream's shorthand, not a transcription slip.
 *
 * <p>The grip carries the whole weapon, so recoil, the draw sweep and the reload swing all apply there
 * and everything else inherits. The barrel assembly hinges as one under {@code FRONT} — which is what
 * makes it break-action — with the latch and drum nested inside it.
 */
public class AtlasWeaponRenderer extends WeaponRenderer {

    private static final Identifier MODEL = dev.golem.core.Ids.of("hbm", "models/weapons/bio_revolver.obj");
    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/models/weapons/bio_revolver.png");

    private WavefrontObj model;
    private RenderType body;

    /** Loaded on first draw: the resource manager is not ready when renderers are declared. */
    private WavefrontObj model() {
        if (model == null) {
            model = WavefrontObj.load(MODEL);
            body = CompatRenderTypes.entityCutout(TEXTURE);
        }
        return model;
    }

    @Override
    public org.joml.Vector3fc[] extents() {
        WavefrontObj obj = model();
        float[] mn = obj.boundsMin();
        float[] mx = obj.boundsMax();
        org.joml.Vector3fc[] corners = new org.joml.Vector3fc[8];
        int i = 0;
        for (int xi = 0; xi < 2; xi++) for (int yi = 0; yi < 2; yi++) for (int zi = 0; zi < 2; zi++) {
            corners[i++] = new org.joml.Vector3f(xi == 0 ? mn[0] : mx[0],
                    yi == 0 ? mn[1] : mx[1], zi == 0 ? mn[2] : mx[2]);
        }
        return corners;
    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        standardAimingTransform(stack, pose,
                -1.0F * offset, -0.75F * offset, offset,
                0, -3.125 / 8D, 0.25);
    }

    @Override
    public void renderFirstPerson(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay) {
        pose.scale(0.125F, 0.125F, 0.125F);
        submitParts(pose, collector, light, overlay, true);
    }

    @Override
    public void renderStatic(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay) {
        submitParts(pose, collector, light, overlay, false);
    }

    // No setupStatic override: the non-first-person framing lives in the base model's display{} block.
    // Upstream's setupInv/setupThirdPerson/setupEntity bodies are GL op chains in 1.12's own GUI space
    // (their setupInv opens with translate(8, 8, 0) — sixteenths of a slot, not block units), so
    // transcribing the calls into a modern PoseStack puts the model somewhere off screen. Composed into
    // a matrix and decomposed into MC's translation/rotation/scale, they are exactly the display entries
    // in gun_obj_base.json, which is where they now live.

    /** The shared part tree. Buses read live when {@code animated}, otherwise the rest pose. */
    private void submitParts(PoseStack pose, SubmitSink collector, int light, int overlay,
            boolean animated) {
        WavefrontObj obj = model();

        double[] recoil = bus("RECOIL", animated);
        double[] reloadMove = bus("RELOAD_MOVE", animated);
        double[] reloadRot = bus("RELOAD_ROT", animated);
        double[] equip = bus("EQUIP", animated);

        pose.translate(recoil[0], recoil[1], recoil[2]);
        pose.mulPose(Axis.XP.rotationDegrees((float) (recoil[2] * 10)));

        // The draw sweep pivots behind the weapon so it swings up into view.
        pose.translate(0, 0, -7);
        pose.mulPose(Axis.XN.rotationDegrees((float) equip[0]));
        pose.translate(0, 0, 7);

        pose.translate(reloadMove[0], reloadMove[1], reloadMove[2]);
        // Deliberately X, Z, Y — the order upstream applies them in.
        pose.mulPose(Axis.XP.rotationDegrees((float) reloadRot[0]));
        pose.mulPose(Axis.ZP.rotationDegrees((float) reloadRot[2]));
        pose.mulPose(Axis.YP.rotationDegrees((float) reloadRot[1]));
        submitPart(collector, pose, body, obj, "Grip", light, overlay);

        pose.pushPose(); // barrel assembly, hinged as one
        pose.mulPose(Axis.XP.rotationDegrees((float) bus("FRONT", animated)[2]));
        submitPart(collector, pose, body, obj, "Barrel", light, overlay);

        pose.pushPose(); // latch, hinged at the top rear of the barrel
        pose.translate(0, 2.3125, -0.875);
        pose.mulPose(Axis.XP.rotationDegrees((float) bus("LATCH", animated)[2]));
        pose.translate(0, -2.3125, 0.875);
        submitPart(collector, pose, body, obj, "Latch", light, overlay);
        pose.popPose();

        pose.pushPose(); // drum: one chamber (60°) per shot, and slides out to reload
        pose.translate(0, 1, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) (bus("DRUM", animated)[2] * 60)));
        pose.translate(0, -1, 0);
        pose.translate(0, 0, bus("DRUM_PUSH", animated)[2]);
        submitPart(collector, pose, body, obj, "Drum", light, overlay);
        pose.popPose();

        pose.popPose(); // end barrel assembly

        pose.pushPose(); // hammer: rests cocked at -45° and falls to 0 as it strikes
        pose.translate(0, 0, -4.5);
        pose.mulPose(Axis.XP.rotationDegrees((float) (-45 + 45 * bus("HAMMER", animated)[2])));
        pose.translate(0, 0, 4.5);
        submitPart(collector, pose, body, obj, "Hammer", light, overlay);
        pose.popPose();
    }

    /**
     * A bus's transform, or the rest pose for a static draw.
     *
     * <p>The animation state belongs to the <em>held</em> item, so an inventory icon that read it live
     * would replay whatever the player's hands are doing.
     */
    private static double[] bus(String name, boolean animated) {
        return animated ? HbmAnimations.getRelevantTransformation(name)
                : new double[] {0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 1, 2};
    }
}
