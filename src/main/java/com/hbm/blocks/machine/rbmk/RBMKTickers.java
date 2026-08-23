package com.hbm.blocks.machine.rbmk;

import com.hbm.tileentity.machine.rbmk.IRBMKTicking;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

final class RBMKTickers {
    private RBMKTickers() {}

    @SuppressWarnings("unchecked")
    static <T extends BlockEntity> @Nullable BlockEntityTicker<T> ticker(
            Level level, BlockEntityType<T> actual, BlockEntityType<?> expected) {
        if (actual != expected) return null;
        return (blockLevel, pos, state, blockEntity) -> {
            IRBMKTicking ticking = (IRBMKTicking) blockEntity;
            if (blockLevel.isClientSide()) ticking.tickClient();
            else ticking.tickServer();
        };
    }
}
