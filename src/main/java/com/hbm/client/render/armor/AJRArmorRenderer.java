package com.hbm.client.render.armor;

import com.hbm.client.model.WavefrontObj;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import dev.golem.core.world.ArmorSlot;
import net.minecraft.resources.Identifier;

/**
 * 1.7 {@code ModelArmorAJR} and {@code ModelArmorAJRO}: the same {@code AJR.obj}, four textures named by the
 * set ({@code ajr_*} or {@code ajro_*}). The OBJ's {@code RocketBox} group is never drawn by either model.
 */
public final class AJRArmorRenderer extends ArmorRenderer {

    private static final Identifier OBJ = Ids.of("hbm", "models/armor/ajr.obj");

    private final String set;

    public AJRArmorRenderer(String set) {
        this.set = set;
    }

    private Surface skin(String part) {
        return Surface.cutout("hbm:textures/armor/" + set + "_" + part + ".png");
    }

    @Override
    protected void setup(Gem.Model<ArmorPose> declaration) {
        WavefrontObj model = WavefrontObj.load(OBJ);

        piece(declaration, ArmorSlot.HELMET, model, "Head", skin("helmet"), Pivot.HEAD);

        piece(declaration, ArmorSlot.CHESTPLATE, model, "Body", skin("chest"), Pivot.BODY);
        piece(declaration, ArmorSlot.CHESTPLATE, model, "LeftArm", skin("arm"), Pivot.LEFT_ARM);
        piece(declaration, ArmorSlot.CHESTPLATE, model, "RightArm", skin("arm"), Pivot.RIGHT_ARM);

        piece(declaration, ArmorSlot.LEGGINGS, model, "LeftLeg", skin("leg"), Pivot.LEFT_LEG);
        piece(declaration, ArmorSlot.LEGGINGS, model, "RightLeg", skin("leg"), Pivot.RIGHT_LEG);

        piece(declaration, ArmorSlot.BOOTS, model, "LeftBoot", skin("leg"), Pivot.LEFT_LEG);
        piece(declaration, ArmorSlot.BOOTS, model, "RightBoot", skin("leg"), Pivot.RIGHT_LEG);
    }
}
