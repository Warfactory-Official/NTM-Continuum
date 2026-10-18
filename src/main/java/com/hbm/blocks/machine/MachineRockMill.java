package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.machine.TileEntityMachineRockMill;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** 1.7 {@code MachineRockMill}: 5×5×3 with two ports on each side. */
public class MachineRockMill extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder {

    private static final int[][] PORTS = {{2, 1}, {2, -1}, {-2, 1}, {-2, -1}, {1, 2}, {-1, 2}, {1, -2}, {-1, -2}};

    public MachineRockMill(Properties properties) { super(properties); }

    @Override protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) { return new TileEntityMachineRockMill(pos, state); }
    @Override public int[] getDimensions() { return new int[] {2, 0, 2, 2, 2, 2}; }
    @Override public int getOffset() { return 2; }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineRockMill mill ? mill : null;
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityMachineRockMill mill && IEnergyConnectorBlock.isPort(PortDef.connections(mill.getPorts()), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityMachineRockMill mill && IEnergyConnectorBlock.isPort(PortDef.connections(mill.getPorts()), pos, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int[] port : PORTS) visitor.cell(core.offset(port[0], 0, port[1]), outwardMask(port[0], port[1], 2));
    }

    /** 1.7 stamps an inventory, power and fluid proxy on the port cells. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (int[] port : PORTS) {
            visitor.passiveCell(core.offset(port[0], 0, port[1]), MASK_ALL, PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS);
        }
    }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {2, 0, 1, ForgeDirection.EAST.ordinal()},
                {-2, 0, 1, ForgeDirection.WEST.ordinal()},
                {2, 0, -1, ForgeDirection.EAST.ordinal()},
                {-2, 0, -1, ForgeDirection.WEST.ordinal()},
                {1, 0, 2, ForgeDirection.SOUTH.ordinal()},
                {1, 0, -2, ForgeDirection.NORTH.ordinal()},
                {-1, 0, 2, ForgeDirection.SOUTH.ordinal()},
                {-1, 0, -2, ForgeDirection.NORTH.ordinal()},
        }, dir);
    }
}
