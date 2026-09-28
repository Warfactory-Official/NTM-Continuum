package com.hbm.blocks.machine.rbmk;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKAutoloader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code RBMKAutoloader}: a nine-tall folded column (core at the bottom) with a thin shaft below and
 * a full-width head from four blocks up. Every cell routes automation to the core, which is what the old
 * {@code TileEntityProxyCombo().inventory()} dummies did; a right-click on any cell opens the core's menu.
 */
public class RBMKAutoloader extends BlockMultiblockMachine implements WorldlyContainerHolder {

    public RBMKAutoloader(Properties properties) {
        super(properties);
        bounding.add(new AABB(-0.125D, 0D, -0.125D, 0.125D, 4D, 0.125D));
        bounding.add(new AABB(-0.5D, 4D, -0.5D, 0.5D, 9D, 0.5D));
    }

    @Override protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) { return new BlockEntityRBMKAutoloader(pos, state); }
    @Override public int[] getDimensions() { return new int[]{8, 0, 0, 0, 0, 0}; }
    @Override public int getOffset() { return 0; }

    @Override public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof BlockEntityRBMKAutoloader loader ? loader : null;
    }
}
