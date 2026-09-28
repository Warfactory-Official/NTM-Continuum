package com.hbm.items.block;

import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * 1.7 {@code ItemGlyphBlock}: the glyph brick's item names its glyph in a plain grey line. 1.7 read the glyph off
 * the item damage; the port flattens the sixteen glyphs into {@code brick_jungle_glyph_<n>}, so each item carries
 * its own index.
 */
public class ItemGlyphBlock extends CompatBlockItem {

    private static final String[] NAMES = {
            "Hourglass", "Eye", "'Pillar'", "IOI", "Delta", "VTPC", "Cool S", "Trefoil", "Pony", "Sparkle", "PiP",
            "Triangles", "Linux Mint", "13", "Digamma", "Celestial Altar"
    };

    private final int glyph;

    public ItemGlyphBlock(Block block, Properties properties, int glyph) {
        super(block, properties);
        this.glyph = glyph;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        if (glyph >= 0 && glyph < NAMES.length) tooltip.add(Component.literal(NAMES[glyph]));
    }
}
