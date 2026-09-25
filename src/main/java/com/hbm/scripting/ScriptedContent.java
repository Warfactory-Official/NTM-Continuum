package com.hbm.scripting;

import com.google.gson.JsonObject;
import com.hbm.Constants;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModAutogenItems;
import dev.golem.core.platform.Platform;
import dev.golem.core.registry.DeferredRegister;
import com.hbm.scripting.recipes.TableHandler;
import dev.golem.core.script.GolemScriptPlugin;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Where startup scripts meet this mod's registries.
 *
 * <p>Loaders order KubeJS's script load and this mod's bootstrap differently (Fabric shuffles in dev),
 * so NTMEvents fires when the later of the two arrives. Forge-likes flush registries after every
 * constructor, so items minted here always make the flush; on Fabric a late arrival flushes its own.
 * Hazards need resolved items, so they wait for {@link #hazardsReady} as well.
 */
public final class ScriptedContent {

    private ScriptedContent() {}

    private static boolean bootstrapped;
    private static boolean scriptsLoaded;
    private static boolean posted;
    private static boolean flushed;
    private static boolean hazardsReady;
    private static List<Runnable> hazardEdits = List.of();
    private static final List<Runnable> REGISTERED = new ArrayList<>();

    private static final Map<String, String> ITEM_SPRITES = new LinkedHashMap<>();
    private static DeferredRegister.Items lateItems;
    private static DeferredRegister.Blocks lateBlocks;
    private static final Map<String, String> LANG = new LinkedHashMap<>();
    private static final List<Consumer<GolemScriptPlugin.Resources>> DATA = new ArrayList<>();
    private static final List<Consumer<GolemScriptPlugin.Resources>> ASSETS = new ArrayList<>();

    public static synchronized void bootstrapDone() {
        bootstrapped = true;
        tryPost();
    }

    /** Fabric: registries went to vanilla before scripts may have run. */
    public static synchronized void registriesFlushed() {
        flushed = true;
    }

    public static synchronized void hazardsReady() {
        hazardsReady = true;
        if (posted) runHazards();
    }

    static synchronized void scriptsLoaded() {
        if (posted) {
            reload();
            return;
        }
        scriptsLoaded = true;
        tryPost();
    }

    public static void lang(String key, String value) {
        LANG.put(key, value);
    }

    static void generateLang(GolemScriptPlugin.Lang out) {
        LANG.forEach((key, value) -> out.add(Constants.MOD_ID, key, value));
    }

    private static void tryPost() {
        if (!bootstrapped || !scriptsLoaded || posted) return;
        posted = true;
        NTMEvents events = NTMEvents.current();

        NTMEvents.post("shapes", events.shapes, new ShapesEvent());
        NTMEvents.post("materials", events.materials, new MaterialsEvent());
        ScriptedMaterials.mint();
        NTMEvents.post("fluids", events.fluids, new FluidsEvent());
        mintAutogenItems();
        NTMEvents.post("fuels", events.fuels, new FuelsEvent());
        if (lateBlocks != null) lateBlocks.flushToVanilla();
        if (lateItems != null) lateItems.flushToVanilla();
        if (!ITEM_SPRITES.isEmpty()) Constants.LOG.info("[scripts] minted {} item(s)", ITEM_SPRITES.size());
        NTMEvents.post("recipes", events.recipes, new RecipesEvent());
        HazardsEvent hazards = new HazardsEvent();
        NTMEvents.post("hazards", events.hazards, hazards);
        hazardEdits = List.copyOf(hazards.edits());
        if (hazardsReady) runHazards();

        if (Platform.isClient()) ClientSync.run();
    }

    private static void mintAutogenItems() {
        for (MaterialShapes shape : ModAutogenItems.shapeOrder()) {
            for (NTMMaterial material : Mats.orderedList) {
                if (!material.autogen.contains(shape) || ModAutogenItems.has(shape, material)) continue;
                ModAutogenItems.register(items(ModAutogenItems.ITEMS), shape, material);
                ITEM_SPRITES.put(ModAutogenItems.base(shape) + "_" + material.name, ModAutogenItems.art(shape) + "_" + material.name);
            }
        }
    }

    /** {@code own} before the registries flush; after, a register this class flushes itself. */
    public static DeferredRegister.Items items(DeferredRegister.Items own) {
        if (!flushed) return own;
        if (lateItems == null) lateItems = DeferredRegister.createItems(Constants.MOD_ID);
        return lateItems;
    }

    /** {@code own} before the registries flush; after, a register this class flushes itself. */
    public static DeferredRegister.Blocks blocks(DeferredRegister.Blocks own) {
        if (!flushed) return own;
        if (lateBlocks == null) lateBlocks = DeferredRegister.createBlocks(Constants.MOD_ID);
        return lateBlocks;
    }

    /** A minted item drawn from one sprite, named {@code name} when not null. */
    public static void itemAsset(String id, String sprite, @Nullable String name) {
        ITEM_SPRITES.put(id, sprite);
        if (name != null) LANG.put("item." + Constants.MOD_ID + "." + id, name);
    }

    /** Runs once items exist and hazards are set up, before scripted hazard edits. */
    public static void whenRegistered(Runnable edit) {
        REGISTERED.add(edit);
    }

    /**
     * A startup-script reload: recipe and hazard edits re-apply; shapes, materials, fluids and fuels
     * are registry content and stay as launched.
     */
    private static void reload() {
        NTMEvents events = NTMEvents.current();
        ScriptedRecipes.clear();
        RecipesEvent recipes = new RecipesEvent();
        recipes.tables().values().forEach(TableHandler::reset);
        NTMEvents.post("recipes", events.recipes, recipes);
        if (hazardsReady) undoHazards();
        HazardsEvent hazards = new HazardsEvent();
        NTMEvents.post("hazards", events.hazards, hazards);
        hazardEdits = List.copyOf(hazards.edits());
        if (hazardsReady) hazardEdits.forEach(ScriptedContent::runSafely);
        Constants.LOG.info("[scripts] reloaded recipe and hazard edits; new content applies at launch only");
    }

    private static void undoHazards() {
        HazardsEvent.undoAll();
    }

    private static void runSafely(Runnable edit) {
        try {
            edit.run();
        } catch (RuntimeException e) {
            Constants.LOG.error("[scripts] hazard edit failed: {}", e.getMessage(), e);
        }
    }

    private static void runHazards() {
        for (Runnable edit : REGISTERED) {
            try {
                edit.run();
            } catch (RuntimeException e) {
                Constants.LOG.error("[scripts] post-registration edit failed: {}", e.getMessage(), e);
            }
        }
        for (Runnable edit : hazardEdits) {
            try {
                edit.run();
            } catch (RuntimeException e) {
                Constants.LOG.error("[scripts] hazard edit failed: {}", e.getMessage(), e);
            }
        }
    }

    /** Server data-pack files (tags, loot, recipes, worldgen) for minted content. */
    public static void data(Consumer<GolemScriptPlugin.Resources> generator) {
        DATA.add(generator);
    }

    /** Client assets beyond the one-sprite item models {@link #itemAsset} covers. */
    public static void assets(Consumer<GolemScriptPlugin.Resources> generator) {
        ASSETS.add(generator);
    }

    static void generateData(GolemScriptPlugin.Resources out) {
        DATA.forEach(generator -> generator.accept(out));
    }

    static void generateAssets(GolemScriptPlugin.Resources out) {
        ASSETS.forEach(generator -> generator.accept(out));
        for (String name : ITEM_SPRITES.keySet()) {
            JsonObject textures = new JsonObject();
            textures.addProperty("layer0", ITEM_SPRITES.get(name));
            JsonObject model = new JsonObject();
            model.addProperty("parent", "minecraft:item/generated");
            model.add("textures", textures);
            out.json(Constants.MOD_ID, "models/item/" + name, model);

            JsonObject reference = new JsonObject();
            reference.addProperty("type", "minecraft:model");
            reference.addProperty("model", Constants.MOD_ID + ":item/" + name);
            JsonObject definition = new JsonObject();
            definition.add("model", reference);
            out.json(Constants.MOD_ID, "items/" + name, definition);
        }
    }

    private static final class ClientSync {
        static void run() {
            com.hbm.client.AutogenSprites.sync();
            com.hbm.client.FluidSprites.sync();
        }
    }
}
