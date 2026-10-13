package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineMiningLaser;
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
 * Mining Laser (1.12.2 {@code machine_mining_laser}): a ceiling-mounted 3×3×3 dummyable: the core hangs
 * one block below the clicked ceiling face ({@link #getHeightOffset()} = −1) with a 3D plus of dummy
 * cells; {@link #fillSpace} marks the old proxy-arm cells as extras (the port's connector geometry works
 * off {@code getConPos} ports instead of proxy tiles). Power connects at the TOP arm, fluid (crude oil
 * out) at the 4 side arms, and every cell exposes the core's 21 output slots to hoppers.
 */
public class MachineMiningLaser extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachineMiningLaser(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineMiningLaser(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 1, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    /** Ceiling mount: the core sits one below the placement position (as in 1.12.2). */
    @Override
    public int getHeightOffset() {
        return -1;
    }

    /** Any face of the multiblock exposes the core's inventory to hoppers (extract the 21 output slots). */
    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineMiningLaser te ? te : null;
    }

    /**
     * Two call sites with two disjoint cell sets: 1.7 sends oil over the four horizontals and subscribes
     * for power at the arm above, so unioning them would let a cable onto an oil outlet.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        visitor.cell(core.east(), MASK_EAST, ROLE_FLUID);
        visitor.cell(core.west(), MASK_WEST, ROLE_FLUID);
        visitor.cell(core.south(), MASK_SOUTH, ROLE_FLUID);
        visitor.cell(core.north(), MASK_NORTH, ROLE_FLUID);
        visitor.cell(core.above(), MASK_UP, ROLE_POWER);
    }

    /**
     * 1.7's top cell is a {@code TileEntityProxyEnergy} and carries power alone; the four side cells are
     * {@code inventory().fluid()} and carry no power at all.
     */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int sides = PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.east(), MASK_ALL, sides);
        visitor.passiveCell(core.west(), MASK_ALL, sides);
        visitor.passiveCell(core.south(), MASK_ALL, sides);
        visitor.passiveCell(core.north(), MASK_ALL, sides);
        visitor.passiveCell(core.above(), MASK_ALL, PASSIVE_POWER_IN);
    }
}
