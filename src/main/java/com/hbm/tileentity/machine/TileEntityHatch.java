package com.hbm.tileentity.machine;

import com.hbm.blocks.ModRadarUtilityBlocks;
import com.hbm.blocks.machine.BlockSeal;
import com.hbm.tileentity.ModRadarUtilityBlockEntities;
import dev.golem.core.data.CompatBlockEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntityHatch}: the controller position under the 1.7 keys {@code x1/y1/z1}. Every server tick the
 * hatch checks that a {@code seal_controller} still stands there with an intact frame, and turns to air if not.
 */
public class TileEntityHatch extends CompatBlockEntity {

    public int x;
    public int y;
    public int z;

    public TileEntityHatch(BlockPos pos, BlockState state) {
        super(ModRadarUtilityBlockEntities.HATCH.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityHatch hatch) {
        BlockPos controller = new BlockPos(hatch.x, hatch.y, hatch.z);

        if (level.getBlockState(controller).getBlock() != ModRadarUtilityBlocks.seal_controller.get()) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        } else if (BlockSeal.getFrameSize(level, controller) == 0) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    public void setControllerPos(BlockPos pos) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.setChanged();
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.x = input.getIntOr("x1", 0);
        this.y = input.getIntOr("y1", 0);
        this.z = input.getIntOr("z1", 0);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("x1", this.x);
        output.putInt("y1", this.y);
        output.putInt("z1", this.z);
    }
}
