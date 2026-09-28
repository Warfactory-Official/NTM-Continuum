package com.hbm.client.render.armor;

import com.hbm.client.model.WavefrontObj;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import dev.golem.core.world.ArmorSlot;
import net.minecraft.resources.Identifier;

/**
 * 1.7 {@code ModelArmorNCRPA} (NCR ranger power armour): {@code ncrpa.obj}, textures per region, boots on the leg
 * bones. The helmet's {@code Eyes} copy the head pose and are drawn with the helmet texture still bound, fullbright
 * with lighting off - an emissive surface at full light.
 */
public final class NCRPAArmorRenderer extends ArmorRenderer {

    private static final Identifier OBJ = Ids.of("hbm", "models/armor/ncrpa.obj");

    private static final Surface HELMET = skin("ncrpa_helmet");
    private static final Surface EYES = Surface.emissive("hbm:textures/armor/ncrpa_helmet.png");
    private static final Surface CHEST = skin("ncrpa_chest");
    private static final Surface ARM = skin("ncrpa_arm");
    private static final Surface LEG = skin("ncrpa_leg");

    private static Surface skin(String name) {
        return Surface.cutout("hbm:textures/armor/" + name + ".png");
    }

    @Override
    protected void setup(Gem.Model<ArmorPose> declaration) {
        WavefrontObj model = WavefrontObj.load(OBJ);

        piece(declaration, ArmorSlot.HELMET, model, "Helmet", HELMET, Pivot.HEAD);
        piece(declaration, ArmorSlot.HELMET, model, "Eyes", EYES, Pivot.HEAD)
                .light(state -> 0xF000F0);

        piece(declaration, ArmorSlot.CHESTPLATE, model, "LeftArm", ARM, Pivot.LEFT_ARM);
        piece(declaration, ArmorSlot.CHESTPLATE, model, "RightArm", ARM, Pivot.RIGHT_ARM);
        piece(declaration, ArmorSlot.CHESTPLATE, model, "Chest", CHEST, Pivot.BODY);

        piece(declaration, ArmorSlot.LEGGINGS, model, "LeftLeg", LEG, Pivot.LEFT_LEG);
        piece(declaration, ArmorSlot.LEGGINGS, model, "RightLeg", LEG, Pivot.RIGHT_LEG);

        piece(declaration, ArmorSlot.BOOTS, model, "LeftBoot", LEG, Pivot.LEFT_LEG);
        piece(declaration, ArmorSlot.BOOTS, model, "RightBoot", LEG, Pivot.RIGHT_LEG);
    }
}
