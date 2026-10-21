package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.interfaces.IArmorModDash;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Attributes;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** 1.7 {@code ItemModCloud} (bottled cloud): a chestplate module with three dashes and +12.5% speed. */
public class ItemModCloud extends ItemArmorMod implements IArmorModDash {

    public ItemModCloud(Item.Properties properties) {
        super(ArmorModHandler.plate_only, false, true, false, false, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.WHITE, "Grants horizontal dashes"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(ChatFormatting.RED, stack, " (Dashes)"));
    }

    @Override
    public void collectModifiers(ItemStack armor, ArmorSlot slot, List<Modifier> modifiers) {
        modifiers.add(new Modifier(Modifier.MOVEMENT_SPEED, 0.125D, Attributes.Op.MULTIPLY_TOTAL));
    }

    @Override
    public int getDashes() {
        return 3;
    }
}
