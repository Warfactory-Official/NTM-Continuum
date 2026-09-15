package com.hbm.client.render.weapon;

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
}
