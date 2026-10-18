package com.hbm.blocks.machine;

import com.hbm.lib.ForgeDirection;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineCompressorCompact;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Compact pressure compressor. The dimensions, offset, six upper-ring ports and extra-cell
 * placement are copied from the original {@code MachineCompressorCompact}; the folded core
 * machinery delegates pressure processing to {@link com.hbm.tileentity.machine.TileEntityMachineCompressorBase}.
 */
public class MachineCompressorCompact extends BlockMultiblockMachine
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachineCompressorCompact(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineCompressorCompact(pos, state);
    }

    /** 1.12.2 {@code getDimensions()} and {@code getOffset()}. */
    @Override public int[] getDimensions() { return new int[]{2, 0, 1, 1, 3, 3}; }
    @Override public int getOffset() { return 1; }

    /**
     * {@code makeExtra} marked six cells on the y+1 ring. Their outward faces are open on every side:
     * the old proxy recovered the direction later through {@code getConPos()}.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();
        visitor.cell(core.above().relative(rot, 3), MASK_ALL);
        visitor.cell(core.above().relative(rot.getOpposite(), 3), MASK_ALL);
        visitor.cell(core.above().relative(facing).relative(rot), MASK_ALL);
        visitor.cell(core.above().relative(facing).relative(rot.getOpposite()), MASK_ALL);
        visitor.cell(core.above().relative(facing.getOpposite()).relative(rot), MASK_ALL);
        visitor.cell(core.above().relative(facing.getOpposite()).relative(rot.getOpposite()), MASK_ALL);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(core.above().relative(rot, 3), MASK_ALL, domains);
        visitor.passiveCell(core.above().relative(rot.getOpposite(), 3), MASK_ALL, domains);
        visitor.passiveCell(core.above().relative(facing).relative(rot), MASK_ALL, domains);
        visitor.passiveCell(core.above().relative(facing).relative(rot.getOpposite()), MASK_ALL, domains);
        visitor.passiveCell(core.above().relative(facing.getOpposite()).relative(rot), MASK_ALL, domains);
        visitor.passiveCell(core.above().relative(facing.getOpposite()).relative(rot.getOpposite()), MASK_ALL, domains);
    }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {3, 1, 0, ForgeDirection.EAST.ordinal()},
                {-3, 1, 0, ForgeDirection.WEST.ordinal()},
                {1, 1, 1, ForgeDirection.SOUTH.ordinal()},
                {-1, 1, 1, ForgeDirection.SOUTH.ordinal()},
                {1, 1, -1, ForgeDirection.NORTH.ordinal()},
                {-1, 1, -1, ForgeDirection.NORTH.ordinal()},
        }, dir);
    }
}
