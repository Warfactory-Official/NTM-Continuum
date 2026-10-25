package com.hbm.capability;

import com.hbm.Constants;
import com.hbm.blocks.multiblock.MultiblockSurface;
import dev.golem.core.world.ItemExposure;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Every HBM block's items for other mods. Side-less, a read-only view for viewers (Holo Inventory Modern): its own
 * container, or its multiblock core's from any part. On a face, pipes and storage networks get what a hopper gets.
 */
public final class ForeignItems {

    private ForeignItems() {}

    /** Common init, once, before the loaders' capability registration. */
    public static void register() {
        Predicate<Block> hbm = block -> Constants.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
        ItemExposure.exposeContents(hbm, ForeignItems::contentsAt);
        ItemExposure.exposeAutomation(hbm, ForeignItems::hopperContainerAt);
    }

    /** Vanilla's hopper lookup: the block's {@code WorldlyContainerHolder} container, else its block entity. */
    public static @Nullable Container hopperContainerAt(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
        if (state.getBlock() instanceof WorldlyContainerHolder holder) return holder.getContainer(state, level, pos);
        return be instanceof Container container ? container : null;
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
