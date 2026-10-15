package com.hbm.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.Constants;
import com.hbm.inventory.material.ModMaterialTags;
import com.hbm.inventory.recipes.ModComponentRecipes;
import com.hbm.inventory.recipes.ModCraftingRecipes;
import com.hbm.inventory.recipes.ModShapedRecipes;
import com.hbm.inventory.recipes.ModShapelessRecipes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.gen.CraftingRecipesGenerated;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModItemModels;
import com.hbm.items.special.ItemAutogen;
import com.hbm.lib.ModDamageTypes;
import com.hbm.registration.Reg;
import com.hbm.world.gen.ModBiomeModifiers;
import dev.golem.core.Ids;
import dev.golem.core.dejson.BlockStates;
import dev.golem.core.dejson.Data;
import dev.golem.core.dejson.Dejson;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds each code-declared block model against the reality a unit test cannot reach: the registry,
 * the block's own state definition, and the asset files the declaration is meant to replace.
 *
 * <p>A declaration that misses a state is not a crash: it is an untextured cube, in a world,
 * noticed by a player. These tests turn that into a build failure.
 *
 * <p>The parity tests read the JSON {@code dejsonGenerate} wrote off the classpath, so a match says the
 * declared file was generated, packed, and is loadable at the path the game will look for it. That is the
 * delivery half of the system, and a mistake in the build would break it silently.
 */
public final class DejsonGameTests {

    private DejsonGameTests() {}

    private static void declareAll() {
        ModItemModels.declare();
        ModMaterialTags.declare();
        ModDamageTypes.declare();
        ModBiomeModifiers.declare();
        ModCraftingRecipes.declare();
        ModShapedRecipes.declare();
        ModShapelessRecipes.declare();
        ModComponentRecipes.declare();
        CraftingRecipesGenerated.declare();
    }

    /**
     * Every {@code .tag(ItemTag)} chained on a BLOCK handle names an item that exists. Vanilla rejects a whole item
     * tag file over one unknown required entry and logs nothing a test would see, so this is the loud failure.
     */
    public static void blockItemTagsResolve(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        Set<String> members = Reg.blockItemTagMembers();
        for (String member : members) {
            String id = member.substring(0, member.indexOf(' '));
            if (!BuiltInRegistries.ITEM.containsKey(Ids.parse(id))) missing.add(member);
        }
        Constants.LOG.info("reg_block_item_tags_resolve: {} block-declared item-tag members checked", members.size());
        helper.assertTrue(missing.isEmpty(), missing.size() + " block-declared item-tag members have no item: " + missing);
        helper.succeed();
    }

    public static void coversEveryBlockState(GameTestHelper helper) {
        declareAll();
        List<String> problems = new ArrayList<>();

        int checked = 0;
        int unregistered = 0;

        for (Dejson.Declaration declaration : Dejson.declarations()) {
            Block block = BlockStates.block(declaration.blockId());
            if (block == null) {
                unregistered++;
                continue;
            }
            checked++;
            for (String property : BlockStates.unknownProperties(block, declaration.dispatch())) {
                problems.add(declaration.blockId() + " names property '" + property
                        + "' which the block does not have");
            }
            for (String state : BlockStates.uncovered(block, declaration.dispatch())) {
                problems.add(declaration.blockId() + " has no model for " + state);
            }
        }

        helper.assertTrue(problems.isEmpty(), join(problems));
        helper.assertTrue(checked > 0, "no declared block is registered, so nothing was checked");
        Constants.LOG.info("[DEJSON] state coverage: {} of {} declarations are registered blocks, "
                        + "every state of every one of them is modelled ({} declared for blocks "
                        + "this port has assets for but has not registered)",
                checked, Dejson.declarations().size(), unregistered);
        helper.succeed();
    }

    /**
     * Every material-shape sprite is either computed or authored: nothing is left to a file that was
     * deleted.
     *
     * <p>The build cannot see this failure. {@code dejsonVerify} holds the declarations against
     * the texture tree, but both halves of a material-shape sprite are declarations: the item comes
     * from {@link Mats}, and the remap that draws it comes from the table in {@link ModItemModels}.
     * Add a shape to a material and the item registers, the recipe resolves, and the only symptom is
     * a black-and-magenta square in a creative tab. So the live registry is asked: for every
     * {@link ItemAutogen} that exists, is there something that draws it?
     */
    public static void everyAutogenSpriteIsDrawn(GameTestHelper helper) {
        declareAll();
        List<String> problems = new ArrayList<>();
        Map<String, String> generated = Dejson.generatedSprites();
        int computed = 0;
        int authored = 0;

        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof ItemAutogen autogen)) {
                continue;
            }
            String art = ModAutogenItems.base(autogen.shape);
            String id = art + "_" + autogen.material.name;
            String sprite = Constants.MOD_ID + ":items/" + id;
            boolean isComputed = generated.containsKey(sprite);
            boolean hasFile = DejsonGameTests.class.getResource(
                    "/assets/" + Constants.MOD_ID + "/textures/items/" + ModAutogenItems.spriteOf(id) + ".png") != null;

            if (isComputed && hasFile) {
                // Harmless but worth saying: the file is what the atlas ends up using, and the
                // declaration next to it is then dead weight that reads as if it were in charge.
                problems.add(id + " is both computed and shipped as a file; one of the two is stale");
            } else if (isComputed) {
                computed++;
                if (autogen.material.solidColorLight == autogen.material.solidColorDark) {
                    problems.add(id + " is computed from a flat colour band, which draws a silhouette;"
                            + " it needs an authored sprite the way 1.7's aot() gave bismuth one");
                }
            } else if (hasFile) {
                authored++;
            } else {
                problems.add(id + " has no sprite at all: not computed from " + art
                        + " and no texture file either");
            }
        }

        helper.assertTrue(problems.isEmpty(), join(problems));
        helper.assertTrue(computed > 0, "no material-shape sprite is computed, so nothing was checked");
        Constants.LOG.info("[DEJSON] material-shape sprites: {} computed from {} piece(s) of shape art, "
                        + "{} authored",
                computed, generated.values().stream().distinct().count(), authored);
        helper.succeed();
    }

    public static void matchesShippedBlockStates(GameTestHelper helper) {
        declareAll();
        List<String> problems = new ArrayList<>();
        int compared = 0;

        for (Dejson.Declaration declaration : Dejson.declarations()) {
            JsonObject shipped = read(assetPath(declaration.blockId(), "blockstates"));
            if (shipped == null) {
                continue;
            }
            compared++;
            if (!shipped.equals(Dejson.blockState(declaration.blockId()))) {
                problems.add(declaration.blockId() + " generates a different blockstate than its file");
            }
        }

        helper.assertTrue(problems.isEmpty(), join(problems));
        helper.assertTrue(compared > 0, "no declared block had a shipped blockstate to compare against");
        Constants.LOG.info("[DEJSON] blockstate parity: {} generated blockstates match their file", compared);
        helper.succeed();
    }

    public static void matchesShippedModels(GameTestHelper helper) {
        declareAll();
        List<String> problems = new ArrayList<>();
        int compared = 0;

        for (Dejson.Declaration declaration : Dejson.declarations()) {
            for (Map.Entry<String, JsonObject> model : declaration.models().entrySet()) {
                JsonObject shipped = read(assetPath(model.getKey(), "models"));
                if (shipped == null) {
                    continue;
                }
                compared++;
                if (!normalise(shipped).equals(normalise(model.getValue()))) {
                    problems.add(model.getKey() + " generates a different model than its file");
                }
            }
            if (declaration.hasItem()) {
                JsonObject shipped = read(assetPath(declaration.blockId(), "items"));
                if (shipped != null) {
                    compared++;
                    if (!shipped.equals(Dejson.itemDefinition(declaration.itemId()))) {
                        problems.add(declaration.itemId()
                                + " generates a different item definition than its file");
                    }
                }
            }
        }

        helper.assertTrue(problems.isEmpty(), join(problems));
        helper.assertTrue(compared > 0, "no declared model had a shipped file to compare against");
        Constants.LOG.info("[DEJSON] model parity: {} generated models and item definitions match their file",
                compared);
        helper.succeed();
    }

    /**
     * A legacy-assets marker: a bare parent at {@code hbm:item/<frame>_legacy}, or that parent plus the declared
     * sprite model's own {@code display}, which legacy-assets keeps for a {@code _sprite_legacy} item so the hand
     * poses survive (the meteorite swords).
     */
    private static boolean isLegacyFrameMarker(JsonObject model, JsonObject declared) {
        if (!model.has("parent") || !model.get("parent").isJsonPrimitive()
                || !model.get("parent").getAsString().startsWith(Constants.MOD_ID + ":item/")
                || !model.get("parent").getAsString().endsWith("_legacy")) return false;
        if (model.size() == 1) return true;
        return model.size() == 2 && model.has("display") && declared.has("display")
                && model.get("display").equals(declared.get("display"));
    }

    public static void matchesShippedItems(GameTestHelper helper) {
        declareAll();
        List<String> problems = new ArrayList<>();
        int models = 0;
        int definitions = 0;

        for (Dejson.ItemDeclaration declaration : Dejson.itemDeclarations()) {
            JsonObject shippedModel = read(assetPath(declaration.itemId(), "models"));
            JsonObject generated = declaration.models().get(declaration.itemId());
            // A shares() declaration emits no model of its own; below 1.21.4 legacy-assets derives a bare
            // parent pointer at that path, which is not this declaration's to compare.
            // Below 1.21.4 nothing reads an item definition, so legacy-assets rewrites the model at the item's
            // own path into a bare pointer at its renderer frame's legacy parent (hbm:item/<frame>_legacy) for a
            // custom-renderer item that declared a sprite model there. That file is legacy-assets' product, which
            // the definition comparison below still covers; it is not this declaration saying something else.
            if (shippedModel != null && generated != null && !isLegacyFrameMarker(shippedModel, generated)) {
                models++;
                if (!contains(normalise(shippedModel), normalise(generated))) {
                    problems.add(declaration.itemId() + " is on the classpath saying something "
                            + "other than what it was declared to say");
                }
            }
            JsonObject shippedDefinition = read(assetPath(declaration.definitionId(), "items"));
            if (shippedDefinition != null) {
                definitions++;
                if (!shippedDefinition.equals(Dejson.itemDefinition(declaration.itemId()))) {
                    problems.add(declaration.definitionId()
                            + " generates a different item definition than its file");
                }
            }
        }

        helper.assertTrue(problems.isEmpty(), join(problems));
        helper.assertTrue(models > 0 && definitions > 0,
                "no declared item had a shipped model or definition to compare against");
        Constants.LOG.info("[DEJSON] item parity: {} models and {} definitions match their file",
                models, definitions);
        helper.succeed();
    }

    /**
     * Every declared data file was packed, at a path the target actually reads, saying what it was
     * declared to say.
     *
     * <p>A missing model is a purple cube and somebody notices. A missing loot table is a block
     * that drops nothing, and a missing tag is a recipe that refuses a valid ingredient; both look
     * like a balance decision rather than a broken build, which is why this is a test and not an
     * expectation that someone will spot it.
     *
     * <p>The path is checked in both spellings because the build renames the modern singular
     * registry directories to the pre-1.21 plural ones on the targets that need them.
     *
     * <p>A file under another loader's directory is expected to be missing: each loader's
     * {@code processResources} excludes the other two. What is checked there is that the exclusion
     * took the whole directory, since a half-packed loader tree is a build mistake.
     */
    public static void matchesShippedData(GameTestHelper helper) {
        declareAll();
        List<String> problems = new ArrayList<>();
        Map<String, int[]> loaderTrees = new LinkedHashMap<>();
        int tables = 0;
        int others = 0;
        Map<String, JsonObject> folded = foldedAtlases(Data.files());

        for (Map.Entry<String, JsonObject> declared : Data.files().entrySet()) {
            String path = declared.getKey();
            if (folded.containsKey(path) && folded.get(path) == null) {
                // folded away: legacy-assets must have deleted it after merging its sources into blocks.json
                if (!readAll("/" + path).isEmpty()) {
                    problems.add(path + " was packed on a target with no items atlas; legacy-assets should have "
                            + "folded it into " + BLOCKS_ATLAS + " and removed it");
                }
                others++;
                continue;
            }
            // Every copy on the classpath: a shared tag such as c:glass_panes is also packed by Fabric API,
            // and whichever copy the loader answers first is not necessarily this mod's.
            List<JsonObject> copies = readAll("/" + path);
            if (copies.isEmpty()) {
                copies = readAll("/" + legacy(path));
            }
            JsonObject shipped = copies.isEmpty() ? null : copies.get(0);
            String tree = loaderTree(path);
            if (tree != null) {
                int[] counts = loaderTrees.computeIfAbsent(tree, key -> new int[2]);
                counts[0]++;
                if (shipped == null) {
                    counts[1]++;
                    continue;
                }
            }
            if (shipped == null) {
                problems.add(path + " was declared but no such file was packed");
                continue;
            }
            if (path.contains("/loot_table/")) {
                tables++;
            } else {
                others++;
            }
            //? if <1.21.2 {
            /*// legacy-recipes.gradle.kts rewrites every recipe's ingredient shape for this era, so a packed
            // recipe is expected to differ from its declaration; that it was packed at all is the check.
            if (path.contains("/recipe/")) continue;
            *///?}
            //? if <1.20.5 {
            /*// The same script converts component-era loot tables (crate drops, match_tool predicates) to
            // the NBT shape below 1.20.5, so their packed content is expected to differ too.
            if (path.contains("/loot_table/")) continue;
            *///?}
            JsonElement expected = unwrapped(folded.getOrDefault(path, declared.getValue()));
            if (path.contains("/tags/") && tagMatches(copies, expected)) continue;
            if (copies.stream().noneMatch(copy -> unwrapped(withoutAddedOptionalBiomeTags(path, copy, expected)).equals(expected))) {
                problems.add(path + " is on the classpath saying something other than "
                        + "what it was declared to say");
            }
        }

        loaderTrees.forEach((tree, counts) -> {
            if (counts[1] != 0 && counts[1] != counts[0]) {
                problems.add(tree + " is half packed: " + (counts[0] - counts[1]) + " of "
                        + counts[0] + " files are there, so the loader exclusion missed some");
            }
        });

        helper.assertTrue(problems.isEmpty(), join(problems));
        helper.assertTrue(tables > 0 && others > 0, "no declared data file was found to compare against");
        Constants.LOG.info("[DEJSON] data parity: {} loot tables and {} tags, damage types, recipes "
                + "and biome modifiers match their file; {} loader tree(s) declared",
                tables, others, loaderTrees.size());
        helper.succeed();
    }

    private static final String ITEMS_ATLAS = "assets/minecraft/atlases/items.json";
    private static final String BLOCKS_ATLAS = "assets/minecraft/atlases/blocks.json";

    /**
     * What {@code legacy-assets.gradle.kts} must have made of the declared atlases on a target without an
     * items atlas (below 1.21.4 item sprites are stitched into the blocks atlas, and Core's
     * {@code Sprites.Atlas.ITEMS} names the blocks atlas there). The fold only happens when both files
     * were declared: {@code blocks.json} then carries its own sources followed by every items source it
     * did not already list, with its other keys unchanged, and {@code items.json} is deleted (mapped to
     * {@code null} here, meaning "must not be packed"). Otherwise nothing is folded and the map is empty,
     * so every atlas is compared exactly as declared.
     */
    private static Map<String, JsonObject> foldedAtlases(Map<String, JsonObject> files) {
        Map<String, JsonObject> folded = new LinkedHashMap<>();
        if ("minecraft:items".equals(dev.golem.core.render.Sprites.Atlas.ITEMS.id())) {
            return folded;
        }
        JsonObject items = files.get(ITEMS_ATLAS);
        JsonObject blocks = files.get(BLOCKS_ATLAS);
        if (items == null || blocks == null || !items.has("sources")) {
            return folded;
        }
        JsonObject merged = blocks.deepCopy();
        JsonArray sources = blocks.has("sources") ? blocks.getAsJsonArray("sources").deepCopy() : new JsonArray();
        JsonArray own = sources.deepCopy();
        for (JsonElement source : items.getAsJsonArray("sources")) {
            if (!own.contains(source)) {
                sources.add(source.deepCopy());
            }
        }
        merged.add("sources", sources);
        folded.put(BLOCKS_ATLAS, merged);
        folded.put(ITEMS_ATLAS, null);
        return folded;
    }

    /**
     * An ingredient written the way the target spells it, so the two spellings compare equal.
     *
     * <p>An ingredient is a bare id from 1.21.2 on and a one-key object below it: {@code "hbm:x"}
     * against {@code {"item": "hbm:x"}}, and {@code "#c:x"} against {@code {"tag": "c:x"}}. The
     * declaration is written in the modern spelling and {@code legacyRecipeCompatibility} rewrites
     * it for the targets that need the old one. The declaration is not the authority on that
     * difference, so it is normalised away here.
     */
    /**
     * A packed biome tag less the optional entries its declaration does not have: legacy-biomes.gradle.kts adds the
     * 1.20.1 spelling of each convention biome tag next to the declared one (none are added on 1.21+).
     */
    private static JsonObject withoutAddedOptionalBiomeTags(String path, JsonObject shipped, JsonElement declared) {
        if (!path.contains("/tags/worldgen/biome/") || !shipped.has("values") || !declared.isJsonObject()
                || !declared.getAsJsonObject().has("values")) return shipped;
        JsonArray declaredValues = declared.getAsJsonObject().getAsJsonArray("values");
        JsonArray kept = new JsonArray();
        for (JsonElement value : shipped.getAsJsonArray("values")) {
            boolean optional = value.isJsonObject() && value.getAsJsonObject().has("required")
                    && !value.getAsJsonObject().get("required").getAsBoolean();
            if (optional && !declaredValues.contains(value)) continue;
            kept.add(value);
        }
        JsonObject copy = shipped.deepCopy();
        copy.add("values", kept);
        return copy;
    }

    private static JsonElement unwrapped(JsonElement element) {
        if (element.isJsonArray()) {
            JsonArray unwrapped = new JsonArray();
            for (JsonElement item : element.getAsJsonArray()) {
                unwrapped.add(unwrapped(item));
            }
            return unwrapped;
        }
        if (!element.isJsonObject()) {
            return element;
        }
        JsonObject object = element.getAsJsonObject();
        if (object.size() == 1) {
            JsonElement item = object.get("item");
            if (item != null && item.isJsonPrimitive()) {
                return item;
            }
            JsonElement tag = object.get("tag");
            if (tag != null && tag.isJsonPrimitive()) {
                return new com.google.gson.JsonPrimitive("#" + tag.getAsString());
            }
        }
        JsonObject unwrapped = new JsonObject();
        for (String key : object.keySet()) {
            unwrapped.add(key, unwrapped(object.get(key)));
        }
        return unwrapped;
    }

    /** The {@code data/<ns>/<loader>} tree a path belongs to, or null if it is loader-neutral. */
    private static String loaderTree(String path) {
        String[] parts = path.split("/");
        if (parts.length > 3 && "data".equals(parts[0])
                && ("forge".equals(parts[2]) || "neoforge".equals(parts[2]) || "fabric".equals(parts[2]))) {
            return parts[0] + "/" + parts[1] + "/" + parts[2];
        }
        return null;
    }

    /** The pre-1.21 spelling of a data path: the registry directories were plural. */
    private static String legacy(String path) {
        return path.replace("/loot_table/", "/loot_tables/")
                .replace("/recipe/", "/recipes/")
                .replace("/advancement/", "/advancements/")
                .replace("/structure/", "/structures/")
                .replace("/tags/item/", "/tags/items/")
                .replace("/tags/block/", "/tags/blocks/")
                .replace("/tags/fluid/", "/tags/fluids/");
    }

    /**
     * A problem list long enough to be useless in a test report is truncated. Every one of these
     * checks runs over thousands of declarations, and a systematic mistake fails all of them at
     * once; the first few name it just as well as all of them do.
     */
    private static String join(List<String> problems) {
        if (problems.size() <= 8) {
            return String.join("; ", problems);
        }
        return String.join("; ", problems.subList(0, 8))
                + "; and " + (problems.size() - 8) + " more";
    }

    /**
     * Whether every entry the declaration made survives into the file that was packed.
     *
     * <p>Not equality, because a later build step is allowed to add to a generated model and one
     * here does: below 26.1 {@code legacyItemAssets} appends an {@code overrides} block to the
     * models behind an item-variant {@code select}, which is how an RBMK pellet's ten enrichment
     * skins are drawn on a target with no item-definition layer. The declaration is still the
     * authority on the parent and the textures; it is not the authority on everything in
     * the file.
     */
    private static boolean contains(JsonObject packed, JsonObject declared) {
        for (String key : declared.keySet()) {
            if (!declared.get(key).equals(packed.get(key))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Every enum-backed family whose constants look different really has a model for each.
     *
     * <p>A {@code multiTexture} flag is a claim that nothing holds anyone to: {@code ammo_arty} shipped one
     * sprite for twelve shells and drew the plain one twelve times, through a green build and a green suite.
     * Families are one item per constant, so each member needs its own item definition, and the members of a
     * family must not all resolve to the same one, except the holotapes, which 1.7 drew with one texture. The
     * definition is read off the classpath because that is the file the client resolves.
     */
    public static void variantModelsPerConstant(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        java.util.Map<Class<?>, Set<String>> drawn = new java.util.LinkedHashMap<>();
        java.util.Map<Class<?>, Integer> members = new java.util.LinkedHashMap<>();

        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof com.hbm.items.ItemEnumMulti family)) continue;
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            JsonObject definition = read(assetPath(id, "items"));
            if (definition == null || !definition.has("model")) {
                problems.add(id + " has no item definition on the classpath");
                continue;
            }
            drawn.computeIfAbsent(family.theEnum, k -> new java.util.HashSet<>()).add(definition.get("model").toString());
            members.merge(family.theEnum, 1, Integer::sum);
        }
        for (var entry : members.entrySet()) {
            if (entry.getKey() == com.hbm.items.special.ItemHolotapeImage.EnumHoloImage.class) continue;
            int models = drawn.get(entry.getKey()).size();
            if (models < entry.getValue()) {
                problems.add(entry.getKey().getSimpleName() + " draws its " + entry.getValue() + " items with only "
                        + models + " model(s)");
            }
        }

        helper.assertTrue(problems.isEmpty(), join(problems));
        helper.assertTrue(!members.isEmpty(), "no enum item family was found in the registry");
        Constants.LOG.info("[DEJSON] variant models: {} enum families draw one model per constant", members.size());
        helper.succeed();
    }

    private static String assetPath(String id, String folder) {
        int colon = id.indexOf(':');
        return "/assets/" + id.substring(0, colon) + "/" + folder + "/" + id.substring(colon + 1) + ".json";
    }

    /**
     * Treats a bare {@code block/cube_all} parent as the {@code minecraft:} one it resolves to.
     * Both spellings are on disk; the generator emits the namespaced form.
     */
    private static JsonObject normalise(JsonObject model) {
        JsonObject copy = model.deepCopy();
        JsonElement parent = copy.get("parent");
        if (parent != null && parent.getAsString().indexOf(':') < 0) {
            copy.addProperty("parent", "minecraft:" + parent.getAsString());
        }
        return copy;
    }

    private static List<JsonObject> readAll(String path) {
        List<JsonObject> copies = new ArrayList<>();
        try {
            var urls = DejsonGameTests.class.getClassLoader().getResources(path.substring(1));
            while (urls.hasMoreElements()) {
                try (InputStream stream = urls.nextElement().openStream()) {
                    copies.add(JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                            .getAsJsonObject());
                }
            }
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("could not read " + path, failure);
        }
        if (copies.isEmpty()) {
            JsonObject single = read(path);
            if (single != null) copies.add(single);
        }
        return copies;
    }

    private static JsonObject read(String path) {
        try (InputStream stream = DejsonGameTests.class.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("could not read " + path, failure);
        }
    }

    /**
     * A tag declared by several mods (HBM and an addon both joining {@code minecraft:mineable/pickaxe}) is one merged
     * declaration in game but one file per mod jar, merged by vanilla. It matches when every declared member is in the
     * union of the classpath copies and some copy (the mod's own) holds nothing undeclared.
     */
    private static boolean tagMatches(List<JsonObject> copies, JsonElement expected) {
        if (!(expected instanceof JsonObject declared) || !declared.has("values")) return false;
        java.util.Set<String> wanted = tagValues(declared);
        java.util.Set<String> union = new java.util.HashSet<>();
        boolean ownCopy = false;
        for (JsonObject copy : copies) {
            JsonElement unwrapped = unwrapped(copy);
            if (!(unwrapped instanceof JsonObject object) || !object.has("values")) continue;
            java.util.Set<String> values = tagValues(object);
            union.addAll(values);
            if (wanted.containsAll(values)) ownCopy = true;
        }
        return ownCopy && union.containsAll(wanted);
    }

    private static java.util.Set<String> tagValues(JsonObject tag) {
        java.util.Set<String> values = new java.util.HashSet<>();
        for (JsonElement value : tag.getAsJsonArray("values")) {
            values.add(value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString());
        }
        return values;
    }
}
