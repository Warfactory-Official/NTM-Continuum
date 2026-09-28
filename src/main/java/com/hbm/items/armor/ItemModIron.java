package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Attributes;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** 1.7 {@code ItemModIron} (iron cladding): +0.5 knockback resistance per piece. */
public class ItemModIron extends ItemArmorMod {

    public ItemModIron(Item.Properties properties) {
        super(ArmorModHandler.cladding, true, true, true, true, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.WHITE, "+0.5 knockback resistance"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        // Upstream's installed line spells it "resistence".
        tooltip.add(modLine(ChatFormatting.WHITE, stack, " (+0.5 knockback resistence)"));
    }

    @Override
    public void collectModifiers(ItemStack armor, ArmorSlot slot, List<Modifier> modifiers) {
        modifiers.add(new Modifier(Modifier.KNOCKBACK_RESISTANCE, 0.5D, Attributes.Op.ADD));
    }
}
