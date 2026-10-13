package com.hbm.api.script;

import com.hbm.capability.HbmLivingProps;
import com.hbm.handler.HazmatRegistry;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.hazard.type.IHazardType;
import com.hbm.inventory.fluid.FluidHazard;
import com.hbm.inventory.fluid.FluidProperties;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.util.ContaminationUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.core.Ids;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The {@code HBM} script global (KubeJS): a small, stable facade over HBM's public APIs. It adds no behaviour of its
 * own: every setter is the same call HBM's own code makes (radiation still caps at 2500, digamma at 10 kills, asbestos
 * and black lung at their maximum hurt), and every lookup returns plain values (strings, numbers, booleans, and
 * read-only lists and maps of them), never a live HBM object a script could change.
 *
 * <p>Entity and chunk setters act on the logical server only and do nothing on a client. Ids are strings:
 * fluids by registry name ({@code "hbm:oil"}, {@code "minecraft:water"}, a bare {@code "oil"} is {@code hbm:oil}),
 * materials by name ({@code "iron"}), hazard types by id ({@code "hbm:radiation"}).
 *
 * <pre>{@code
 * // server script
 * PlayerEvents.tick(e => { if (HBM.getRadiation(e.player) > 800) e.player.tell('Get out!') })
 * HBM.addChunkRadiation(e.level, e.player.blockPosition(), 50)
 * HBM.fluid('hbm:oil').temperature
 * }</pre>
 */
public final class HbmScriptApi {

    public static final HbmScriptApi INSTANCE = new HbmScriptApi();

    private HbmScriptApi() {}

    // Entities (HbmLivingProps)

    /** Accumulated radiation in RAD (0..2500). */
    public double getRadiation(LivingEntity entity) {
        return HbmLivingProps.getRadiation(entity);
    }

    public void setRadiation(LivingEntity entity, double rad) {
        if (server(entity)) HbmLivingProps.setRadiation(entity, rad);
    }

    /** Adds {@code rad} directly (no armour, no immunity; clamped to 0..2500); see {@link #contaminate} for a dose. */
    public void addRadiation(LivingEntity entity, double rad) {
        if (server(entity)) HbmLivingProps.incrementRadiation(entity, rad);
    }

    /** Neutron activation the entity carries (what its next contamination adds). */
    public double getNeutron(LivingEntity entity) {
        return HbmLivingProps.getNeutron(entity);
    }

    public void setNeutron(LivingEntity entity, double rad) {
        if (server(entity)) HbmLivingProps.setNeutron(entity, rad);
    }

    /** Digamma (0..10; each point halves max health, 10 kills). */
    public double getDigamma(LivingEntity entity) {
        return HbmLivingProps.getDigamma(entity);
    }

    public void setDigamma(LivingEntity entity, double digamma) {
        if (server(entity)) HbmLivingProps.setDigamma(entity, digamma);
    }

    public void addDigamma(LivingEntity entity, double digamma) {
        if (server(entity)) HbmLivingProps.incrementDigamma(entity, digamma);
    }

    /** Asbestos exposure in ticks; {@link #getMaxAsbestos()} hurts and resets. */
    public int getAsbestos(LivingEntity entity) {
        return HbmLivingProps.getAsbestos(entity);
    }

    public void setAsbestos(LivingEntity entity, int asbestos) {
        if (server(entity)) HbmLivingProps.setAsbestos(entity, asbestos);
    }

    public void addAsbestos(LivingEntity entity, int asbestos) {
        if (server(entity)) HbmLivingProps.incrementAsbestos(entity, asbestos);
    }

    public int getMaxAsbestos() {
        return HbmLivingProps.maxAsbestos;
    }

    /** Coal dust (black lung) exposure in ticks; {@link #getMaxBlackLung()} hurts and resets. */
    public int getBlackLung(LivingEntity entity) {
        return HbmLivingProps.getBlackLung(entity);
    }

    public void setBlackLung(LivingEntity entity, int blackLung) {
        if (server(entity)) HbmLivingProps.setBlackLung(entity, blackLung);
    }

    public void addBlackLung(LivingEntity entity, int blackLung) {
        if (server(entity)) HbmLivingProps.incrementBlackLung(entity, blackLung);
    }

    public int getMaxBlackLung() {
        return HbmLivingProps.maxBlacklung;
    }

    /**
     * HBM's own contamination path ({@code ContaminationUtil.contaminate}): armour, creative mode, immunity and
     * radiation resistance apply as for any HBM source.
     *
     * @param hazardType        {@code MONOXIDE}, {@code RADIATION}, {@code NEUTRON} or {@code DIGAMMA}
     * @param contaminationType what protects against it: {@code FARADAY}, {@code HAZMAT}, {@code HAZMAT2},
     *                          {@code DIGAMMA}, {@code DIGAMMA2}, {@code CREATIVE}, {@code RAD_BYPASS} or {@code NONE}
     * @return whether it got through; false on a client or for an unknown name
     */
    public boolean contaminate(LivingEntity entity, String hazardType, String contaminationType, double amount) {
        if (!server(entity)) return false;
        ContaminationUtil.HazardType hazard = enumOrNull(ContaminationUtil.HazardType.class, hazardType);
        ContaminationUtil.ContaminationType cont = enumOrNull(ContaminationUtil.ContaminationType.class, contaminationType);
        if (hazard == null || cont == null) return false;
        return ContaminationUtil.contaminate(entity, hazard, cont, amount);
    }

    /** Whether radiation does not affect {@code entity} (HBM's immune mobs, other mods' answers). */
    public boolean isRadImmune(Entity entity) {
        return ContaminationUtil.isRadImmune(entity);
    }

    /** The entity's radiation resistance (armour, cladding, Rad-X), as the geiger counter shows it. */
    public double getRadiationResistance(LivingEntity entity) {
        return HazmatRegistry.getResistance(entity);
    }

    /** The share of a radiation dose that reaches {@code entity} after its resistance (1 = all of it). */
    public double getRadiationMultiplier(LivingEntity entity) {
        return ContaminationUtil.calculateRadiationMod(entity);
    }

    /** One armour piece's radiation resistance, cladding included. */
    public double getRadiationResistance(ItemStack stack) {
        return HazmatRegistry.getResistance(stack);
    }

    // Chunk radiation (RadiationSystemNT; server levels only, 0 / no-op on a client)

    public double getChunkRadiation(Level level, BlockPos pos) {
        return onServerThread(level, () -> ChunkRadiationManager.proxy.getRadiation(level, pos));
    }

    public double getChunkRadiation(Level level, double x, double y, double z) {
        return getChunkRadiation(level, BlockPos.containing(x, y, z));
    }

    public void setChunkRadiation(Level level, BlockPos pos, double rad) {
        onServerThread(level, () -> ChunkRadiationManager.proxy.setRadiation(level, pos, rad));
    }

    public void setChunkRadiation(Level level, double x, double y, double z, double rad) {
        setChunkRadiation(level, BlockPos.containing(x, y, z), rad);
    }

    public void addChunkRadiation(Level level, BlockPos pos, double rad) {
        onServerThread(level, () -> ChunkRadiationManager.proxy.incrementRad(level, pos, rad));
    }

    public void addChunkRadiation(Level level, double x, double y, double z, double rad) {
        addChunkRadiation(level, BlockPos.containing(x, y, z), rad);
    }

    /** Adds {@code rad} but not above {@code max} (what HBM's radiation sources do). */
    public void addChunkRadiationCapped(Level level, BlockPos pos, double rad, double max) {
        onServerThread(level, () -> ChunkRadiationManager.proxy.incrementRad(level, pos, rad, max));
    }

    public void removeChunkRadiation(Level level, BlockPos pos, double rad) {
        onServerThread(level, () -> ChunkRadiationManager.proxy.decrementRad(level, pos, rad));
    }

    public void removeChunkRadiation(Level level, double x, double y, double z, double rad) {
        removeChunkRadiation(level, BlockPos.containing(x, y, z), rad);
    }

    // Fluids

    /** Every HBM fluid id, in HBM's display order ({@code Fluids.getInNiceOrder}), {@code hbm:none} excluded. */
    public List<String> fluids() {
        List<String> ids = new ArrayList<>();
        for (FluidType fluid : Fluids.getInNiceOrder()) if (fluid != Fluids.NONE) ids.add(fluid.getRegistryName());
        return Collections.unmodifiableList(ids);
    }

    /** Whether {@code id} names an HBM fluid (any registered fluid is one: another mod's becomes HBM's when met). */
    public boolean hasFluid(String id) {
        return lookupFluid(id) != null;
    }

    /**
     * What HBM knows about a fluid, or null for an unknown id: {@code id}, {@code name} (the 1.7 name),
     * {@code displayName}, {@code color}, {@code tint}, {@code temperature} (degrees Celsius), {@code hot},
     * {@code foreign} (another mod's), {@code traits} (as {@link #fluidTraits}), {@code hazard} (as {@link #fluidHazard})
     * and {@code properties} (the fluid's {@code fluid_property} JSON, as a string for {@code JSON.parse}).
     */
    public @Nullable Map<String, Object> fluid(String id) {
        FluidType fluid = lookupFluid(id);
        if (fluid == null) return null;
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", fluid.getRegistryName());
        info.put("name", fluid.getName());
        info.put("displayName", fluid.getDisplayNameString());
        info.put("color", fluid.getColor());
        info.put("tint", fluid.getTint());
        info.put("temperature", fluid.temperature);
        info.put("hot", fluid.isHot());
        info.put("foreign", fluid.isForeign());
        info.put("traits", traits(fluid));
        info.put("hazard", hazard(fluid));
        info.put("properties", properties(fluid).toString());
        return Collections.unmodifiableMap(info);
    }

    /**
     * The fluid's trait and flag names as its {@code fluid_property} file spells them: its phase ({@code liquid},
     * {@code gaseous}, ...), flags ({@code leadcontainer}, {@code viscous}, ...) and parameterised traits ({@code flammable},
     * {@code corrosive}, ...); empty for an unknown id.
     */
    public List<String> fluidTraits(String id) {
        FluidType fluid = lookupFluid(id);
        return fluid == null ? List.of() : traits(fluid);
    }

    /** Whether the fluid has the named trait or flag (as {@link #fluidTraits}). */
    public boolean fluidHasTrait(String id, String trait) {
        return fluidTraits(id).contains(trait);
    }

    /** The fluid's 1.7 hazard diamond: {@code poison}, {@code flammability}, {@code reactivity}, {@code symbol}. */
    public @Nullable Map<String, Object> fluidHazard(String id) {
        FluidType fluid = lookupFluid(id);
        return fluid == null ? null : hazard(fluid);
    }

    /** The fluid's temperature in degrees Celsius (room temperature 20), or 0 for an unknown id. */
    public int fluidTemperature(String id) {
        FluidType fluid = lookupFluid(id);
        return fluid == null ? 0 : fluid.temperature;
    }

    /** The fluid's colour as 0xRRGGBB, or 0 for an unknown id. */
    public int fluidColor(String id) {
        FluidType fluid = lookupFluid(id);
        return fluid == null ? 0 : fluid.getColor();
    }

    // Materials (Mats)

    /** Every material name, in registration order. */
    public List<String> materials() {
        List<String> names = new ArrayList<>();
        for (NTMMaterial material : Mats.orderedList) names.add(material.name);
        return Collections.unmodifiableList(names);
    }

    /**
     * What HBM knows about a material, or null for an unknown name: {@code name}, {@code displayName},
     * {@code solidColorLight}, {@code solidColorDark}, {@code moltenColor}, {@code smelting} ({@code SMELTABLE},
     * {@code ADDITIVE}, ...), {@code smeltsInto}, {@code convIn}, {@code convOut}, {@code autogen} (shape names) and
     * {@code tags} (shape name to its {@code c:} item tag).
     */
    public @Nullable Map<String, Object> material(String name) {
        NTMMaterial material = name == null ? null : Mats.byName(name);
        if (material == null) return null;
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", material.name);
        info.put("displayName", material.getDisplayName());
        info.put("solidColorLight", material.solidColorLight);
        info.put("solidColorDark", material.solidColorDark);
        info.put("moltenColor", material.moltenColor);
        info.put("smelting", material.smeltable.name());
        info.put("smeltsInto", material.smeltsInto == null ? material.name : material.smeltsInto.name);
        info.put("convIn", material.convIn);
        info.put("convOut", material.convOut);
        List<String> autogen = new ArrayList<>();
        for (MaterialShapes shape : material.autogen) autogen.add(shape.name());
        info.put("autogen", Collections.unmodifiableList(autogen));
        Map<String, String> tags = new LinkedHashMap<>();
        for (MaterialShapes shape : MaterialShapes.values()) {
            if (shape.quantity > 0) tags.put(shape.name(), material.tag(shape).location().toString());
        }
        info.put("tags", Collections.unmodifiableMap(tags));
        return Collections.unmodifiableMap(info);
    }

    // Item hazards (HazardSystem)

    /**
     * The hazards of a stack as HBM applies them: one entry per hazard, {@code type} (its id, {@code hbm:radiation})
     * and {@code level} (after its modifiers, as the tooltip shows it). Empty for a harmless stack.
     */
    public List<Map<String, Object>> itemHazards(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return List.of();
        List<Map<String, Object>> hazards = new ArrayList<>();
        for (HazardEntry entry : HazardSystem.getHazardsFromStack(stack)) {
            Map<String, Object> hazard = new LinkedHashMap<>();
            hazard.put("type", hazardId(entry.type));
            hazard.put("level", IHazardModifier.evalAllModifiers(stack, null, entry.baseLevel, entry.mods));
            hazards.add(Collections.unmodifiableMap(hazard));
        }
        return Collections.unmodifiableList(hazards);
    }

    /** The stack's total level of one hazard type ({@code "hbm:radiation"}, or a bare {@code "radiation"}). */
    public double itemHazardLevel(ItemStack stack, String type) {
        if (stack == null || stack.isEmpty() || type == null) return 0D;
        IHazardType hazard;
        try {
            hazard = HazardRegistry.getType(Ids.parse(type.indexOf(':') < 0 ? "hbm:" + type : type));
        } catch (RuntimeException invalid) {
            return 0D;
        }
        return hazard == null ? 0D : HazardSystem.getHazardLevelFromStack(stack, hazard);
    }

    /** The stack's radiation in RAD/s (what the tooltip and the geiger counter use). */
    public double itemRadiation(ItemStack stack) {
        return stack == null || stack.isEmpty() ? 0D : HazardSystem.getRawRadsFromStack(stack);
    }

    /** Every hazard type id ({@code hbm:radiation}, {@code hbm:asbestos}, ..., other mods' too). */
    public List<String> hazardTypes() {
        List<String> ids = new ArrayList<>();
        for (var id : HazardRegistry.types().keySet()) ids.add(id.toString());
        return Collections.unmodifiableList(ids);
    }


    private static boolean server(Entity entity) {
        return entity != null && !entity.level().isClientSide();
    }

    private static <E extends Enum<E>> @Nullable E enumOrNull(Class<E> type, @Nullable String name) {
        if (name == null) return null;
        try {
            return Enum.valueOf(type, name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** The fluid {@code id} names, or null; never logs HBM's "unknown fluid in saved data" warning. */
    private static @Nullable FluidType lookupFluid(@Nullable String id) {
        if (id == null || id.isBlank()) return null;
        String full = id.indexOf(':') < 0 ? "hbm:" + id.toLowerCase(Locale.ROOT) : id;
        for (FluidType fluid : Fluids.getAll()) {
            // the registry name, or the name it was registered under (water, lava and none read as hbm:water ... too)
            if (fluid.getRegistryName().equals(full) || (fluid.getNamespace() + ":" + fluid.getKey()).equals(full)) {
                return fluid == Fluids.NONE ? null : fluid;
            }
        }
        try {
            if (!BuiltInRegistries.FLUID.containsKey(Ids.parse(full))) return null;
        } catch (RuntimeException invalid) {
            return null;
        }
        FluidType fluid = Fluids.byRegistryName(full); // another mod's fluid: HBM's open model makes it an HBM fluid
        return fluid == Fluids.NONE ? null : fluid; // minecraft:empty is no fluid
    }

    private static JsonObject properties(FluidType fluid) {
        try {
            return FluidProperties.write(fluid);
        } catch (RuntimeException e) {
            return new JsonObject(); // a trait the file format cannot hold (an addon trait registered without a writer)
        }
    }

    private static List<String> traits(FluidType fluid) {
        JsonObject json = properties(fluid);
        List<String> names = new ArrayList<>();
        JsonElement type = json.get("type");
        if (type != null && type.isJsonArray()) for (JsonElement phase : type.getAsJsonArray()) names.add(phase.getAsString());
        else if (type != null) names.add(type.getAsString());
        if (json.has("flags")) for (JsonElement flag : json.getAsJsonArray("flags")) names.add(flag.getAsString());
        if (json.has("traits")) names.addAll(json.getAsJsonObject("traits").keySet());
        return Collections.unmodifiableList(names);
    }

    private static Map<String, Object> hazard(FluidType fluid) {
        FluidHazard hazard = fluid.getHazard();
        if (hazard == null) hazard = FluidHazard.NONE;
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("poison", hazard.poison());
        info.put("flammability", hazard.flammability());
        info.put("reactivity", hazard.reactivity());
        info.put("symbol", hazard.symbol().fileName());
        return Collections.unmodifiableMap(info);
    }

    private static String hazardId(IHazardType type) {
        var id = HazardRegistry.getTypeId(type);
        return id == null ? type.getClass().getSimpleName() : id.toString();
    }

    /**
     * Runs chunk-radiation work on the level's server thread: chunk radiation reads loaded chunks, which vanilla answers
     * there alone, and some script events arrive off it (KubeJS 6 posts chat on a worker thread). A write called
     * elsewhere is queued to the server thread; a read waits for it (never call one from a thread the server waits on).
     */
    private static void onServerThread(Level level, Runnable work) {
        if (level instanceof ServerLevel server && !server.getServer().isSameThread()) {
            server.getServer().execute(work);
        } else {
            work.run();
        }
    }

    private static double onServerThread(Level level, DoubleSupplier read) {
        if (level instanceof ServerLevel server && !server.getServer().isSameThread()) {
            return server.getServer().submit(read::getAsDouble).join();
        }
        return read.getAsDouble();
    }
}
