package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntityMachineSatLinker;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code MachineSatLinker}, the "Satellite ID Manager": a plain cube ({@code machine_satlinker_top} on top
 * and bottom, {@code machine_satlinker_side} around). A plain click opens its GUI; sneaking passes. Breaking it
 * spills the three chips.
 */
public class MachineSatLinker extends CompatBlock implements TickingBlock {

    public MachineSatLinker(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineSatLinker(pos, state);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TileEntityMachineSatLinker linker) {
            dev.golem.core.gui.Menus.openWithPos(serverPlayer, linker, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
