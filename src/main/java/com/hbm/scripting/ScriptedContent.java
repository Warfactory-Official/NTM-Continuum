package com.hbm.scripting;

import com.hbm.api.content.HazardsEvent;
import com.hbm.api.content.RecipesEvent;
import com.hbm.api.content.FuelsEvent;
import com.hbm.api.content.FluidsEvent;
import com.hbm.api.content.MaterialsEvent;
import com.hbm.api.content.ShapesEvent;
import com.google.gson.JsonObject;
import com.hbm.Constants;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModAutogenItems;
import dev.golem.core.registry.DeferredRegister;
import com.hbm.api.content.NtmPhases;
import dev.golem.core.content.Phase;
import dev.golem.core.content.Pipeline;
import dev.golem.core.script.GolemScriptPlugin;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Script and minted-content plumbing for {@link NtmPhases}: KubeJS listeners subscribe last on every phase, in phase
 * order, once scripts load; a startup-script reload replaces them and replays recipes and hazards.
 */
public final class ScriptedContent {

    private ScriptedContent() {}

    private static boolean registriesReady;
    private static final List<Runnable> REGISTERED = new ArrayList<>();
    private static final List<Pipeline.Subscription<?>> SCRIPT_SUBSCRIPTIONS = new ArrayList<>();

    private static final Map<String, String> ITEM_SPRITES = new LinkedHashMap<>();
    private static final Map<String, String> LANG = new LinkedHashMap<>();
    private static final List<Consumer<GolemScriptPlugin.Resources>> DATA = new ArrayList<>();
    private static final List<Consumer<GolemScriptPlugin.Resources>> ASSETS = new ArrayList<>();

    /** HBM's {@code setup}, after {@code Hazards.init}: items resolve; queued {@link #whenRegistered} edits run. */
    public static synchronized void registriesReady() {
        registriesReady = true;
        REGISTERED.forEach(ScriptedContent::runSafely);
        REGISTERED.clear();
    }

    static synchronized void scriptsLoaded() {
        boolean reload = !SCRIPT_SUBSCRIPTIONS.isEmpty();
        SCRIPT_SUBSCRIPTIONS.forEach(Pipeline.Subscription::remove);
        SCRIPT_SUBSCRIPTIONS.clear();
        NTMEvents events = NTMEvents.current();
        subscribe(NtmPhases.SHAPES, "shapes", events.shapes);
        subscribe(NtmPhases.MATERIALS, "materials", events.materials);
        subscribe(NtmPhases.FLUIDS, "fluids", events.fluids);
        subscribe(NtmPhases.FUELS, "fuels", events.fuels);
        subscribe(NtmPhases.RECIPES, "recipes", events.recipes);
        subscribe(NtmPhases.HAZARDS, "hazards", events.hazards);
        if (reload) {
            Pipeline.reload(NtmPhases.RECIPES, NtmPhases.HAZARDS);
            Constants.LOG.info("[scripts] reloaded recipe and hazard edits; new content applies at launch only");
        }
    }

    private static <E> void subscribe(Phase<E> phase, String name, List<Consumer<E>> listeners) {
        if (listeners.isEmpty()) return;
        SCRIPT_SUBSCRIPTIONS.add(Pipeline.onLast(phase, event -> NTMEvents.post(name, listeners, event)));
    }

    public static void lang(String key, String value) {
        LANG.put(key, value);
    }

    static void generateLang(GolemScriptPlugin.Lang out) {
        LANG.forEach((key, value) -> out.add(Constants.MOD_ID, key, value));
    }

    public static void mintMaterials() {
        ScriptedMaterials.mint();
    }

    public static void mintAutogenItems() {
        for (MaterialShapes shape : ModAutogenItems.shapeOrder()) {
            for (NTMMaterial material : Mats.orderedList) {
                if (!material.autogen.contains(shape) || ModAutogenItems.has(shape, material)) continue;
                ModAutogenItems.register(items(ModAutogenItems.ITEMS), shape, material);
                ITEM_SPRITES.put(ModAutogenItems.base(shape) + "_" + material.name, ModAutogenItems.art(shape) + "_" + material.name);
            }
        }
    }

    public static DeferredRegister.Items items(DeferredRegister.Items own) {
        return DeferredRegister.late(own);
    }

    public static DeferredRegister.Blocks blocks(DeferredRegister.Blocks own) {
        return DeferredRegister.late(own);
    }

    /** A minted item drawn from one sprite, named {@code name} when not null. */
    public static void itemAsset(String id, String sprite, @Nullable String name) {
        ITEM_SPRITES.put(id, sprite);
        if (name != null) LANG.put("item." + Constants.MOD_ID + "." + id, name);
    }

    /** Once items exist and hazards are set up (now, if already, after flushing late registers), before hazard-phase handlers. */
    public static synchronized void whenRegistered(Runnable edit) {
        if (!registriesReady) {
            REGISTERED.add(edit);
            return;
        }
        DeferredRegister.flushLate();
        runSafely(edit);
    }

    public static void runSafely(Runnable edit) {
        try {
            edit.run();
        } catch (RuntimeException e) {
            Constants.LOG.error("[scripts] edit failed: {}", e.getMessage(), e);
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
}
