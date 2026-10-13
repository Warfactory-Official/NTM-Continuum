package com.hbm.handler;

import com.hbm.items.IEquipReceiver;
import dev.golem.core.platform.Lifecycle;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.7 {@code ModEventHandler.onLivingUpdate}'s held-item half: a player whose held item's <em>item</em> differs from
 * last tick's ({@code previousEquipment[0]}), now holding an {@link IEquipReceiver}, gets {@code onEquip}.
 *
 * <p>1.7 compared against the equipment snapshot vanilla took at the end of the previous update; the port keeps that
 * snapshot itself, once per server tick for every player. A player who just joined starts from "nothing held", as
 * 1.7's {@code previousEquipment} did, so logging in with a crucible in hand deploys it.
 */
public final class EquipReceiverHandler {

    /** The last tick's held item, or the receiver's {@link IEquipReceiver#equipKey} for it. */
    private static final Map<UUID, Object> PREVIOUS = new ConcurrentHashMap<>();
    private static final Map<UUID, Object> PREVIOUS_OFF = new ConcurrentHashMap<>();

    private EquipReceiverHandler() {}

    public static void init() {
        Lifecycle.onServerTick(EquipReceiverHandler::tick);
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ItemStack held = player.getMainHandItem();
            Item item = held.isEmpty() ? Items.AIR : held.getItem();
            Object now = item instanceof IEquipReceiver keyed ? keyed.equipKey(held) : item;
            Object before = PREVIOUS.put(player.getUUID(), now);
            if (!held.isEmpty() && !now.equals(before) && item instanceof IEquipReceiver receiver) {
                receiver.onEquip(player, held);
            }
            // 1.12.2's off-hand edge (onEquip(player, OFF_HAND)); only receivers that override onEquipOffHand act on it
            ItemStack off = player.getOffhandItem();
            Item offItem = off.isEmpty() ? Items.AIR : off.getItem();
            Object offNow = offItem instanceof IEquipReceiver keyed ? keyed.equipKey(off) : offItem;
            Object offBefore = PREVIOUS_OFF.put(player.getUUID(), offNow);
            if (!off.isEmpty() && !offNow.equals(offBefore) && offItem instanceof IEquipReceiver receiver) {
                receiver.onEquipOffHand(player, off);
            }
        }
        if (PREVIOUS.size() > server.getPlayerList().getPlayerCount() * 2 + 16) {
            PREVIOUS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
            PREVIOUS_OFF.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        }
    }
}
