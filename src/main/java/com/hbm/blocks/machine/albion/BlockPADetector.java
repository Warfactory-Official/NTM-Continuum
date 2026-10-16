package com.hbm.blocks.machine.albion;

import com.hbm.blocks.ITooltipProvider;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.albion.TileEntityPADetector;
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

/** The detector: the 5x5x5 collision chamber that ends the beamline and holds the products. */
public class BlockPADetector extends BlockMultiblockMachine
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder, ITooltipProvider {

    public BlockPADetector(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPADetector(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 2, 2, 2, 4, 4};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public int getHeightOffset() {
        return 2;
    }

    /** The detector's service column: one side face of the hall, three courses tall and one deep either way. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        BlockPos side = core.relative(facing.getClockWise(), -4);
        visitor.cell(side, MASK_EAST, ROLE_ALL);
        visitor.cell(side.above(), MASK_EAST, ROLE_ALL);
        visitor.cell(side.below(), MASK_EAST, ROLE_ALL);
        visitor.cell(side.relative(facing), MASK_EAST, ROLE_ALL);
        visitor.cell(side.relative(facing, -1), MASK_EAST, ROLE_ALL);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        BlockPos side = core.relative(facing.getClockWise(), -4);
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(side, MASK_ALL, domains);
        visitor.passiveCell(side.above(), MASK_ALL, domains);
        visitor.passiveCell(side.below(), MASK_ALL, domains);
        visitor.passiveCell(side.relative(facing), MASK_ALL, domains);
        visitor.passiveCell(side.relative(facing, -1), MASK_ALL, domains);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityPADetector te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityPADetector te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityPADetector detector ? detector : null;
    }

}
