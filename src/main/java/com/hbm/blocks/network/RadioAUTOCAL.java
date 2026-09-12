package com.hbm.blocks.network;

import com.hbm.blocks.BlockDummyable;
import com.hbm.radio.ModRadioBlockEntities;
import com.hbm.tileentity.network.TileEntityRadioAUTOCAL;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The one-block-core AUTOCAL unit from the old Redstone-over-Radio network. */
public final class RadioAUTOCAL extends BlockDummyable {
    public RadioAUTOCAL(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityRadioAUTOCAL(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                  BlockHitResult hit) {
        return player.isShiftKeyDown() ? InteractionResult.TRY_WITH_EMPTY_HAND
                : super.onUseWithoutItem(state, level, pos, player, hit);
    }

}
