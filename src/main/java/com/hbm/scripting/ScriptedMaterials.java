package com.hbm.scripting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.Constants;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.hazard.HazardData;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.type.IHazardType;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModAutogenItems;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.script.GolemScriptPlugin;
import dev.golem.core.world.BiomeFeatures;
import dev.golem.core.world.CompatDropExperienceBlock;
import dev.golem.core.world.DataPaths;
import dev.golem.core.world.LootJson;
import dev.golem.core.world.RecipeJson;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.hbm.inventory.material.MaterialShapes.BILLET;
import static com.hbm.inventory.material.MaterialShapes.BLOCK;
import static com.hbm.inventory.material.MaterialShapes.CRYSTAL;
import static com.hbm.inventory.material.MaterialShapes.DUST;
import static com.hbm.inventory.material.MaterialShapes.INGOT;
import static com.hbm.inventory.material.MaterialShapes.NUGGET;
import static com.hbm.inventory.material.MaterialShapes.ORE;
import static com.hbm.inventory.material.MaterialShapes.PLATE;

/**
 * Items, blocks, ores and radiation {@link MaterialsEvent} declares for scripted materials, minted once the
 * event has run. Sprites without an authored texture are recoloured from a built-in material's art.
 */
public final class ScriptedMaterials {

    private ScriptedMaterials() {}

    /** A sprite {@code <source>_<material>} recoloured from {@code band} onto the material's colours. */
    public record Remap(String source, boolean blockAtlas, int bandLight, int bandDark, NTMMaterial material) {}

    private record Art(String sprite, NTMMaterial band) {}

    private static final Map<MaterialShapes, Art> ART = Map.of(
            NUGGET, new Art(Constants.MOD_ID + ":items/nugget_lead", Mats.MAT_LEAD),
            BILLET, new Art(Constants.MOD_ID + ":items/billet_neptunium", Mats.MAT_NEPTUNIUM),
            INGOT, new Art(Constants.MOD_ID + ":items/ingot_steel", Mats.MAT_STEEL),
            DUST, new Art(Constants.MOD_ID + ":items/powder_iron", Mats.MAT_IRON),
            PLATE, new Art(Constants.MOD_ID + ":items/plate_iron", Mats.MAT_IRON),
            CRYSTAL, new Art(Constants.MOD_ID + ":items/crystal_iron", Mats.MAT_IRON),
            BLOCK, new Art(Constants.MOD_ID + ":blocks/block_lead", Mats.MAT_LEAD));
    private static final String ORE_OVERLAY = Constants.MOD_ID + ":blocks/ore_overlay";
    private static final int ORE_BAND_LIGHT = 0xFFFFFF;
    private static final int ORE_BAND_DARK = 0xD8D8D8;
    private static final String ORE_STEP = "underground_ores";

    static final class Declared {
        final NTMMaterial material;
        final Set<MaterialShapes> items = new LinkedHashSet<>();
        final Map<MaterialShapes, String> textures = new LinkedHashMap<>();
        boolean block;
        MaterialsEvent.OreSpec ore;
        double rads;
        @Nullable String hazard;
        double hazardLevel;

        final Map<MaterialShapes, RegistryObject<? extends Item>> minted = new LinkedHashMap<>();
        final List<String> oreIds = new ArrayList<>();

        Declared(NTMMaterial material) {
            this.material = material;
        }
    }

    private static final Map<NTMMaterial, Declared> SPECS = new LinkedHashMap<>();
    private static final List<Remap> REMAPS = new ArrayList<>();
    private static final Map<String, JsonObject> BLOCK_MODELS = new LinkedHashMap<>();
    private static boolean generatorsAdded;

    static Declared spec(NTMMaterial material) {
        return SPECS.computeIfAbsent(material, Declared::new);
    }

    /** Client sprites to declare, in minting order. */
    public static synchronized List<Remap> remaps() {
        return List.copyOf(REMAPS);
    }

    static synchronized void mint() {
        for (Declared spec : SPECS.values()) {
            try {
                for (MaterialShapes shape : spec.items) mintItem(spec, shape);
                if (spec.block) mintBlock(spec);
                if (spec.ore != null) mintOre(spec);
                if (spec.rads > 0 || spec.hazard != null) radiation(spec);
                ScriptedContent.whenRegistered(() -> smelting(spec));
            } catch (RuntimeException e) {
                Constants.LOG.error("[scripts] material {} failed: {}", spec.material.name, e.getMessage(), e);
            }
        }
        if (!generatorsAdded && !SPECS.isEmpty()) {
            generatorsAdded = true;
            ScriptedContent.data(ScriptedMaterials::generateData);
            ScriptedContent.assets(ScriptedMaterials::generateAssets);
        }
    }

    private static String id(MaterialShapes shape, NTMMaterial material) {
        return shape.itemPrefix + "_" + material.name;
    }

    private static String sprite(Declared spec, MaterialShapes shape) {
        String authored = spec.textures.get(shape);
        if (authored != null) return authored;
        Art art = ART.get(shape);
        if (art == null) throw new IllegalArgumentException(shape + " has no default art; give texture('" + shape + "', sprite)");
        REMAPS.add(new Remap(art.sprite(), shape == BLOCK, art.band().solidColorLight, art.band().solidColorDark, spec.material));
        return art.sprite() + "_" + spec.material.name;
    }

    private static void mintItem(Declared spec, MaterialShapes shape) {
        String id = id(shape, spec.material);
        RegistryObject<Item> item = ScriptedContent.items(ModAutogenItems.ITEMS).registerSimpleItem(id);
        ModTabs.addTo(HbmTab.MATERIALS, item);
        spec.minted.put(shape, item);
        ScriptedContent.itemAsset(id, sprite(spec, shape), shape.displayName(spec.material.getDisplayName()));
    }

    private static void mintBlock(Declared spec) {
        String id = id(BLOCK, spec.material);
        RegistryObject<Block> block = ScriptedContent.blocks(ModOreBlocks.BLOCKS).registerSimpleBlock(id,
                p -> p.strength(5F, 10F).sound(SoundType.METAL).requiresCorrectToolForDrops());
        RegistryObject<? extends Item> item = ScriptedContent.items(ModAutogenItems.ITEMS).registerBlockItem(id, block);
        ModTabs.addTo(HbmTab.BLOCKS, item);
        spec.minted.put(BLOCK, item);
        String texture = sprite(spec, BLOCK);
        BLOCK_MODELS.put(id, cubeAll(texture));
        ScriptedContent.lang("block." + Constants.MOD_ID + "." + id, BLOCK.displayName(spec.material.getDisplayName()));
    }

    private static void mintOre(Declared spec) {
        MaterialsEvent.OreSpec ore = spec.ore;
        String overlay = spec.textures.get(ORE);
        if (overlay == null) {
            REMAPS.add(new Remap(ORE_OVERLAY, true, ORE_BAND_LIGHT, ORE_BAND_DARK, spec.material));
            overlay = ORE_OVERLAY + "_" + spec.material.name;
        }
        String name = ORE.displayName(spec.material.getDisplayName());
        List<String> stones = ore.stones();
        for (int i = 0; i < stones.size(); i++) {
            String stone = stones.get(i);
            String id = i == 0 ? "ore_" + spec.material.name : "ore_" + stonePrefix(stone) + "_" + spec.material.name;
            float hardness = stone.equals("deepslate") ? ore.hardness * 1.5F : ore.hardness;
            SoundType sound = switch (stone) {
                case "deepslate" -> SoundType.DEEPSLATE;
                case "netherrack" -> SoundType.NETHER_ORE;
                default -> SoundType.STONE;
            };
            RegistryObject<CompatDropExperienceBlock> block = ScriptedContent.blocks(ModOreBlocks.BLOCKS).registerBlock(id,
                    p -> new CompatDropExperienceBlock(UniformInt.of(ore.xpMin, ore.xpMax), p),
                    p -> p.strength(hardness, 3F).sound(sound).requiresCorrectToolForDrops());
            RegistryObject<? extends Item> item = ScriptedContent.items(ModAutogenItems.ITEMS).registerBlockItem(id, block);
            ModTabs.addTo(HbmTab.RESOURCES, item);
            spec.oreIds.add(id);
            BLOCK_MODELS.put(id, oreModel(stoneTexture(stone), overlay));
            ScriptedContent.lang("block." + Constants.MOD_ID + "." + id, i == 0 ? name : titled(stonePrefix(stone)) + " " + name);
            ScriptedContent.whenRegistered(() -> {
                if (smelts(spec.material)) Mats.registerEntry(item.get(), oreYield(spec.material));
            });
        }
        if (smelts(spec.material)) Mats.registerTagEntry("c:" + ORE.tagPlural + "/" + spec.material.name, oreYield(spec.material));
        BiomeFeatures.inject(ore.dimension, feature(spec.material), ORE_STEP);
    }

    private static List<Mats.MaterialStack> oreYield(NTMMaterial material) {
        return List.of(new Mats.MaterialStack(material, INGOT.quantity * 2), new Mats.MaterialStack(Mats.MAT_STONE, INGOT.quantity / 4));
    }

    private static String feature(NTMMaterial material) {
        return Constants.MOD_ID + ":scripted_ore_" + material.name;
    }

    private static String stonePrefix(String stone) {
        return switch (stone) {
            case "netherrack" -> "nether";
            case "end_stone" -> "end";
            default -> stone.substring(stone.indexOf(':') + 1);
        };
    }

    private static String titled(String snake) {
        StringBuilder out = new StringBuilder();
        for (String part : snake.split("_")) {
            if (part.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    private static String blockId(String stone) {
        return stone.indexOf(':') >= 0 ? stone : "minecraft:" + stone;
    }

    private static String stoneTexture(String stone) {
        String id = blockId(stone);
        int colon = id.indexOf(':');
        return id.substring(0, colon) + ":block/" + id.substring(colon + 1);
    }

    private static boolean smelts(NTMMaterial material) {
        return material.smeltsInto.smeltable == NTMMaterial.SmeltingBehavior.SMELTABLE
                || material.smeltsInto.smeltable == NTMMaterial.SmeltingBehavior.ADDITIVE;
    }

    private static void smelting(Declared spec) {
        if (!smelts(spec.material)) return;
        spec.minted.forEach((shape, item) ->
                Mats.registerEntry(item.get(), List.of(new Mats.MaterialStack(spec.material, shape.quantity))));
    }

    private static final Map<MaterialShapes, Float> RAD_MULT = Map.of(
            NUGGET, HazardRegistry.nugget, INGOT, HazardRegistry.ingot, DUST, HazardRegistry.powder,
            BILLET, HazardRegistry.billet, BLOCK, HazardRegistry.block, PLATE, HazardRegistry.plate,
            CRYSTAL, HazardRegistry.crystal);

    private static void radiation(Declared spec) {
        ScriptedContent.whenRegistered(() -> {
            IHazardType extra = spec.hazard == null ? null : HazardsEvent.hazardType(spec.hazard);
            float extraLevel = (float) spec.hazardLevel;
            HazardRegistry.registerMat(spec.material, (float) spec.rads, extra, extraLevel);
            spec.minted.forEach((shape, item) -> {
                Float mult = RAD_MULT.get(shape);
                if (mult == null) return;
                HazardData data = new HazardData().setMutex(0b1).addEntry(HazardRegistry.RADIATION, (float) spec.rads * mult);
                if (extra != null && extraLevel > 0) data.addEntry(extra, extraLevel);
                HazardSystem.register(item.get(), data);
            });
        });
    }

    private static JsonObject cubeAll(String texture) {
        JsonObject textures = new JsonObject();
        textures.addProperty("all", texture);
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/cube_all");
        model.add("textures", textures);
        return model;
    }

    private static JsonObject oreModel(String base, String overlay) {
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", base);
        textures.addProperty("base", base);
        textures.addProperty("overlay", overlay);
        JsonArray elements = new JsonArray();
        elements.add(cube("#base"));
        elements.add(cube("#overlay"));
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/block");
        model.addProperty("render_type", "minecraft:cutout");
        model.add("textures", textures);
        model.add("elements", elements);
        return model;
    }

    private static JsonObject cube(String texture) {
        JsonObject faces = new JsonObject();
        for (String side : new String[] {"down", "up", "north", "south", "west", "east"}) {
            JsonObject face = new JsonObject();
            face.addProperty("texture", texture);
            face.addProperty("cullface", side);
            faces.add(side, face);
        }
        JsonObject element = new JsonObject();
        element.add("from", vector(0, 0, 0));
        element.add("to", vector(16, 16, 16));
        element.add("faces", faces);
        return element;
    }

    private static JsonArray vector(int x, int y, int z) {
        JsonArray v = new JsonArray();
        v.add(x);
        v.add(y);
        v.add(z);
        return v;
    }

    private static void generateAssets(GolemScriptPlugin.Resources out) {
        BLOCK_MODELS.forEach((id, model) -> {
            String ref = Constants.MOD_ID + ":block/" + id;
            out.json(Constants.MOD_ID, "models/block/" + id, model);

            JsonObject variant = new JsonObject();
            variant.addProperty("model", ref);
            JsonObject variants = new JsonObject();
            variants.add("", variant);
            JsonObject state = new JsonObject();
            state.add("variants", variants);
            out.json(Constants.MOD_ID, "blockstates/" + id, state);

            JsonObject item = new JsonObject();
            item.addProperty("parent", ref);
            out.json(Constants.MOD_ID, "models/item/" + id, item);

            JsonObject reference = new JsonObject();
            reference.addProperty("type", "minecraft:model");
            reference.addProperty("model", ref);
            JsonObject definition = new JsonObject();
            definition.add("model", reference);
            out.json(Constants.MOD_ID, "items/" + id, definition);
        });
    }

    private static void generateData(GolemScriptPlugin.Resources out) {
        Map<String, List<String>> itemTags = new TreeMap<>();
        Map<String, List<String>> blockTags = new TreeMap<>();
        for (Declared spec : SPECS.values()) {
            NTMMaterial material = spec.material;
            spec.minted.keySet().forEach(shape -> itemTags.computeIfAbsent("c:" + shape.tagPlural + "/" + material.name,
                    key -> new ArrayList<>()).add(hbm(id(shape, material))));
            if (spec.block) {
                String block = hbm(id(BLOCK, material));
                blockTags.computeIfAbsent("c:" + BLOCK.tagPlural + "/" + material.name, key -> new ArrayList<>()).add(block);
                blockTags.computeIfAbsent("minecraft:mineable/pickaxe", key -> new ArrayList<>()).add(block);
                blockTags.computeIfAbsent("minecraft:needs_stone_tool", key -> new ArrayList<>()).add(block);
                out.json(Constants.MOD_ID, DataPaths.blockLootTable(id(BLOCK, material)), LootJson.dropSelf(block));
            }
            recipes(out, spec);
            if (spec.ore != null && spec.oreIds.size() == spec.ore.stones().size()) ore(out, spec, itemTags, blockTags);
        }
        writeTags(out, itemTags, DataPaths::itemTag);
        writeTags(out, blockTags, DataPaths::blockTag);
    }

    private static void recipes(GolemScriptPlugin.Resources out, Declared spec) {
        String mat = spec.material.name;
        if (spec.minted.containsKey(INGOT) && spec.minted.containsKey(BLOCK)) {
            compress(out, hbm(id(INGOT, spec.material)), hbm(id(BLOCK, spec.material)), "block_" + mat);
        }
        if (spec.minted.containsKey(NUGGET) && spec.minted.containsKey(INGOT)) {
            compress(out, hbm(id(NUGGET, spec.material)), hbm(id(INGOT, spec.material)), "ingot_" + mat);
        }
    }

    private static void compress(GolemScriptPlugin.Resources out, String small, String large, String name) {
        out.json(Constants.MOD_ID, DataPaths.recipe("scripted/" + name + "_from_parts"),
                RecipeJson.shaped(large, 1, List.of("###", "###", "###"), Map.of('#', small)));
        out.json(Constants.MOD_ID, DataPaths.recipe("scripted/" + name + "_to_parts"),
                RecipeJson.shapeless(small, 9, List.of(large)));
    }

    private static void ore(GolemScriptPlugin.Resources out, Declared spec, Map<String, List<String>> itemTags,
                            Map<String, List<String>> blockTags) {
        MaterialsEvent.OreSpec ore = spec.ore;
        NTMMaterial material = spec.material;
        String drop = dropItem(spec, ore.drop);
        JsonArray targets = new JsonArray();
        List<String> stones = ore.stones();
        for (int i = 0; i < stones.size(); i++) {
            String block = hbm(spec.oreIds.get(i));
            itemTags.computeIfAbsent("c:" + ORE.tagPlural + "/" + material.name, key -> new ArrayList<>()).add(block);
            blockTags.computeIfAbsent("c:" + ORE.tagPlural + "/" + material.name, key -> new ArrayList<>()).add(block);
            blockTags.computeIfAbsent("minecraft:mineable/pickaxe", key -> new ArrayList<>()).add(block);
            if (ore.tool != null) blockTags.computeIfAbsent("minecraft:needs_" + ore.tool + "_tool", key -> new ArrayList<>()).add(block);
            out.json(Constants.MOD_ID, DataPaths.blockLootTable(spec.oreIds.get(i)),
                    drop == null ? LootJson.dropSelf(block) : LootJson.oreDrops(block, drop, ore.dropMin, ore.dropMax));
            targets.add(target(stones.get(i), block));
        }

        JsonObject config = new JsonObject();
        config.addProperty("size", ore.veinSize);
        config.addProperty("discard_chance_on_air_exposure", 0F);
        config.add("targets", targets);
        JsonObject configured = new JsonObject();
        configured.addProperty("type", "minecraft:ore");
        configured.add("config", config);
        String path = "scripted_ore_" + material.name;
        out.json(Constants.MOD_ID, "worldgen/configured_feature/" + path, configured);

        JsonArray placement = new JsonArray();
        JsonObject count = new JsonObject();
        count.addProperty("type", "minecraft:count");
        count.addProperty("count", ore.count);
        placement.add(count);
        placement.add(type("minecraft:in_square"));
        JsonObject height = new JsonObject();
        height.addProperty("type", ore.triangle ? "minecraft:trapezoid" : "minecraft:uniform");
        height.add("min_inclusive", absolute(ore.minY));
        height.add("max_inclusive", absolute(ore.maxY));
        JsonObject range = new JsonObject();
        range.addProperty("type", "minecraft:height_range");
        range.add("height", height);
        placement.add(range);
        placement.add(type("minecraft:biome"));
        JsonObject placed = new JsonObject();
        placed.addProperty("feature", feature(material));
        placed.add("placement", placement);
        out.json(Constants.MOD_ID, "worldgen/placed_feature/" + path, placed);

        JsonObject modifier = BiomeFeatures.modifier(ore.dimension, feature(material), ORE_STEP);
        if (modifier != null) out.json(Constants.MOD_ID, DataPaths.biomeModifier(path), modifier);
    }

    /** Null: the ore drops itself. */
    private static @Nullable String dropItem(Declared spec, String drop) {
        if (drop.equals("self")) return null;
        MaterialShapes shape = MaterialShapes.byName(drop.toUpperCase(Locale.US));
        if (shape == null) return drop;
        if (spec.minted.containsKey(shape)) return hbm(id(shape, spec.material));
        String base = ModAutogenItems.base(shape);
        if (base != null && ModAutogenItems.has(shape, spec.material)) return hbm(base + "_" + spec.material.name);
        Constants.LOG.error("[scripts] {} has no {} item to drop; its ore drops itself", spec.material.name, shape);
        return null;
    }

    private static JsonObject target(String stone, String block) {
        JsonObject test = new JsonObject();
        switch (stone) {
            case "stone" -> {
                test.addProperty("predicate_type", "minecraft:tag_match");
                test.addProperty("tag", "minecraft:stone_ore_replaceables");
            }
            case "deepslate" -> {
                test.addProperty("predicate_type", "minecraft:tag_match");
                test.addProperty("tag", "minecraft:deepslate_ore_replaceables");
            }
            default -> {
                test.addProperty("predicate_type", "minecraft:block_match");
                test.addProperty("block", blockId(stone));
            }
        }
        JsonObject state = new JsonObject();
        state.addProperty("Name", block);
        JsonObject target = new JsonObject();
        target.add("target", test);
        target.add("state", state);
        return target;
    }

    private static JsonObject type(String type) {
        JsonObject object = new JsonObject();
        object.addProperty("type", type);
        return object;
    }

    private static JsonObject absolute(int y) {
        JsonObject anchor = new JsonObject();
        anchor.addProperty("absolute", y);
        return anchor;
    }

    private static void writeTags(GolemScriptPlugin.Resources out, Map<String, List<String>> tags,
                                  java.util.function.UnaryOperator<String> folder) {
        tags.forEach((tag, values) -> {
            int colon = tag.indexOf(':');
            JsonArray list = new JsonArray();
            values.forEach(list::add);
            JsonObject json = new JsonObject();
            json.addProperty("replace", false);
            json.add("values", list);
            out.json(tag.substring(0, colon), folder.apply(tag.substring(colon + 1)), json);
        });
    }

    private static String hbm(String path) {
        return Constants.MOD_ID + ":" + path;
    }
}
