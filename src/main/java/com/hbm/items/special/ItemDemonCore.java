package com.hbm.items.special;

import com.hbm.items.ModItems;
import com.hbm.items.ModToolItems;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** The suspended demon core: drop it and the screwdriver slips, closing the core. */
public class ItemDemonCore extends CompatItem {

    public ItemDemonCore(Properties properties) {
        super(properties);
    }

    /** 1.7 {@code onEntityItemUpdate}: the grounded drop becomes the closed core plus a loose screwdriver. */
    public static void tickDropped(ItemEntity entity) {
        if (entity.level().isClientSide() || !entity.onGround()) return;
        if (!(entity.getItem().getItem() instanceof ItemDemonCore)) return;
        entity.setItem(new ItemStack(ModItems.demon_core_closed.get()));
        entity.level().addFreshEntity(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(),
                new ItemStack(ModToolItems.screwdriver.get())));
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendCompatHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("[").append(Component.translatable("trait.drop")).append("]").withStyle(ChatFormatting.RED));
    }
}
