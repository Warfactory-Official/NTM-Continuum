package com.hbm.delegacy.legacy;

import com.hbm.Constants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Where a DE stack first meets the game: a player joining, a menu opening, an item entity spawning. */
@EventBusSubscriber(modid = Constants.MOD_ID)
public final class LegacyTriggers {
    private LegacyTriggers() {}

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String where = "player " + player.getScoreboardName();
        LegacyStacks.convertAll(player.getInventory(), player.server, where);
        LegacyStacks.convertAll(player.getEnderChestInventory(), player.server, where + " ender chest");
    }

    @SubscribeEvent
    public static void open(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        AbstractContainerMenu menu = event.getContainer();
        for (Slot slot : menu.slots) {
            if (!LegacyStacks.isLegacy(slot.getItem())) continue;
            String where = "menu " + menu.getClass().getSimpleName() + " slot " + slot.index;
            slot.set(LegacyStacks.convert(slot.getItem(), player.server, where));
        }
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity item)) return;
        if (!LegacyStacks.isLegacy(item.getItem())) return;
        String where = "item entity at " + item.blockPosition().toShortString();
        ItemStack out = LegacyStacks.convert(item.getItem(), event.getLevel().getServer(), where);
        if (out.isEmpty()) event.setCanceled(true);
        else item.setItem(out);
    }
}
