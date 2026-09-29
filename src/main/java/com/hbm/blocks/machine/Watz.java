package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModAutogenItems;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityWatz;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * One segment of the Watz power plant — a cross whose arms reach three blocks out on each horizontal
 * axis, three blocks tall. Segments stack every three blocks; the bottom one runs the simulation for
 * the whole column (see {@link TileEntityWatz}).
 *
 * <p>The footprint is not a box, so it is built from five overlapping {@link MultiblockHandlerXR}
 * passes — the base dimensions plus four with negative extents that carve the arms — exactly as
 * upstream. Nine cells are promoted to ports: the four arm tips at core level and at +2, plus the
 * centre-top cell.
 *
 * <p>Unlike most machines this block has no item: it only comes into being by building the shape
 * around a {@code struct_watz_core} and letting it assemble.
 */
public class Watz extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, WorldlyContainerHolder {

    /** Suppresses the component drop while the reactor is tearing itself apart in a meltdown. */
    public static boolean drop = true;

    public Watz(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityWatz(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 3, 3, 1, 1}; }

    @Override public int getOffset() { return 3; }

    /** The four extra passes, with the negative extents that turn the box into a cross. */
    private static final int[][] ARMS = {
            {2, 0, 2, 2, 2, -2},
            {2, 0, 2, 2, -2, 2},
            {2, 0, 1, 1, 3, -3},
            {2, 0, 1, 1, -3, 3},
    };

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        if (state.getValue(CELL) && !MultiblockSurface.carriesPassive(level, pos, PASSIVE_ITEMS)) return null;
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityWatz watz ? watz : null;
    }

    /**
     * Upstream's hand-rolled drop table: breaking the core returns the whole build. It is code rather
     * than a loot table because the reactor is assembled in world and has no block item of its own.
     */
    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (!level.isClientSide() && !state.getValue(CELL) && drop) {
            drop(level, pos, new ItemStack(ModMachineBlocks.watz_casing_tooled.get(), 48));
            for (int i = 0; i < 3; i++) {
                drop(level, pos, new ItemStack(ModAutogenItems.get(MaterialShapes.BOLT, Mats.MAT_DURA), 64));
            }
            drop(level, pos, new ItemStack(com.hbm.blocks.ModBlocks.watz_element.get(), 36));
            drop(level, pos, new ItemStack(com.hbm.blocks.ModBlocks.watz_cooler.get(), 26));
            drop(level, pos, new ItemStack(ModMachineBlocks.struct_watz_core.get(), 1));
        }
        super.onBlockRemoved(state, level, pos, movedByPiston);
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return;
        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
    }

    /**
     * The nine promoted cells: the four arm tips at core level take their feed from below, and the four
     * at +2 plus the crown vent upward. Upstream states them in world axes; the cross is symmetric about
     * both, so the facing makes no difference to where they land.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int[] arm : ARMS) MultiblockHandlerXR.visitBox(core, arm, facing, visitor);

        for (int[] arm : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) {
            visitor.cell(core.offset(arm[0], 0, arm[1]), MASK_DOWN);
            visitor.cell(core.offset(arm[0], 2, arm[1]), MASK_UP);
        }
        visitor.cell(core.offset(0, 2, 0), MASK_UP);
    }

    /** The column's own inlet hangs beneath the core itself. */
    @Override public int coreMask() { return MASK_DOWN; }

    /** 1.7 stamps {@code inventory().fluid()} on every one of the nine. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        for (int[] arm : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) {
            visitor.passiveCell(core.offset(arm[0], 0, arm[1]), MASK_ALL, domains);
            visitor.passiveCell(core.offset(arm[0], 2, arm[1]), MASK_ALL, domains);
        }
        visitor.passiveCell(core.offset(0, 2, 0), MASK_ALL, domains);
    }

    /** 1.7 {@code Watz.getAllDimensions}, read only by the placement highlight. */
    @Override
    public int[][] getAllDimensions() {
        return new int[][] {
            new int[] {2, 0, 3, 3, 1, 1},
            new int[] {2, 0, 2, 2, 2, -2},
            new int[] {2, 0, 2, 2, -2, 2},
            new int[] {2, 0, 1, 1, 3, -3},
            new int[] {2, 0, 1, 1, -3, 3},
        };
    }
}
