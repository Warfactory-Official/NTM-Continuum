package com.hbm.blocks.machine.fusion;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockFootprint;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge;
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

/** The irregular 1.12.2 plasma-forge shell and its ten external machine ports. */
public class MachineFusionPlasmaForge extends BlockMultiblockMachine
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachineFusionPlasmaForge(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFusionPlasmaForge(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 2, 2, 5, 5}; }
    @Override public int getOffset() { return 5; }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityFusionPlasmaForge forge ? forge : null;
    }

    /** 1.7: the shoulders and the two upper decks, beyond getDimensions. */
    private static final int[][] EXTRA_BOXES = {
            {2, 0, 3, -2, 4, 4},
            {2, 0, -2, 3, 4, 4},
            {2, 0, 4, -3, 3, 3},
            {2, 0, -3, 4, 3, 3},
            {2, 0, 5, -4, 2, 2},
            {2, 0, -4, 5, 2, 2},
            {3, -2, 1, 1, 5, 5},
            {4, -3, 0, 0, 4, 4}};

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        if (!super.checkRequirement(level, core, facing, clicked)) return false;
        for (int[] box : EXTRA_BOXES) {
            if (!MultiblockFootprint.checkSpace(level, core, box, clicked, facing)) return false;
        }
        return true;
    }

    /** A five-wide band of ports across each end. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int[] box : EXTRA_BOXES) MultiblockFootprint.visitBox(core, box, facing, visitor);

        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();
        for (int i = -2; i <= 2; i++) {
            visitor.cell(core.offset(dx * 5 + rx * i, 0, dz * 5 + rz * i), MASK_SOUTH);
            visitor.cell(core.offset(-dx * 5 + rx * i, 0, -dz * 5 + rz * i), MASK_NORTH);
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        for (int i = -2; i <= 2; i++) {
            visitor.passiveCell(core.offset(dx * 5 + rx * i, 0, dz * 5 + rz * i), MASK_ALL, domains);
            visitor.passiveCell(core.offset(-dx * 5 + rx * i, 0, -dz * 5 + rz * i), MASK_ALL, domains);
        }
    }
}
