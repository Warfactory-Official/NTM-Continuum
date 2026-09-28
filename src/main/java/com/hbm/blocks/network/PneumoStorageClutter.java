package com.hbm.blocks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoStorageClutter;

/** 1.7 {@code PneumoStorageClutter}: a 54-slot bank; its contents ride the dropped item. */
public class PneumoStorageClutter extends PneumaticStorageBlockBase {

    public PneumoStorageClutter(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPneumoStorageClutter(pos, state);
    }

    /** 1.7 {@code removedByPlayer} with {@code /ntmserver CRATE_KEEP_CONTENTS} off: see {@link com.hbm.blocks.generic.CrateKeepContents}. */
    @Override
    protected void onPlayerWillDestroy(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                                       net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.entity.player.Player player) {
        com.hbm.blocks.generic.CrateKeepContents.breakLoose(level, pos, state, player);
        super.onPlayerWillDestroy(level, pos, state, player);
    }
}
