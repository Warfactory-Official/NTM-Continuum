package com.hbm.blocks.network;

import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/** 1.7's {@code ColorUtil.getColorFromDye}: the dye ore-dictionary colours, looked up through the {@code c:dyes/*} tags. */
public final class PylonDyes {

    private PylonDyes() {}

    private static final Map<DyeColor, Integer> NAME_TO_COLOR = new EnumMap<>(DyeColor.class);

    static {
        NAME_TO_COLOR.put(DyeColor.BLACK, 1973019);
        NAME_TO_COLOR.put(DyeColor.RED, 11743532);
        NAME_TO_COLOR.put(DyeColor.GREEN, 3887386);
        NAME_TO_COLOR.put(DyeColor.BROWN, 5320730);
        NAME_TO_COLOR.put(DyeColor.BLUE, 2437522);
        NAME_TO_COLOR.put(DyeColor.PURPLE, 8073150);
        NAME_TO_COLOR.put(DyeColor.CYAN, 2651799);
        NAME_TO_COLOR.put(DyeColor.LIGHT_GRAY, 11250603);
        NAME_TO_COLOR.put(DyeColor.GRAY, 4408131);
        NAME_TO_COLOR.put(DyeColor.PINK, 14188952);
        NAME_TO_COLOR.put(DyeColor.LIME, 4312372);
        NAME_TO_COLOR.put(DyeColor.YELLOW, 14602026);
        NAME_TO_COLOR.put(DyeColor.LIGHT_BLUE, 6719955);
        NAME_TO_COLOR.put(DyeColor.MAGENTA, 12801229);
        NAME_TO_COLOR.put(DyeColor.ORANGE, 15435844);
        NAME_TO_COLOR.put(DyeColor.WHITE, 15790320);
    }

    /** The dye colour of {@code stack}, or 0 when it is not a dye. */
    public static int colorFromDye(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        for (DyeColor color : DyeColor.values()) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, Ids.of("c", "dyes/" + color.getName()));
            if (stack.is(tag)) return NAME_TO_COLOR.getOrDefault(color, 0);
        }
        return 0;
    }
}
