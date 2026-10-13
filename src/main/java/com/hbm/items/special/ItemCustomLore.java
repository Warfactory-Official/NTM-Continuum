package com.hbm.items.special;

import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Common-safe (vanilla-only) port of HBM's ItemCustomLore base.
 *
 * <p>The 1.12.2 original resolved a single translation key {@code <descriptionId>.desc}, split its
 * value on {@code '$'}, and printed one tooltip line per fragment. Common code cannot resolve-and-split
 * a single key, so each line has its own key: {@code item.hbm.<id>.desc.0}, {@code .desc.1}, ... The lang
 * generator splits the old {@code '$'}-joined string into these keys (a contiguous run starting at 0).
 *
 * <p>The tooltip appends {@code .desc.0, .desc.1, ...} and stops at the first key absent from the
 * active language ({@link Language#has} is server/common safe).
 *
 * <p>TODO(port): per-item rarity -> {@code Item.Properties.rarity}; enchantment glint for the special
 * items (old {@code hasEffect}) -> {@code DataComponents.ENCHANTMENT_GLINT_OVERRIDE}; the
 * {@code ModItems.undefined} scrambling-name gimmick -> a dedicated special item.
 */
public class ItemCustomLore extends CompatItem {

    public ItemCustomLore(Item.Properties properties) {
        super(properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        String base = this.getDescriptionId() + ".desc.";
        Language language = Language.getInstance();
        for (int i = 0; language.has(base + i); i++) {
            tooltip.add(Component.translatable(base + i));
        }
    }
}
