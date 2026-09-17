package com.hbm.items.special;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.Nullable;

public final class CarriedBlockItems {
    private static final Logger LOGGER = LoggerFactory.getLogger("NTM");

    private CarriedBlockItems() {
    }

    public static @Nullable BlockEntity load(BlockState state, BlockPos pos, CompoundTag data, HolderLookup.Provider registries) {
        String owner = state.getBlock().getClass().getName();
        // Foreign deserializers may need a resident level, before inventory callbacks can even be refused.
        if (!owner.startsWith("net.minecraft.") && !owner.startsWith("com.hbm.")) return null;
        BlockEntity blockEntity = ((EntityBlock) state.getBlock()).newBlockEntity(pos, state);
        // Like BlockEntity.loadStatic, this copy has no level: inventory edits cannot notify its old world position.
        dev.golem.core.registry.BlockEntities.loadWithComponents(blockEntity, data, registries);
        return blockEntity;
    }
}
