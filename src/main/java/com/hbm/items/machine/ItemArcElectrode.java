package com.hbm.items.machine;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** One fresh arc-furnace electrode variant; the old metadata variants are separate 1.21 items. */
public class ItemArcElectrode extends dev.golem.core.world.CompatItem {

    public enum ElectrodeType {
        GRAPHITE("Graphite Electrode", 10),
        LANTHANIUM("Lanthanium Electrode", 100),
        DESH("Desh Electrode", 500),
        SATURNITE("Saturnite Electrode", 1500);

        public final String displayName;
        public final int durability;

        ElectrodeType(String displayName, int durability) {
            this.displayName = displayName;
            this.durability = durability;
        }
    }

    public final ElectrodeType type;

    public ItemArcElectrode(Item.Properties properties, ElectrodeType type) {
        super(properties);
        this.type = type;
    }

    public static int getDurability(ItemStack stack) {
        return stack.getDamageValue();
    }

    public static int getMaxDurability(ItemStack stack) {
        return stack.getItem() instanceof ItemArcElectrode electrode
                ? electrode.type.durability : 0;
    }

    /** Damages one operating electrode and reports whether it has reached its old wear limit. */
    public static boolean damage(ItemStack stack) {
        int durability = getDurability(stack) + 1;
        stack.setDamageValue(durability);
        return durability >= getMaxDurability(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(type.displayName);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getDurability(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int max = getMaxDurability(stack);
        return max <= 0 ? 0 : Math.max(0, 13 - getDurability(stack) * 13 / max);
    }
}
