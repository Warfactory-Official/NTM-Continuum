package com.hbm.blocks.machine.albion;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.albion.TileEntityPAQuadrupole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The focusing magnet: a 3x3x3 cube with its ports directly above, below, and along the beam. */
public class BlockPAQuadrupole extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public BlockPAQuadrupole(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPAQuadrupole(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 1, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public int getHeightOffset() {
        return 1;
    }

    /** Four ports: the column above and below, and one along the beam either way. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        visitor.cell(core.above(), MASK_UP, ROLE_ALL);
        visitor.cell(core.below(), MASK_DOWN, ROLE_ALL);
        visitor.cell(core.relative(facing), MASK_SOUTH, ROLE_ALL);
        visitor.cell(core.relative(facing, -1), MASK_NORTH, ROLE_ALL);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(core.above(), MASK_ALL, domains);
        visitor.passiveCell(core.below(), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -1), MASK_ALL, domains);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityPAQuadrupole te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityPAQuadrupole te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

}
