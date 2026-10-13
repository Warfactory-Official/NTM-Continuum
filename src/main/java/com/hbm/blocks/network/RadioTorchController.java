package com.hbm.blocks.network;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.network.RadioTargets;
import com.hbm.tileentity.network.TileEntityRadioTorchController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Command receiver redstone-over-radio torch. */
public final class RadioTorchController extends RadioTorchBase {
    public RadioTorchController(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityRadioTorchController(pos, state);
    }

    @Override
    protected boolean canAttachTo(LevelReader level, BlockPos supportPos, BlockState support, Direction facing) {
        return RadioTargets.valueProvider(level, supportPos) != null;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityRadioTorchController controller)) return null;
        List<String> lines = new ArrayList<>();
        if (controller.channel != null && !controller.channel.isEmpty()) {
            lines.add("&[5636095&]Freq: " + controller.channel);
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, lines);
    }
}
