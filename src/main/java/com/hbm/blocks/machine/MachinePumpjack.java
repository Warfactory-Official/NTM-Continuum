package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockFootprint;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachinePumpjack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Pumpjack block — 1:1 port of the 1.12.2 {@code MachinePumpjack}: dims {@code {3,0,0,0,0,6}} (4 tall,
 * 7 long single-row footprint behind the core) plus the two 2-wide ground strips and the 4 "extra"
 * corner ports around the well head (old checkRequirement/fillSpace verbatim). The old meta&ge;6 proxy
 * cells map to the extras.
 */
public class MachinePumpjack extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachinePumpjack(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachinePumpjack(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 0, 0, 0, 6};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    private static final int[] RAIL_NEAR = {0, 0, -1, 1, 1, 1};
    private static final int[] RAIL_FAR = {0, 0, 1, -1, 2, 2};

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        return super.checkRequirement(level, core, facing, clicked)
                && MultiblockFootprint.checkSpace(level, core, new int[]{0, 0, -1, 1, -2, 4}, clicked, facing)
                && MultiblockFootprint.checkSpace(level, core, new int[]{0, 0, 1, -1, -1, 5}, clicked, facing);
    }

    /**
     * The two rails around the well head, and its four corners. 1.7 derives the ports with a formula that
     * only lands all four for a north/south pump; the corners are read off the cells instead, so every
     * facing has all four.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        BlockPos head = wellHead(core, facing);
        MultiblockFootprint.visitBox(head, RAIL_NEAR, facing, visitor);
        MultiblockFootprint.visitBox(head, RAIL_FAR, facing, visitor);

        visitor.cell(head.offset(1, 0, 1), MASK_SOUTH);
        visitor.cell(head.offset(1, 0, -1), MASK_NORTH);
        visitor.cell(head.offset(-1, 0, 1), MASK_SOUTH);
        visitor.cell(head.offset(-1, 0, -1), MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        BlockPos head = wellHead(core, facing);
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(head.offset(1, 0, 1), MASK_ALL, domains);
        visitor.passiveCell(head.offset(1, 0, -1), MASK_ALL, domains);
        visitor.passiveCell(head.offset(-1, 0, 1), MASK_ALL, domains);
        visitor.passiveCell(head.offset(-1, 0, -1), MASK_ALL, domains);
    }

    private static BlockPos wellHead(BlockPos core, Direction facing) {
        Direction rot = facing.getCounterClockWise();
        return core.offset(rot.getStepX() * 3, 0, rot.getStepZ() * 3);
    }
}
