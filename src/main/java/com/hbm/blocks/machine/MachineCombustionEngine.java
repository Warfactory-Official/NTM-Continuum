package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineCombustionEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Combustion Engine block — 1:1 port of the 1.12.2 {@code MachineCombustionEngine}: a low 4×3
 * the folded core (dims {@code {1,0,1,0,3,2}}, offset 0; 4 makeExtra power/fluid ports). */
public class MachineCombustionEngine extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachineCombustionEngine(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineCombustionEngine(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{1, 0, 1, 0, 3, 2}; }
    @Override public int getOffset() { return 0; }

    /** Four ports at the engine's shoulders, two forward and two back. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction back = facing.getOpposite();
        Direction rot = facing.getClockWise();
        visitor.cell(core.relative(rot), MASK_SOUTH);
        visitor.cell(core.relative(rot.getOpposite()), MASK_SOUTH);
        visitor.cell(core.relative(back).relative(rot), MASK_NORTH);
        visitor.cell(core.relative(back).relative(rot.getOpposite()), MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction back = facing.getOpposite();
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(core.relative(rot), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot.getOpposite()), MASK_ALL, domains);
        visitor.passiveCell(core.relative(back).relative(rot), MASK_ALL, domains);
        visitor.passiveCell(core.relative(back).relative(rot.getOpposite()), MASK_ALL, domains);
    }
}
