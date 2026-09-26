package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockMaskTable;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineBlastFurnace;
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

/** The 3x3x8 blast furnace multiblock and its fluid-network port cells. */
public class MachineBlastFurnace extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachineBlastFurnace(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineBlastFurnace(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{6, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 1; }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineBlastFurnace furnace
                ? furnace : null;
    }

    /**
     * Three of the four ground neighbours are ports, and 1.7 names them in <b>world</b> axes -- POS_X,
     * NEG_X, POS_Z -- while the two stack ports rotate with the machine. The set is not symmetric (there
     * is no north port), so the ground course cannot be baked in the facing frame; {@link #maskAt} gives
     * it back the world directions.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        visitor.cell(core.offset(1, 0, 0), MASK_EAST);
        visitor.cell(core.offset(-1, 0, 0), MASK_WEST);
        visitor.cell(core.offset(0, 0, 1), MASK_SOUTH);
        visitor.cell(core.relative(facing), MASK_NONE);
        visitor.cell(core.relative(facing).above(3), MASK_SOUTH);
        visitor.cell(core.relative(facing).above(5), MASK_SOUTH);
        visitor.cell(core.above(6), MASK_UP);
    }

    @Override
    public int maskAt(int lx, int ly, int lz, Direction facing) {
        int baked = maskAt(lx, ly, lz);
        if (baked == MultiblockMaskTable.NOT_A_CELL || ly != 0) return baked;
        int dx = MultiblockMaskTable.worldX(lx, ly, lz, facing);
        int dz = MultiblockMaskTable.worldZ(lx, ly, lz, facing);
        if (dz == 0 && dx == 1) return worldFace(Direction.EAST, facing);
        if (dz == 0 && dx == -1) return worldFace(Direction.WEST, facing);
        if (dx == 0 && dz == 1) return worldFace(Direction.SOUTH, facing);
        return MASK_NONE;
    }

    private static int worldFace(Direction side, Direction facing) {
        return 1 << MultiblockMaskTable.toLocal(side, facing).ordinal();
    }

    /** 1.7 stamps {@code inventory().fluid()} on all seven promoted cells. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.offset(1, 0, 0), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-1, 0, 0), MASK_ALL, domains);
        visitor.passiveCell(core.offset(0, 0, 1), MASK_ALL, domains);
        visitor.passiveCell(core.offset(0, 0, -1), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing).above(3), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing).above(5), MASK_ALL, domains);
        visitor.passiveCell(core.above(6), MASK_ALL, domains);
    }
}
