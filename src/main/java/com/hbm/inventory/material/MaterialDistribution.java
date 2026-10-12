package com.hbm.inventory.material;

import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.hbm.inventory.recipes.loader.MaterialCodecs;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * What an item consists of in molten material (1.7 {@code MatDistribution}, a {@code SerializableRecipe} in 1.7 too):
 * the datapack table {@code hbm:material_distribution}, one file per item or tag in
 * {@code data/hbm/recipe/material_distribution/}:
 *
 * <pre>
 * {"type": "hbm:material_distribution", "input": "minecraft:obsidian", "materials": [{"material": "obsidian", "amount": 648}]}
 * {"type": "hbm:material_distribution", "input": "#c:ores/iron", "materials": [{"material": "iron", "amount": 144}, ...]}
 * </pre>
 *
 * The crucible, the foundry and the arc furnace read it through {@link Mats#getMaterialsFromItem}: a tag row (1.7
 * {@code registerOre}) is checked against the item's tags before the {@code c:<shape>/<material>} scan and replaces it,
 * an item row (1.7 {@code registerEntry}) adds to whatever the tags gave. A second row for the same item or tag replaces
 * the first in recipe-id order (1.7's {@code put}). The rows were converted once from the former Java table
 * ({@code Mats.ensureSmeltingEntries}, 1.7 {@code registerDefaults} restricted to ported content); the shape scan,
 * autogen items and scraps stay rules in {@link Mats}.
 */
public final class MaterialDistribution {

    private MaterialDistribution() {}

    /** One row: an item or a tag and the material it melts into, in quanta. */
    public record Row(Either<TagKey<Item>, Item> input, List<Mats.MaterialStack> materials) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GenericRecipeCodec.INGREDIENT.fieldOf("input").forGetter(Row::input),
            MaterialCodecs.STACK.listOf().fieldOf("materials").forGetter(Row::materials)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("material_distribution", () -> CODEC);

    private record Index(Map<Item, List<Mats.MaterialStack>> items, Map<String, List<Mats.MaterialStack>> tags) {}

    private static volatile Index index;

    static {
        TABLE.changed.register(() -> index = null);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static Index index() {
        Index current = index;
        if (current != null) return current;
        Map<Item, List<Mats.MaterialStack>> items = new LinkedHashMap<>();
        Map<String, List<Mats.MaterialStack>> tags = new LinkedHashMap<>();
        for (Row row : TABLE.rows()) {
            List<Mats.MaterialStack> materials = List.copyOf(row.materials());
            row.input().ifLeft(tag -> tags.put(tag.location().toString(), materials))
                    .ifRight(item -> items.put(item, materials));
        }
        current = new Index(Map.copyOf(items), Map.copyOf(tags));
        index = current;
        return current;
    }

    /** The item rows (1.7 {@code registerEntry}); callers copy the stacks they hand out. */
    public static Map<Item, List<Mats.MaterialStack>> items() {
        return index().items();
    }

    /** The tag rows (1.7 {@code registerOre}), keyed by full tag id ({@code c:ores/iron}). */
    public static Map<String, List<Mats.MaterialStack>> tags() {
        return index().tags();
    }
}
