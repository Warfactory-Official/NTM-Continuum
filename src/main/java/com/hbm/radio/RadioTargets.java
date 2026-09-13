package com.hbm.radio;

import com.hbm.api.redstoneoverradio.IRORInfo;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.multiblock.MultiblockSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Resolves an ROR attachment the same way old Compat.getTileStandard did for multiblock cells. */
public final class RadioTargets {
    private RadioTargets() {}

    public static @Nullable IRORValueProvider valueProvider(BlockGetter level, BlockPos pos) {
        Object target = resolve(level, pos);
        return target instanceof IRORValueProvider provider ? provider : null;
    }

    public static @Nullable IRORInteractive interactive(BlockGetter level, BlockPos pos) {
        Object target = resolve(level, pos);
        return target instanceof IRORInteractive interactive ? interactive : null;
    }

    public static @Nullable IRORInfo info(BlockGetter level, BlockPos pos) {
        Object target = resolve(level, pos);
        return target instanceof IRORInfo info ? info : null;
    }

    public static @Nullable Container container(BlockGetter level, BlockPos pos) {
        Object target = resolve(level, pos);
        return target instanceof Container container ? container : null;
    }

    private static @Nullable Object resolve(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof IRORInfo) return block;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof IRORInfo) return blockEntity;

        // The state test comes first for the same reason the crumbling hook makes it: a folded cell's
        // owner is only known to the index, and asking for a plain block's owner costs a client-side scan.
        if (MultiblockSurface.foldedCore(state) != null || MultiblockSurface.isFoldedCell(state)) {
            BlockEntity core = MultiblockSurface.coreEntity(level, pos);
            if (core != null) return core;
        }
        // Containers are also direct targets, even though vanilla inventory block entities are not ROR info.
        return blockEntity;
    }
}
