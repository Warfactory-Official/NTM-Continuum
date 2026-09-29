package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.machine.TileEntityICF;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The inertial confinement fusion reactor (1.7 {@code MachineICF}): a 17 x 6 x 3 vessel (dims
 * {@code {5, 0, 1, 1, 8, 8}}, offset 1) with a three-high bulge on both long sides at y+2..y+4
 * ({@code {1, 1, -1, 2, 8, 8}} and {@code {1, 1, 2, -1, 8, 8}} around y+3). It is normally made by a
 * {@code struct_icf_core}, which places it facing its own direction with the core on the seed.
 *
 * <p>Five 1.7 extras ({@code TileEntityProxyCombo().inventory().fluid()}): the top centre at y+5, and one on each
 * bulge six blocks out either way at y+3. {@code getConPos} reaches them from above, and from outside each bulge
 * along the facing axis; it also reaches the core itself from below.
 */
public class MachineICF extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, WorldlyContainerHolder {

    private static final int[] BULGE_FRONT = {1, 1, -1, 2, 8, 8};
    private static final int[] BULGE_BACK = {1, 1, 2, -1, 8, 8};

    public MachineICF(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityICF(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{5, 0, 1, 1, 8, 8};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        if (!super.checkRequirement(level, core, facing, clicked)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core.above(3), BULGE_FRONT, clicked, facing)) return false;
        return MultiblockHandlerXR.checkSpace(level, core.above(3), BULGE_BACK, clicked, facing);
    }

    /**
     * Masks are stated for the south-facing bake: the bulge extras two blocks along the facing open south, the
     * two behind open north, and the top centre opens up. The core's own bottom face is {@link #coreMask}.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MultiblockHandlerXR.visitBox(core.above(3), BULGE_FRONT, facing, visitor);
        MultiblockHandlerXR.visitBox(core.above(3), BULGE_BACK, facing, visitor);

        Direction rot = facing.getClockWise();
        visitor.cell(core.above(5), MASK_UP);
        visitor.cell(core.relative(facing, 2).relative(rot, 6).above(3), MASK_SOUTH);
        visitor.cell(core.relative(facing, 2).relative(rot, -6).above(3), MASK_SOUTH);
        visitor.cell(core.relative(facing, -2).relative(rot, 6).above(3), MASK_NORTH);
        visitor.cell(core.relative(facing, -2).relative(rot, -6).above(3), MASK_NORTH);
    }

    @Override
    public int coreMask() {
        return MASK_DOWN;
    }

    /** 1.7 stamps an item-and-fluid proxy on all five extras. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.above(5), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, 2).relative(rot, 6).above(3), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, 2).relative(rot, -6).above(3), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -2).relative(rot, 6).above(3), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -2).relative(rot, -6).above(3), MASK_ALL, domains);
    }

    /** Hoppers reach the reactor's slots through the core and the five proxy extras. */
    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        if (state.getValue(CELL) && !MultiblockSurface.carriesPassive(level, pos, PASSIVE_ITEMS)) return null;
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityICF icf ? icf : null;
    }

    /** 1.7 {@code MachineICF.getAllDimensions}, read only by the placement highlight. */
    @Override
    public int[][] getAllDimensions() {
        return new int[][] {
            new int[] {5, 0, 1, 1, 8, 8},
            new int[] {1, 1, -1, 2, 8, 8, 0, 3, 0},
            new int[] {1, 1, 2, -1, 8, 8, 0, 3, 0},
        };
    }
}
