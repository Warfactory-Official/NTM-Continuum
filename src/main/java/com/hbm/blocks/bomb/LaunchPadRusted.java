package com.hbm.blocks.bomb;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.IBomb;
import com.hbm.tileentity.bomb.TileEntityLaunchPadRusted;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code LaunchPadRusted}: the old silo pad, 3x3 with the small pad's collision boxes. It has no proxies, no
 * power or fluid, and drops nothing when broken. A detonator or a redstone rising edge fires its locked-in
 * missile when codes, key and designator are in place.
 */
public class LaunchPadRusted extends BlockMultiblockMachine implements IBomb {

    public LaunchPadRusted(Properties properties) {
        super(properties);
        this.bounding.add(new AABB(-1.5D, 0D, -1.5D, -0.5D, 1D, -0.5D));
        this.bounding.add(new AABB(0.5D, 0D, -1.5D, 1.5D, 1D, -0.5D));
        this.bounding.add(new AABB(-1.5D, 0D, 0.5D, -0.5D, 1D, 1.5D));
        this.bounding.add(new AABB(0.5D, 0D, 0.5D, 1.5D, 1D, 1.5D));
        this.bounding.add(new AABB(-0.5D, 0.5D, -1.5D, 0.5D, 1D, 1.5D));
        this.bounding.add(new AABB(-1.5D, 0.5D, -0.5D, 1.5D, 1D, 0.5D));
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityLaunchPadRusted(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{0, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 1; }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityLaunchPadRusted pad) {
            return pad.launch();
        }
        return BombReturnCode.UNDEFINED;
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighborBlock, movedByPiston);
        if (level.isClientSide()) return;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityLaunchPadRusted pad) {
            pad.updateRedstonePower(level, pos);
        }
    }
}
