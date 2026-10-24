package com.hbm.items.food;

import com.hbm.handler.ConsumableTreatment;
import com.hbm.handler.ConsumableTreatment.Context;
import com.hbm.handler.ConsumableTreatments;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Edible HBM pills (1.7 {@code ItemPill}); what each does is its {@code hbm:consumable_treatment}. */
public final class ItemPill extends CompatItem implements ConsumableTreatments.Handled {

    private final String id;

    public ItemPill(Item.Properties properties, String id) {
        super(properties);
        this.id = id;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (ConsumableTreatments.blocked(this, player)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return compatVanillaUse(level, player, hand);
    }

    @Override
    protected int compatUseDuration(ItemStack stack, LivingEntity entity) {
        return 10;
    }

    @Override
    protected UseAnimation compatUseAnimation(ItemStack stack) {
        return UseAnimation.EAT;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        ConsumableTreatment treatment = ConsumableTreatments.of(this);
        if (treatment != null && level instanceof ServerLevel server && entity instanceof Player player) {
            treatment.run(Context.eaten(server, player));
        }
        return result;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        switch (id) {
            case "pill_iodine" -> tooltip.add(Component.literal("Removes negative effects"));
            case "plan_c" -> tooltip.add(Component.literal("Deadly"));
            case "radx" -> tooltip.add(Component.literal("Increases radiation resistance by 0.2 (37%) for 3 minutes"));
            case "siox" -> tooltip.add(Component.literal("Reverses mesothelioma with the power of Asbestos!"));
            case "pill_herbal" -> {
                tooltip.add(Component.literal("Effective treatment against lung disease and mild radiation poisoning"));
                tooltip.add(Component.literal("Comes with side effects"));
            }
            case "xanax" -> tooltip.add(Component.literal("Removes 500mDRX"));
            case "fmn" -> tooltip.add(Component.literal("Removes all DRX above 2,000mDRX"));
            case "five_htp" -> tooltip.add(Component.literal("Removes all DRX, Stability for 10 minutes"));
            default -> {
            }
        }
    }
}
