package com.hbm.util;

import java.util.List;

import com.hbm.Constants;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7's GuiContainer prefixed every tooltip line after the name with GRAY, so item descriptions written without a
 * colour show gray there and white on modern versions. Lines and parts with their own colour keep it.
 */
public final class TooltipColours {

    private TooltipColours() {}

    /** Grays the uncoloured lines after the name of an HBM item's tooltip. */
    public static void apply(ItemStack stack, List<Component> lines) {
        if (Constants.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())) grayFrom(lines, 1);
    }

    /** Grays every line from {@code from} on whose root style has no colour. */
    public static void grayFrom(List<Component> lines, int from) {
        for (int i = from; i < lines.size(); i++) {
            Component line = lines.get(i);
            if (line.getStyle().getColor() == null) lines.set(i, line.copy().withStyle(ChatFormatting.GRAY));
        }
    }
}
