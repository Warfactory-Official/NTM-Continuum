package com.hbm.client;

import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Ticks the local player's universal-grenade pin counter, as 1.7 {@code ItemGrenadeUniversal.onUpdate} did on the client
 * as well as the server. {@code inventoryTick} runs on the server only here, and without the client counter the
 * throw's client half never passed its draw-time gate, so the arm never swung.
 */
public final class GrenadeUniversalClient {

    private static ItemStack previous = ItemStack.EMPTY;
    private static boolean registered;

    private GrenadeUniversalClient() {}

    public static void register() {
        if (registered) return;
        registered = true;
        dev.golem.core.client.ClientTicks.onEndTick(GrenadeUniversalClient::tick);
    }

    private static void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        ItemStack held = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        if (player == null || !(held.getItem() instanceof ItemGrenadeUniversal)) {
            previous = ItemStack.EMPTY;
            return;
        }
        ItemGrenadeUniversal.clientHeldTick(player, held, previous);
        previous = held.copy();
    }
}
