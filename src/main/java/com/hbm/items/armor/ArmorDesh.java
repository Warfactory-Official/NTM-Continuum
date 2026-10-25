package com.hbm.items.armor;

import com.hbm.inventory.fluid.FluidType;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Attributes;
import dev.golem.core.world.GolemArmorMaterial;
import java.util.function.Supplier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

/**
 * 1.7 {@code ArmorDesh}: the steam suit. Each piece carries a {@code -0.025} movement-speed modifier with
 * operation 1 (multiply base), keyed by the piece's slot, whether or not the set is complete or fuelled - 1.7's
 * {@code getItemAttributeModifiers} did not look at either. 1.7 items applied those modifiers while equipped;
 * {@link #updateModifiers} re-asserts them from the player tick and drops a slot's modifier when the piece leaves.
 */
public class ArmorDesh extends ArmorFSBFueled {

    public ArmorDesh(GolemArmorMaterial material, ArmorSlot slot, Properties properties, Supplier<FluidType> fuel,
                     int maxFuel, int fillRate, int consumption, int drain) {
        super(material, slot, properties, fuel, maxFuel, fillRate, consumption, drain);
    }

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET};

    /** Per worn piece, 1.7 {@code ArmorModHandler.fixedUUIDs[armorType]}: one modifier per slot. */
    public static void updateModifiers(Player player) {
        var speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        for (EquipmentSlot slot : SLOTS) {
            String id = "hbm:armor_desh_" + slot.getName();
            if (player.getItemBySlot(slot).getItem() instanceof ArmorDesh) {
                Attributes.setTransient(speed, id, -0.025D, Attributes.Op.MULTIPLY_BASE);
            } else if (Attributes.has(speed, id)) {
                Attributes.removeTransient(speed, id);
            }
        }
    }
}
