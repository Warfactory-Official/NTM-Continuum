package com.hbm.items.armor;

import com.hbm.items.ModArmorItems;
import dev.golem.core.world.ArmorSlot;
import com.hbm.lib.ModDamageSources;
import dev.golem.core.world.Compat;
import dev.golem.core.world.GolemArmorMaterial;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Common BJ full-set armor behavior, including its legacy flat-helmet safety eject. */
public class ArmorBJ extends ArmorFSBPowered {

    public ArmorBJ(GolemArmorMaterial material, ArmorSlot slot, Properties properties,
                   long maxPower, long chargeRate, long consumption, long drain) {
        super(material, slot, properties, maxPower, chargeRate, consumption, drain);
    }

    @Override
    public void armorTick(Level level, Player player, ItemStack stack) {
        super.armorTick(level, player, stack);
        if (this != ModArmorItems.bj_helmet.get() || !ArmorFSB.hasFSBArmorIgnoreCharge(player)
                || ArmorFSB.hasFSBArmor(player) || !(level instanceof ServerLevel server)) return;

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) return;
        if (!player.getInventory().add(helmet)) player.drop(helmet, false);
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        Compat.hurt(player, server, ModDamageSources.lunar(server), 1000F);
    }
}
