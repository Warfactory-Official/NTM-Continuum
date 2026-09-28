package com.hbm.items.armor;

import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.CompatArmorItem;
import dev.golem.core.world.GolemArmorMaterial;

/**
 * 1.7 {@code MaskOfInfamy}: an iron-material helmet with its own armor texture ({@code MaskOfInfamy.png},
 * here the {@code infamy} equipment asset) and nothing else.
 */
public class MaskOfInfamy extends CompatArmorItem {

    public MaskOfInfamy(GolemArmorMaterial material, Properties properties) {
        super(material, ArmorSlot.HELMET, properties);
    }
}
