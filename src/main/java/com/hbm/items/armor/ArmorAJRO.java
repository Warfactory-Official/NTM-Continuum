package com.hbm.items.armor;

import com.hbm.items.gear.ArmorFSBPowered;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.GolemArmorMaterial;

/**
 * 1.7 {@code ArmorAJRO}: the red/black repaint of the AJR suit. Upstream's class only supplied {@code ModelArmorAJRO} and the item renderer,
 * both named here through {@code setArmorModel("ajro")} and the {@code armor_ajro_*} item models; power,
 * drain and the set bonus are {@link ArmorFSBPowered}'s.
 */
public class ArmorAJRO extends ArmorFSBPowered {

    public ArmorAJRO(GolemArmorMaterial material, ArmorSlot slot, Properties properties,
                   long maxPower, long chargeRate, long consumption, long drain) {
        super(material, slot, properties, maxPower, chargeRate, consumption, drain);
    }
}
