package com.hbm.items.special;

import com.hbm.items.ModStorageDrumItems;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * One member of the long/short storage-drum waste families: the family ({@link #base()}) and the waste class (1.7's
 * metadata, {@link #variant()}) are the item itself, as in ntm-next.
 */
public final class ItemWasteStorage extends ItemNuclearWaste {

    public enum Family {
        LONG,
        SHORT
    }

    /** 1.7 {@code ItemWasteLong.WasteClass}, in 1.7's order (the order is the 1.7 metadata: QMAW ids, recipe names). */
    public enum LongWasteClass {
        URANIUM235("Uranium-235", 0, 0),
        URANIUM233("Uranium-233", 0, 50),
        NEPTUNIUM("Neptunium-237", 0, 100),
        THORIUM("Thorium-232", 0, 0),
        SCHRABIDIUM("Schrabidium-326", 0, 250);

        public final String displayName;
        public final int liquid;
        public final int gas;

        LongWasteClass(String displayName, int liquid, int gas) {
            this.displayName = displayName;
            this.liquid = liquid;
            this.gas = gas;
        }
    }

    /** 1.7 {@code ItemWasteShort.WasteClass}, in 1.7's order (the order is the 1.7 metadata: QMAW ids, recipe names). */
    public enum ShortWasteClass {
        URANIUM235("Uranium-235", 0, 100),
        URANIUM233("Uranium-233", 50, 100),
        NEPTUNIUM("Neptunium-237", 150, 500),
        PLUTONIUM239("Plutonium-239", 250, 1000),
        PLUTONIUM240("Plutonium-240", 350, 1000),
        PLUTONIUM241("Plutonium-241", 500, 1000),
        AMERICIUM242("Americium-242", 750, 1000),
        SCHRABIDIUM("Schrabidium-326", 1000, 1000);

        public final String displayName;
        public final int liquid;
        public final int gas;

        ShortWasteClass(String displayName, int liquid, int gas) {
            this.displayName = displayName;
            this.liquid = liquid;
            this.gas = gas;
        }
    }

    private final Family family;
    private final String base;
    private final int variant;

    public ItemWasteStorage(Properties properties, Family family, String base, int variant) {
        super(properties);
        this.family = family;
        this.base = base;
        this.variant = variant;
    }

    public Family family() {
        return family;
    }

    /** The family's id, e.g. {@code nuclear_waste_long_tiny}. */
    public String base() {
        return base;
    }

    /** The waste class (1.7 metadata). */
    public int variant() {
        return variant;
    }

    public int variant(ItemStack stack) {
        return variant;
    }

    public String variantName(ItemStack stack) {
        return family == Family.LONG
                ? LongWasteClass.values()[variant(stack)].displayName
                : ShortWasteClass.values()[variant(stack)].displayName;
    }

    /** 1.7 {@code new ItemStack(item, 1, variant)}: the member of the item's family for waste class {@code variant}. */
    public static ItemStack make(Item item, int variant) {
        if (!(item instanceof ItemWasteStorage waste)) {
            throw new IllegalArgumentException("Not a storage-drum waste item: " + item);
        }
        return ModStorageDrumItems.family(waste.base).stack(variant);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(variantName(stack)).withStyle(ChatFormatting.ITALIC));
    }
}
