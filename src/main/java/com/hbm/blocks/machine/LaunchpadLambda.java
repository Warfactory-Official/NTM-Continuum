package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.machine.TileEntityLaunchpadLambda;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code LaunchpadLambda}: a 15x15 silo deck two blocks high whose rim rises two more, core seven blocks ahead of
 * the click. The five cells on the front edge one block up take power, fluids and items.
 */
public class LaunchpadLambda extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    private static final int[][] WALLS = {
            {2, -2, 7, -6, 7, 7},
            {2, -2, -6, 7, 7, 7},
            {2, -2, 7, 7, 7, -6},
            {2, -2, 7, 7, -6, 7},
    };

    public LaunchpadLambda(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityLaunchpadLambda(pos, state);
    }

    @Override public int[] getDimensions() { return new int[] {1, 0, 7, 7, 7, 7}; }
    @Override public int getOffset() { return 7; }

    @Override
    public int[][] getAllDimensions() {
        return new int[][] {
                {1, 0, 7, 7, 7, 7},
                {2, -2, 7, -6, 7, 7},
                {2, -2, -6, 7, 7, 7},
                {2, -2, 7, 7, 7, -6},
                {2, -2, 7, 7, -6, 7},
        };
    }

    /** {@code TileEntityLaunchpadLambda.getPorts}: seven forward, two to six to the left, one up. */
    private static BlockPos port(BlockPos core, Direction facing, int i) {
        return core.relative(facing, 7).relative(facing.getClockWise(), -(2 + i)).above();
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityLaunchpadLambda pad
                && IEnergyConnectorBlock.isPort(PortDef.connections(pad.getPorts()), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityLaunchpadLambda pad
                && IEnergyConnectorBlock.isPort(PortDef.connections(pad.getPorts()), pos, dir);
    }

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        if (!super.checkRequirement(level, core, facing, clicked)) return false;
        for (int[] wall : WALLS) {
            if (!MultiblockHandlerXR.checkSpace(level, core, wall, clicked, facing)) return false;
        }
        return true;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int[] wall : WALLS) MultiblockHandlerXR.visitBox(core, wall, facing, visitor);
        for (int i = 0; i < 5; i++) visitor.cell(port(core, facing, i), MASK_ALL);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (int i = 0; i < 5; i++) {
            visitor.passiveCell(port(core, facing, i), MASK_ALL, PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS);
        }
    }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {-2, 1, 7, ForgeDirection.SOUTH.ordinal()},
                {-3, 1, 7, ForgeDirection.SOUTH.ordinal()},
                {-4, 1, 7, ForgeDirection.SOUTH.ordinal()},
                {-5, 1, 7, ForgeDirection.SOUTH.ordinal()},
                {-6, 1, 7, ForgeDirection.SOUTH.ordinal()},
        }, dir);
    }
}
