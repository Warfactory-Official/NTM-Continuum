package com.hbm.tileentity.network;

import com.hbm.blocks.network.RadioTorchBase;
import dev.golem.core.world.Redstone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.ModBlockEntities;

public class TileEntityRadioTorchSender extends TileEntityRadioTorchBase {
    public TileEntityRadioTorchSender(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIO_TORCH_SENDER.get(), pos, state);
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        Direction facing = getTorchFacing();
        int input = Redstone.read(level, worldPosition.relative(facing.getOpposite()), facing);
        boolean shouldSend = polling;
        if (input != lastState) {
            lastState = input;
            setLit(input > 0);
            setChanged();
            shouldSend = true;
        }
        if (shouldSend && channel != null && !channel.isEmpty()) {
            String signal = customMap ? mapping[input] : Integer.toString(input);
            if (signal != null && !signal.isEmpty()) RTTYSystem.broadcast(level, channel, signal);
        }
        syncRadio();
    }

    @Override
    protected String titleKey() {
        return "container.rttySender";
    }
}
