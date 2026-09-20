package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.machine.TileEntityMachineAnnihilator;

/**
 * 1.7 {@code MachineAnnihilator}: a 9 (along) x 3 (across) x 3 (high) body, core four blocks behind the
 * clicked position, with a 3x3x11 chimney (y -2..+8) three blocks behind the core. {@code makeExtra}
 * stamps {@code inventory().fluid()} proxies on the front cell and the two front flank cells.
 */
public final class MachineAnnihilator extends BlockMultiblockMachine
        implements IFluidConnectorBlockMK2, net.minecraft.world.WorldlyContainerHolder {

    private static final int[] CHIMNEY = {8, -2, 1, 1, 1, 1};

    public MachineAnnihilator(Properties properties) { super(properties); }

    @Override protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineAnnihilator(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 4, 4, 1, 1}; }
    @Override public int getOffset() { return 4; }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineAnnihilator annihilator ? annihilator : null;
    }

    /** 1.7: {@code x + dir.offsetX * (o - 3)} from the clicked block is three behind the core. */
    private static BlockPos chimney(BlockPos core, Direction facing) {
        return core.relative(facing, -3);
    }

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        return super.checkRequirement(level, core, facing, clicked)
                && MultiblockHandlerXR.checkSpace(level, chimney(core, facing), CHIMNEY, clicked, facing);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MultiblockHandlerXR.visitBox(chimney(core, facing), CHIMNEY, facing, visitor);

        Direction rot = facing.getClockWise(); // 1.7 dir.getRotation(UP); +rot is local WEST
        visitor.cell(core.relative(facing, 4), MASK_SOUTH);
        visitor.cell(core.relative(facing, 3).relative(rot), MASK_WEST);
        visitor.cell(core.relative(facing, 3).relative(rot, -1), MASK_EAST);
    }

    /** 1.7 stamps {@code inventory().fluid()} on the three. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.relative(facing, 4), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, 3).relative(rot), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, 3).relative(rot, -1), MASK_ALL, domains);
    }
}
