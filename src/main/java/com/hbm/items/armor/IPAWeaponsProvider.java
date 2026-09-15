package com.hbm.items.armor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IPAWeaponsProvider {
    static IPAMelee getMeleeComponentCommon(Player player) {
        return player == null ? null : ArmorPAWeapons.melee(player);
    }

    static IPAMelee getMeleeComponentClient() {
        return ArmorPAWeapons.clientMelee();
    }

    static IPARanged getRangedComponentCommon(Player player) {
        return player == null ? null : ArmorPAWeapons.ranged(player);
    }

    static IPARanged getRangedComponentClient() {
        return ArmorPAWeapons.clientRanged();
    }

    IPAMelee getMeleeComponent(Player player);
    IPARanged getRangedComponent(Player player);
}
