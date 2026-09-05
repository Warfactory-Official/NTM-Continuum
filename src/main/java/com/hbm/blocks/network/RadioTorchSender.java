package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityRadioTorchSender;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class RadioTorchSender extends RadioTorchBase {
    public RadioTorchSender(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityRadioTorchSender(pos, state);
    }
}
