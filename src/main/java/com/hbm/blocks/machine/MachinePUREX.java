package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachinePUREX;
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

/** The original five-by-five PUREX plant footprint, with edge power and fluid ports. */
public final class MachinePUREX extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder {
    public MachinePUREX(Properties properties) { super(properties); }
    @Override protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) { return new TileEntityMachinePUREX(pos, state); }
    @Override public int[] getDimensions() { return new int[]{4, 0, 2, 2, 2, 2}; }
    @Override public int getOffset() { return 2; }
    @Override public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachinePUREX purex ? purex : null;
    }
    @Override public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityMachinePUREX purex && IEnergyConnectorBlock.isPort(purex.getConPos(), pos, dir);
    }
    @Override public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return MultiblockSurface.coreEntity(world, new BlockPos(x, y, z)) instanceof TileEntityMachinePUREX purex
                && IEnergyConnectorBlock.isPort(purex.getConPos(), new BlockPos(x, y, z), dir);
    }
    

    /** The outer ring of the footprint is the port surface; each cell opens on the side it faces outward. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (Math.abs(i) == 2 || Math.abs(j) == 2) visitor.cell(core.offset(i, 0, j), outwardMask(i, j, 2));
            }
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (Math.abs(i) == 2 || Math.abs(j) == 2) {
                    visitor.passiveCell(core.offset(i, 0, j), MASK_ALL,
                            PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS);
                }
            }
        }
    }
}
