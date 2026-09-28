package com.hbm.items.armor;

import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.CompatArmorItem;
import dev.golem.core.world.GolemArmorMaterial;

/**
 * 1.7 {@code ArmorAshGlasses}: the ash goggles, worn as {@code ModelGlasses} ({@code goggles.obj} with
 * {@code textures/armor/goggles.png}). They protect against light and sand (ArmorUtil), let the wearer see gas
 * clouds, and keep most of the ash overlay out of the eyes.
 */
public class ArmorAshGlasses extends CompatArmorItem implements IWornModel {

    public ArmorAshGlasses(GolemArmorMaterial material, ArmorSlot slot, Properties properties) {
        super(material, slot, properties);
    }

    @Override
    public String wornModel() {
        return "ashglasses";
    }
}
