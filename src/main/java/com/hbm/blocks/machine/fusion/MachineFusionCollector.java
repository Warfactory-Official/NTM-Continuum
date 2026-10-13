package com.hbm.blocks.machine.fusion;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.fusion.TileEntityFusionCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Fusion collector: a passive dish on the plasma network that raises the torus's output rate
 * without consuming any of it. All the behaviour is in {@link TileEntityFusionCollector}.
 */
public class MachineFusionCollector extends BlockMultiblockMachine {

    public MachineFusionCollector(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFusionCollector(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{3, 0, 2, 1, 2, 2}; }

    @Override public int getOffset() { return 1; }

    /** 1.7 {@code MachineFusionCollector.getAABBExtras}, read only by the placement highlight. */
    @Override
    public double[][] getAABBExtras() {
        return new double[][] {
            {1.5, 3.5, -2.5, -2.5, 1, -1}
        };
    }
}
