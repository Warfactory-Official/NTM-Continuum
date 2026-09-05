package com.hbm.inventory.recipes;

import com.hbm.blocks.ModFallingBlocks;
import com.hbm.inventory.recipes.gen.ShredderRecipesGenerated;
import com.hbm.items.ModItems;
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
 * universal fallback</b>: the 1.12.2 {@code getShredderResult} returns {@code ModItems.scrap} for any
 * unlisted item, which is why the shredder accepts (and grinds) literally anything.
 *
 * <p>The original built this table in two passes, and so do we:
 * <ol>
 *   <li><b>Explicit entries</b> ({@code registerDefaults}) — ~175 hand-listed recipes, ported
 *       mechanically to {@link ShredderRecipesGenerated} by {@code port-tools/gen_shredder_recipes.py}
 *       (entries whose items aren't ported yet are skipped and counted; re-run after a content wave).</li>
 *   <li><b>The tag sweep</b> ({@code registerPost}) — the old ore-dictionary scan, here ported onto
 *       the tag system. The original iterated <i>every</i> ore-dict name; we iterate every {@code c:}
 *       item tag (loader-provided conventional tags included, so vanilla redstone/lapis/coal/emerald
 *       and their deepslate variants participate): each item in {@code c:ingots/<mat>},
 *       {@code c:plates/<mat>} and {@code c:gems/<mat>} grinds to one {@code c:dusts/<mat>};
 *       {@code c:ores/<mat>} and {@code c:raw_materials/<mat>} → 2 dust (raw materials are the modern
 *       ore-drop, kept at ore yield so the doubling mechanic survives either route);
 *       {@code c:storage_blocks/<mat>} → 9 dust; {@code c:nuggets/<mat>} → one tiny dust; and any
 *       material dust itself → generic {@link ModItems#dust}. Because the inputs come from tags,
 *       <i>another mod's</i> iron ingot shreds to HBM iron powder — exactly the cross-mod behaviour
 *       the ore dictionary gave the original.</li>
 * </ol>
 * Explicit entries are registered first and win (the old {@code setRecipe} was first-wins, and
 * {@code registerDefaults} ran before {@code registerPost}).
 *
 * <p>Registration is lazy + idempotent (called on the shredder's first tick), like the port's other
 * machine recipe sets — by then the datapack is loaded, so the {@code c:} tags are bound and
 * enumerable.
 */
public final class ShredderRecipes {

    private ShredderRecipes() {}

    private static final Map<Item, ItemStack> recipes = new HashMap<>();
    private static boolean initialized = false;

    /** The result of shredding {@code stack} — never empty; unlisted items yield scrap. */
    public static ItemStack getShredderResult(ItemStack stack) {
        register();
        if (stack.isEmpty()) return new ItemStack(ModItems.scrap.get());
        ItemStack result = recipes.get(stack.getItem());
        return result != null ? result.copy() : new ItemStack(ModItems.scrap.get());
    }

    /** All registered entries, for JEI/tooling. Keys are input items, values the (immutable) outputs. */
    public static Map<Item, ItemStack> getRecipes() {
        register();
        return Collections.unmodifiableMap(recipes);
    }

    /** First-wins, matching the old {@code setRecipe}'s {@code containsKey} guard. */
    private static void set(Item in, ItemStack out) {
        if (in != null && !out.isEmpty()) recipes.putIfAbsent(in, out);
    }

    public static void register() {
        if (initialized) return;
        initialized = true;

        ShredderRecipesGenerated.registerAll(ShredderRecipes::set);   // explicit entries win
        registerModernParity();
        registerVanillaSweeps();
        registerTagSweep();
    }

    /**
     * Modern-only counterparts of explicit 1.12 entries — blocks that didn't exist in 1.12.2 but are
     * variants of ones the old {@code registerDefaults} listed, so they must match those outputs rather
     * than fall through to the tag sweep (deepslate diamond ore would otherwise give diamond powder
     * while stone diamond ore gives diamond gravel).
     */
    private static void registerModernParity() {
        set(Items.DEEPSLATE_DIAMOND_ORE, new ItemStack(ModFallingBlocks.gravel_diamond.get(), 2));
        set(Items.DEEPSLATE, new ItemStack(Items.GRAVEL));
        set(Items.COBBLED_DEEPSLATE, new ItemStack(Items.GRAVEL));
    }

    /**
     * The old loops over the {@code logWood}/{@code plankWood}/{@code treeSapling} ore-dict lists and the
     * 16 wool / stained-clay metas — now the equivalent vanilla tags, so modded woods work too.
     */
    private static void registerVanillaSweeps() {
        for (Item item : itemsOf(ItemTags.LOGS)) set(item, new ItemStack(ModItems.powder_sawdust.get(), 4));
        for (Item item : itemsOf(ItemTags.PLANKS)) set(item, new ItemStack(ModItems.powder_sawdust.get(), 1));
        for (Item item : itemsOf(ItemTags.SAPLINGS)) set(item, new ItemStack(Items.STICK, 1));
        for (Item item : itemsOf(ItemTags.WOOL)) set(item, new ItemStack(Items.STRING, 4));
        for (Item item : itemsOf(ItemTags.TERRACOTTA)) set(item, new ItemStack(Items.CLAY_BALL, 4));
        // Upstream moved this from registerDefaults into its dustLapis ore-dict loop. Keep it ahead of
        // the generic dust sweep so first-wins still yields cobalt rather than generic dust.
        for (Item item : itemsOf(cTag("dusts/lapis"))) {
            set(item, new ItemStack(ModItems.powder_cobalt_tiny.get()));
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
    private static void registerTagSweep() {
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
                    if (shapes.containsKey(one)) for (Item item : itemsOf(shapes.get(one))) set(item, new ItemStack(d, 1));
                }
                for (String two : new String[]{"ores", "raw_materials"}) {
                    if (shapes.containsKey(two)) for (Item item : itemsOf(shapes.get(two))) set(item, new ItemStack(d, 2));
                }
                if (shapes.containsKey("storage_blocks")) {
                    for (Item item : itemsOf(shapes.get("storage_blocks"))) set(item, new ItemStack(d, 9));
                }
            }
            if (shapes.containsKey("nuggets")) {
                Optional<Item> tiny = firstOf(cTag("dusts_tiny/" + mat));
                if (tiny.isEmpty()) tiny = firstOfId("powder_" + mat + "_tiny");
                if (tiny.isPresent()) {
                    for (Item item : itemsOf(shapes.get("nuggets"))) set(item, new ItemStack(tiny.get(), 1));
                }
            }
            // any material dust grinds down into generic dust (the old `name.startsWith("dust")` branch)
            if (shapes.containsKey("dusts")) {
                for (Item item : itemsOf(shapes.get("dusts"))) set(item, new ItemStack(ModItems.dust.get(), 1));
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

    /** The first item in a tag — the tag-system equivalent of {@code OreDictionary.getOres(name).get(0)}. */
    private static Optional<Item> firstOf(TagKey<Item> tag) {
        for (Item item : itemsOf(tag)) return Optional.of(item);
        return Optional.empty();
    }

    /** A direct {@code hbm:<id>} lookup (the tiny dusts have no {@code c:} tag). */
    private static Optional<Item> firstOfId(String path) {
        return BuiltInRegistries.ITEM.getOptional(dev.golem.core.Ids.of("hbm", path));
    }
}
