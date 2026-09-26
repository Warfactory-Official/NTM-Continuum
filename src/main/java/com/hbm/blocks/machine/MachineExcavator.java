package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockFootprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.machine.TileEntityMachineExcavator;

/**
 * 1.7 {@code MachineExcavator} (the large mining drill): a 7x7x4 body whose core sits three blocks
 * above and three behind the clicked position, standing on four 2x2x3 legs. The old {@code makeExtra}
 * stamps {@code power().fluid()} proxies on two front cells and the two side cells one block up;
 * {@code getConPos} looks outward from those, which become the port faces here.
 */
public final class MachineExcavator extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    /**
     * {@code getAllDimensions()[1..3]} / {@code checkRequirement} / {@code fillSpace}: three boxes below
     * the body (y -3..-1) - two 2x2 front corner columns and one 7x2 slab across the whole back edge.
     * Odd-looking, but it is exactly what 1.7 checks and fills; ported verbatim.
     */
    private static final int[][] LEGS = {
            {-1, 3, 3, -2, 3, -2},
            {-1, 3, 3, -2, -2, 3},
            {-1, 3, -2, 3, 3, 3}
    };

    public MachineExcavator(Properties properties) { super(properties); }

    @Override protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineExcavator(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{3, 0, 3, 3, 3, 3}; }
    @Override public int getOffset() { return 3; }
    @Override public int getHeightOffset() { return 3; }

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        if (!super.checkRequirement(level, core, facing, clicked)) return false;
        for (int[] leg : LEGS) if (!MultiblockFootprint.checkSpace(level, core, leg, clicked, facing)) return false;
        return true;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int[] leg : LEGS) MultiblockFootprint.visitBox(core, leg, facing, visitor);

        Direction rot = facing.getClockWise(); // 1.7 dir.getRotation(UP)
        BlockPos front = core.relative(facing, 3).above();
        visitor.cell(front.relative(rot), MASK_SOUTH);
        visitor.cell(front.relative(rot, -1), MASK_SOUTH);
        // +rot is the clockwise flank, which is local WEST in the mask frame.
        visitor.cell(core.relative(rot, 3).above(), MASK_WEST);
        visitor.cell(core.relative(rot, -3).above(), MASK_EAST);
    }

    /** 1.7 stamps {@code power().fluid()} proxies on the four port cells. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        BlockPos front = core.relative(facing, 3).above();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(front.relative(rot), MASK_ALL, domains);
        visitor.passiveCell(front.relative(rot, -1), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot, 3).above(), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot, -3).above(), MASK_ALL, domains);
    }
}
