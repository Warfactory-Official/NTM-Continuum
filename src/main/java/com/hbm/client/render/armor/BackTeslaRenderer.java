package com.hbm.client.render.armor;

import com.hbm.client.model.WavefrontObj;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import dev.golem.core.world.ArmorSlot;
import net.minecraft.resources.Identifier;

/**
 * 1.7 {@code ModelBackTesla}: a {@code ModelArmorBase} whose body is the whole {@code mod_tesla} OBJ,
 * posed on the torso. Drawn for a chestplate carrying the back-mounted tesla module.
 */
public class BackTeslaRenderer extends ArmorRenderer {

    private static final Identifier OBJ = Ids.of("hbm", "models/armor/mod_tesla.obj");
    private static final Surface SKIN = Surface.cutout("hbm:textures/models/armor/mod_tesla.png");

    @Override
    protected void setup(Gem.Model<ArmorPose> declaration) {
        WavefrontObj model = WavefrontObj.load(OBJ);
        piece(declaration, ArmorSlot.CHESTPLATE, model, "Cube", SKIN, Pivot.BODY);
    }
}
