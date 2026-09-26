package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.machine.TileEntityMachineOreSlopper;

/**
 * 1.7 {@code MachineOreSlopper} (the B.O.P.): a 7 (along) x 3 (across) x 4 (high) footprint, core three
 * blocks behind the clicked position. {@code makeExtra} stamps {@code inventory().power().fluid()} proxies
 * on the two ends and the six side cells, and {@code getConPos} looks outward from those.
 */
public final class MachineOreSlopper extends BlockMultiblockMachine
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, net.minecraft.world.WorldlyContainerHolder {

    public MachineOreSlopper(Properties properties) {
        super(properties);

        // Base
        this.bounding.add(new AABB(-3.5, 0, -1.5, 3.5, 1, 1.5));
        // Slop bucket
        this.bounding.add(new AABB(0.5, 1, -1.5, 3.5, 3.25, 1.5));
        // Shredder
        this.bounding.add(new AABB(-2.25, 1, -1.5, 0.25, 3.25, -0.75));
        this.bounding.add(new AABB(-2.25, 1, 0.75, 0.25, 3.25, 1.5));
        this.bounding.add(new AABB(-2.25, 1, -1.5, -2, 3.25, 1.5));
        this.bounding.add(new AABB(0, 1, -1.5, 0.25, 3.25, 1.5));
        this.bounding.add(new AABB(-2, 1, -0.75, 0, 2, 0.75));
        // Outlet
        this.bounding.add(new AABB(-3.25, 1, -1, -2.25, 3, 1));
    }

    @Override protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineOreSlopper(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{3, 0, 3, 3, 1, 1}; }
    @Override public int getOffset() { return 3; }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineOreSlopper slopper ? slopper : null;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise(); // 1.7 dir.getRotation(UP); +rot is local WEST

        visitor.cell(core.relative(facing, 3), MASK_SOUTH);
        visitor.cell(core.relative(facing, -3), MASK_NORTH);
        for (int along = -2; along <= 2; along += 2) {
            visitor.cell(core.relative(facing, along).relative(rot), MASK_WEST);
            visitor.cell(core.relative(facing, along).relative(rot, -1), MASK_EAST);
        }
    }

    /** 1.7 stamps {@code inventory().power().fluid()} on the eight. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.relative(facing, 3), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -3), MASK_ALL, domains);
        for (int along = -2; along <= 2; along += 2) {
            visitor.passiveCell(core.relative(facing, along).relative(rot), MASK_ALL, domains);
            visitor.passiveCell(core.relative(facing, along).relative(rot, -1), MASK_ALL, domains);
        }
    }
}
