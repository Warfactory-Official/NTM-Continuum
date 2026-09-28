package com.hbm.client.render.armor;

import com.hbm.client.model.WavefrontObj;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import dev.golem.core.world.ArmorSlot;
import net.minecraft.resources.Identifier;

/** 1.7 {@code ModelArmorBismuth}: one texture over every part, boots on the leg bones. */
public final class BismuthArmorRenderer extends ArmorRenderer {

    private static final Identifier OBJ = Ids.of("hbm", "models/armor/bismuth.obj");
    private static final Surface SKIN = Surface.cutout("hbm:textures/armor/bismuth.png");

    @Override
    protected void setup(Gem.Model<ArmorPose> declaration) {
        WavefrontObj model = WavefrontObj.load(OBJ);

        piece(declaration, ArmorSlot.HELMET, model, "Head", SKIN, Pivot.HEAD);

        piece(declaration, ArmorSlot.CHESTPLATE, model, "LeftArm", SKIN, Pivot.LEFT_ARM);
        piece(declaration, ArmorSlot.CHESTPLATE, model, "RightArm", SKIN, Pivot.RIGHT_ARM);
        piece(declaration, ArmorSlot.CHESTPLATE, model, "Body", SKIN, Pivot.BODY);

        piece(declaration, ArmorSlot.LEGGINGS, model, "LeftLeg", SKIN, Pivot.LEFT_LEG);
        piece(declaration, ArmorSlot.LEGGINGS, model, "RightLeg", SKIN, Pivot.RIGHT_LEG);

        piece(declaration, ArmorSlot.BOOTS, model, "LeftFoot", SKIN, Pivot.LEFT_LEG);
        piece(declaration, ArmorSlot.BOOTS, model, "RightFoot", SKIN, Pivot.RIGHT_LEG);
    }
}
