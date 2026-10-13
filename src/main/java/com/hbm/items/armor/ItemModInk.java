package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.DamageEvents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

/** 1.7 {@code ItemModInk} (林 Ink): a 10% chance to nullify a hit, dropping flowers when it does. */
public class ItemModInk extends ItemArmorMod {

    /**
     * 1.7 {@code red_flower} metas 0..8: poppy, blue orchid, allium, azure bluet, the four tulips and
     * oxeye daisy. {@code nextInt(9)} picks one of them uniformly.
     */
    private static final Item[] RED_FLOWERS = {Items.POPPY, Items.BLUE_ORCHID, Items.ALLIUM, Items.AZURE_BLUET,
            Items.RED_TULIP, Items.ORANGE_TULIP, Items.WHITE_TULIP, Items.PINK_TULIP, Items.OXEYE_DAISY};

    public ItemModInk(Item.Properties properties) {
        super(ArmorModHandler.extra, true, true, true, true, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.LIGHT_PURPLE, "10% chance to nullify damage"));
        tooltip.add(coloured(ChatFormatting.LIGHT_PURPLE, "Flowers!"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(ChatFormatting.LIGHT_PURPLE, stack, " (10% chance to nullify damage)"));
    }

    @Override
    public void modDamage(DamageEvents.DamageEvent event, ItemStack armor) {
        LivingEntity entity = event.entity();
        RandomSource rand = entity.level().getRandom();
        if (rand.nextInt(10) == 0) {
            event.setAmount(0);
            if (!entity.level().isClientSide()) {
                if (rand.nextInt(10) == 0) drop(entity, new ItemStack(Items.DANDELION));
                drop(entity, new ItemStack(RED_FLOWERS[rand.nextInt(9)]));
            }
        }
    }

    /** 1.7 {@code entityDropItem(stack, 1.0F)}: an item entity one block above the entity's feet. */
    static void drop(LivingEntity entity, ItemStack stack) {
        ItemEntity item = new ItemEntity(
                entity.level(), entity.getX(), entity.getY() + 1.0D, entity.getZ(), stack);
        item.setDefaultPickUpDelay();
        entity.level().addFreshEntity(item);
    }
}
