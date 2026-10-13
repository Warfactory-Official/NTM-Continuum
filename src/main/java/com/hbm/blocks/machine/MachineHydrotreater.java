package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachineHydrotreater;
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

/** Hydrotreater block: 3×3×7 the folded core (dims {@code {6,0,1,1,1,1}}, offset 1) with the
 * 4 base corners marked as extras (the fluid ports). */
public class MachineHydrotreater extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachineHydrotreater(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineHydrotreater(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{6, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 1; }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineHydrotreater te ? te : null;
    }

    /** The four base corners are the port ring. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int i = -1; i <= 1; i += 2) {
            for (int j = -1; j <= 1; j += 2) {
                visitor.cell(core.offset(i, 0, j), outwardMask(i, j, 1));
            }
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (int i = -1; i <= 1; i += 2) {
            for (int j = -1; j <= 1; j += 2) {
                visitor.passiveCell(core.offset(i, 0, j), MASK_ALL, PASSIVE_POWER_IN | PASSIVE_FLUID_IN);
            }
        }
    }
}
