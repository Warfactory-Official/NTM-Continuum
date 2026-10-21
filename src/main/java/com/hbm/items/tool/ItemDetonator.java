package com.hbm.items.tool;

import com.hbm.api.event.HbmEvents;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.IBomb.BombReturnCode;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.ModSounds;
import com.hbm.world.chunk.PendingDetonations;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Shift right-click a bomb block to bind its position, right-click in the air to set it off. For a launch pad
 * "setting it off" means launching, so this plus a designator is the whole manual launch path.
 *
 * <p>The bound position is one packed {@code BlockPos} component. A bound block that is no longer a bomb (a pad
 * broken since binding) reports "linked position incompatible or unloaded".
 */
public class ItemDetonator extends CompatItem {

    public ItemDetonator(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Shift right-click to set position,"));
        tooltip.add(Component.literal("right-click to detonate!"));

        BlockPos bound = ModDataComponents.DETONATOR_TARGET.get(stack).orElse(null);
        if (bound == null) {
            tooltip.add(Component.literal("No position set!").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal("Linked to " + bound.getX() + ", " + bound.getY() + ", " + bound.getZ())
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    /** Shift right-click on a block binds it. A plain right-click falls through to the block's own use. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ModDataComponents.DETONATOR_TARGET.set(context.getItemInHand(), pos);
        if (!level.isClientSide()) {
            player.sendSystemMessage(prefix().append(Component.literal("Position set!").withStyle(ChatFormatting.GREEN)));
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BOOP.get(),
                SoundSource.PLAYERS, 2.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockPos bound = ModDataComponents.DETONATOR_TARGET.get(stack).orElse(null);
        if (bound == null) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(prefix().append(Component.literal("No position set!").withStyle(ChatFormatting.RED)));
            }
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ServerLevel server = (ServerLevel) level;
        var chunk = server.getChunkSource().getChunkNow(bound.getX() >> 4, bound.getZ() >> 4);
        if (chunk == null) {
            PendingDetonations.queue(server, bound, player.getUUID());
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BLEEP.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        if (!(chunk.getBlockState(bound).getBlock() instanceof IBomb bomb)) {
            player.sendSystemMessage(prefix().append(Component.translatable(BombReturnCode.ERROR_NO_BOMB.getUnlocalizedMessage())
                    .withStyle(ChatFormatting.RED)));
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BLEEP.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        PendingDetonations.handoff(server, bound);
        BombReturnCode result = HbmEvents.detonate(bomb, level, bound, player);
        // A block that answers UNDEFINED has no message of its own; it still fired.
        String key = result.getUnlocalizedMessage().isEmpty()
                ? BombReturnCode.DETONATED.getUnlocalizedMessage() : result.getUnlocalizedMessage();
        player.sendSystemMessage(prefix().append(Component.translatable(key)
                .withStyle(result.wasSuccessful() || result == BombReturnCode.UNDEFINED
                        ? ChatFormatting.YELLOW : ChatFormatting.RED)));
        return InteractionResult.SUCCESS;
    }

    private static MutableComponent prefix() {
        return Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.translatable("item.hbm.detonator").withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA));
    }
}
