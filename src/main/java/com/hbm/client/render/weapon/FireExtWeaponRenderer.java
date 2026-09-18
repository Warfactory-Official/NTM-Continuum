package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** GEM port of the non-Sedna ItemRenderFireExt renderer. */
public final class FireExtWeaponRenderer extends WeaponRenderer {
    private static final Surface NORMAL = SpecialWeaponRenderers.texture("fireext_normal");
    private static final Surface FOAM = SpecialWeaponRenderers.texture("fireext_foam");
    private static final Surface SAND = SpecialWeaponRenderers.texture("fireext_sand");
    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(Ids.of("hbm", "models/weapons/fireext.obj"));
        return model;
    }

    /**
     * Upstream's extinguisher is the one Sedna gun that is not an {@code ItemRenderWeaponBase}: it is a
     * plain {@code IItemRenderer}, so 1.7 handed it the matrix the engine had already put the held item
     * in, and the chain in {@link #setupFirstPerson} is written against <em>that</em> frame. Every other
     * weapon here discards it and rebuilds the pose from the camera; doing so for this one lands the
     * chain's half-block translate and 0.35 scale in camera space, which puts a three-block canister
     * around the near plane and draws nothing. So the hand pose the item model supplies is kept, and
     * only the turn lag and walk sway every Sedna weapon adds are skipped — 1.7 gave it neither.
     */
    @Override
    public void submitFirstPerson(ItemStack stack, PoseStack pose, dev.golem.core.render.SubmitSink collector,
            int light, int overlay, boolean otherArm) {
        interp = dev.golem.core.client.Frames.partialTick();
        pose.pushPose();
        setupFirstPerson(stack, pose);
        renderFirstPerson(stack, pose, collector, light, overlay,
                otherArm ? net.minecraft.world.InteractionHand.OFF_HAND
                        : net.minecraft.world.InteractionHand.MAIN_HAND);
        pose.popPose();
    }

    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(25F));
        pose.translate(0.5F, -0.5F, -0.5F);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(80F));
        pose.scale(0.35F, 0.35F, 0.35F);
    }
    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        declaration.part(obj.part("Cylinder"), NORMAL)
                .skin(FireExtWeaponRenderer::skin);
    }
    private static Surface skin(WeaponPose state) {
        return switch (SpecialWeaponRenderers.ammoPath(state)) {
            case "fext_foam" -> FOAM;
            case "fext_sand" -> SAND;
            default -> NORMAL;
        };
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack, float spinDegrees) {
        pose.translate(2F, 14F, 0F);
        pose.mulPose(Axis.YP.rotationDegrees(-90F));
        pose.mulPose(Axis.XP.rotationDegrees(-135F));
        pose.mulPose(Axis.YP.rotationDegrees(spinDegrees));
        pose.scale(4.5F, 4.5F, -4.5F);
    }

    @Override
    protected float guiSpinSpeed() {
        return 0.1F;
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        pose.scale(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(20F));
        pose.mulPose(Axis.of(new org.joml.Vector3f(0F, 1F, 1F).normalize()).rotationDegrees(-5F));
        pose.mulPose(Axis.YP.rotationDegrees(10F));
        pose.mulPose(Axis.XP.rotationDegrees(15F));
        pose.translate(0.75F, -2.75F, 0.5F);
    }

    @Override
    protected void setupEntity(PoseStack pose, ItemStack stack) {
        pose.scale(0.3F, 0.3F, 0.3F);
    }
}
