package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemModRevive}: Bottle of Scrumpy (one revive) and Wild Pegasus Dry Whiskey (three). The
 * revive itself is {@code ArmorModEvents.onAllowDeath}: each use costs one durability point and the
 * module is removed when the last one is spent.
 */
public class ItemModRevive extends ItemArmorMod {

    private final boolean whiskey;

    public ItemModRevive(int durability, boolean whiskey, Item.Properties properties) {
        super(ArmorModHandler.extra, false, false, true, false, properties.durability(durability));
        this.whiskey = whiskey;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        if (!whiskey) {
            tooltip.add(coloured(ChatFormatting.GOLD, "But how did you survive?"));
            tooltip.add(coloured(ChatFormatting.RED, "I was drunk."));
        } else {
            tooltip.add(coloured(ChatFormatting.DARK_GRAY, "Explosive ")
                    .append(coloured(ChatFormatting.RED, "Reactive "))
                    .append(coloured(ChatFormatting.DARK_GRAY, "Plot "))
                    .append(coloured(ChatFormatting.RED, "Armor")));
        }
        tooltip.add(Component.empty());
        tooltip.add(coloured(ChatFormatting.GOLD, (stack.getMaxDamage() - stack.getDamageValue()) + " revives left"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(ChatFormatting.GOLD, stack,
                " (" + (stack.getMaxDamage() - stack.getDamageValue()) + " revives left)"));
    }
}
