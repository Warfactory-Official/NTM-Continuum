package com.hbm.handler.radiation;

import com.hbm.interfaces.IRadResistantBlock;
import com.hbm.lib.HbmBlockTags;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.world.BlockChanges;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The radiation engine's walls: {@link IRadResistantBlock}s that say so and the {@link HbmBlockTags#RADIATION_RESISTANT}
 * tag. The classes mark their own sections; this marks them for tag members, and all of them when the tag changes.
 */
public final class RadiationWalls {

    private static Set<Block> tagged = Set.of();

    private RadiationWalls() {}

    public static void init() {
        BlockChanges.onChange((level, pos, oldState, newState) -> {
            if (oldState.is(HbmBlockTags.RADIATION_RESISTANT) != newState.is(HbmBlockTags.RADIATION_RESISTANT)) {
                RadiationSystemNT.markSectionForRebuild(level, pos);
            }
        });
        Lifecycle.onDatapackReloaded(server -> {
            Set<Block> now = new HashSet<>();
            for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(HbmBlockTags.RADIATION_RESISTANT)) now.add(holder.value());
            if (now.equals(tagged)) return;
            tagged = now;
            RadiationSystemNT.markAllForRebuild();
        });
    }

    /** Whether the block at section-local {@code x, y, z} of section {@code cx, sy, cz} seals radiation. */
    static boolean isWall(ServerLevel level, BlockState state, int cx, int sy, int cz, int x, int y, int z) {
        if (state.is(HbmBlockTags.RADIATION_RESISTANT)) return true;
        return state.getBlock() instanceof IRadResistantBlock r
                && r.isRadResistant(level, new BlockPos((cx << 4) + x, (sy << 4) + y, (cz << 4) + z));
    }
}
