package com.hbm.client.render.armor;

import com.hbm.client.model.WavefrontObj;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import dev.golem.core.world.ArmorSlot;

/**
 * The single-OBJ helmets of 1.7 {@code ModelArmorBase}: {@code ModelGlasses} (the ash goggles, {@code goggles.obj}
 * with {@code textures/armor/goggles.png}) and {@code ModelHat} (the Fabulous Hat, {@code hat.obj}). Both render the
 * whole OBJ on the head bone for the helmet slot only.
 */
public final class HeadModelArmorRenderer extends ArmorRenderer {

    private final String obj;
    private final String texture;

    public HeadModelArmorRenderer(String obj, String texture) {
        this.obj = obj;
        this.texture = texture;
    }

    @Override
    protected void setup(Gem.Model<ArmorPose> declaration) {
        WavefrontObj model = WavefrontObj.load(Ids.of("hbm", "models/armor/" + obj + ".obj"));
        declaration.part(model.all(), Surface.cutout("hbm:textures/armor/" + texture + ".png"))
                .visibleIf(state -> state.slot == ArmorSlot.HELMET)
                .pose(state -> Pivot.HEAD.pose(state.wearer))
                .scale(1F / 16F);
    }
}
