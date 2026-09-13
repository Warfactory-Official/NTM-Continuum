package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachinePrecAss;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Precision Assembly Machine: the four-striker 3x3x3 precision variant of the assembly machine. */
public final class MachinePrecAss extends MachineAssemblyMachine implements IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachinePrecAss(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachinePrecAss(pos, state);
    }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachinePrecAss te ? te : null;
    }

}
