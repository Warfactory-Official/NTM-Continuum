package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockFootprint;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityReactorZirnox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The five-block-tall ZIRNOX reactor multiblock and its original five access cells. */
public class MachineZirnox extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, WorldlyContainerHolder {
    public MachineZirnox(Properties properties) { super(properties); }
    @Override protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) { return new TileEntityReactorZirnox(pos, state); }
    @Override public int[] getDimensions() { return new int[]{1, 0, 2, 2, 2, 2}; }
    @Override public int getOffset() { return 2; }
    @Override public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        if (state.getValue(CELL) && !MultiblockSurface.carriesPassive(level, pos, PASSIVE_ITEMS)) return null;
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityReactorZirnox reactor ? reactor : null;
    }
    @Override protected void onNeighborChanged(BlockState state, Level level, BlockPos pos,
                                                net.minecraft.world.level.block.Block block, boolean movedByPiston) {
        if (level.isClientSide()) return;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityReactorZirnox reactor)) return;
        boolean powered = false;
        for (int dx = -2; dx <= 2 && !powered; dx++) {
            for (int dy = 0; dy <= 4 && !powered; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if ((dx == -2 || dx == 2 || dy == 0 || dy == 4 || dz == -2 || dz == 2)
                            && level.hasNeighborSignal(core.offset(dx, dy, dz))) {
                        powered = true;
                        break;
                    }
                }
            }
        }
        reactor.setRedstonePowered(powered);
    }
    

    /** The three passes that carry the boiler's drum above the base box. */
    private static final int[][] DRUM = {
            {4, -2, 1, 1, 1, 1},
            {4, -2, 0, 0, 2, -2},
            {4, -2, 0, 0, -2, 2}};

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        if (!super.checkRequirement(level, core, facing, clicked)) return false;
        for (int[] box : DRUM) {
            if (!MultiblockFootprint.checkSpace(level, core, box, clicked, facing)) return false;
        }
        return true;
    }

    /** Water in and steam out either flank, at the two drum heights; the crown takes items only. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int[] box : DRUM) MultiblockFootprint.visitBox(core, box, facing, visitor);

        Direction rot = facing.getClockWise();
        int rx = rot.getStepX(), rz = rot.getStepZ();
        visitor.cell(core.offset(rx * 2, 1, rz * 2), MASK_WEST);
        visitor.cell(core.offset(rx * 2, 3, rz * 2), MASK_WEST);
        visitor.cell(core.offset(-rx * 2, 1, -rz * 2), MASK_EAST);
        visitor.cell(core.offset(-rx * 2, 3, -rz * 2), MASK_EAST);
    }

    /**
     * 1.7 stamps {@code TileEntityProxyCombo(true, true, true)} on all five promoted cells. The power arm
     * of that stamp is inert here: the proxy gates it on the core being a receiver, and the ZIRNOX runs
     * on nothing but water.
     */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int rx = rot.getStepX(), rz = rot.getStepZ();
        int domains = PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.offset(rx * 2, 1, rz * 2), MASK_ALL, domains);
        visitor.passiveCell(core.offset(rx * 2, 3, rz * 2), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-rx * 2, 1, -rz * 2), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-rx * 2, 3, -rz * 2), MASK_ALL, domains);
        visitor.passiveCell(core.offset(0, 4, 0), MASK_ALL, domains);
    }
}
