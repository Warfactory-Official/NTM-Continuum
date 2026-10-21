package com.hbm.client.render.armor;

import com.hbm.client.model.WavefrontObj;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import dev.golem.core.world.ArmorSlot;
import net.minecraft.resources.Identifier;

/**
 * 1.7 {@code ModelArmorTaurun}. The legs and boots are nudged apart by 0.01 block: upstream translated -0.01
 * before the left piece and +0.02 more before the right.
 */
public final class TaurunArmorRenderer extends ArmorRenderer {

    private static final Identifier OBJ = Ids.of("hbm", "models/armor/taurun.obj");

    private static final Surface HELMET = skin("taurun_helmet");
    private static final Surface CHEST = skin("taurun_chest");
    private static final Surface ARM = skin("taurun_arm");
    private static final Surface LEG = skin("taurun_leg");

    private static Surface skin(String name) {
        return Surface.cutout("hbm:textures/armor/" + name + ".png");
    }

    @Override
    protected void setup(Gem.Model<ArmorPose> declaration) {
        WavefrontObj model = WavefrontObj.load(OBJ);

        piece(declaration, ArmorSlot.HELMET, model, "Helmet", HELMET, Pivot.HEAD);

        piece(declaration, ArmorSlot.CHESTPLATE, model, "Chest", CHEST, Pivot.BODY);
        piece(declaration, ArmorSlot.CHESTPLATE, model, "LeftArm", ARM, Pivot.LEFT_ARM);
        piece(declaration, ArmorSlot.CHESTPLATE, model, "RightArm", ARM, Pivot.RIGHT_ARM);

        nudged(declaration, ArmorSlot.LEGGINGS, model, "LeftLeg", Pivot.LEFT_LEG, -0.01F);
        nudged(declaration, ArmorSlot.LEGGINGS, model, "RightLeg", Pivot.RIGHT_LEG, 0.01F);

        nudged(declaration, ArmorSlot.BOOTS, model, "LeftBoot", Pivot.LEFT_LEG, -0.01F);
        nudged(declaration, ArmorSlot.BOOTS, model, "RightBoot", Pivot.RIGHT_LEG, 0.01F);
    }

    /** As {@code piece}, with upstream's sideways translate applied outside the bone transform. */
    private static void nudged(Gem.Model<ArmorPose> declaration, ArmorSlot slot, WavefrontObj model, String group,
                               Pivot pivot, float x) {
        declaration.part(model.part(group), LEG)
                .visibleIf(state -> state.slot == slot)
                .translate(x, 0F, 0F)
                .pose(state -> pivot.pose(state.wearer))
                .scale(1F / 16F);
    }
}
