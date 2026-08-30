package com.hbm.inventory.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Localized gas-centrifuge info tooltips, kept free of client-only screen classes for testing. */
public final class GasCentTooltips {

    private GasCentTooltips() {}

    public static List<Component> enrichment() {
        return lines("desc.gui.gasCent.enrichment", ChatFormatting.DARK_GREEN, 4);
    }

    public static List<Component> output() {
        return lines("desc.gui.gasCent.output", ChatFormatting.GOLD, 2);
    }

    private static List<Component> lines(String key, ChatFormatting headingStyle, int bodyLines) {
        List<Component> lines = new ArrayList<>(bodyLines + 1);
        lines.add(Component.translatable(key + ".heading").withStyle(headingStyle));
        for (int i = 0; i < bodyLines; i++) {
            lines.add(Component.translatable(key + ".body." + i));
        }
        return lines;
    }
}
