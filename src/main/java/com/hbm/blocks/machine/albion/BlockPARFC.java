package com.hbm.blocks.machine.albion;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.api.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.albion.TileEntityPARFC;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The RF cavity: nine blocks of beamline that add momentum, with power and coolant ports above and below. */
public class BlockPARFC extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public BlockPARFC(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPARFC(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 1, 1, 1, 4, 4};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public int getHeightOffset() {
        return 1;
    }

    /** Six coolant/power ports: the column through the core and the two towers, top and bottom. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction beam = facing.getCounterClockWise();
        for (int along = -1; along <= 1; along++) {
            BlockPos at = core.relative(beam, along * 3);
            visitor.cell(at.above(), MASK_UP, ROLE_ALL);
            visitor.cell(at.below(), MASK_DOWN, ROLE_ALL);
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        Direction beam = facing.getCounterClockWise();
        for (int along = -1; along <= 1; along++) {
            BlockPos at = core.relative(beam, along * 3);
            visitor.passiveCell(at.above(), MASK_ALL, domains);
            visitor.passiveCell(at.below(), MASK_ALL, domains);
        }
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, Direction side) {
        ForgeDirection dir = ForgeDirection.of(side);
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityPARFC te
                && com.hbm.lib.Ports.isPort(te.getConPos(), pos, dir.toDirection());
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, Direction side) {
        ForgeDirection dir = ForgeDirection.of(side);
        BlockPos pos = new BlockPos(x, y, z);
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityPARFC te
                && com.hbm.lib.Ports.isPort(te.getConPos(), pos, dir.toDirection());
    }

}
