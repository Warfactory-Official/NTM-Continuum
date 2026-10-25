package com.hbm.items.armor;

import com.hbm.client.ClientPlayerAccess;
import com.hbm.items.ModArmorItems;
import java.util.IdentityHashMap;
import java.util.Map;
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

    // Client-only callers. The lookup goes through ClientPlayerAccess so this common class carries no
    // client type in any signature: a LocalPlayer here would make the verifier load client classes on
    // a dedicated server when ModArmorItems links this class.
    public static IPAMelee clientMelee() {
        Player player = ClientPlayerAccess.player();
        return player == null ? null : melee(player);
    }

    public static IPARanged clientRanged() {
        Player player = ClientPlayerAccess.player();
        return player == null ? null : ranged(player);
    }

    private static boolean hasPowerArmorSet(Player player) {
        if (!ArmorFSB.hasFSBArmorIgnoreCharge(player)) return false;
        Item chest = player.getItemBySlot(EquipmentSlot.CHEST).getItem();
        return chest == ModArmorItems.rpa_plate.get() || chest == ModArmorItems.ncrpa_plate.get();
    }
}
