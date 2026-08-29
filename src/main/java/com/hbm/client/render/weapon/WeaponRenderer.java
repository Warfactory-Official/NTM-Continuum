package com.hbm.client.render.weapon;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * The first-person body of a Sedna weapon.
 *
 * <p>Ported from 1.12.2 {@code ItemRenderWeaponBase}. The essential thing this class does — and the thing
 * that makes a first-person weapon look right rather than like a floating item — is
 * <b>{@link PoseStack#setIdentity()} followed by the camera's rotation</b>. A held weapon is not posed by
 * vanilla's item display transforms at all: it is drawn in camera space, exactly as 1.12's cancelled
 * {@code RenderHandEvent} did. Trying to express it as a {@code display{}} slot produces a gun that is
 * the wrong size, in the wrong place, and rotating with the wrong pivot.
 *
 * <p>On top of that frame come the same three layers upstream applies, in order: the turn lag (the
 * weapon trails the crosshair when you look around), the walk sway, and finally the gun's own
 * {@code setupFirstPerson} offset with its aim-down-sights lerp.
 */
public abstract class WeaponRenderer {

    /** Partial tick of the current frame; the aiming lerp and the sway both read it. */
    protected static float interp;

    /**
     * Draws this weapon in the player's hands.
     *
     * <p>The pose arrives carrying vanilla's item transform, which is discarded outright: everything
     * below is built from the camera.
     */
    public void submitFirstPerson(ItemStack stack, PoseStack pose, SubmitSink collector, int light,
            int overlay, boolean otherArm) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        interp = dev.golem.core.client.Frames.partialTick();

        pose.pushPose();
        // Camera space, however this version expresses it — see HandSpace for why that differs.
        dev.golem.core.client.HandSpace.enterCameraSpace(pose);
        // The off hand is the right hand's body mirrored through the view plane.
        if (otherArm) pose.scale(-1F, 1F, 1F);
        setupTransformsAndSubmit(stack, player, pose, collector, light, overlay);
        pose.popPose();
    }

    /** Turn lag, walk sway, then the weapon's own offset — upstream's {@code setupTransformsAndRender}. */
    protected void setupTransformsAndSubmit(ItemStack stack, LocalPlayer player, PoseStack pose,
            SubmitSink collector, int light, int overlay) {

        float swayMagnitude = getSwayMagnitude(stack);
        float swayPeriod = getSwayPeriod(stack);
        float turnMagnitude = getTurnMagnitude(stack);

        // The weapon lags behind the camera when you turn: the difference between where you are looking
        // and where the arms have caught up to.
        float armPitch = Mth.lerp(interp, player.xBobO, player.xBob);
        float armYaw = Mth.lerp(interp, player.yBobO, player.yBob);
        pose.mulPose(Axis.XP.rotationDegrees((player.getViewXRot(interp) - armPitch) * 0.1F * turnMagnitude));
        pose.mulPose(Axis.YP.rotationDegrees((player.getViewYRot(interp) - armYaw) * 0.1F * turnMagnitude));

        pose.mulPose(Axis.YP.rotationDegrees(180));

        float distanceInterp = dev.golem.core.client.ViewBob.walkDistance(player, interp);
        float bob = dev.golem.core.client.ViewBob.bob(player, interp);
        pose.translate(
                Mth.sin(distanceInterp * (float) Math.PI * swayPeriod) * bob * 0.5F * swayMagnitude,
                -Math.abs(Mth.cos(distanceInterp * (float) Math.PI * swayPeriod) * bob) * swayMagnitude,
                0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(distanceInterp * (float) Math.PI * swayPeriod) * bob * 3.0F));
        pose.mulPose(Axis.XP.rotationDegrees(
                Math.abs(Mth.cos(distanceInterp * (float) Math.PI * swayPeriod - 0.2F) * bob) * 5.0F));

        setupFirstPerson(stack, pose);
        renderFirstPerson(stack, pose, collector, light, overlay);
    }

    /** Where the weapon sits relative to the camera. Overridden per weapon. */
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 1);
        if (Minecraft.getInstance().player.isShiftKeyDown()) {
            pose.translate(0, -3.875 / 8D, 0);
        } else {
            float offset = 0.8F;
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.translate(offset, -0.75F * offset, -0.5F * offset);
            pose.mulPose(Axis.YP.rotationDegrees(180));
        }
    }

    /** The upstream FOV adjustment for this renderer; most renderers deliberately leave it unchanged. */
    public float viewFov(ItemStack stack, float fov, float partialTick) {
        return fov;
    }

    protected static float aimingFov(ItemStack stack, float fov, float partialTick, float zoom) {
        return ItemGunBaseNT.applyAimingFov(fov, partialTick, zoom);
    }

    /** Lerps between the hip position and the sighted one by how far the aim transition has run. */
    public static void standardAimingTransform(ItemStack stack, PoseStack pose,
            double sX, double sY, double sZ, double aX, double aY, double aZ) {
        float aimingProgress = ItemGunBaseNT.prevAimingProgress
                + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
        pose.translate(sX + (aX - sX) * aimingProgress,
                sY + (aY - sY) * aimingProgress,
                sZ + (aZ - sZ) * aimingProgress);
    }

    /** Submits one named OBJ part at the current pose. */
    protected static void submitPart(SubmitSink collector, PoseStack pose, RenderType type,
            WavefrontObj model, String part, int light, int overlay) {
        collector.submitCustomGeometry(pose, type,
                (drawPose, buffer) -> model.renderPart(part, drawPose, buffer, light, overlay));
    }

    protected float getSwayMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 0.1F : 0.5F;
    }

    protected float getSwayPeriod(ItemStack stack) {
        return 0.75F;
    }

    protected float getTurnMagnitude(ItemStack stack) {
        return 2.75F;
    }

    /** The weapon's own geometry, drawn with its animation buses applied. */
    public abstract void renderFirstPerson(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay);

    /** The same geometry at rest, for the inventory icon, the ground and third person. */
    public abstract void renderStatic(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay);

    /**
     * Extra framing for the non-first-person draws. Identity by default, and normally left that way.
     *
     * <p>Those contexts are framed by the base model's {@code display{}} block, which is where upstream's
     * {@code setupInv}/{@code setupThirdPerson}/{@code setupEntity} op chains belong once composed: they
     * are written in 1.12's GUI space (sixteenths of a slot), so replaying the calls against a modern
     * {@link PoseStack} puts the weapon somewhere off screen. First person is the exception — that body
     * is genuinely code, because it is rebuilt from the camera every frame.
     */
    public void setupStatic(PoseStack pose, net.minecraft.world.item.ItemDisplayContext context) {}

    /**
     * The corners of this weapon's bounding box, in model units.
     *
     * <p>26.1 sizes and bounds a GUI item from these; a layer left with no extents reports an empty box,
     * which is enough on its own for the icon not to appear.
     */
    public abstract org.joml.Vector3fc[] extents();
}
