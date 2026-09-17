package com.hbm.blocks.machine.albion;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.albion.TileEntityPARFC;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The RF cavity: nine blocks of beamline that add momentum, with power and coolant ports above and below. */
public class BlockPARFC extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public BlockPARFC(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPARFC(pos, state);
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

    /** The cavity's six proxies: the two side towers and the column through the core, top and bottom. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(core.relative(rot, 3).above(), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot, -3).above(), MASK_ALL, domains);
        visitor.passiveCell(core.above(), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot, 3).below(), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot, -3).below(), MASK_ALL, domains);
        visitor.passiveCell(core.below(), MASK_ALL, domains);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityPARFC te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityPARFC te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

}
