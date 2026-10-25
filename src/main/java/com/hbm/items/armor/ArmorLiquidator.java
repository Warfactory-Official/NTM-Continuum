package com.hbm.items.armor;

import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Attributes;
import dev.golem.core.world.GolemArmorMaterial;
import java.util.List;

/**
 * 1.7 {@code ArmorLiquidator}: the Liquidator suit. Every piece replaces its item attributes with +100
 * knockback resistance (operation 0) and -10% base movement speed (operation 1), applied per worn piece
 * by {@code ArmorModEvents} under the old {@code fixedUUIDs}. The helmet wears an M65 over the head
 * (see {@code ArmorModRenderLayer}); the body pieces use the plain liquidator texture. Its helmet draws
 * {@code overlay_dark}.
 */
public class ArmorLiquidator extends ArmorFSB {

    public ArmorLiquidator(GolemArmorMaterial material, ArmorSlot slot, Properties properties) {
        super(material, slot, properties);
    }

    /** 1.7 {@code getItemAttributeModifiers}. */
    public void collectModifiers(List<ItemArmorMod.Modifier> modifiers) {
        modifiers.add(new ItemArmorMod.Modifier(ItemArmorMod.Modifier.KNOCKBACK_RESISTANCE, 100D, Attributes.Op.ADD));
        modifiers.add(new ItemArmorMod.Modifier(ItemArmorMod.Modifier.MOVEMENT_SPEED, -0.1D, Attributes.Op.MULTIPLY_BASE));
    }
}
