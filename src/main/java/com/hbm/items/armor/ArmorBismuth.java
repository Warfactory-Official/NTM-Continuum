package com.hbm.items.armor;

import com.hbm.items.gear.ArmorFSB;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.GolemArmorMaterial;

/**
 * 1.7 {@code ArmorBismuth}: an ordinary {@link ArmorFSB} whose only addition was its OBJ body
 * ({@code ModelArmorBismuth}) and item renderer. Both are named through {@code setArmorModel("bismuth")}
 * and the {@code armor_bismuth_*} item-model animation; the three dashes come from the FSB dash count.
 */
public class ArmorBismuth extends ArmorFSB {

    public ArmorBismuth(GolemArmorMaterial material, ArmorSlot slot, Properties properties) {
        super(material, slot, properties);
    }
}
