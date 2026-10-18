package com.hbm.items.tool;

import java.util.function.BiConsumer;
import net.minecraft.world.item.ItemStack;

/**
 * The client-only screens of the utility tools and books (1.7 {@code IGUIProvider.provideGUI} with no
 * container), opened from the item's client-side use. The client registrations install the opener, so no
 * screen class is named from common code.
 */
public final class UtilityToolScreens {

    public enum Kind { PAGER, RTTY_DESIGNATOR, FLUID_MULTI, GUIDE, BOOK_LORE, HOLOTAPE, CLAY_TABLET }

    private static BiConsumer<Kind, ItemStack> opener = (kind, stack) -> { };

    private UtilityToolScreens() {}

    public static void setClientOpener(BiConsumer<Kind, ItemStack> open) {
        opener = open;
    }

    public static void open(Kind kind, ItemStack stack) {
        opener.accept(kind, stack);
    }
}
