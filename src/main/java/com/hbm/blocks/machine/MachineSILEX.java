package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntitySILEX;
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

/**
 * Laser Isotope Separation Chamber, "SILEX" (1.12.2 {@code machine_silex}): a 3×3×3 dummyable chamber
 * that dissolves isotope-bearing material and lets a {@code machine_fel}'s beam sort it.
 *
 * <p>It takes <b>no power of its own</b> — all the energy is in the laser. What it does need is
 * peroxide, which arrives through the two ports {@link #fillSpace} adds at {@code y+1} on either side,
 * perpendicular to the machine's facing. Those two extras are the only fluid connection points.
 */
public class MachineSILEX extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachineSILEX(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntitySILEX(pos, state);
    }

    /** Any face exposes the core inventory: insert into slot 0, extract from the 5–10 queue. */
    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntitySILEX te ? te : null;
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    /** Two fluid inlets at the waist, one on each side across the facing axis. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();
        visitor.cell(core.offset(rot.getStepX(), 1, rot.getStepZ()), MASK_WEST);
        visitor.cell(core.offset(-rot.getStepX(), 1, -rot.getStepZ()), MASK_EAST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.offset(rot.getStepX(), 1, rot.getStepZ()), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-rot.getStepX(), 1, -rot.getStepZ()), MASK_ALL, domains);
    }
}
