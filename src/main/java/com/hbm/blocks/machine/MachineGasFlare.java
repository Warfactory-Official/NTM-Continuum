package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Flare Stack block — 1:1 port of the 1.12.2 {@code MachineGasFlare}: a 12-tall 3×3 the folded core
 * (dims {@code {11,0,1,1,1,1}}, offset 1) with the 4 base side cells marked as extras (the fluid/power
 * ports on {@code getConPos}).
 */
public class MachineGasFlare extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachineGasFlare(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineGasFlare(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{11, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    /** Four inlets around the base of the stack. */
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
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(core.offset(1, 0, 0), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-1, 0, 0), MASK_ALL, domains);
        visitor.passiveCell(core.offset(0, 0, 1), MASK_ALL, domains);
        visitor.passiveCell(core.offset(0, 0, -1), MASK_ALL, domains);
    }
}
