package com.hbm.blocks.machine.albion;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.api.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.albion.TileEntityPADipole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The bending magnet: a 3x3x3 cube that takes the beam from any horizontal side. */
public class BlockPADipole extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public BlockPADipole(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPADipole(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 1, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public int getHeightOffset() {
        return 1;
    }

    /**
     * The dipole's eight ports sit on world axes rather than the placement's, exactly as 1.7 writes them:
     * the four edge cells of the upper course and of the lower one.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int dy = -1; dy <= 1; dy += 2) {
            int mask = dy > 0 ? MASK_UP : MASK_DOWN;
            visitor.cell(core.offset(1, dy, 0), mask, ROLE_ALL);
            visitor.cell(core.offset(-1, dy, 0), mask, ROLE_ALL);
            visitor.cell(core.offset(0, dy, 1), mask, ROLE_ALL);
            visitor.cell(core.offset(0, dy, -1), mask, ROLE_ALL);
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        for (int dy = -1; dy <= 1; dy += 2) {
            visitor.passiveCell(core.offset(1, dy, 0), MASK_ALL, domains);
            visitor.passiveCell(core.offset(-1, dy, 0), MASK_ALL, domains);
            visitor.passiveCell(core.offset(0, dy, 1), MASK_ALL, domains);
            visitor.passiveCell(core.offset(0, dy, -1), MASK_ALL, domains);
        }
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, Direction side) {
        ForgeDirection dir = ForgeDirection.of(side);
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityPADipole te
                && com.hbm.lib.Ports.isPort(te.getConPos(), pos, dir.toDirection());
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, Direction side) {
        ForgeDirection dir = ForgeDirection.of(side);
        BlockPos pos = new BlockPos(x, y, z);
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityPADipole te
                && com.hbm.lib.Ports.isPort(te.getConPos(), pos, dir.toDirection());
    }

}
