package com.hbm.items.tool;

import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.IBomb.BombReturnCode;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Detonator — 1.12.2's {@code ItemDetonator}, and the way a player fires anything by hand.
 *
 * <p><b>Shift</b> right-click a bomb block to bind its position; right-click in the air to set it off.
 * For a launch pad "setting it off" means launching, so this plus a designator is the whole manual
 * launch path — without it a pad can only be fired by redstone, which is what made the first playtest
 * conclude there was no way to launch at all.
 *
 * <p>The bound position was three NBT ints ({@code x}/{@code y}/{@code z}) and is now one packed
 * {@code BlockPos} component. Both failure cases keep upstream's chat feedback, which is the only thing
 * that tells a player <em>why</em> nothing happened: "position not set" when the detonator is unbound,
 * and "target incompatible or too far away" when the bound block is not a bomb (a launch pad broken
 * since binding, or a mis-click).
 */
public class ItemDetonator extends CompatItem {

    public ItemDetonator(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("desc.calldet1"));
        tooltip.add(Component.translatable("desc.calldet2"));

        BlockPos bound = ModDataComponents.DETONATOR_TARGET.get(stack).orElse(null);
        if (bound == null) {
            tooltip.add(Component.translatable("chat.posnoset").withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.translatable("chat.possetxyz", bound.getX(), bound.getY(), bound.getZ())
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    /** Shift right-click on a block binds it. A plain right-click falls through to the block's own use. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ModDataComponents.DETONATOR_TARGET.set(context.getItemInHand(), pos);

        if (!level.isClientSide()) {
            player.sendSystemMessage(Component.literal("[")
                    .append(Component.translatable("chat.posset")).append("]").withStyle(ChatFormatting.DARK_GREEN));
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BOOP.get(),
                SoundSource.AMBIENT, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockPos bound = ModDataComponents.DETONATOR_TARGET.get(stack).orElse(null);

        if (bound == null) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.translatable("chat.posnoseterror").withStyle(ChatFormatting.RED));
            }
            return InteractionResult.PASS;
        }

        if (!(level.getBlockState(bound).getBlock() instanceof IBomb bomb)) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.translatable("chat.postoofarerror").withStyle(ChatFormatting.RED));
            }
            return InteractionResult.PASS;
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BLEEP.get(),
                SoundSource.AMBIENT, 1.0F, 1.0F);

        if (!level.isClientSide()) {
            BombReturnCode result = bomb.explode(level, bound, player);
            // Upstream always reports "detonated"; the return code carries the real answer (a pad with no
            // designator, no fuel or an unlaunchable missile says so), so it is what gets printed.
            Component message = result.getUnlocalizedMessage().isEmpty()
                    ? Component.translatable("chat.detonated")
                    : Component.translatable(result.getUnlocalizedMessage());
            player.sendSystemMessage(Component.literal("[").append(message).append("]")
                    .withStyle(result.wasSuccessful() ? ChatFormatting.DARK_GREEN : ChatFormatting.RED));
        }
        return InteractionResult.SUCCESS;
    }
}
