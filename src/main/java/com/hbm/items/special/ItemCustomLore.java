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
 * <p>1.7's {@code setRarity} is {@code Item.Properties.rarity} at registration; {@link #setEffect} is its glint.
 * TODO(items): the {@code ModItems.undefined} scrambling-name gimmick.
 */
public class ItemCustomLore extends CompatItem {

    private boolean hasEffect;

    public ItemCustomLore(Item.Properties properties) {
        super(properties);
    }

    public ItemCustomLore setEffect() {
        this.hasEffect = true;
        return this;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasEffect || super.isFoil(stack);
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
