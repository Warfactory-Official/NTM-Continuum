package com.hbm.blocks.generic;

import dev.golem.core.world.CompatBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1.7 {@code BlockWriting}: the marked concrete bricks. A right-click without sneaking recites the
 * long-term nuclear waste warning, in red, to the player.
 */
public class BlockWriting extends CompatBlock {

    private static final String[] LINES = {
            "You should not have come here.",
            "This is not a place of honor. No great deed is commemorated here.",
            "Nothing of value is here.",
            "What is here is dangerous and repulsive.",
            "We considered ourselves a powerful culture. We harnessed the hidden fire, and used it for our own purposes.",
            "Then we saw the fire could burn within living things, unnoticed until it destroyed them.",
            "And we were afraid.",
            "We built great tombs to hold the fire for one hundred thousand years, after which it would no longer kill.",
            "If this place is opened, the fire will not be isolated from the world, and we will have failed to protect you.",
            "Leave this place and never come back."
    };

    public BlockWriting(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        } else if (!player.isShiftKeyDown()) {
            for (String line : LINES) {
                player.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.RED));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
