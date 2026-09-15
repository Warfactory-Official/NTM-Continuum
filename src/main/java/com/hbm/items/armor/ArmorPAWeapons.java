package com.hbm.items.armor;

import com.hbm.items.gear.ArmorFSB;
import com.hbm.items.ModArmorItems;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/** Maps the source PA weapon components onto the current component-backed armor items. */
public final class ArmorPAWeapons {
    private static final Map<Item, IPAMelee> MELEE = new IdentityHashMap<>();
    private static final Map<Item, IPARanged> RANGED = new IdentityHashMap<>();

    private ArmorPAWeapons() {}

    public static void registerMelee(Item chest, IPAMelee component) { MELEE.put(chest, component); }
    public static void registerRanged(Item chest, IPARanged component) { RANGED.put(chest, component); }

    public static IPAMelee melee(Player player) {
        if (!hasPowerArmorSet(player)) return null;
        return MELEE.get(player.getItemBySlot(EquipmentSlot.CHEST).getItem());
    }

    public static IPARanged ranged(Player player) {
        if (!hasPowerArmorSet(player)) return null;
        return RANGED.get(player.getItemBySlot(EquipmentSlot.CHEST).getItem());
    }

    public static IPAMelee clientMelee() {
        return Minecraft.getInstance().player == null ? null : melee(Minecraft.getInstance().player);
    }

    public static IPARanged clientRanged() {
        return Minecraft.getInstance().player == null ? null : ranged(Minecraft.getInstance().player);
    }

    private static boolean hasPowerArmorSet(Player player) {
        if (!ArmorFSB.hasFSBArmorIgnoreCharge(player)) return false;
        Item chest = player.getItemBySlot(EquipmentSlot.CHEST).getItem();
        return chest == ModArmorItems.rpa_plate.get() || chest == ModArmorItems.ncrpa_plate.get();
    }
}
