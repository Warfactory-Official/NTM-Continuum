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

    private static final Map<UUID, Item> PREVIOUS = new ConcurrentHashMap<>();

    private EquipReceiverHandler() {}

    public static void init() {
        Lifecycle.onServerTick(EquipReceiverHandler::tick);
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ItemStack held = player.getMainHandItem();
            Item now = held.isEmpty() ? Items.AIR : held.getItem();
            Item before = PREVIOUS.put(player.getUUID(), now);
            if (!held.isEmpty() && before != now && now instanceof IEquipReceiver receiver) {
                receiver.onEquip(player, held);
            }
        }
        if (PREVIOUS.size() > server.getPlayerList().getPlayerCount() * 2 + 16) {
            PREVIOUS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        }
    }
}
