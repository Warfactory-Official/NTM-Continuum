package com.hbm.interfaces;

import dev.golem.core.hook.Hook;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Supplies the data shown beside the crosshair when a player looks at a block.
 *
 * <p>This contract is independent of GUI classes so registered blocks remain safe to
 * load on a dedicated server. Legacy formatting markers in {@link Data#lines()} are interpreted by
 * the client renderer, not by the block implementation.
 */
public interface ILookOverlay {

    /** Returns this block's current readout, or {@code null} when this position has no readout. */
    Data getLookOverlay(Level level, BlockPos pos, Player player);

    /** A readout for a block that does not supply one itself. */
    @FunctionalInterface
    interface Provider {
        @org.jspecify.annotations.Nullable Data getLookOverlay(Level level, BlockPos pos, Player player);
    }

    /**
     * Readouts for blocks that are no {@code ILookOverlay} (or answered {@code null}), asked in registration order; the
     * first non-null answer is shown (ADDON-API-INVENTORY C-26). HBM registers none. NTM: Space: hbm-space's
     * {@code BlockRefueler} tank line. Asked on the client only.
     */
    Hook<Provider> PROVIDERS = Hook.notifying();

    /** The first {@link #PROVIDERS} answer for {@code pos}, or null. */
    static @org.jspecify.annotations.Nullable Data provided(Level level, BlockPos pos, Player player) {
        for (Provider provider : PROVIDERS) {
            Data data = provider.getLookOverlay(level, pos, player);
            if (data != null) return data;
        }
        return null;
    }

    /** Plain overlay data consumed by the client renderer. Colors are RGB values. */
    record Data(String title, int titleColor, int backgroundColor, List<String> lines) {
        public Data {
            lines = List.copyOf(lines);
        }
    }
}
