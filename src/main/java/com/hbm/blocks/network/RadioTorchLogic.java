package com.hbm.blocks.network;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.network.TileEntityRadioTorchLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class RadioTorchLogic extends RadioTorchBase {
    public RadioTorchLogic(Properties properties) { super(properties); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityRadioTorchLogic(pos, state);
    }

    @Override public boolean isSignalSource(BlockState state) { return true; }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof TileEntityRadioTorchLogic logic ? logic.lastState : 0;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityRadioTorchLogic logic)) return null;

        List<String> lines = new ArrayList<>();
        if (logic.channel != null && !logic.channel.isEmpty()) {
            lines.add("&[5636095&]Freq: " + logic.channel);
        }
        lines.add("&[16733525&]Signal: " + logic.lastState);
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, lines);
    }
}
