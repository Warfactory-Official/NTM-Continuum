package com.hbm.client.render.armor;

import com.hbm.client.model.WavefrontObj;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import dev.golem.core.world.ArmorSlot;
import net.minecraft.resources.Identifier;

/**
 * T-51b's worn body — the port's first modelled armour set.
 *
 * <p>The part tree is 1.12.2's {@code ModelArmorT51}: eight named objects in one OBJ, four textures
 * across them, and the boots drawn on the <b>leg</b> bones rather than bones of their own (the vanilla
 * humanoid has no foot bone, which is why upstream's model class copies the leg angles into its
 * {@code leftFoot}/{@code rightFoot} renderers).
 *
 * <p>Textures are per-region, not per-slot: the chestplate draws its torso from {@code t51_chest} and
 * its arms from {@code t51_arm}, and the leggings and boots share {@code t51_leg}.
 */
public class T51ArmorRenderer extends ArmorRenderer {

    private static final Identifier OBJ = Ids.of("hbm", "models/armor/t51.obj");

    private static final Surface HELMET = skin("t51_helmet");
    private static final Surface CHEST = skin("t51_chest");
    private static final Surface ARM = skin("t51_arm");
    private static final Surface LEG = skin("t51_leg");

    private static Surface skin(String name) {
        return Surface.cutout("hbm:textures/models/armor/" + name + ".png");
    }

    @Override
    protected void setup(Gem.Model<ArmorPose> declaration) {
        WavefrontObj model = WavefrontObj.load(OBJ);

        piece(declaration, ArmorSlot.HELMET, model, "Helmet", HELMET, Pivot.HEAD);

        piece(declaration, ArmorSlot.CHESTPLATE, model, "Chest", CHEST, Pivot.BODY);
        piece(declaration, ArmorSlot.CHESTPLATE, model, "LeftArm", ARM, Pivot.LEFT_ARM);
        piece(declaration, ArmorSlot.CHESTPLATE, model, "RightArm", ARM, Pivot.RIGHT_ARM);

        piece(declaration, ArmorSlot.LEGGINGS, model, "LeftLeg", LEG, Pivot.LEFT_LEG);
        piece(declaration, ArmorSlot.LEGGINGS, model, "RightLeg", LEG, Pivot.RIGHT_LEG);

        piece(declaration, ArmorSlot.BOOTS, model, "LeftBoot", LEG, Pivot.LEFT_LEG);
        piece(declaration, ArmorSlot.BOOTS, model, "RightBoot", LEG, Pivot.RIGHT_LEG);
    }
}
