package com.hbm.items.armor;

import com.hbm.items.gear.ArmorFSBPowered;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.GolemArmorMaterial;

/**
 * 1.7 {@code ArmorAJR}: Steel Ranger (T-45 AJR) power armour. Upstream's class only supplied {@code ModelArmorAJR} and the item renderer,
 * both named here through {@code setArmorModel("ajr")} and the {@code armor_ajr_*} item models; power,
 * drain and the set bonus are {@link ArmorFSBPowered}'s.
 */
public class ArmorAJR extends ArmorFSBPowered {

    public ArmorAJR(GolemArmorMaterial material, ArmorSlot slot, Properties properties,
                   long maxPower, long chargeRate, long consumption, long drain) {
        super(material, slot, properties, maxPower, chargeRate, consumption, drain);
    }
}
