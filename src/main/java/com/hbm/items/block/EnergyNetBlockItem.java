package com.hbm.items.block;

import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Function;

/**
 * A block item carrying the old {@code ITooltipProvider} lines: {@code addStandardInfo}'s
 * {@code block.hbm.<id>.desc.N} lines in yellow (one per 1.7 {@code $} split, always shown, as the rest of
 * the port does) plus whatever {@code addInformation} literally wrote.
 */
public class EnergyNetBlockItem extends CompatBlockItem {

    private final int descLines;
    private final Function<ItemStack, List<Component>> lines;

    public EnergyNetBlockItem(Block block, Properties properties, int descLines, Function<ItemStack, List<Component>> lines) {
        super(block, properties);
        this.descLines = descLines;
        this.lines = lines;
    }

    public EnergyNetBlockItem(Block block, Properties properties, int descLines) {
        this(block, properties, descLines, stack -> List.of());
    }

    /** {@code GOLD label + YELLOW value}, the pylon tooltip shape. */
    public static Component pair(String label, String value) {
        return Component.literal(label).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(value).withStyle(ChatFormatting.YELLOW));
    }

    public static Component gold(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GOLD);
    }

    /** The pylon family's two lines: connection type and range. */
    public static Function<ItemStack, List<Component>> pylon(String type, String range) {
        return stack -> List.of(pair("Connection Type: ", type), pair("Connection Range: ", range));
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        String id = BuiltInRegistries.BLOCK.getKey(getBlock()).getPath();
        for (int i = 0; i < descLines; i++) {
            tooltip.add(Component.translatable("block.hbm." + id + ".desc." + i).withStyle(ChatFormatting.YELLOW));
        }
        tooltip.addAll(lines.apply(stack));
    }
}
