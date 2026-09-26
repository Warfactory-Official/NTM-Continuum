package com.hbm.blocks.machine.albion;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.api.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.albion.TileEntityPASource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

/** The particle source: the accelerator's injector, and the machine that owns the running beam. */
public class BlockPASource extends BlockMultiblockMachine
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public BlockPASource(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPASource(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 1, 1, 1, 4, 4};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public int getHeightOffset() {
        return 1;
    }

    /**
     * The ten cells the source pushes through, each open on the one face its port looks out of: three
     * along the beam either way, the side outlet, and three underneath.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();

        for (int side = -1; side <= 1; side++) {
            visitor.cell(core.relative(facing).relative(rot, side * 2), MASK_SOUTH, ROLE_ALL);
            visitor.cell(core.relative(facing, -1).relative(rot, side * 2), MASK_NORTH, ROLE_ALL);
            visitor.cell(core.below().relative(rot, side * 2), MASK_DOWN, ROLE_ALL);
        }
        visitor.cell(core.relative(rot, 4), MASK_WEST, ROLE_ALL);
    }

    /** Every port takes both a cable and a coolant duct, so each cell answers for either domain. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;

        for (int side = -1; side <= 1; side++) {
            visitor.passiveCell(core.relative(facing).relative(rot, side * 2), MASK_ALL, domains);
            visitor.passiveCell(core.relative(facing, -1).relative(rot, side * 2), MASK_ALL, domains);
            visitor.passiveCell(core.below().relative(rot, side * 2), MASK_ALL, domains);
        }
        visitor.passiveCell(core.relative(rot, 4), MASK_ALL, domains);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, Direction side) {
        ForgeDirection dir = ForgeDirection.of(side);
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityPASource te
                && com.hbm.lib.Ports.isPort(te.getConPos(), pos, dir.toDirection());
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, Direction side) {
        ForgeDirection dir = ForgeDirection.of(side);
        BlockPos pos = new BlockPos(x, y, z);
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityPASource te
                && com.hbm.lib.Ports.isPort(te.getConPos(), pos, dir.toDirection());
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityPASource source ? source : null;
    }

}
