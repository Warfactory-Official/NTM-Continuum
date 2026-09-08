package com.hbm.blocks.turret;

import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.turret.TileEntityTurretSentryDamaged;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The wrecked sentry. It drops nothing: 1.12.2 gives it {@code getItemDropped -> Items.AIR}. */
public class TurretSentryDamaged extends TurretSentry {

    public TurretSentryDamaged(Properties properties) {
        super(properties);
    }

    /** 1.12.2's wreck is a bare block container: right-clicking it opens nothing. */
    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityTurretSentryDamaged(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                           BlockEntityType<T> type) {
        return TurretBaseNT.ticker(level, type, ModBlockEntities.TURRET_SENTRY_DAMAGED.get());
    }
}
