package com.hbm.items.special;

import com.hbm.advancement.HbmCriteria;
import com.hbm.handler.ConsumableTreatment;
import com.hbm.handler.ConsumableTreatment.Context;
import com.hbm.handler.ConsumableTreatments;
import com.hbm.items.ModFinalConsumableItems;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ItemCigarette}, shared by the FFI cigarette and the health pipe: a 30-tick bow-drawn "use" after
 * which the smoker coughs. The cigarette deposits black lung, asbestos and 100 RAD; the pipe a little black lung,
 * ten seconds of nausea and 10 HP of healing. Both are {@code hbm:consumable_treatment}.
 *
 * <p>Not reproduced: the {@code vomit}/{@code smoke} aux particle burst (no port of that particle yet) and the
 * No. 9 achievement, whose helmet does not exist in the port.
 */
public class ItemCigarette extends CompatItem implements ConsumableTreatments.Handled {

    public ItemCigarette(Properties properties) {
        super(properties);
    }

    @Override
    protected UseAnimation compatUseAnimation(ItemStack stack) {
        return UseAnimation.BOW;
    }

    @Override
    protected int compatUseDuration(ItemStack stack, LivingEntity entity) {
        return 30;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        boolean cigarette = this == ModFinalConsumableItems.cigarette.get();
        ItemStack smoked = stack.copyWithCount(1);
        stack.shrink(1); // 1.7 consumes the cigarette even in creative

        if (level instanceof ServerLevel server) {
            ConsumableTreatment treatment = ConsumableTreatments.of(this);
            if (treatment != null) treatment.run(Context.eaten(server, entity));
            // 1.7 achNo9: the advancement's player predicate asks for the No. 9 helmet
            if (cigarette && entity instanceof ServerPlayer player) HbmCriteria.SMOKED.fire(player, smoked);
        }
        return stack;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        if (this == ModFinalConsumableItems.cigarette.get()) {
            tooltip.add(Component.literal("✓ Asbestos filter").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("✓ High in tar").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("✓ Tobacco contains 100% Polonium-210").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("✓ Yum").withStyle(ChatFormatting.RED));
        } else {
            ChatFormatting[] colors = {ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.YELLOW,
                    ChatFormatting.GREEN, ChatFormatting.AQUA, ChatFormatting.BLUE, ChatFormatting.DARK_PURPLE,
                    ChatFormatting.LIGHT_PURPLE};
            int len = 2000;
            ChatFormatting color = colors[(int) (System.currentTimeMillis() % len * colors.length / len)];
            tooltip.add(Component.literal("This can't be good for me, but I feel ")
                    .append(Component.literal("GREAT").withStyle(color)));
        }
    }
}
