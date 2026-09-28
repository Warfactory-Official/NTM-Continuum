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

/** 1.7 {@code ItemModHealth}: heart pieces, containers, boosters, the Heart of Darkness and the Black Diamond. */
public class ItemModHealth extends ItemArmorMod {

    private final float health;
    private final boolean nostalgia;

    public ItemModHealth(float health, boolean nostalgia, Item.Properties properties) {
        super(ArmorModHandler.extra, false, true, false, false, properties);
        this.health = health;
        this.nostalgia = nostalgia;
    }

    private static ChatFormatting colour() {
        return System.currentTimeMillis() % 1000 < 500 ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE;
    }

    /** Upstream's {@code Math.round(health * 10) * 0.1}, double arithmetic and all. */
    private String amount() {
        return String.valueOf(Math.round(health * 10) * 0.1);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(colour(), "+" + amount() + " health"));
        tooltip.add(Component.empty());
        if (nostalgia) {
            tooltip.add(coloured(ChatFormatting.DARK_GRAY, "Nostalgia"));
            tooltip.add(Component.empty());
        }
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(colour(), stack, " (+" + amount() + " health)"));
    }

    @Override
    public void collectModifiers(ItemStack armor, ArmorSlot slot, List<Modifier> modifiers) {
        modifiers.add(new Modifier(Modifier.MAX_HEALTH, health, Attributes.Op.ADD));
    }
}
