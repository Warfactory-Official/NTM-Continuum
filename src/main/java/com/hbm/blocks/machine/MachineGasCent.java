package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineGasCent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** Four-block gas centrifuge tower with energy and fluid ports around its core. */
public class MachineGasCent extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2,
        WorldlyContainerHolder {

    public MachineGasCent(Properties properties) {
        super(properties);
        this.bounding.add(new AABB(-0.5D, 0D, -0.5D, 0.5D, 1D, 0.5D));
        this.bounding.add(new AABB(-0.4375D, 1D, -0.4375D, 0.4375D, 4D, 0.4375D));
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineGasCent(pos, state);
    }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineGasCent te ? te : null;
    }

    @Override public int[] getDimensions() { return new int[]{3, 0, 0, 0, 0, 0}; }
    @Override public int getOffset() { return 0; }

    /** 1.7's six-direction loop minus UP, buried under the machine's own cell. */
    @Override
    public int coreMask() {
        return MASK_HORIZONTAL | MASK_DOWN;
    }
}
