package com.hbm.items.food;

import com.hbm.handler.ConsumableTreatment;
import com.hbm.handler.ConsumableTreatment.Context;
import com.hbm.handler.ConsumableTreatments;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ItemCanteen}: a reusable flask whose durability bar is its cooldown. Drinking (10 ticks) sets the
 * damage to the maximum; it counts back down by one every 20 ticks the holder exists, and only an empty bar
 * lets it be drunk again. The stylish flask is the only canteen. The cooldown is the item's durability, so it stays
 * here; the drink's effects are its {@code hbm:consumable_treatment}.
 */
public class ItemCanteen extends CompatItem implements ConsumableTreatments.Handled {

    public ItemCanteen(Properties properties) {
        super(properties);
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot,
                                       boolean held) {
        if (stack.getDamageValue() > 0 && owner.tickCount % 20 == 0) stack.setDamageValue(stack.getDamageValue() - 1);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() == 0 && !ConsumableTreatments.blocked(this, player)) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        stack.setDamageValue(stack.getMaxDamage());
        ConsumableTreatment treatment = ConsumableTreatments.of(this);
        if (treatment != null && level instanceof ServerLevel server) treatment.run(Context.eaten(server, entity));
        return stack;
    }

    @Override
    protected int compatUseDuration(ItemStack stack, LivingEntity entity) {
        return 10;
    }

    @Override
    protected UseAnimation compatUseAnimation(ItemStack stack) {
        return UseAnimation.DRINK;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal("Cooldown: 3 minutes"));
        tooltip.add(Component.literal("Nausea I for 10 seconds"));
        tooltip.add(Component.literal("Strength III for 30 seconds"));
        tooltip.add(Component.literal(""));
        // 1.7 swaps this line for a hammer-and-sickle joke when the session's random polaroid id is 11
        tooltip.add(Component.literal("Smells like disinfectant, tastes like disinfectant."));
    }
}
