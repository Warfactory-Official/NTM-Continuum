package com.hbm.blocks.network;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.network.RadioTargets;
import com.hbm.tileentity.network.TileEntityRadioTorchReader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class RadioTorchReader extends RadioTorchBase {
    public RadioTorchReader(Properties properties) { super(properties); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityRadioTorchReader(pos, state);
    }

    @Override
    protected boolean canAttachTo(LevelReader level, BlockPos supportPos, BlockState support, Direction facing) {
        return RadioTargets.valueProvider(level, supportPos) != null;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityRadioTorchReader reader)) return null;

        List<String> lines = new ArrayList<>();
        for (int i = 0; i < TileEntityRadioTorchReader.MAPPING_SIZE; i++) {
            if (reader.channels[i] == null || reader.channels[i].isEmpty()) continue;
            if (reader.names[i] == null || reader.names[i].isEmpty()) continue;
            lines.add("&[5636095&]" + reader.channels[i] + ": " + reader.names[i]);
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, lines);
    }
}
