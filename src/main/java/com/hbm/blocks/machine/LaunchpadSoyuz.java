package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.machine.TileEntityLaunchpadSoyuz;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code LaunchpadSoyuz}: the pad, its service tower, the 58-block carriage rails and the rocket shed, built from
 * nineteen boxes around a core two blocks ahead of the click. The twelve rail ends one block up take power, fluids
 * and items.
 */
public class LaunchpadSoyuz extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    /** {forward, up, right, box}: 1.7 {@code fillSpace}'s extra boxes, origins in core-relative steps. */
    private static final int[][][] BOXES = {
            {{0, 0, 0}, {3, 0, 2, 1, 1, 2}},
            {{0, 2, 0}, {0, 0, 2, 2, -2, 10}},
            {{0, 2, 0}, {0, 0, 10, -2, 2, 10}},
            {{0, 0, 0}, {3, -3, 9, 1, 1, 9}},
            {{0, 2, 0}, {-1, 2, 2, 2, -6, 10}},
            {{0, 2, 0}, {-1, 2, 10, -6, 2, 2}},
            {{0, 2, 0}, {-1, 2, 10, -6, -6, 10}},
            {{2, 0, 0}, {0, 0, 0, 0, -10, 58}},
            {{2, 0, 0}, {1, -1, 0, 0, -56, 58}},
            {{-10, 0, 0}, {0, 0, 0, 0, -10, 58}},
            {{-10, 0, 0}, {1, -1, 0, 0, -56, 58}},
            {{0, 0, 0}, {2, 0, 2, 1, 7, -3}},
            {{-7, 0, 0}, {2, 0, 2, 1, 7, -3}},
            {{-4, 2, 0}, {0, 0, 1, 1, 7, -3}},
            {{-4, 2, 0}, {1, -1, 5, 5, 7, -3}},
            {{-4, 2, 0}, {3, -2, 4, 4, 7, -3}},
            {{-4, 2, 0}, {6, -4, 3, 3, 7, -3}},
            {{-4, 2, 0}, {51, -7, 2, 2, 7, -3}},
            {{0, 0, 5}, {7, 0, -2, 3, 2, 2}},
    };

    /** The space the rocket rises through, checked but not built. */
    private static final int[][] CLEARANCE = {{-4, 0, 0}, {7, 0, -6, 7, 7, -3}};

    /** {forward, right}: the port cells one block up, as {@code TileEntityLaunchpadSoyuz.getPorts}. */
    private static final int[][] PORTS = {
            {2, 1}, {2, -1}, {2, -7}, {2, -9},
            {-10, 1}, {-10, -1}, {-10, -7}, {-10, -9},
            {1, -10}, {-1, -10}, {-7, -10}, {-9, -10},
    };

    public LaunchpadSoyuz(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityLaunchpadSoyuz(pos, state);
    }

    @Override public int getOffset() { return 2; }
    @Override public int[] getDimensions() { return new int[] {2, 0, 2, 2, 2, 2}; }

    @Override
    public int[][] getAllDimensions() {
        return new int[][] {
                {2, 0, 2, 2, 2, 2},
                {3, 0, 2, 1, 1, 2},

                {2, -2, 2, 2, -2, 10},
                {2, -2, 10, -2, 2, 10},
                {3, -3, 9, 1, 1, 9},
                {1, 0, 2, 2, -6, 10},
                {1, 0, 10, -6, 2, 2},
                {1, 0, 10, -6, -6, 10},

                {0, 0, 0, 0, -10, 58, 2, 0, 0},
                {1, -1, 0, 0, -56, 58, 2, 0, 0},
                {0, 0, 0, 0, -10, 58, -10, 0, 0},
                {1, -1, 0, 0, -56, 58, -10, 0, 0},

                {2, 0, 2, 1, 7, -3},
                {2, 0, 2, 1, 7, -3, -6, 0, 0},
                {0, 0, 1, 1, 7, -3, -4, 2, 0},
                {1, -1, 5, 5, 7, -3, -4, 2, 0},
                {3, -2, 4, 4, 7, -3, -4, 2, 0},
                {6, -4, 3, 3, 7, -3, -4, 2, 0},
                {51, -7, 2, 2, 7, -3, -4, 2, 0},

                {7, 0, -2, 3, 2, 2, 0, 0, 5},
        };
    }

    static BlockPos at(BlockPos core, Direction facing, int forward, int up, int right) {
        return core.relative(facing, forward).above(up).relative(facing.getClockWise(), right);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityLaunchpadSoyuz pad
                && IEnergyConnectorBlock.isPort(PortDef.connections(pad.getPorts()), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityLaunchpadSoyuz pad
                && IEnergyConnectorBlock.isPort(PortDef.connections(pad.getPorts()), pos, dir);
    }

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        if (!super.checkRequirement(level, core, facing, clicked)) return false;
        for (int[][] box : BOXES) {
            if (!MultiblockHandlerXR.checkSpace(level, at(core, facing, box[0][0], box[0][1], box[0][2]), box[1], clicked, facing)) return false;
        }
        int[] o = CLEARANCE[0];
        return MultiblockHandlerXR.checkSpace(level, at(core, facing, o[0], o[1], o[2]), CLEARANCE[1], clicked, facing);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int[][] box : BOXES) {
            MultiblockHandlerXR.visitBox(at(core, facing, box[0][0], box[0][1], box[0][2]), box[1], facing, visitor);
        }
        for (int[] port : PORTS) visitor.cell(at(core, facing, port[0], 1, port[1]), MASK_ALL);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (int[] port : PORTS) {
            visitor.passiveCell(at(core, facing, port[0], 1, port[1]), MASK_ALL,
                    PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS);
        }
    }
}
