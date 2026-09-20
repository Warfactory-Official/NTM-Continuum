package com.hbm.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The held-item counterpart of {@link ILookOverlay}: a readout beside the crosshair that depends on the item in
 * the main hand as well as the block looked at (1.7 {@code IItemLookOverlay} / ntm-next {@code buildLookOverlay}).
 * Like {@link ILookOverlay} it names no GUI class, so items stay safe to load on a dedicated server.
 */
public interface IItemLookOverlay {

    /** Returns the readout for {@code stack} while looking at {@code pos}, or {@code null} for none. */
    ILookOverlay.@Nullable Data getLookOverlay(Level level, BlockPos pos, ItemStack stack, Player player);
}
