package com.hbm.blocks.network;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.network.RadioTargets;
import com.hbm.tileentity.network.TileEntityRadioTorchCounter;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Three-filter redstone-over-radio counter torch. */
public final class RadioTorchCounter extends RadioTorchBase {
    public RadioTorchCounter(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityRadioTorchCounter(pos, state);
    }

    @Override
    protected boolean canAttachTo(LevelReader level, BlockPos supportPos, BlockState support, Direction facing) {
        return support.isFaceSturdy(level, supportPos, facing)
                || (Compat.isSolidRender(support, level, supportPos) && !support.isAir())
                || RadioTargets.container(level, supportPos) != null;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityRadioTorchCounter counter)) return null;
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < TileEntityRadioTorchCounter.MAPPING_SIZE; i++) {
            if (counter.channel[i] == null || counter.channel[i].isEmpty()) continue;
            lines.add("&[5636095&]Freq " + (i + 1) + ": " + counter.channel[i]);
            lines.add("&[16733525&]Signal " + (i + 1) + ": " + counter.lastCount[i]);
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, lines);
    }
}
