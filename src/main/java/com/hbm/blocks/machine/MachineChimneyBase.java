package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import com.hbm.api.fluid.FluidType;
import com.hbm.tileentity.machine.TileEntityChimneyBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Shared multiblock and port behavior for the two smoke stacks. */
public abstract class MachineChimneyBase extends BlockMultiblockMachine implements IFluidConnectorBlockMK2 {

    protected MachineChimneyBase(Properties properties) {
        super(properties);
    }

    @Override
    protected abstract BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state);

    /** The four cells at the foot of the stack, each open on the side it faces away from the flue. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        visitor.cell(core.offset(1, 0, 0), MASK_EAST);
        visitor.cell(core.offset(-1, 0, 0), MASK_WEST);
        visitor.cell(core.offset(0, 0, 1), MASK_SOUTH);
        visitor.cell(core.offset(0, 0, -1), MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(core.offset(1, 0, 0), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(-1, 0, 0), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(0, 0, 1), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(0, 0, -1), MASK_ALL, PASSIVE_FLUID_IN);
    }

    /** Only smoke reaches a stack; the geometry alone would take anything a duct offered. */
    @Override
    public boolean acceptsFluid(FluidType type) {
        return TileEntityChimneyBase.isSmoke(type);
    }
}
