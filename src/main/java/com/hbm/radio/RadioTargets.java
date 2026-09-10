package com.hbm.radio;

import com.hbm.api.redstoneoverradio.IRORInfo;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.multiblock.MultiblockSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Resolves an ROR attachment the same way old Compat.getTileStandard did for multiblock dummies.
 *
 * <p>A folded multiblock is the other case: its cells carry no block entity, so a torch stuck to one is
 * walked to the core that does. That walk is gated on the cell state test rather than attempted for
 * every block, because the client half of the lookup is a 33-cubed scan and this runs from
 * {@code canAttachTo} on every neighbour update.
 */
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

        if (block instanceof BlockDummyable dummyable) {
            BlockEntity core = dummyable.findCoreTE(level, pos);
            if (core != null) return core;
        }

        if (level instanceof Level world && MultiblockSurface.isFoldedCell(state)) {
            BlockPos core = MultiblockSurface.coreOfCell(world, pos);
            if (core != null) {
                BlockEntity coreEntity = world.getBlockEntity(core);
                if (coreEntity != null) return coreEntity;
            }
        }
        // Containers are also direct targets, even though vanilla inventory block entities are not ROR info.
        return blockEntity;
    }
}
