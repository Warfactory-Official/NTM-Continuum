package com.hbm.inventory.fluid;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.hbm.Constants;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_PWRModerator;
import com.hbm.inventory.fluid.trait.FT_Pheromone;
import com.hbm.inventory.fluid.trait.FT_Poison;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FT_Toxin;
import com.hbm.inventory.fluid.trait.FT_VentRadiation;
import com.hbm.util.ArmorRegistry.HazardClass;
import dev.golem.core.Ids;
import dev.golem.core.data.DataFiles;
import dev.golem.core.world.Effects;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * A fluid's properties as data (ntm-next's {@code fluid_property} files): {@code data/<ns>/fluid_property/<path>.json}
 * for the fluid {@code <ns>:<path>} (its vanilla fluid id; water is {@code data/minecraft/fluid_property/water.json}).
 *
 * <pre>
 * {
 *   "type": "liquid",                     the phase: liquid, gaseous or plasma, or a list of them (absent: none)
 *   "color": "#3333FF", "tint": "#3333FF", "temperature": 20,
 *   "hazard": {"poison": 3, "flammability": 4, "reactivity": 0, "symbol": "cryogenic"},   1.7's diamond ({@link FluidHazard})
 *   "flags": ["viscous", "nocontainer"],  value-less traits: gaseous_art, viscous, amat, leadcontainer, delicious,
 *                                         noid, nocontainer, unsiphonable
 *   "traits": {                           parameterized traits, by name
 *     "corrosive": {"rating": 50}, "poison": {"withering": true, "level": 2}, "pwrmoderator": {"multiplier": 1.0},
 *     "flammable": {"energy": 300000}, "combustible": {"grade": "AERO", "energy": 1250000},   (1.7 FT_Flammable, FT_Combustible)
 *     "pheromone": {"type": 1}, "ventradiation": {"rad_per_mb": 0.5},
 *     "heatable": {"steps": [{"amount_req": 1, "heat_req": 200, "produces": "hbm:steam", "amount_produced": 10}],
 *                  "efficiency": {"BOILER": 1.0}},
 *     "coolable": {"cools_to": "minecraft:water", "amount_req": 10, "amount_produced": 1, "heat_energy": 200,
 *                  "efficiency": {"TURBINE": 1.0}},
 *     "toxin": {"entries": [{"kind": "direct_damage", "damage": "hbm:cloud", "amount": 2.0, "delay": 20,
 *                            "hazard": "GAS_LUNG", "full_body": false},
 *                           {"kind": "effects", "effects": [{"id": "minecraft:wither", "duration": 100, "amplifier": 1}],
 *                            "hazard": "GAS_MONOXIDE", "full_body": true}]},
 *     "polluting": {"release": {"SOOT": 0.1}, "burn": {"POISON": 0.01}}
 *   }
 * }
 * </pre>
 *
 * <p>HBM's own fluids are files in HBM's jar, applied when the fluids are created so registration, menus and
 * tooltips have them before any world. Every datapack load then applies the files of all datapacks (HBM's, addons',
 * a server's), each replacing its fluid's properties. A fluid without a file keeps what it had. An addon's fluid
 * may get its defaults from code ({@link FluidType#addTraits}) or a file. Another mod changes a fluid it does not
 * own through the patch folder ({@link #PATCH_FOLDER}), never by replacing the fluid's file.
 */
public final class FluidProperties {

    public static final String FOLDER = "fluid_property";

    private FluidProperties() {}

    private static final Map<String, Object> PHASES = new LinkedHashMap<>();
    private static final Map<String, Object> FLAGS = new LinkedHashMap<>();

    private static synchronized void ensureNames() {
        if (!PHASES.isEmpty()) return;
        PHASES.put("liquid", Fluids.LIQUID);
        PHASES.put("gaseous", Fluids.GASEOUS);
        PHASES.put("plasma", Fluids.PLASMA);
        FLAGS.put("gaseous_art", Fluids.EVAP);
        FLAGS.put("viscous", Fluids.VISCOUS);
        FLAGS.put("amat", Fluids.ANTI);
        FLAGS.put("leadcontainer", Fluids.LEADCON);
        FLAGS.put("delicious", Fluids.DELICIOUS);
        FLAGS.put("noid", Fluids.NOID);
        FLAGS.put("nocontainer", Fluids.NOCON);
        FLAGS.put("unsiphonable", Fluids.UNSIPHONABLE);
    }

    /** A trait another mod adds to the file format: its class, how to read its JSON body, and how to write it back. */
    public record TraitType(Class<?> type, Function<JsonObject, Object> reader,
                            Function<Object, JsonObject> writer) {}

    private static final Map<String, TraitType> EXTRA_TRAITS = Collections.synchronizedMap(new LinkedHashMap<>());
    /** Every addon trait and flag class, in registration order (the tooltip's order, {@link FluidType#addInfo}). */
    private static final List<Class<?>> ADDON_TYPES = new CopyOnWriteArrayList<>();

    /** The trait and flag classes other mods registered, in registration order. */
    public static List<Class<?>> addonTraitTypes() {
        return ADDON_TYPES;
    }

    /**
     * Adds a parameterised trait {@code "traits": {"<name>": {...}}} to the {@code fluid_property} format (1.7's
     * {@code FluidTrait} registry; NTM: Space adds rocket fuel and terraforming traits). {@code name} should be
     * {@code <namespace>:<name>} for another mod's trait. Call at mod construction, before any datapack loads.
     */
    public static void registerTrait(String name, Class<?> type, Function<JsonObject, Object> reader,
                                     Function<Object, JsonObject> writer) {
        if (EXTRA_TRAITS.putIfAbsent(name, new TraitType(type, reader, writer)) != null) {
            throw new IllegalStateException("Fluid trait " + name + " registered twice");
        }
        ADDON_TYPES.add(type);
    }

    /** Adds a parameterless trait (a singleton instance) to {@code "flags"}. Call at mod construction. */
    public static synchronized void registerFlag(String name, Object trait) {
        ensureNames();
        if (FLAGS.putIfAbsent(name, trait) != null) throw new IllegalStateException("Fluid flag " + name + " registered twice");
        ADDON_TYPES.add(trait.getClass());
    }

    // Reading

    /** Applies every file of a datapack load ({@code <ns>:<path>} to its JSON) to its fluid; a bad file is skipped. */
    public static synchronized void apply(Map<String, JsonElement> files) {
        var previousFiles = baseFiles;
        baseFiles = Map.copyOf(files);
        for (var id : previousFiles.keySet()) {
            if (files.containsKey(id)) continue;
            var fluid = Fluids.byRegistryNameExact(id);
            if (!fluid.isForeign()) continue;
            var defaults = FOREIGN_DEFAULTS.remove(fluid);
            if (defaults != null) defaults.restore(fluid);
            if (patches.containsKey(id)) PRISTINE.put(fluid, Pristine.of(fluid));
            else PRISTINE.remove(fluid);
        }
        int applied = 0;
        List<String> failures = new ArrayList<>();
        for (Map.Entry<String, JsonElement> file : files.entrySet()) {
            FluidType fluid = Fluids.byRegistryNameExact(file.getKey());
            if (fluid == Fluids.NONE && !file.getKey().equals(Fluids.NONE.getRegistryName())) {
                failures.add(file.getKey() + " names no fluid");
                continue;
            }
            try {
                apply(fluid, file.getValue().getAsJsonObject());
                applied++;
            } catch (RuntimeException e) {
                failures.add(file.getKey() + ": " + e.getMessage());
            }
        }
        // the files just replaced the patched fluids that have one; put every patch back on top
        for (Map.Entry<String, List<JsonObject>> patched : patches.entrySet()) {
            FluidType fluid = Fluids.byRegistryNameExact(patched.getKey());
            try {
                reset(fluid, true);
                patch(fluid, patched.getValue());
            } catch (RuntimeException e) {
                failures.add(patched.getKey() + " (patches): " + e.getMessage());
            }
        }
        // DataFiles refreshes after bound tags change on the server and after client metadata sync.
        Fluids.sharingChanged();
        Constants.LOG.info("[HBM] Applied {} fluid property file(s)", applied);
        if (!failures.isEmpty()) Constants.LOG.warn("[HBM] Skipped {} fluid property file(s): {}", failures.size(), failures);
    }

    /**
     * A mod's own fluids from the {@code fluid_property} files in its own jar ({@code owner} is any of its classes: each
     * mod reads only its own resources), when it creates them; a fluid without a file keeps its code defaults. HBM does
     * this for its fluids in {@code Fluids.init}; an addon calls it after {@link Fluids#register(String, String, int)}.
     */
    public static void applyBundled(Class<?> owner, Iterable<FluidType> fluids) {
        int applied = 0;
        for (FluidType fluid : fluids) {
            String[] id = fluid.getRegistryName().split(":", 2);
            String path = "/data/" + id[0] + "/" + FOLDER + "/" + id[1] + ".json";
            URL url = DataFiles.bundled(owner, path);
            if (url == null) continue;
            try (InputStream in = url.openStream()) {
                apply(fluid, JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
                applied++;
            } catch (IOException | RuntimeException e) {
                throw new IllegalStateException("The bundled fluid property file " + path + " is broken: " + e.getMessage(), e);
            }
        }
        Constants.LOG.info("[HBM] Applied {} bundled fluid property file(s) of {}", applied, owner.getSimpleName());
    }

    /**
     * One {@code fluid_property} file as values: what {@link #read} makes of it and {@link #write(Properties)} writes.
     * {@code color} is null when the file has none (the fluid keeps its own), {@code tint} when it has none (the
     * colour); {@code traits} are the phases, flags and parameterised traits in file order.
     */
    public record Properties(@org.jspecify.annotations.Nullable Integer color, @org.jspecify.annotations.Nullable Integer tint,
                             int temperature, FluidHazard hazard, List<Object> traits, List<TagKey<Fluid>> shares) {

        public Properties {
            hazard = Objects.requireNonNull(hazard);
            traits = List.copyOf(traits);
            shares = List.copyOf(shares);
        }

        public Properties(@org.jspecify.annotations.Nullable Integer color, @org.jspecify.annotations.Nullable Integer tint,
                          int temperature, FluidHazard hazard, List<Object> traits) {
            this(color, tint, temperature, hazard, traits, List.of());
        }

        /** {@code fluid}'s current properties ({@code tint} null when it is the colour). */
        public static Properties of(FluidType fluid) {
            return new Properties(fluid.getColor(), fluid.getTint() != fluid.getColor() ? fluid.getTint() : null,
                    fluid.getTemperature(), fluid.getHazard(), List.copyOf(fluid.traits()), fluid.getSharedTags());
        }
    }

    /** Reads a {@code fluid_property} file. */
    public static Properties read(JsonObject json) {
        List<Object> traits = new ArrayList<>();
        readTraits(json, traits);
        Integer color = json.has("color") ? color(json.get("color")) : null;
        Integer tint = json.has("tint") ? color(json.get("tint")) : null;
        int temperature = json.has("temperature") ? json.get("temperature").getAsInt() : FluidType.ROOM_TEMPERATURE;
        FluidHazard hazard = json.has("hazard") ? FluidHazard.read(json.getAsJsonObject("hazard")) : FluidHazard.NONE;
        return new Properties(color, tint, temperature, hazard, traits,
                json.has("shares") ? readShares(json.get("shares")) : List.of());
    }

    private static List<TagKey<Fluid>> readShares(JsonElement json) {
        List<TagKey<Fluid>> shares = new ArrayList<>();
        Iterable<JsonElement> entries = json.isJsonArray() ? json.getAsJsonArray() : List.of(json);
        for (var entry : entries) {
            String name = entry.getAsString();
            if (!name.startsWith("#")) throw new IllegalArgumentException("fluid share must name a tag: " + name);
            shares.add(TagKey.create(Registries.FLUID, Ids.parse(name.substring(1))));
        }
        return shares;
    }

    private static JsonArray writeShares(List<TagKey<Fluid>> shares) {
        JsonArray json = new JsonArray();
        for (var tag : shares) json.add("#" + tag.location());
        return json;
    }

    /** Replaces {@code fluid}'s colour, tint, temperature, hazard diamond and traits with {@code json}'s. */
    public static void apply(FluidType fluid, JsonObject json) {
        rememberForeign(fluid);
        Properties properties = read(json);
        int color = properties.color() != null ? properties.color() : fluid.getColor();
        int tint = properties.tint() != null ? properties.tint() : color;
        fluid.applyProperties(color, tint, properties.temperature(), properties.hazard(), properties.traits(), properties.shares());
    }

    /**
     * The {@code type}, {@code flags} and {@code traits} of a property file (or of a patch's {@code add}). {@code type}
     * is one phase or a list of them: 1.7 let a fluid carry several (hbm-space's methanol is GASEOUS and LIQUID).
     */
    private static void readTraits(JsonObject json, List<Object> traits) {
        ensureNames();
        if (json.has("type")) {
            JsonElement type = json.get("type");
            if (type.isJsonArray()) for (JsonElement phase : type.getAsJsonArray()) traits.add(named(PHASES, phase.getAsString(), "phase"));
            else traits.add(named(PHASES, type.getAsString(), "phase"));
        }
        if (json.has("flags")) for (JsonElement flag : json.getAsJsonArray("flags")) traits.add(named(FLAGS, flag.getAsString(), "flag"));
        if (json.has("traits")) {
            for (Map.Entry<String, JsonElement> trait : json.getAsJsonObject("traits").entrySet()) {
                traits.add(readTrait(trait.getKey(), trait.getValue().getAsJsonObject()));
            }
        }
    }

    private static Object named(Map<String, Object> names, String name, String what) {
        Object trait = names.get(name);
        if (trait == null) throw new IllegalArgumentException("unknown " + what + " '" + name + "', expected one of " + names.keySet());
        return trait;
    }

    private static int color(JsonElement json) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            String text = json.getAsString();
            return Integer.parseInt(text.startsWith("#") ? text.substring(1) : text, 16);
        }
        return json.getAsInt();
    }

    private static FluidType fluid(JsonElement json) {
        FluidType fluid = Fluids.byRegistryName(json.getAsString());
        if (fluid == Fluids.NONE && !json.getAsString().equals(Fluids.NONE.getRegistryName())) {
            throw new IllegalArgumentException("unknown fluid " + json.getAsString());
        }
        return fluid;
    }

    private static HazardClass hazard(JsonObject json) {
        return json.has("hazard") ? HazardClass.valueOf(json.get("hazard").getAsString()) : null;
    }

    private static Object readTrait(String name, JsonObject json) {
        switch (name) {
            case "corrosive": return new FT_Corrosive(json.get("rating").getAsInt());
            case "flammable": return new FT_Flammable(json.get("energy").getAsLong());
            case "combustible": return new FT_Combustible(FluidCombustion.FuelGrade.valueOf(json.get("grade").getAsString()),
                    json.get("energy").getAsLong());
            case "poison": return new FT_Poison(json.get("withering").getAsBoolean(), json.get("level").getAsInt());
            case "pwrmoderator": return new FT_PWRModerator(json.get("multiplier").getAsDouble());
            case "pheromone": return new FT_Pheromone(json.get("type").getAsInt());
            case "ventradiation": return new FT_VentRadiation(json.get("rad_per_mb").getAsFloat());
            case "heatable": {
                FT_Heatable trait = new FT_Heatable();
                for (JsonElement element : json.getAsJsonArray("steps")) {
                    JsonObject step = element.getAsJsonObject();
                    trait.addStep(step.get("heat_req").getAsInt(), step.get("amount_req").getAsInt(), fluid(step.get("produces")),
                            step.get("amount_produced").getAsInt());
                }
                if (json.has("efficiency")) for (Map.Entry<String, JsonElement> eff : json.getAsJsonObject("efficiency").entrySet()) {
                    trait.setEff(FT_Heatable.HeatingType.valueOf(eff.getKey()), eff.getValue().getAsDouble());
                }
                return trait;
            }
            case "coolable": {
                FT_Coolable trait = new FT_Coolable(fluid(json.get("cools_to")), json.get("amount_req").getAsInt(),
                        json.get("amount_produced").getAsInt(), json.get("heat_energy").getAsInt());
                if (json.has("efficiency")) for (Map.Entry<String, JsonElement> eff : json.getAsJsonObject("efficiency").entrySet()) {
                    trait = trait.setEff(FT_Coolable.CoolingType.valueOf(eff.getKey()), eff.getValue().getAsDouble());
                }
                return trait;
            }
            case "toxin": {
                FT_Toxin trait = new FT_Toxin();
                for (JsonElement element : json.getAsJsonArray("entries")) {
                    JsonObject entry = element.getAsJsonObject();
                    boolean fullBody = entry.get("full_body").getAsBoolean();
                    switch (entry.get("kind").getAsString()) {
                        case "direct_damage" -> trait.addEntry(new FT_Toxin.ToxinDirectDamage(
                                ResourceKey.create(Registries.DAMAGE_TYPE, Ids.parse(entry.get("damage").getAsString())),
                                entry.get("amount").getAsFloat(), entry.get("delay").getAsInt(), hazard(entry), fullBody));
                        case "effects" -> {
                            FT_Toxin.ToxinEffects effects = new FT_Toxin.ToxinEffects(hazard(entry), fullBody);
                            for (JsonElement effect : entry.getAsJsonArray("effects")) {
                                JsonObject e = effect.getAsJsonObject();
                                String id = e.get("id").getAsString();
                                int duration = e.get("duration").getAsInt();
                                int amplifier = e.has("amplifier") ? e.get("amplifier").getAsInt() : 0;
                                // resolved when applied: HBM's own effects are not registered when fluids are created
                                Supplier<MobEffectInstance> template = () -> Effects.instance(id, duration, amplifier);
                                effects.add(template);
                            }
                            trait.addEntry(effects);
                        }
                        default -> throw new IllegalArgumentException("unknown toxin kind " + entry.get("kind"));
                    }
                }
                return trait;
            }
            case "polluting": {
                FT_Polluting trait = new FT_Polluting();
                if (json.has("release")) for (Map.Entry<String, JsonElement> p : json.getAsJsonObject("release").entrySet()) {
                    trait.release(PollutionType.valueOf(p.getKey()), p.getValue().getAsFloat());
                }
                if (json.has("burn")) for (Map.Entry<String, JsonElement> p : json.getAsJsonObject("burn").entrySet()) {
                    trait.burn(PollutionType.valueOf(p.getKey()), p.getValue().getAsFloat());
                }
                return trait;
            }
            default: {
                TraitType extra = EXTRA_TRAITS.get(name);
                if (extra == null) throw new IllegalArgumentException("unknown trait '" + name + "'");
                return extra.reader().apply(json);
            }
        }
    }

    // Patches

    /**
     * The patch folder: {@code data/<own ns>/fluid_property_patch/<fluid ns>/<fluid path>.json} changes another mod's
     * fluid (or one's own) without replacing its {@code fluid_property} file. A same-path override of the file would
     * depend on pack order, which differs between loaders (Fabric 1.20.1), so the project never relies on one (as with
     * {@code recipe_removals}). Each patch lives under its own namespace, so every mod's patches load side by side.
     *
     * <pre>
     * {
     *   "remove": ["polluting", "gaseous"],   trait, flag or phase names to take away
     *   "add": {                              the property file's "type", "flags" and "traits" shapes; a trait the
     *     "traits": {"ntmspace:rocket": {"isp": 308, "thrust": 981000}}     fluid already has is replaced whole
     *   },
     *   "color": "#FFFFFF", "tint": "#FFFFFF", "temperature": 20, "hazard": {...}   optional overrides
     * }
     * </pre>
     *
     * After every load each patched fluid starts again from its own {@code fluid_property} file (or, for a fluid without
     * one, its state before it was first patched), then takes every patch in patch-id order (the patching mod's
     * namespace): removals, then additions, then the overrides. A patch that disappears on {@code /reload} takes its
     * change with it. A patch naming an unregistered fluid (its mod is not installed) is ignored.
     */
    public static final String PATCH_FOLDER = "fluid_property_patch";

    /** The last datapack load's property files, by fluid id: what a patched fluid starts again from. */
    private static Map<String, JsonElement> baseFiles = Map.of();
    /** The last datapack load's patch files, and its patches by fluid id in application order. */
    private static Map<String, JsonElement> patchFiles = Map.of();
    private static Map<String, List<JsonObject>> patches = Map.of();
    /** A patched fluid's state before its first patch, for a fluid without a property file. */
    private static final Map<FluidType, Pristine> PRISTINE = new IdentityHashMap<>();
    private static final Map<FluidType, Pristine> FOREIGN_DEFAULTS = Collections.synchronizedMap(new IdentityHashMap<>());

    private static void rememberForeign(FluidType fluid) {
        if (fluid.isForeign()) FOREIGN_DEFAULTS.computeIfAbsent(fluid, Pristine::of);
    }

    private record Pristine(Properties properties, boolean defined) {

        static Pristine of(FluidType fluid) {
            return new Pristine(fluid.ownProperties(), fluid.hasOwnProperties());
        }

        void restore(FluidType fluid) {
            var color = properties.color();
            fluid.applyProperties(color, properties.tint() != null ? properties.tint() : color, properties.temperature(),
                    properties.hazard(), properties.traits(), properties.shares(), defined);
        }
    }

    /** Declares both data folders (synced to clients by Golem {@code DataFiles}). HBM's entry calls this at mod construction. */
    public static void declare() {
        DataFiles.folder(FOLDER, FluidProperties::apply, ClientFluidProperties::stageBase);
        DataFiles.folder(PATCH_FOLDER, FluidProperties::applyPatches, ClientFluidProperties::stagePatches);
        dev.golem.core.net.GolemConfiguration.declareTask(FluidPropertySnapshot.CODEC, FluidProperties::serverSnapshot);
        DataFiles.AFTER_SYNC.register((server, players) -> {
            var snapshot = serverSnapshot();
            for (var player : players) dev.golem.core.net.GolemNetwork.sendToPlayer(player, FluidPropertySnapshot.CODEC, snapshot);
        });
    }

    /** Captures all resolved metadata by exact vanilla registry name; no live trait object enters the payload. */
    public static synchronized FluidPropertySnapshot serverSnapshot() {
        var entries = new java.util.TreeMap<String, FluidPropertySnapshot.Entry>();
        var kinds = new java.util.TreeMap<String, String>();
        for (var vanilla : net.minecraft.core.registries.BuiltInRegistries.FLUID) {
            var type = Fluids.fromVanillaExact(vanilla);
            var exact = Fluids.toVanilla(type);
            var name = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(exact).toString();
            if (entries.containsKey(name)) continue;
            var kind = Fluids.kindOf(type);
            entries.put(name, new FluidPropertySnapshot.Entry(write(type).toString(),
                    com.hbm.inventory.FluidContainers.of(type), type.hasOwnProperties()));
            kinds.put(name, net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(Fluids.toVanilla(kind)).toString());
        }
        return new FluidPropertySnapshot(entries, kinds);
    }

    /** The server's own declarations, keyed as vanilla fluids, for diagnostics and server integrations. */
    public static synchronized Map<net.minecraft.world.level.material.Fluid, Properties> serverProperties() {
        var owned = new java.util.HashMap<net.minecraft.world.level.material.Fluid, Properties>();
        var snapshot = serverSnapshot();
        for (var row : snapshot.entries().entrySet()) {
            if (row.getValue().own()) {
                var fluid = Fluids.toVanilla(Fluids.byRegistryNameExact(row.getKey()));
                owned.put(fluid, read(com.google.gson.JsonParser.parseString(row.getValue().properties()).getAsJsonObject()));
            }
        }
        return Map.copyOf(owned);
    }

    /** The patch files of the last load, as {@link #applyPatches} received them (GameTests put them back with it). */
    public static synchronized Map<String, JsonElement> patchFiles() {
        return patchFiles;
    }

    /**
     * Applies every patch of a datapack load ({@code <own ns>:<fluid ns>/<fluid path>} to its JSON) after undoing the
     * previous load's patches. The two folders arrive in either order (and separately on a client): each recomputes
     * the patched fluids from the latest files of both, so the end state never depends on the order.
     */
    public static synchronized void applyPatches(Map<String, JsonElement> files) {
        patchFiles = Map.copyOf(files);
        Map<String, List<JsonObject>> next = new LinkedHashMap<>();
        List<String> failures = new ArrayList<>();
        // by the patching namespace, then the path (a plain string sort would put "mod2:" before "mod:")
        Map<String, JsonElement> ordered = new TreeMap<>(Comparator
                .comparing((String id) -> id.substring(0, Math.max(id.indexOf(':'), 0)))
                .thenComparing(id -> id.substring(id.indexOf(':') + 1)));
        ordered.putAll(files);
        for (Map.Entry<String, JsonElement> file : ordered.entrySet()) {
            String[] owner = file.getKey().split(":", 2);
            int slash = owner.length == 2 ? owner[1].indexOf('/') : -1;
            if (slash <= 0) {
                failures.add(file.getKey() + " is not <fluid namespace>/<fluid>");
                continue;
            }
            String fluidId = owner[1].substring(0, slash) + ":" + owner[1].substring(slash + 1);
            FluidType fluid = Fluids.byRegistryNameExact(fluidId);
            if (fluid == Fluids.NONE && !fluidId.equals(Fluids.NONE.getRegistryName())) continue;
            if (!file.getValue().isJsonObject()) {
                failures.add(file.getKey() + " is not an object");
                continue;
            }
            next.computeIfAbsent(fluid.getRegistryName(), k -> new ArrayList<>()).add(file.getValue().getAsJsonObject());
        }
        Map<String, List<JsonObject>> previous = patches;
        patches = next;
        Set<String> touched = new TreeSet<>(previous.keySet());
        touched.addAll(next.keySet());
        int applied = 0;
        for (String id : touched) {
            FluidType fluid = Fluids.byRegistryNameExact(id);
            try {
                reset(fluid, previous.containsKey(id));
                applied += patch(fluid, next.get(id));
                if (fluid.isForeign() && !baseFiles.containsKey(id) && !next.containsKey(id)) {
                    FOREIGN_DEFAULTS.remove(fluid);
                    PRISTINE.remove(fluid);
                }
            } catch (RuntimeException e) {
                failures.add(id + ": " + e.getMessage());
            }
        }
        Constants.LOG.info("[HBM] Applied {} fluid property patch(es) to {} fluid(s)", applied, next.size());
        if (!failures.isEmpty()) Constants.LOG.warn("[HBM] Skipped fluid property patch(es): {}", failures);
    }

    /** Puts a fluid back to its unpatched state: its property file, or its state before it was first patched. */
    private static void reset(FluidType fluid, boolean wasPatched) {
        JsonElement base = baseFiles.get(fluid.getRegistryName());
        if (base != null && base.isJsonObject()) apply(fluid, base.getAsJsonObject());
        else if (wasPatched && PRISTINE.containsKey(fluid)) PRISTINE.get(fluid).restore(fluid);
    }

    /** Applies {@code list} (may be null) to {@code fluid}, remembering its unpatched state first; returns the count. */
    private static int patch(FluidType fluid, List<JsonObject> list) {
        if (list == null || list.isEmpty()) return 0;
        PRISTINE.putIfAbsent(fluid, Pristine.of(fluid));
        for (JsonObject json : list) patch(fluid, json);
        return list.size();
    }

    /**
     * One {@link #PATCH_FOLDER} file as values: what {@link #readPatch} makes of it and {@link #writePatch} writes.
     * {@code remove} holds trait, flag or phase names; {@code add} the traits of {@code "add"} in file order; an override
     * is null when the file has none (the fluid keeps its own).
     */
    public record Patch(List<String> remove, List<Object> add, @org.jspecify.annotations.Nullable Integer color,
                        @org.jspecify.annotations.Nullable Integer tint, @org.jspecify.annotations.Nullable Integer temperature,
                        @org.jspecify.annotations.Nullable FluidHazard hazard,
                        @org.jspecify.annotations.Nullable List<TagKey<Fluid>> shares) {

        public Patch {
            remove = List.copyOf(remove);
            add = List.copyOf(add);
            if (shares != null) shares = List.copyOf(shares);
        }

        public Patch(List<String> remove, List<Object> add, @org.jspecify.annotations.Nullable Integer color,
                     @org.jspecify.annotations.Nullable Integer tint, @org.jspecify.annotations.Nullable Integer temperature,
                     @org.jspecify.annotations.Nullable FluidHazard hazard) {
            this(remove, add, color, tint, temperature, hazard, null);
        }
    }

    /** Reads a {@link #PATCH_FOLDER} file. */
    public static Patch readPatch(JsonObject json) {
        List<String> remove = new ArrayList<>();
        if (json.has("remove")) for (JsonElement name : json.getAsJsonArray("remove")) remove.add(name.getAsString());
        List<Object> add = new ArrayList<>();
        if (json.has("add")) readTraits(json.getAsJsonObject("add"), add);
        return new Patch(remove, add, json.has("color") ? color(json.get("color")) : null,
                json.has("tint") ? color(json.get("tint")) : null,
                json.has("temperature") ? json.get("temperature").getAsInt() : null,
                json.has("hazard") ? FluidHazard.read(json.getAsJsonObject("hazard")) : null,
                json.has("shares") ? readShares(json.get("shares")) : null);
    }

    /** {@code patch} as a {@link #PATCH_FOLDER} file: the inverse of {@link #readPatch}. */
    public static JsonObject writePatch(Patch patch) {
        JsonObject json = new JsonObject();
        if (!patch.remove().isEmpty()) {
            JsonArray remove = new JsonArray();
            patch.remove().forEach(remove::add);
            json.add("remove", remove);
        }
        if (!patch.add().isEmpty()) {
            // the property file's "type", "flags" and "traits" (write always states a temperature; "add" has none)
            JsonObject add = write(new Properties(null, null, FluidType.ROOM_TEMPERATURE, FluidHazard.NONE, patch.add()));
            add.remove("temperature");
            json.add("add", add);
        }
        if (patch.color() != null) json.addProperty("color", hex(patch.color()));
        if (patch.tint() != null) json.addProperty("tint", hex(patch.tint()));
        if (patch.temperature() != null) json.addProperty("temperature", patch.temperature());
        if (patch.hazard() != null) json.add("hazard", patch.hazard().write());
        if (patch.shares() != null) json.add("shares", writeShares(patch.shares()));
        return json;
    }

    /** Applies one patch to {@code fluid}'s current properties: removals, additions, then the overrides. */
    public static void patch(FluidType fluid, JsonObject json) {
        rememberForeign(fluid);
        ensureNames();
        Patch patch = readPatch(json);
        Map<Class<?>, Object> traits = new LinkedHashMap<>();
        for (Object trait : fluid.traits()) traits.put(trait.getClass(), trait);
        for (String name : patch.remove()) traits.remove(traitClass(name));
        for (Object trait : patch.add()) traits.put(trait.getClass(), trait);
        int color = patch.color() != null ? patch.color() : fluid.getColor();
        int tint = patch.tint() != null ? patch.tint() : fluid.getTint();
        int temperature = patch.temperature() != null ? patch.temperature() : fluid.getTemperature();
        FluidHazard hazard = patch.hazard() != null ? patch.hazard() : fluid.getHazard();
        fluid.applyProperties(color, tint, temperature, hazard, new ArrayList<>(traits.values()),
                patch.shares() != null ? patch.shares() : fluid.getSharedTags());
    }

    /** The class a trait, flag or phase name stands for. */
    private static Class<?> traitClass(String name) {
        Object simple = PHASES.containsKey(name) ? PHASES.get(name) : FLAGS.get(name);
        if (simple != null) return simple.getClass();
        Class<?> own = switch (name) {
            case "corrosive" -> FT_Corrosive.class;
            case "flammable" -> FT_Flammable.class;
            case "combustible" -> FT_Combustible.class;
            case "poison" -> FT_Poison.class;
            case "pwrmoderator" -> FT_PWRModerator.class;
            case "pheromone" -> FT_Pheromone.class;
            case "ventradiation" -> FT_VentRadiation.class;
            case "heatable" -> FT_Heatable.class;
            case "coolable" -> FT_Coolable.class;
            case "toxin" -> FT_Toxin.class;
            case "polluting" -> FT_Polluting.class;
            default -> null;
        };
        if (own != null) return own;
        TraitType extra = EXTRA_TRAITS.get(name);
        if (extra == null) throw new IllegalArgumentException("unknown trait '" + name + "'");
        return extra.type();
    }

    /**
     * A mod's own patches from its own jar, when it creates its fluids (like {@link #applyBundled}): for every fluid
     * registered so far, {@code /data/<namespace>/fluid_property_patch/<fluid ns>/<fluid path>.json} next to
     * {@code owner}'s code. So registration, menus and tooltips see an addon's changes to HBM's fluids before any world;
     * the first datapack load recomputes them from the same files.
     *
     * <p>Two mods patching one fluid: every call re-applies all bundles registered so far to the fluids they patch, from
     * the fluid's unpatched state and in the patching namespace's order, which is the order a datapack load applies the
     * same files in. So the state before any world is the state after the first load, whichever mod declared first.
     */
    public static synchronized void applyBundledPatches(Class<?> owner, String namespace) {
        BUNDLES.put(namespace, owner);
        int applied = 0;
        for (FluidType fluid : Fluids.getAll()) {
            String[] id = fluid.getRegistryName().split(":", 2);
            List<JsonObject> list = new ArrayList<>();
            for (Map.Entry<String, Class<?>> bundle : BUNDLES.entrySet()) {
                String path = "/data/" + bundle.getKey() + "/" + PATCH_FOLDER + "/" + id[0] + "/" + id[1] + ".json";
                URL url = DataFiles.bundled(bundle.getValue(), path);
                if (url == null) continue;
                try (InputStream in = url.openStream()) {
                    list.add(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
                } catch (IOException | RuntimeException e) {
                    throw new IllegalStateException("The bundled fluid property patch " + path + " is broken: " + e.getMessage(), e);
                }
            }
            if (list.isEmpty()) continue;
            Pristine pristine = PRISTINE.get(fluid);
            if (pristine != null) pristine.restore(fluid);
            else PRISTINE.put(fluid, Pristine.of(fluid));
            for (JsonObject json : list) patch(fluid, json);
            applied += list.size();
        }
        Constants.LOG.info("[HBM] Applied {} bundled fluid property patch(es) after {} ({} bundle(s))", applied,
                owner.getSimpleName(), BUNDLES.size());
    }

    /** Every mod's bundled patches so far, by patching namespace (sorted: the datapack order). */
    private static final TreeMap<String, Class<?>> BUNDLES = new TreeMap<>();

    // Writing

    /** {@code fluid}'s properties as a {@code fluid_property} file. */
    public static JsonObject write(FluidType fluid) {
        try {
            return write(Properties.of(fluid));
        } catch (IllegalStateException e) {
            throw new IllegalStateException(fluid.getRegistryName() + " " + e.getMessage(), e);
        }
    }

    /** {@code properties} as a {@code fluid_property} file: the inverse of {@link #read}. */
    public static JsonObject write(Properties properties) {
        ensureNames();
        Traits fluid = new Traits(properties.traits());
        JsonObject json = new JsonObject();
        // in trait order, which read takes from the file (hbm-space's methanol is gaseous, then liquid)
        JsonArray phases = new JsonArray();
        for (Object trait : fluid.byClass.values()) {
            for (Map.Entry<String, Object> phase : PHASES.entrySet()) {
                if (phase.getValue().getClass() == trait.getClass()) phases.add(phase.getKey());
            }
        }
        if (phases.size() == 1) json.add("type", phases.get(0));
        else if (phases.size() > 1) json.add("type", phases);
        if (properties.color() != null) json.addProperty("color", hex(properties.color()));
        if (properties.tint() != null) json.addProperty("tint", hex(properties.tint()));
        json.addProperty("temperature", properties.temperature());
        if (!properties.hazard().equals(FluidHazard.NONE)) json.add("hazard", properties.hazard().write());
        if (!properties.shares().isEmpty()) json.add("shares", writeShares(properties.shares()));
        JsonArray flags = new JsonArray();
        for (Map.Entry<String, Object> flag : FLAGS.entrySet()) {
            if (fluid.hasTrait(flag.getValue().getClass())) flags.add(flag.getKey());
        }
        if (!flags.isEmpty()) json.add("flags", flags);
        JsonObject traits = new JsonObject();
        for (String name : List.of("corrosive", "flammable", "combustible", "poison", "pwrmoderator", "pheromone", "ventradiation", "heatable", "coolable",
                "toxin", "polluting")) {
            JsonObject body = writeTrait(name, fluid);
            if (body != null) traits.add(name, body);
        }
        synchronized (EXTRA_TRAITS) {
            for (Map.Entry<String, TraitType> extra : EXTRA_TRAITS.entrySet()) {
                Object trait = fluid.getTrait(extra.getValue().type());
                if (trait != null) traits.add(extra.getKey(), extra.getValue().writer().apply(trait));
            }
        }
        int known = traits.size() + flags.size() + phases.size();
        if (known != properties.traits().size()) {
            throw new IllegalStateException("has traits the property file cannot hold: " + properties.traits());
        }
        if (traits.size() > 0) json.add("traits", traits);
        return json;
    }

    /** A trait list looked up by class, as {@link FluidType} does (the last trait of a class wins). */
    private static final class Traits {
        private final Map<Class<?>, Object> byClass = new LinkedHashMap<>();

        Traits(List<Object> traits) {
            for (Object trait : traits) byClass.put(trait.getClass(), trait);
        }

        boolean hasTrait(Class<?> type) {
            return byClass.containsKey(type);
        }

        <T> T getTrait(Class<T> type) {
            return type.cast(byClass.get(type));
        }
    }

    private static String hex(int color) {
        return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

    private static JsonObject writeTrait(String name, Traits fluid) {
        JsonObject json = new JsonObject();
        switch (name) {
            case "corrosive" -> {
                FT_Corrosive t = fluid.getTrait(FT_Corrosive.class);
                if (t == null) return null;
                json.addProperty("rating", t.getRating());
            }
            case "flammable" -> {
                FT_Flammable t = fluid.getTrait(FT_Flammable.class);
                if (t == null) return null;
                json.addProperty("energy", t.getHeatEnergy());
            }
            case "combustible" -> {
                FT_Combustible t = fluid.getTrait(FT_Combustible.class);
                if (t == null) return null;
                json.addProperty("grade", t.getGrade().name());
                json.addProperty("energy", t.getCombustionEnergy());
            }
            case "poison" -> {
                FT_Poison t = fluid.getTrait(FT_Poison.class);
                if (t == null) return null;
                json.addProperty("withering", t.isWithering());
                json.addProperty("level", t.getLevel());
            }
            case "pwrmoderator" -> {
                FT_PWRModerator t = fluid.getTrait(FT_PWRModerator.class);
                if (t == null) return null;
                json.addProperty("multiplier", t.getMultiplier());
            }
            case "pheromone" -> {
                FT_Pheromone t = fluid.getTrait(FT_Pheromone.class);
                if (t == null) return null;
                json.addProperty("type", t.getType());
            }
            case "ventradiation" -> {
                FT_VentRadiation t = fluid.getTrait(FT_VentRadiation.class);
                if (t == null) return null;
                json.addProperty("rad_per_mb", t.getRadPerMB());
            }
            case "heatable" -> {
                FT_Heatable t = fluid.getTrait(FT_Heatable.class);
                if (t == null) return null;
                JsonArray steps = new JsonArray();
                for (FT_Heatable.HeatingStep step : t.steps()) {
                    JsonObject s = new JsonObject();
                    s.addProperty("amount_req", step.amountReq);
                    s.addProperty("heat_req", step.heatReq);
                    s.addProperty("produces", step.typeProduced.getRegistryName());
                    s.addProperty("amount_produced", step.amountProduced);
                    steps.add(s);
                }
                json.add("steps", steps);
                JsonObject eff = new JsonObject();
                for (FT_Heatable.HeatingType type : FT_Heatable.HeatingType.values()) {
                    if (t.getEfficiencies().containsKey(type)) eff.addProperty(type.name(), t.getEfficiencies().get(type));
                }
                json.add("efficiency", eff);
            }
            case "coolable" -> {
                FT_Coolable t = fluid.getTrait(FT_Coolable.class);
                if (t == null) return null;
                json.addProperty("cools_to", t.coolsTo.getRegistryName());
                json.addProperty("amount_req", t.amountReq);
                json.addProperty("amount_produced", t.amountProduced);
                json.addProperty("heat_energy", t.heatEnergy);
                JsonObject eff = new JsonObject();
                for (FT_Coolable.CoolingType type : FT_Coolable.CoolingType.values()) {
                    if (t.getEfficiencies().containsKey(type)) eff.addProperty(type.name(), t.getEfficiencies().get(type));
                }
                json.add("efficiency", eff);
            }
            case "toxin" -> {
                FT_Toxin t = fluid.getTrait(FT_Toxin.class);
                if (t == null) return null;
                JsonArray entries = new JsonArray();
                for (FT_Toxin.ToxinEntry entry : t.entries) {
                    JsonObject e = new JsonObject();
                    if (entry instanceof FT_Toxin.ToxinDirectDamage damage) {
                        e.addProperty("kind", "direct_damage");
                        e.addProperty("damage", Ids.keyId(damage.damage).toString());
                        e.addProperty("amount", damage.amount);
                        e.addProperty("delay", damage.delay);
                    } else if (entry instanceof FT_Toxin.ToxinEffects effects) {
                        e.addProperty("kind", "effects");
                        JsonArray list = new JsonArray();
                        for (Supplier<MobEffectInstance> template : effects.effects) {
                            MobEffectInstance instance = template.get();
                            JsonObject effect = new JsonObject();
                            effect.addProperty("id", Effects.id(instance));
                            effect.addProperty("duration", instance.getDuration());
                            effect.addProperty("amplifier", instance.getAmplifier());
                            list.add(effect);
                        }
                        e.add("effects", list);
                    } else {
                        throw new IllegalStateException("toxin entry kind not in the property file: " + entry.getClass());
                    }
                    if (entry.clazz != null) e.addProperty("hazard", entry.clazz.name());
                    e.addProperty("full_body", entry.fullBody);
                    entries.add(e);
                }
                json.add("entries", entries);
            }
            case "polluting" -> {
                FT_Polluting t = fluid.getTrait(FT_Polluting.class);
                if (t == null) return null;
                JsonObject release = new JsonObject();
                t.releaseMap.forEach((type, amount) -> release.add(type.name(), new JsonPrimitive(amount)));
                JsonObject burn = new JsonObject();
                t.burnMap.forEach((type, amount) -> burn.add(type.name(), new JsonPrimitive(amount)));
                if (release.size() > 0) json.add("release", release);
                if (burn.size() > 0) json.add("burn", burn);
            }
            default -> throw new IllegalArgumentException(name);
        }
        return json;
    }
}
