package com.hbm.items.special;

import java.util.function.Consumer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Common-safe (vanilla-only) port of HBM's ItemCustomLore base.
 *
 * <p>The 1.12.2 original resolved a single translation key {@code <descriptionId>.desc}, split its
 * value on {@code '$'}, and printed one tooltip line per fragment. In 26.1 we can't resolve-and-split
 * a single key in common code, so each line gets its own key instead:
 * {@code item.hbm.<id>.desc.0}, {@code .desc.1}, ... The lang generator splits the old
 * {@code '$'}-joined string into these per-line keys (a contiguous run starting at 0).
 *
 * <p>At runtime we append {@code .desc.0, .desc.1, ...} and stop at the first key absent from the
 * active language ({@link Language#has} is server/common safe).
 *
 * <p>TODO(port): per-item rarity -> {@code Item.Properties.rarity}; enchantment glint for the special
 * items (old {@code hasEffect}) -> {@code DataComponents.ENCHANTMENT_GLINT_OVERRIDE}; the
 * {@code ModItems.undefined} scrambling-name gimmick -> a dedicated special item.
 */
public class ItemCustomLore extends Item {

    public ItemCustomLore(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag flag) {
        String base = this.getDescriptionId() + ".desc.";
        Language language = Language.getInstance();
        for (int i = 0; language.has(base + i); i++) {
            adder.accept(Component.translatable(base + i));
        }
    }
}
