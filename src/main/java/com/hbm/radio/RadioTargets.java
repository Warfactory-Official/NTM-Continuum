package com.hbm.radio;

import com.hbm.api.redstoneoverradio.IRORInfo;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/** Resolves an ROR attachment the same way old Compat.getTileStandard did for multiblock dummies. */
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
        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof IRORInfo) return block;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof IRORInfo) return blockEntity;

        if (block instanceof BlockDummyable dummyable) {
            BlockEntity core = dummyable.findCoreTE(level, pos);
            if (core != null) return core;
        }
        // Containers are also direct targets, even though vanilla inventory block entities are not ROR info.
        return blockEntity;
    }
}
