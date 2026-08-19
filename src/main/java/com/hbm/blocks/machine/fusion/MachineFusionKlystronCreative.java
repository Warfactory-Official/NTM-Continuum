package com.hbm.blocks.machine.fusion;

import com.hbm.blocks.BlockDummyable;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.fusion.TileEntityFusionKlystronCreative;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.lib.ForgeDirection;

/**
 * Creative klystron - same shell as the real one, but it needs no power, no air and no ports, so it
 * has none. Behaviour lives in {@link TileEntityFusionKlystronCreative}.
 */
public class MachineFusionKlystronCreative extends BlockDummyable {

    public MachineFusionKlystronCreative(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFusionKlystronCreative(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{3, 0, 4, 3, 2, 2}; }

    @Override public int getOffset() { return 3; }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);
        MultiblockHandlerXR.fillSpace(level, core.getX(), core.getY(), core.getZ(),
                new int[]{4, -3, 4, 3, 1, 1}, this, dir);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.FUSION_KLYSTRON_CREATIVE.get(), TileEntityFusionKlystronCreative::clientTick)
                : createTickerHelper(type, ModBlockEntities.FUSION_KLYSTRON_CREATIVE.get(), TileEntityFusionKlystronCreative::serverTick);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
