package com.hbm.blocks.machine.rbmk;

import com.hbm.tileentity.ModRBMKBlockEntities;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKTerminal;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** RTTY command terminal; its screen is a bare client screen rather than a container menu. */
public class RBMKTerminal extends RBMKMiniPanelBase implements dev.golem.core.registry.TickingBlock {

    public static Consumer<BlockEntityRBMKTerminal> OPEN_SCREEN = terminal -> {};

    public RBMKTerminal(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityRBMKTerminal(pos, state);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide() && level.getBlockEntity(pos) instanceof BlockEntityRBMKTerminal terminal) {
            OPEN_SCREEN.accept(terminal);
        }
        return InteractionResult.SUCCESS;
    }

}
