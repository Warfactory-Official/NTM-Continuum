package com.hbm.blocks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoStorageClutter;

/** 1.7 {@code PneumoStorageClutter}: a 54-slot bank; its contents ride the dropped item. */
public class PneumoStorageClutter extends PneumaticStorageBlockBase {

    public PneumoStorageClutter(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPneumoStorageClutter(pos, state);
    }
}
