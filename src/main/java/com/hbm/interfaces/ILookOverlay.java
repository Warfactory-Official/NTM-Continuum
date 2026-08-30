package com.hbm.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Supplies the data shown beside the crosshair when a player looks at a block.
 *
 * <p>This contract is deliberately independent of GUI classes so registered blocks remain safe to
 * load on a dedicated server. Legacy formatting markers in {@link Data#lines()} are interpreted by
 * the client renderer, not by the block implementation.
 */
public interface ILookOverlay {

    /** Returns this block's current readout, or {@code null} when this position has no readout. */
    Data getLookOverlay(Level level, BlockPos pos, Player player);

    /** Plain overlay data consumed by the client renderer. Colors are RGB values. */
    record Data(String title, int titleColor, int backgroundColor, List<String> lines) {
        public Data {
            lines = List.copyOf(lines);
        }
    }
}
