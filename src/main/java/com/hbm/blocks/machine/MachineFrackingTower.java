package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachineFrackingTower;
import com.hbm.tileentity.machine.oil.TileEntityOilDrillBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Fracking Tower block — 1:1 port of the 1.12.2 {@code MachineFrackingTower}: a tall derrick built on a
 * 1×1 core column (dims {@code {3,0,0,0,0,0}}, offset 0), plus a y+2 platform, four corner legs, two
 * downward drill shafts and a y+15 crown (the old custom checkRequirement/fillSpace, copied verbatim).
 */
public class MachineFrackingTower extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachineFrackingTower(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineFrackingTower(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    /** 1.7: the platform, the four legs, the two drill shafts and the crown, all beyond getDimensions. */
    private static final int[] PLATFORM = {1, 0, 3, 3, 3, 3};
    private static final int[] LEG = {-1, 2, 0, 1, 0, 1};
    private static final int[] SHAFT_UPPER = {10, -4, 2, 2, 2, 2};
    private static final int[] SHAFT_LOWER = {24, -9, 1, 1, 1, 1};
    private static final int[] CROWN = {1, 0, 1, 1, -2, 3};

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        return MultiblockHandlerXR.checkSpace(level, core.above(2), PLATFORM, clicked, facing)
                && legsFree(level, core, clicked, facing)
                && MultiblockHandlerXR.checkSpace(level, core, SHAFT_UPPER, clicked, facing)
                && MultiblockHandlerXR.checkSpace(level, core, SHAFT_LOWER, clicked, facing)
                && MultiblockHandlerXR.checkSpace(level, core.above(15), CROWN, clicked, facing)
                && super.checkRequirement(level, core, facing, clicked);
    }

    private static boolean legsFree(Level level, BlockPos core, BlockPos clicked, Direction facing) {
        for (BlockPos leg : legs(core)) {
            if (!MultiblockHandlerXR.checkSpace(level, leg, LEG, clicked, Direction.NORTH)) return false;
        }
        return true;
    }

    /**
     * No super: the derrick's own boxes are the footprint, and the legs and the crown are stated in
     * world axes rather than rotated, as upstream states them.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        MultiblockHandlerXR.visitBox(core, getDimensions(), facing, visitor);
        MultiblockHandlerXR.visitBox(core.above(2), PLATFORM, facing, visitor);
        for (BlockPos leg : legs(core)) MultiblockHandlerXR.visitBox(leg, LEG, Direction.NORTH, visitor);
        MultiblockHandlerXR.visitBox(core, SHAFT_UPPER, facing, visitor);
        MultiblockHandlerXR.visitBox(core, SHAFT_LOWER, facing, visitor);
        MultiblockHandlerXR.visitBox(core.above(15), CROWN, Direction.WEST, visitor);
    }

    private static BlockPos[] legs(BlockPos core) {
        return new BlockPos[]{
                core.offset(-2, 2, -2), core.offset(-2, 2, 3),
                core.offset(3, 2, -2), core.offset(3, 2, 3)};
    }

    /** The whole port surface is the core's own four horizontals. */
    @Override public int coreMask() { return MASK_HORIZONTAL; }
}
