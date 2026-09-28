package com.hbm.items.special;

import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemDrop}, the part that is still its own item: {@code beta} ("Beta Features"). A dropped beta
 * vanishes at once, and like every {@code ItemDrop} it is labelled a dangerous drop. The rest of 1.7's
 * {@code ItemDrop} chain (detonators, antimatter, singularities) lives with those items.
 *
 * <p>Carrying it pins the food level at 10, turning any surplus into health - see
 * {@code ModArmorSetBItems.tickPlayer}.
 */
public class ItemDrop extends CompatItem {

    public ItemDrop(Properties properties) {
        super(properties);
    }

    /** 1.7 {@code onEntityItemUpdate}: {@code if(this == ModItems.beta) setDead}. */
    public static void tickDropped(ItemEntity entity) {
        if (entity.getItem().getItem() instanceof ItemDrop) entity.discard();
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal("[").append(Component.translatable("trait.drop")).append("]")
                .withStyle(ChatFormatting.RED));
    }
}
