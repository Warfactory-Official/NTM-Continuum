package com.hbm.gametest;

import com.hbm.api.data.ArmorDataMaps;
import dev.golem.core.Ids;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The full set a chestplate is in, from the {@code hbm:armor_sets} data map (worn stacks; an absent piece is empty). */
final class SetSource {

    private SetSource() {}

    static ItemStack[] find(Item chest) {
        ArmorDataMaps.ArmorSet set = ArmorDataMaps.ARMOR_SETS.get(chest);
        if (set == null) return null;
        return new ItemStack[] {stack(set.helmet()), new ItemStack(chest), stack(set.legs()), stack(set.boots())};
    }

    private static ItemStack stack(Optional<String> id) {
        return id.map(s -> new ItemStack(BuiltInRegistries.ITEM.getValue(Ids.parse(s))))
                .orElse(ItemStack.EMPTY);
    }
}
