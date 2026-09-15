package com.hbm.client.render.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import org.jspecify.annotations.Nullable;

/**
 * The first-person body of a Sedna weapon.
 *
 * <p>A held weapon is not posed by vanilla's item display transforms at all: it is drawn in camera
 * space, as 1.12's cancelled {@code RenderHandEvent} did — {@link PoseStack#setIdentity()} then the
 * camera's rotation. Expressed as a {@code display{}} slot instead it is the wrong size, in the wrong
 * place, rotating about the wrong pivot. On that frame come upstream's three layers in order: turn
 * lag, walk sway, then the weapon's own offset with its aim-down-sights lerp.
 */
public abstract class WeaponRenderer {

    protected static float interp;

    private final WeaponPose pose = new WeaponPose();
    private Gem.@Nullable Model<WeaponPose> model;

    protected abstract void setup(Gem.Model<WeaponPose> declaration);

    /** Built on first use, which is after resources load and so after {@link #setup} can name groups. */
    public final Gem.Model<WeaponPose> gem() {
        Gem.Model<WeaponPose> built = model;
        if (built == null) {
            built = Gem.model();
            built.overlay(state -> state.overlay);
            setup(built);
            built.seal();
            model = built;
        }
        return built;
    }

    /** The pose arrives carrying vanilla's item transform, which is discarded: this is camera space. */
    public void submitFirstPerson(ItemStack stack, PoseStack pose, SubmitSink collector, int light,
            int overlay, boolean otherArm) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        interp = dev.golem.core.client.Frames.partialTick();

        pose.pushPose();
        dev.golem.core.client.HandSpace.enterCameraSpace(pose);
        if (otherArm) pose.scale(-1F, 1F, 1F);
        setupTransformsAndSubmit(stack, player, pose, collector, light, overlay, otherArm);
        pose.popPose();
    }

    /** Turn lag, walk sway, then the weapon's own offset — upstream's {@code setupTransformsAndRender}. */
    protected void setupTransformsAndSubmit(ItemStack stack, LocalPlayer player, PoseStack pose,
            SubmitSink collector, int light, int overlay, boolean otherArm) {

        float swayMagnitude = getSwayMagnitude(stack);
        float swayPeriod = getSwayPeriod(stack);
        float turnMagnitude = getTurnMagnitude(stack);

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
        renderFirstPerson(stack, pose, collector, light, overlay,
                otherArm ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
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
        pose.translate(aimLerp(sX, aX), aimLerp(sY, aY), aimLerp(sZ, aZ));
    }

    protected static float aimLerp(double hip, double sighted) {
        float aimingProgress = ItemGunBaseNT.prevAimingProgress
                + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
        return (float) (hip + (sighted - hip) * aimingProgress);
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

    public final void renderFirstPerson(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay, InteractionHand hand) {
        replay(stack, true, pose, collector, light, overlay,
                hand == InteractionHand.OFF_HAND ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                        : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                Minecraft.getInstance().player, hand);
    }

    public final void renderStatic(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay) {
        renderStatic(stack, pose, collector, light, overlay, null);
    }

    public final void renderStatic(ItemStack stack, PoseStack pose, SubmitSink collector,
            int light, int overlay, net.minecraft.world.entity.@Nullable LivingEntity owner) {
        replay(stack, false, pose, collector, light, overlay, staticContext, owner, InteractionHand.MAIN_HAND);
    }

    private void replay(ItemStack stack, boolean animated, PoseStack poseStack, SubmitSink collector,
            int light, int overlay, ItemDisplayContext context,
            net.minecraft.world.entity.@Nullable LivingEntity owner, InteractionHand hand) {
        pose.begin(stack, animated, overlay, context, owner, hand);
        Gem.replay(gem(), pose, poseStack, collector, light);
        WeaponEffects.render(pose, poseStack, collector, light, overlay);
    }

    /**
     * Extra framing for the non-first-person draws, normally left identity: those contexts are framed
     * by the base model's {@code display{}} block. Upstream's {@code setupInv}/{@code setupThirdPerson}
     * chains are written in 1.12 GUI space and put the weapon off screen if replayed as calls.
     */
    public void setupStatic(PoseStack pose, ItemDisplayContext context) {
        staticContext = context;
    }

    /** Recorded because {@code renderStatic} is not handed it and Dani's rest pose depends on it. */
    private ItemDisplayContext staticContext = ItemDisplayContext.NONE;

    /** 26.1 sizes a GUI item from these; no extents means an empty box and no icon at all. */
    public abstract org.joml.Vector3fc[] extents();
}
