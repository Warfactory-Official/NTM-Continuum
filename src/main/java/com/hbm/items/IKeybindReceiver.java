package com.hbm.items;

import com.hbm.handler.HbmKeybinds.EnumKeybind;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * An item that reacts to HBM's own keybinds while held.
 *
 * <p>Ported from 1.12.2 {@code com.hbm.items.IKeybindReceiver}. The client reports key state changes;
 * {@link com.hbm.handler.HbmKeybindsServer} finds the held stack and, if its item implements this,
 * hands the edge over. Everything a gun does therefore happens server-side from a state change, never
 * from the client asserting an action.
 */
public interface IKeybindReceiver {

    /** Whether this item cares about {@code keybind} at all. */
    boolean canHandleKeybind(Player player, ItemStack stack, EnumKeybind keybind);

    /** Reacts to a key going down ({@code newState} true) or coming back up. */
    void handleKeybind(Player player, ItemStack stack, EnumKeybind keybind, boolean newState);
}
