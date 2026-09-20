package com.hbm.items.tool;

import com.hbm.items.ModItems;
import com.hbm.blocks.ModSecurityContent;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * An item carrying a pin configuration: keys, counterfeit keys and padlocks. Port of 1.7.10's
 * {@code ItemKeyPin}; the old {@code pins} NBT integer is the typed {@link ModSecurityContent#KEY_PINS}
 * component. The counterfeit key ({@code key_fake}) can neither be copied nor re-pinned. Only the
 * keys ({@link ItemKey}) open locks; padlocks ({@code ItemLock}) carry pins but are not {@code KeyPins}.
 */
public class ItemKeyPin extends CompatItem {

    public ItemKeyPin(Properties properties) {
        super(properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        if (pins(stack) != 0)
            tooltip.add(Component.literal("Pin configuration: " + pins(stack)));
        else
            tooltip.add(Component.literal("Pins not set!"));

        if (this == ModItems.key_fake.get()) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("Pins can neither be changed, nor copied."));
        }
    }

    /** The old static {@code getPins}; renamed so {@link ItemKey} can offer the instance-side {@code KeyPins.getPins}. */
    public static int pins(ItemStack stack) {
        return ModSecurityContent.KEY_PINS.getOr(stack, 0);
    }

    public static void setPins(ItemStack stack, int i) {
        ModSecurityContent.KEY_PINS.set(stack, i);
    }

    public boolean canTransfer() {
        return this != ModItems.key_fake.get();
    }
}
