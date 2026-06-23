package com.hbm.inventory.gui;

import com.hbm.lib.Library;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class GuiUtil {

    private GuiUtil() {}

    public static List<Component> energyTooltip(long power, long maxPower) {
        return List.of(Component.literal(Library.getShortNumber(power) + "HE/" + Library.getShortNumber(maxPower) + "HE"));
    }

    public static List<Component> energyTooltip(long power, long maxPower, long delta) {
        List<Component> list = new ArrayList<>();
        list.add(Component.literal(Library.getShortNumber(power) + "HE/" + Library.getShortNumber(maxPower) + "HE"));
        if (delta > 0) {
            list.add(Component.literal("+" + Library.getShortNumber(delta) + "HE/s").withStyle(ChatFormatting.GREEN));
        } else if (delta < 0) {
            list.add(Component.literal("-" + Library.getShortNumber(Math.abs(delta)) + "HE/s").withStyle(ChatFormatting.RED));
        } else {
            list.add(Component.literal("0HE/s").withStyle(ChatFormatting.YELLOW));
        }
        return list;
    }
}
