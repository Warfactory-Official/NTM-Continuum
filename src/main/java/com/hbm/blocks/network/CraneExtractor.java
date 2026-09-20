package com.hbm.blocks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.network.TileEntityCraneExtractor;

/** 1.7 {@code blocks/network/CraneExtractor} ("Conveyor Ejector"): the block half is just the tile factory. */
public class CraneExtractor extends BlockCraneBase {

    public CraneExtractor(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCraneExtractor(pos, state);
    }
}
