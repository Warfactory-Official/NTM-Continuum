package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineEPress;
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

/** Electric Press: a 1×1×3 dummyable column (core at the bottom, two dummies stacked above). */
public class MachineEPress extends BlockMultiblockMachine implements IEnergyConnectorBlock, WorldlyContainerHolder {

    public MachineEPress(Properties properties) {
        super(properties);
    }

    /** Any face of the column routes hoppers/pipes to the core's sided inventory. */
    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineEPress te ? te : null;
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineEPress(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    /** 1.7's six-direction loop minus UP, buried under the machine's own cell. */
    @Override
    public int coreMask() {
        return MASK_HORIZONTAL | MASK_DOWN;
    }
}
