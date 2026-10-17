package com.hbm.blocks;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/**
 * 1.7 {@code ITooltipProvider.addStandardInfo} and the physical left Shift poll that gates every hidden tooltip
 * line. Common code cannot reach the keyboard, so the client installs the poll; elsewhere Shift reads as up.
 */
public final class StandardInfo {

    private static volatile BooleanSupplier shiftHeld = () -> false;

    private StandardInfo() {}

    public static void setShiftHeldPoll(BooleanSupplier poll) {
        shiftHeld = Objects.requireNonNull(poll);
    }

    /** 1.7 {@code Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)}. */
    public static boolean shiftHeld() {
        return shiftHeld.getAsBoolean();
    }

    /**
     * The {@code key + ".N"} lines (1.7's {@code $} splits of {@code key}) in yellow while Shift is held, else the
     * hint. A missing key prints itself, as 1.7's {@code I18nUtil.resolveKeyArray} did.
     */
    public static void add(List<Component> tooltip, String key) {
        if (shiftHeld()) addLines(tooltip, key);
        else tooltip.add(holdShift());
    }

    /** The yellow {@code key + ".N"} lines without the Shift gate. */
    public static void addLines(List<Component> tooltip, String key) {
        Language language = Language.getInstance();
        if (!language.has(key + ".0")) {
            tooltip.add(Component.literal(key).withStyle(ChatFormatting.YELLOW));
            return;
        }
        for (int i = 0; language.has(key + "." + i); i++) {
            tooltip.add(Component.translatable(key + "." + i).withStyle(ChatFormatting.YELLOW));
        }
    }

    /** {@code Hold <LSHIFT> to display more info}, dark grey italic with a yellow key name. */
    public static Component holdShift() {
        return holdShift("more info");
    }

    /** {@code Hold <LSHIFT> to display <what>}. */
    public static Component holdShift(String what) {
        return Component.literal("Hold <").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
                .append(Component.literal("LSHIFT").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC))
                .append(Component.literal("> to display " + what)
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
