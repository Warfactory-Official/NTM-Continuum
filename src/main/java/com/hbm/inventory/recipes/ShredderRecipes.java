package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.ModItems;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Shredder recipes — an exact-item lookup {@code input Item → output ItemStack}, with <b>scrap as the
 * universal fallback</b>: 1.7's {@code getShredderResult} returns {@code ModItems.scrap} for any
 * unlisted item, which is why the shredder accepts (and grinds) literally anything.
 *
 * <p>The table is built in two passes, as in 1.7:
 * <ol>
 *   <li><b>Explicit rows</b> ({@code registerDefaults}): the datapack table {@code hbm:shredder}, one file per input
 *       in {@code data/hbm/recipe/shredder/} ({@code {"type": "hbm:shredder", "input": "hbm:scrap", "output":
 *       "hbm:dust"}}; the input may also be a {@code #tag}, which stands for each member). The rows were converted once
 *       from the former Java table (the generated 1.12.2 explicit list, the 1.7 entries restored in fix phase 3, the
 *       tungsten crate and bobblehead, every bedrock ore grade and the modern deepslate counterparts) and are the
 *       source now; {@code gen/ShredderRecipesGenerated} and its generator are retired.</li>
 *   <li><b>The tag sweeps</b> ({@code registerPost}) stay rules: the vanilla log/plank/sapling/wool/terracotta tags
 *       and the old ore-dictionary scan on {@code c:} tags ({@code c:ingots/<mat>}, {@code c:plates/<mat>},
 *       {@code c:gems/<mat>} grind to one {@code c:dusts/<mat>}; ores and raw materials to 2; storage blocks to 9 (4
 *       without an ingot or gem); dusts to generic dust). Because the inputs come from tags, <i>another mod's</i>
 *       iron ingot shreds to HBM iron powder — exactly the cross-mod behaviour the ore dictionary gave 1.7.</li>
 * </ol>
 * Every entry is first-wins (1.7's {@code setRecipe} kept the first): the explicit rows in recipe-id order, then the
 * sweeps. The bobblehead's variant scrap ({@link DecoCrateMachineRecipes#bobbleScrap}) is a rule ahead of the table.
 *
 * <p>The index is built on first use after each reload (or client sync), when the {@code c:} tags are bound and
 * enumerable.
 */
public final class ShredderRecipes {

    private ShredderRecipes() {}

    /** One explicit row: an item or a tag (each member), and what it grinds into. */
    public record Row(Either<TagKey<Item>, Item> input, ItemOut output) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GenericRecipeCodec.INGREDIENT.fieldOf("input").forGetter(Row::input),
            GenericRecipeCodec.ITEM_OUT.fieldOf("output").forGetter(Row::output)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("shredder", () -> CODEC);

    private static volatile Map<Item, ItemStack> recipes;

    static {
        TABLE.changed.register(() -> recipes = null);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    /** The result of shredding {@code stack} — never empty; unlisted items yield scrap. */
    public static ItemStack getShredderResult(ItemStack stack) {
        Map<Item, ItemStack> table = table();
        if (stack.isEmpty()) return new ItemStack(ModItems.scrap.get());
        ItemStack bobble = DecoCrateMachineRecipes.bobbleScrap(stack);
        if (bobble != null) return bobble;
        ItemStack result = table.get(stack.getItem());
        return result != null ? result.copy() : new ItemStack(ModItems.scrap.get());
    }

    /** All entries (explicit rows and sweeps), for JEI/tooling. Keys are input items, values the (shared) outputs. */
    public static Map<Item, ItemStack> getRecipes() {
        return Collections.unmodifiableMap(table());
    }

    /** Builds the index if a reload invalidated it (kept for the existing callers). */
    public static void register() {
        table();
    }

    private static synchronized Map<Item, ItemStack> table() {
        Map<Item, ItemStack> current = recipes;
        if (current != null) return current;
        Map<Item, ItemStack> next = new HashMap<>();
        for (Row row : TABLE.rows()) {
            ItemStack out = row.output().stack();
            row.input().ifLeft(tag -> { for (Item item : itemsOf(tag)) set(next, item, out); })
                    .ifRight(item -> set(next, item, out));
        }
        registerVanillaSweeps(next);
        registerTagSweep(next);
        recipes = next;
        return next;
    }

    /** First-wins, matching the old {@code setRecipe}'s {@code containsKey} guard. */
    private static void set(Map<Item, ItemStack> recipes, Item in, ItemStack out) {
        if (in != null && !out.isEmpty()) recipes.putIfAbsent(in, out);
    }

    /**
     * The old loops over the {@code logWood}/{@code plankWood}/{@code treeSapling} ore-dict lists and the
     * 16 wool / stained-clay metas — now the equivalent vanilla tags, so modded woods work too.
     */
    private static void registerVanillaSweeps(Map<Item, ItemStack> recipes) {
        for (Item item : itemsOf(ItemTags.LOGS)) set(recipes, item, new ItemStack(ModItems.powder_sawdust.get(), 4));
        for (Item item : itemsOf(ItemTags.PLANKS)) set(recipes, item, new ItemStack(ModItems.powder_sawdust.get(), 1));
        for (Item item : itemsOf(ItemTags.SAPLINGS)) set(recipes, item, new ItemStack(Items.STICK, 1));
        for (Item item : itemsOf(ItemTags.WOOL)) set(recipes, item, new ItemStack(Items.STRING, 4));
        for (Item item : itemsOf(ItemTags.TERRACOTTA)) set(recipes, item, new ItemStack(Items.CLAY_BALL, 4));
        // Upstream moved this from registerDefaults into its dustLapis ore-dict loop. Keep it ahead of
        // the generic dust sweep so first-wins still yields cobalt rather than generic dust.
        for (Item item : itemsOf(cTag("dusts/lapis"))) {
            set(recipes, item, new ItemStack(ModItems.powder_cobalt_tiny.get()));
        }
    }

    /**
     * The dust a material's shapes grind into when it isn't {@code c:dusts/<mat>} — the old fork's
     * ore-dict aliases ({@code dustColtan} was {@code powder_coltan_ore}, the crushed-ore intermediate,
     * not {@code powder_coltan}).
     */
    private static final Map<String, String> DUST_OVERRIDES = Map.of(
            "coltan", "powder_coltan_ore",
            // the raw-ore storage blocks are their own tag "material" (c:storage_blocks/raw_iron)
            "raw_iron", "powder_iron", "raw_gold", "powder_gold", "raw_copper", "powder_copper");

    /**
     * The ore-dictionary sweep, on tags. Mirrors the old {@code registerPost} shape-by-shape — and like
     * it, iterates the whole namespace ({@code Registry.getTags()} over {@code c:}), not just HBM's own
     * material list, so vanilla and cross-mod materials get recipes whenever a {@code c:dusts/<mat>}
     * exists.
     */
    private static void registerTagSweep(Map<Item, ItemStack> recipes) {
        // bucket every c:<shape>/<material> item tag by material
        Map<String, Map<String, TagKey<Item>>> byMat = new HashMap<>();
        dev.golem.core.world.Compat.itemTagKeys().forEach(tag -> {
            Identifier id = tag.location();
            int slash = id.getPath().indexOf('/');
            if (!id.getNamespace().equals("c") || slash < 0) return;
            String shape = id.getPath().substring(0, slash);
            String mat = id.getPath().substring(slash + 1);
            byMat.computeIfAbsent(mat, k -> new HashMap<>()).put(shape, tag);
        });

        for (Map.Entry<String, Map<String, TagKey<Item>>> entry : byMat.entrySet()) {
            String mat = entry.getKey();
            Map<String, TagKey<Item>> shapes = entry.getValue();

            String override = DUST_OVERRIDES.get(mat);
            Optional<Item> dust = override != null ? firstOfId(override) : firstOf(cTag("dusts/" + mat));
            if (dust.isPresent()) {
                Item d = dust.get();
                for (String one : new String[]{"ingots", "plates", "gems"}) {
                    if (shapes.containsKey(one)) for (Item item : itemsOf(shapes.get(one))) set(recipes, item, new ItemStack(d, 1));
                }
                for (String two : new String[]{"ores", "raw_materials"}) {
                    if (shapes.containsKey(two)) for (Item item : itemsOf(shapes.get(two))) set(recipes, item, new ItemStack(d, 2));
                }
                if (shapes.containsKey("storage_blocks")) {
                    // 1.7: 9 dust, but 4 when the material has no ingot or gem (redstone, sulfur, niter, ...)
                    int count = shapes.containsKey("ingots") || shapes.containsKey("gems") ? 9 : 4;
                    for (Item item : itemsOf(shapes.get("storage_blocks"))) set(recipes, item, new ItemStack(d, count));
                }
            }
            // (1.7 has no nugget branch: nuggets fall through to scrap. The port's nugget -> tiny dust sweep was removed.)
            // 1.7: "dustTiny*" grinds to generic tiny dust, any other "dust*" to generic dust. A few tiny dusts are
            // also tagged as c:dusts/<x>_tiny; they count as tiny dusts here.
            if (shapes.containsKey("dusts_tiny")) {
                for (Item item : itemsOf(shapes.get("dusts_tiny"))) set(recipes, item, new ItemStack(ModItems.dust_tiny.get(), 1));
            }
            if (shapes.containsKey("dusts")) {
                Item generic = mat.endsWith("_tiny") ? ModItems.dust_tiny.get() : ModItems.dust.get();
                for (Item item : itemsOf(shapes.get("dusts"))) set(recipes, item, new ItemStack(generic, 1));
            }
        }
    }

    // --- tag / registry helpers ---

    private static TagKey<Item> cTag(String path) {
        return TagKey.create(Registries.ITEM, dev.golem.core.Ids.of("c", path));
    }

    /** The items in an item tag; empty when the tag doesn't exist (the {@code doesOreNameExist} guard). */
    private static Iterable<Item> itemsOf(TagKey<Item> tag) {
        List<Item> items = new ArrayList<>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) items.add(holder.value());
        return items;
    }

    /**
     * 1.7 {@code Compat.getPreferredOreOutput}: the tag member from the earliest namespace in {@code hbm.cfg}
     * {@code 1.42_preferredOutputMod}, else the first member ({@code OreDictionary.getOres(name).get(0)}).
     */
    private static Optional<Item> firstOf(TagKey<Item> tag) {
        Item first = null;
        Item preferred = null;
        int lowestPref = -1;
        String[] mods = com.hbm.config.GeneralConfig.preferredOutputMod;
        for (Item item : itemsOf(tag)) {
            if (first == null) first = item;
            String modid = BuiltInRegistries.ITEM.getKey(item).getNamespace();
            for (int i = 0; i < mods.length; i++) {
                if (modid.equals(mods[i])) {
                    if (lowestPref < 0 || i < lowestPref) {
                        preferred = item;
                        lowestPref = i;
                    }
                    break;
                }
            }
        }
        return Optional.ofNullable(preferred != null ? preferred : first);
    }

    /** A direct {@code hbm:<id>} lookup (the tiny dusts have no {@code c:} tag). */
    private static Optional<Item> firstOfId(String path) {
        return BuiltInRegistries.ITEM.getOptional(dev.golem.core.Ids.of("hbm", path));
    }
}
