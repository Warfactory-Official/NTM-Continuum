package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.machine.TileEntityMachineSuperComputer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** 1.7 {@code MachineSuperComputer}: the cabinet, its crown and the cable run toward the front. */
public class MachineSuperComputer extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder {

    private static final int[][] EXTRAS = {
            {6, -6, 3, 3, 1, 1},
            {6, -6, 1, 1, 3, 3},
            {7, -7, 1, 1, 1, 1},
            {2, 0, -3, 8, 1, 1}
    };

    public MachineSuperComputer(Properties properties) { super(properties); }

    @Override protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) { return new TileEntityMachineSuperComputer(pos, state); }
    @Override public int[] getDimensions() { return new int[] {5, 0, 3, 3, 3, 3}; }
    @Override public int getOffset() { return 8; }

    @Override
    public int[][] getAllDimensions() {
        return new int[][] {getDimensions(), EXTRAS[0], EXTRAS[1], EXTRAS[2], EXTRAS[3]};
    }

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        if (!super.checkRequirement(level, core, facing, clicked)) return false;
        for (int[] dim : EXTRAS) if (!MultiblockHandlerXR.checkSpace(level, core, dim, clicked, facing)) return false;
        return true;
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineSuperComputer computer ? computer : null;
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityMachineSuperComputer computer && IEnergyConnectorBlock.isPort(PortDef.connections(computer.getPorts()), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityMachineSuperComputer computer && IEnergyConnectorBlock.isPort(PortDef.connections(computer.getPorts()), pos, dir);
    }

    /** Port masks are stated for the south-facing bake, where the clockwise side is west. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int[] dim : EXTRAS) MultiblockHandlerXR.visitBox(core, dim, facing, visitor);

        Direction rot = facing.getClockWise();
        visitor.cell(core.relative(facing, 8), MASK_SOUTH);
        visitor.cell(core.relative(facing, 7).relative(rot), MASK_WEST);
        visitor.cell(core.relative(facing, 7).relative(rot, -1), MASK_EAST);
        visitor.cell(core.relative(facing, 5).relative(rot), MASK_WEST);
        visitor.cell(core.relative(facing, 5).relative(rot, -1), MASK_EAST);
    }

    /** 1.7 stamps an inventory, power and fluid proxy on the five port cells. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.relative(facing, 8), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, 7).relative(rot), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, 7).relative(rot, -1), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, 5).relative(rot), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, 5).relative(rot, -1), MASK_ALL, domains);
    }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {0, 0, 8, ForgeDirection.SOUTH.ordinal()},
                {1, 0, 7, ForgeDirection.EAST.ordinal()},
                {-1, 0, 7, ForgeDirection.WEST.ordinal()},
                {1, 0, 5, ForgeDirection.EAST.ordinal()},
                {-1, 0, 5, ForgeDirection.WEST.ordinal()},
        }, dir);
    }
}
