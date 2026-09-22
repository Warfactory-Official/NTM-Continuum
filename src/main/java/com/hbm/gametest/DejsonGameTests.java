package com.hbm.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.Constants;
import com.hbm.blocks.ModBlockDrops;
import com.hbm.blocks.ModBlockModels;
import com.hbm.inventory.material.ModMaterialTags;
import com.hbm.inventory.recipes.ModComponentRecipes;
import com.hbm.inventory.recipes.ModCraftingRecipes;
import com.hbm.inventory.recipes.ModShapedRecipes;
import com.hbm.inventory.recipes.ModShapelessRecipes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModItemModels;
import com.hbm.items.special.ItemAutogen;
import com.hbm.lib.ModDamageTypes;
import com.hbm.world.gen.ModBiomeModifiers;
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
 * <p>A declaration that misses a state is not a crash — it is an untextured cube, in a world,
 * noticed by a player. These tests are what turns that into a build failure, and they were the
 * precondition for deleting the files the declarations replaced.
 *
 * <p>Those files are now gone, which changes what the parity tests prove rather than retiring them.
 * The JSON they read off the classpath is the JSON {@code dejsonGenerate} wrote, so a match no
 * longer says "the declaration agrees with what a person wrote" — it says the declared file was
 * generated, packed, and is loadable at the path the game will look for it. That is the delivery
 * half of the system, and it is the half a mistake in the build would break silently.
 */
public final class DejsonGameTests {

    private DejsonGameTests() {}

    private static void declareAll() {
        ModBlockModels.declare();
        com.hbm.blocks.RailDeclarations.declare();
        ModItemModels.declare();
        ModBlockDrops.declare();
        ModMaterialTags.declare();
        ModDamageTypes.declare();
        ModBiomeModifiers.declare();
        ModCraftingRecipes.declare();
        ModShapedRecipes.declare();
        ModShapelessRecipes.declare();
        ModComponentRecipes.declare();
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
     * Every material-shape sprite is either computed or authored — nothing is left to a file that was
     * deleted.
     *
     * <p>This is the failure the build cannot see. {@code dejsonVerify} holds the declarations against
     * the texture tree, but both halves of a material-shape sprite are declarations: the item comes
     * from {@link Mats}, and the remap that draws it comes from the table in {@link ModItemModels}.
     * Add a shape to a material and the item registers, the recipe resolves, and the only symptom is
     * a black-and-magenta square in a creative tab. So the question is asked of the live registry:
     * for every {@link ItemAutogen} that actually exists, is there something that draws it?
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
                    "/assets/" + Constants.MOD_ID + "/textures/items/" + id + ".png") != null;

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
            if (shippedModel != null && generated != null) {
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
     * that drops nothing, and a missing tag is a recipe that refuses a valid ingredient — both look
     * like a balance decision rather than a broken build, which is why this is a test and not an
     * expectation that someone will spot it.
     *
     * <p>The path is checked in both spellings because the build renames the modern singular
     * registry directories to the pre-1.21 plural ones on the targets that need them, after the
     * generated and authored files have become the same pile of files.
     *
     * <p>A file under another loader's directory is <em>meant</em> to be missing here: each loader's
     * {@code processResources} excludes the other two, so the jar carries no inert files. What is
     * checked there is that the exclusion took the whole directory — a loader tree that is half
     * packed is a build mistake, and an empty one is not.
     */
    public static void matchesShippedData(GameTestHelper helper) {
        declareAll();
        List<String> problems = new ArrayList<>();
        Map<String, int[]> loaderTrees = new LinkedHashMap<>();
        int tables = 0;
        int others = 0;

        for (Map.Entry<String, JsonObject> declared : Data.files().entrySet()) {
            String path = declared.getKey();
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
            JsonElement expected = unwrapped(declared.getValue());
            if (copies.stream().noneMatch(copy -> unwrapped(copy).equals(expected))) {
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

    /**
     * An ingredient written the way the target spells it, so the two spellings compare equal.
     *
     * <p>An ingredient is a bare id from 1.21.2 on and a one-key object below it — {@code "hbm:x"}
     * against {@code {"item": "hbm:x"}}, and {@code "#c:x"} against {@code {"tag": "c:x"}}. The
     * declaration is written in the modern spelling and {@code legacyRecipeCompatibility} rewrites
     * it for the targets that need the old one, exactly as it does for the recipes that are still
     * files. That is a real difference in the file and not one the declaration is the authority on,
     * so it is normalised away here rather than asserted either way.
     */
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
     * authority on the parent and the textures; it simply is not the authority on everything in
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
     * Every enum-backed family that says its constants look different really has a model for each.
     *
     * <p>{@code multiTexture} is a claim about art and nothing held anyone to it: {@code ammo_arty}
     * shipped one sprite for twelve shells and drew the plain one twelve times, through a green build
     * and a green suite. The definition is read off the classpath rather than off a declaration
     * because that is the file the client resolves, whichever of the two put it there.
     */
    public static void variantModelsPerConstant(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        int families = 0;

        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof com.hbm.items.ItemEnumMulti family) || !family.multiTexture) continue;
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            JsonObject definition = read(assetPath(id, "items"));
            if (definition == null) {
                problems.add(id + " has no item definition on the classpath");
                continue;
            }
            families++;
            int constants = family.theEnum.getEnumConstants().length;
            List<String> drawn = new ArrayList<>();
            for (int variant = 0; variant < constants; variant++) {
                String model = selectedModel(definition, variant);
                if (model == null) {
                    problems.add(id + " does not select a model for variant " + variant
                            + " of " + constants);
                    break;
                }
                drawn.add(model);
            }
            if (drawn.size() == constants && Set.copyOf(drawn).size() < constants) {
                problems.add(id + " draws its " + constants + " variants with only "
                        + Set.copyOf(drawn).size() + " model(s)");
            }
        }

        helper.assertTrue(problems.isEmpty(), join(problems));
        helper.assertTrue(families > 0, "no multi-texture enum family was found in the registry");
        Constants.LOG.info("[DEJSON] variant models: {} enum families draw one model per constant",
                families);
        helper.succeed();
    }

    /** The model an {@code hbm:item_variant} definition resolves a variant to, or null if it has none. */
    private static String selectedModel(JsonObject definition, int variant) {
        JsonObject model = definition.getAsJsonObject("model");
        if (model == null || !model.has("type")
                || !"minecraft:select".equals(model.get("type").getAsString())
                || !model.has("property")
                || !"hbm:item_variant".equals(model.get("property").getAsString())) {
            return null;
        }
        JsonArray cases = model.getAsJsonArray("cases");
        if (cases != null) {
            for (JsonElement element : cases) {
                JsonObject entry = element.getAsJsonObject();
                if (entry.has("when") && entry.get("when").getAsString().equals(String.valueOf(variant))) {
                    return entry.get("model").toString();
                }
            }
        }
        JsonElement fallback = model.get("fallback");
        return fallback == null ? null : fallback.toString();
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
}
