package com.hbm.capability;

import com.hbm.Constants;
import com.hbm.blocks.multiblock.MultiblockSurface;
import dev.golem.core.world.ItemExposure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Every HBM block's items as a read-only view for other mods' viewers (Holo Inventory Modern): its own container,
 * or its multiblock core's from any part. Items still move only through the sided vanilla rules hoppers use.
 */
public final class ForeignItems {

    private ForeignItems() {}

    /** Common init, once, before the loaders' capability registration. */
    public static void register() {
        ItemExposure.exposeContents(block -> Constants.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace()),
                ForeignItems::contentsAt);
    }

    public static @Nullable Container contentsAt(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
        if (state.getBlock() instanceof WorldlyContainerHolder holder) {
            Container container = holder.getContainer(state, level, pos);
            if (container != null) return container;
        }
        if (be instanceof Container container) return container;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof Container container ? container : null;
    }
}
