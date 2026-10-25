package com.hbm.items.armor;

import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.GolemArmorMaterial;

/**
 * 1.7 {@code ArmorDigamma}: Fau's digamma suit. Upstream's class only supplied {@code ModelArmorDigamma} and the item renderer,
 * both named here through {@code setArmorModel("fau")} and the {@code armor_fau_*} item models; power,
 * drain and the set bonus are {@link ArmorFSBPowered}'s.
 */
public class ArmorDigamma extends ArmorFSBPowered {

    public ArmorDigamma(GolemArmorMaterial material, ArmorSlot slot, Properties properties,
                   long maxPower, long chargeRate, long consumption, long drain) {
        super(material, slot, properties, maxPower, chargeRate, consumption, drain);
    }
}
