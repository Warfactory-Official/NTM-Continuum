package com.hbm.items.machine;

import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 1.7 {@code upgrade_radius} and {@code upgrade_health}: plain {@code ItemMachineUpgrade()} instances (type
 * {@code SPECIAL}, tier 0) whose only consumer is the forcefield emitter, and whose tooltip lines were
 * special-cased by identity in {@code ItemMachineUpgrade.addInformation}. The lines are the 1.7 literals.
 */
public class ItemForceFieldUpgrade extends ItemMachineUpgrade {

    private final boolean radius;

    public ItemForceFieldUpgrade(boolean radius, Item.Properties properties) {
        super(UpgradeType.SPECIAL, 0, properties);
        this.radius = radius;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        if (radius) {
            tooltip.add(Component.literal("Forcefield Range Upgrade").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("Radius +16 / Consumption +500"));
        } else {
            tooltip.add(Component.literal("Forcefield Health Upgrade").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("Max. Health +50 / Consumption +250"));
        }
        tooltip.add(Component.literal("Stacks to 16"));
    }
}
