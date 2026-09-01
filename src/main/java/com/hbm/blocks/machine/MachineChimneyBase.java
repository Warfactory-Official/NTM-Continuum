package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityChimneyBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Shared multiblock and port behavior for the two smoke stacks. */
public abstract class MachineChimneyBase extends BlockDummyable implements IFluidConnectorBlockMK2 {

    protected MachineChimneyBase(Properties properties) {
        super(properties);
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);

        int x = core.getX(), y = core.getY(), z = core.getZ();
        makeExtra(level, x + 1, y, z);
        makeExtra(level, x - 1, y, z);
        makeExtra(level, x, y, z + 1);
        makeExtra(level, x, y, z - 1);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        if (!TileEntityChimneyBase.isSmoke(type)
                || dir == ForgeDirection.UNKNOWN || dir == ForgeDirection.UP || dir == ForgeDirection.DOWN) {
            return false;
        }

        BlockPos cell = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(cell);
        if (state.getBlock() != this || state.getValue(META) < extra || state.getValue(META) >= offset + 2) {
            return false;
        }

        BlockPos core = findCore(world, cell);
        if (core == null || !(world.getBlockEntity(core) instanceof TileEntityChimneyBase)) return false;
        return cell.getY() == core.getY()
                && cell.getX() - core.getX() == dir.offsetX
                && cell.getZ() - core.getZ() == dir.offsetZ;
    }

    @Override
    protected abstract BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state);
}
