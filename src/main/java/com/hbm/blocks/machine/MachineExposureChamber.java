package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineExposureChamber;

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

/** The large Exposure Chamber multiblock and its five external power ports. */
public class MachineExposureChamber extends BlockMultiblockMachine implements IEnergyConnectorBlock, WorldlyContainerHolder {

    public MachineExposureChamber(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineExposureChamber(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{4, 0, 2, 2, 2, 2};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineExposureChamber chamber
                ? chamber : null;
    }

    private static final int[] SPINE = {3, 0, 0, 0, -3, 8};
    private static final int[] SHOULDER_LEFT = {0, 0, 1, -1, -3, 6};
    private static final int[] SHOULDER_RIGHT = {0, 0, -1, 1, -3, 6};
    private static final int[] HEAD_LEFT = {3, 0, 1, -1, 0, 1};
    private static final int[] HEAD_RIGHT = {3, 0, -1, 1, 0, 1};

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        if (!super.checkRequirement(level, core, facing, clicked)) return false;
        BlockPos head = head(core, facing);
        return MultiblockHandlerXR.checkSpace(level, core, SPINE, clicked, facing)
                && MultiblockHandlerXR.checkSpace(level, core.above(2), SHOULDER_LEFT, clicked, facing)
                && MultiblockHandlerXR.checkSpace(level, core.above(2), SHOULDER_RIGHT, clicked, facing)
                && MultiblockHandlerXR.checkSpace(level, head, HEAD_LEFT, clicked, facing)
                && MultiblockHandlerXR.checkSpace(level, head, HEAD_RIGHT, clicked, facing);
    }

    /** Four cable ports either side of the emitter head, and one straight off its nose. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        BlockPos head = head(core, facing);
        MultiblockHandlerXR.visitBox(core, SPINE, facing, visitor);
        MultiblockHandlerXR.visitBox(core.above(2), SHOULDER_LEFT, facing, visitor);
        MultiblockHandlerXR.visitBox(core.above(2), SHOULDER_RIGHT, facing, visitor);
        MultiblockHandlerXR.visitBox(head, HEAD_LEFT, facing, visitor);
        MultiblockHandlerXR.visitBox(head, HEAD_RIGHT, facing, visitor);

        Direction rot = facing.getCounterClockWise();
        visitor.cell(head.relative(facing), MASK_SOUTH);
        visitor.cell(head.relative(facing, -1), MASK_NORTH);
        visitor.cell(head.relative(rot).relative(facing), MASK_SOUTH);
        visitor.cell(head.relative(rot).relative(facing, -1), MASK_NORTH);
        visitor.cell(head.relative(rot), MASK_EAST);
    }

    /** 1.7 stamps {@code inventory().power()} on the five. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        BlockPos head = head(core, facing);
        Direction rot = facing.getCounterClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_ITEMS;
        visitor.passiveCell(head.relative(facing), MASK_ALL, domains);
        visitor.passiveCell(head.relative(facing, -1), MASK_ALL, domains);
        visitor.passiveCell(head.relative(rot).relative(facing), MASK_ALL, domains);
        visitor.passiveCell(head.relative(rot).relative(facing, -1), MASK_ALL, domains);
        visitor.passiveCell(head.relative(rot), MASK_ALL, domains);
    }

    private static BlockPos head(BlockPos core, Direction facing) {
        return core.relative(facing.getCounterClockWise(), 7);
    }

    /** 1.7 {@code MachineExposureChamber.getAllDimensions}, read only by the placement highlight. */
    @Override
    public int[][] getAllDimensions() {
        return new int[][] {
            getDimensions(),
            new int[] {3, 0, 0, 0, -3, 8},
            new int[] {0, 0, 1, -1, -3, 6, 0, 2, 0},
            new int[] {0, 0, -1, 1, -3, 6, 0, 2, 0},
            new int[] {3, 0, 1, -1, 0, 1, 0, 0, -7},
            new int[] {3, 0, -1, 1, 0, 1, 0, 0, -7},
        };
    }
}
