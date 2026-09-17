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
    @Override public org.joml.Vector3fc[] extents() { return SpecialWeaponRenderers.extents(model()); }
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
