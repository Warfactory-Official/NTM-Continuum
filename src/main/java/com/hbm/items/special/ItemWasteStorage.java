package com.hbm.items.special;

import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** The metadata-backed long/short waste families used by the legacy storage drum. */
public final class ItemWasteStorage extends ItemNuclearWaste {

    public enum Family {
        LONG,
        SHORT
    }

    public enum LongWasteClass {
        THORIUM("Thorium-232", 0, 0),
        URANIUM233("Uranium-233", 0, 50),
        URANIUM235("Uranium-235", 0, 0),
        NEPTUNIUM("Neptunium-237", 0, 100),
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

    public enum ShortWasteClass {
        URANIUM233("Uranium-233", 50, 100),
        URANIUM235("Uranium-235", 0, 100),
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

    public ItemWasteStorage(Properties properties, Family family) {
        super(properties);
        this.family = family;
    }

    public Family family() {
        return family;
    }

    public int variant(ItemStack stack) {
        int length = family == Family.LONG ? LongWasteClass.values().length : ShortWasteClass.values().length;
        return rectify(ModDataComponents.storageWasteClass(stack), length);
    }

    public String variantName(ItemStack stack) {
        return family == Family.LONG
                ? LongWasteClass.values()[variant(stack)].displayName
                : ShortWasteClass.values()[variant(stack)].displayName;
    }

    /** Makes the modern equivalent of a metadata-bearing old waste stack. */
    public static ItemStack make(Item item, int variant) {
        if (!(item instanceof ItemWasteStorage waste)) {
            throw new IllegalArgumentException("Not a storage-drum waste item: " + item);
        }
        ItemStack stack = new ItemStack(item);
        int length = waste.family == Family.LONG ? LongWasteClass.values().length : ShortWasteClass.values().length;
        ModDataComponents.storageWasteClass(stack, rectify(variant, length));
        return stack;
    }

    private static int rectify(int variant, int length) {
        return Math.abs(variant) % length;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(variantName(stack)).withStyle(ChatFormatting.ITALIC));
    }
}
