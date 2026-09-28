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
 * 1.7 {@code ItemModTwoKick} (Ballistic Gauntlet): bare-handed punches fire 12 gauge shells from the
 * wearer's inventory. The shot is {@code ArmorModEvents.onPlayerAttack}, upstream's {@code onPlayerPunch}.
 */
public class ItemModTwoKick extends ItemArmorMod {

    public ItemModTwoKick(Item.Properties properties) {
        super(ArmorModHandler.servos, false, true, false, false, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\"I've had worse\"").withStyle(ChatFormatting.ITALIC));
        tooltip.add(coloured(ChatFormatting.YELLOW, "Punches fire 12 gauge shells"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(ChatFormatting.YELLOW, stack, " (Shotgun punches)"));
    }
}
