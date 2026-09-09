package com.hbm.handler;

import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.IKeybindReceiver;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side landing point for a reported keybind change.
 *
 * <p>Ported from 1.12.2 {@code HbmKeybindsServer}. Both hands are offered the edge, so an akimbo or
 * offhand weapon reacts too, and an item that does not claim the keybind is skipped rather than being
 * handed an event it would have to filter itself.
 */
public class HbmKeybindsServer {

    private HbmKeybindsServer() {}

    public static void onPressedServer(ServerPlayer player, EnumKeybind keybind, boolean state) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof IKeybindReceiver receiver
                    && receiver.canHandleKeybind(player, stack, keybind)) {
                receiver.handleKeybind(player, stack, keybind, state);
            }
        }
        // Jetpacks are worn, not held. Keep the existing hand scan for guns and route only the
        // appended flight keys to the server-side flight state.
        if (keybind == EnumKeybind.JETPACK || keybind == EnumKeybind.JETPACK_HOVER
                || keybind == EnumKeybind.JETPACK_HUD || keybind == EnumKeybind.TOGGLE_JETPACK
                || keybind == EnumKeybind.TOGGLE_HEAD) {
            JetpackHandler.handleKeybind(player, keybind, state);
        }
    }
}
