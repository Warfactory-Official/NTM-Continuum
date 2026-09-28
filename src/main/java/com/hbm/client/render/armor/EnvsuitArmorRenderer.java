package com.hbm.client.render.armor;

import com.hbm.client.model.WavefrontObj;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import dev.golem.core.world.ArmorSlot;
import net.minecraft.resources.Identifier;

/**
 * 1.7 {@code ModelArmorEnvsuit}: the helmet is alpha-blended; its {@code Lamps} copy the head pose and are drawn
 * untextured in (1, 1, 0.8) at full brightness with lighting off - here a white emissive surface with that tint.
 * The OBJ's {@code Tail} group was never drawn. Feet are {@code LeftFoot}/{@code RightFoot} on the leg bones.
 */
public final class EnvsuitArmorRenderer extends ArmorRenderer {

    private static final Identifier OBJ = Ids.of("hbm", "models/armor/envsuit.obj");

    private static final Surface HELMET = Surface.translucent("hbm:textures/armor/envsuit_helmet.png");
    private static final Surface LAMPS = Surface.emissive("hbm:textures/models/misc/white.png");
    private static final Surface CHEST = skin("envsuit_chest");
    private static final Surface ARM = skin("envsuit_arm");
    private static final Surface LEG = skin("envsuit_leg");

    private static Surface skin(String name) {
        return Surface.cutout("hbm:textures/armor/" + name + ".png");
    }

    @Override
    protected void setup(Gem.Model<ArmorPose> declaration) {
        WavefrontObj model = WavefrontObj.load(OBJ);

        piece(declaration, ArmorSlot.HELMET, model, "Helmet", HELMET, Pivot.HEAD);
        piece(declaration, ArmorSlot.HELMET, model, "Lamps", LAMPS, Pivot.HEAD)
                .tint(state -> 0xFFFFFFCC)
                .light(state -> 0xF000F0);

        piece(declaration, ArmorSlot.CHESTPLATE, model, "Chest", CHEST, Pivot.BODY);
        piece(declaration, ArmorSlot.CHESTPLATE, model, "LeftArm", ARM, Pivot.LEFT_ARM);
        piece(declaration, ArmorSlot.CHESTPLATE, model, "RightArm", ARM, Pivot.RIGHT_ARM);

        piece(declaration, ArmorSlot.LEGGINGS, model, "LeftLeg", LEG, Pivot.LEFT_LEG);
        piece(declaration, ArmorSlot.LEGGINGS, model, "RightLeg", LEG, Pivot.RIGHT_LEG);

        piece(declaration, ArmorSlot.BOOTS, model, "LeftFoot", LEG, Pivot.LEFT_LEG);
        piece(declaration, ArmorSlot.BOOTS, model, "RightFoot", LEG, Pivot.RIGHT_LEG);
    }
}
