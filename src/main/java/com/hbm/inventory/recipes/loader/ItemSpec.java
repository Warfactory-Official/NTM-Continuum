package com.hbm.inventory.recipes.loader;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.world.ItemStacks;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * An item stack as a recipe row states it: item, count and {@link ItemData}, held as such until the table rebuilds
 * ({@link #stack}); 26.1 decodes recipes before item components exist, so a row never builds an {@link ItemStack}
 * while it is read. JSON: {@code "id"}, or {@code {"id", "count", "item_data"}}.
 */
public record ItemSpec(Item item, int count, ItemData data) {

    public static final MapCodec<ItemSpec> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ItemStacks.itemCodec().fieldOf("id").forGetter(ItemSpec::item),
            Codec.INT.optionalFieldOf("count", 1).forGetter(ItemSpec::count),
            ItemData.CODEC.optionalFieldOf("item_data").forGetter(spec -> spec.data().isEmpty()
                    ? Optional.empty() : Optional.of(spec.data()))
    ).apply(i, (item, count, data) -> new ItemSpec(item, count, data.orElse(ItemData.NONE))));

    /** {@code "id"} for one plain item, else the object. */
    public static final Codec<ItemSpec> CODEC = Codec.either(ItemStacks.itemCodec(), MAP_CODEC.codec()).xmap(
            either -> either.map(item -> new ItemSpec(item, 1, ItemData.NONE), spec -> spec),
            spec -> spec.count() == 1 && spec.data().isEmpty() ? Either.left(spec.item()) : Either.right(spec));

    public ItemStack stack() {
        return data.apply(new ItemStack(item, count));
    }

    /** {@code stack} in this form, or an error when its data cannot be stated ({@link ItemData#of}). */
    public static DataResult<ItemSpec> of(ItemStack stack) {
        return ItemData.of(stack).map(data -> new ItemSpec(stack.getItem(), stack.getCount(), data));
    }

    /** {@code namespace:path x count {data}}, for {@link GenericRecipeCodec#describe}. */
    public String describe() {
        return BuiltInRegistries.ITEM.getKey(item) + "x" + count + data.describe();
    }
}
