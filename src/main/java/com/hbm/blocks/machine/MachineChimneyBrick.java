package com.hbm.blocks.machine;

import com.hbm.smoke.SmokeChainRegistration;
import com.hbm.tileentity.machine.TileEntityChimneyBase;
import com.hbm.tileentity.machine.TileEntityChimneyBrick;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The 13-high brick smokestack. */
public final class MachineChimneyBrick extends MachineChimneyBase {

    public MachineChimneyBrick(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityChimneyBrick(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{12, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 1; }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                              BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, SmokeChainRegistration.CHIMNEY_BRICK.get(), TileEntityChimneyBase::clientTick)
                : createTickerHelper(type, SmokeChainRegistration.CHIMNEY_BRICK.get(), TileEntityChimneyBase::serverTick);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
