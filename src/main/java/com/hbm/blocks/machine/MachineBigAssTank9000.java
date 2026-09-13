package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineBAT9000;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The legacy 5x5x5 BAT-9000 multiblock. */
public class MachineBigAssTank9000 extends MachineGiantTank {

    public MachineBigAssTank9000(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineBAT9000(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{4, 0, 2, 2, 1, 1};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    private static final int[][] ARMS = {{4, 0, 1, 1, 2, -2}, {4, 0, 1, 1, -2, 2}};

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        if (!super.checkRequirement(level, core, facing, clicked)) return false;
        for (int[] arm : ARMS) {
            if (!MultiblockHandlerXR.checkSpace(level, core, arm, clicked, facing)) return false;
        }
        return true;
    }

    /** Eight intake caps, two on each flank, stated in world axes as upstream states them. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int[] arm : ARMS) MultiblockHandlerXR.visitBox(core, arm, facing, visitor);
        for (int off = -1; off <= 1; off += 2) {
            visitor.cell(core.offset(off, 0, 2), MASK_SOUTH);
            visitor.cell(core.offset(off, 0, -2), MASK_NORTH);
            visitor.cell(core.offset(2, 0, off), MASK_EAST);
            visitor.cell(core.offset(-2, 0, off), MASK_WEST);
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (int off = -1; off <= 1; off += 2) {
            visitor.passiveCell(core.offset(off, 0, 2), MASK_ALL, PASSIVE_FLUID_IN);
            visitor.passiveCell(core.offset(off, 0, -2), MASK_ALL, PASSIVE_FLUID_IN);
            visitor.passiveCell(core.offset(2, 0, off), MASK_ALL, PASSIVE_FLUID_IN);
            visitor.passiveCell(core.offset(-2, 0, off), MASK_ALL, PASSIVE_FLUID_IN);
        }
    }
}
