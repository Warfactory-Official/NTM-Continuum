package com.hbm.client.render.armor;

import com.hbm.client.model.WavefrontObj;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import dev.golem.core.world.ArmorSlot;
import net.minecraft.resources.Identifier;

/**
 * 1.7 {@code ModelArmorRPA} (Remnant power armour): {@code remnant.obj}, textures per region like T-51 (the chestplate
 * draws its arms from {@code rpa_arm} and its torso from {@code rpa_chest}; leggings and boots share {@code rpa_leg};
 * boots on the leg bones).
 *
 * <p>Two chest extras. {@code Glow} copies the body pose and is drawn with the chest texture still bound, fullbright
 * with lighting off ({@code setLightmapTextureCoords(240, 240)}, {@code GL_LIGHTING} disabled) - an emissive surface
 * at full light, as the envsuit lamps. {@code Fan} rides the body's pivot and spins about Z through
 * {@code (0, 4.875, 0)} px at {@code -currentTimeMillis / 2 % 360} degrees, wall-clock like 1.7.
 */
public final class RPAArmorRenderer extends ArmorRenderer {

    private static final Identifier OBJ = Ids.of("hbm", "models/armor/remnant.obj");

    private static final Surface HELMET = skin("rpa_helmet");
    private static final Surface CHEST = skin("rpa_chest");
    private static final Surface ARM = skin("rpa_arm");
    private static final Surface LEG = skin("rpa_leg");
    private static final Surface GLOW = Surface.emissive("hbm:textures/armor/rpa_chest.png");

    private static Surface skin(String name) {
        return Surface.cutout("hbm:textures/armor/" + name + ".png");
    }

    @Override
    protected void setup(Gem.Model<ArmorPose> declaration) {
        WavefrontObj model = WavefrontObj.load(OBJ);

        piece(declaration, ArmorSlot.HELMET, model, "Head", HELMET, Pivot.HEAD);

        piece(declaration, ArmorSlot.CHESTPLATE, model, "LeftArm", ARM, Pivot.LEFT_ARM);
        piece(declaration, ArmorSlot.CHESTPLATE, model, "RightArm", ARM, Pivot.RIGHT_ARM);
        piece(declaration, ArmorSlot.CHESTPLATE, model, "Body", CHEST, Pivot.BODY);
        piece(declaration, ArmorSlot.CHESTPLATE, model, "Glow", GLOW, Pivot.BODY)
                .light(state -> 0xF000F0);
        piece(declaration, ArmorSlot.CHESTPLATE, model, "Fan", CHEST, Pivot.BODY)
                .spin(Gem.Axes.Z, 0F, 4.875F, 0F, state -> (float) (-System.currentTimeMillis() / 2D % 360D));

        piece(declaration, ArmorSlot.LEGGINGS, model, "LeftLeg", LEG, Pivot.LEFT_LEG);
        piece(declaration, ArmorSlot.LEGGINGS, model, "RightLeg", LEG, Pivot.RIGHT_LEG);

        piece(declaration, ArmorSlot.BOOTS, model, "LeftBoot", LEG, Pivot.LEFT_LEG);
        piece(declaration, ArmorSlot.BOOTS, model, "RightBoot", LEG, Pivot.RIGHT_LEG);
    }
}
