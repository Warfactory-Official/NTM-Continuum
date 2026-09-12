package com.hbm.blocks.network;

import com.hbm.blocks.BlockDummyable;
import com.hbm.radio.ModRadioBlockEntities;
import com.hbm.tileentity.network.TileEntityRadioTelex;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The 1.12.2 {@code radio_telex}: a two-cell-wide, five-line wireless text machine. */
public final class RadioTelex extends BlockDummyable {

    public RadioTelex(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityRadioTelex(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 0, 0, 1, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    /** Sneaking is deliberately not a GUI action, matching the old block's activation path. */
    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                   BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return super.onUseWithoutItem(state, level, pos, player, hit);
    }

}
