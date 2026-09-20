package com.hbm.blocks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.network.TileEntityCraneGrabber;

/** 1.7 {@code blocks/network/CraneGrabber}: the block half is just the tile factory. */
public class CraneGrabber extends BlockCraneBase {

    public CraneGrabber(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCraneGrabber(pos, state);
    }
}
