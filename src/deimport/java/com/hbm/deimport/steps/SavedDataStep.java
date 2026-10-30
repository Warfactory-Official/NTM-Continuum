package com.hbm.deimport.steps;

import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.Dimensions;
import com.hbm.deimport.engine.Step;
import com.hbm.deimport.engine.Unit;
import com.hbm.deimport.source.de7.WorldTables;
import com.hbm.delegacy.AtomicNbt;
import com.hbm.delegacy.DeAddons;
import com.hbm.delegacy.de7.Tables;
import com.hbm.delegacy.de7.Walker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * World saved data in every dimension. DE wraps each file's content in one more {@code data} level, which Continuum's
 * reader would reject and replace with empty data. Known files are unwrapped and reshaped; the others are parked whole.
 * Every original is parked first.
 */
public final class SavedDataStep implements Step {
    private static final List<String> KNOWN = List.of("hbm_impact_data", "hbm_pollution", "hbm_satellites");
    private static final String[] POLLUTION = {"soot", "poison", "heavymetal", "fallout"};
    private static final List<String> PARKED = List.of("hbm_annihilator", "hbm_soyuz_render");

    @Override
    public String id() {
        return "saveddata@1";
    }

    @Override
    public boolean applies(Ctx ctx) {
        return true;
    }

    /**
     * Each file is a unit: its original is parked and committed before the rewrite replaces it, and a rewritten file is
     * no longer wrapped, so a crash in between converts the original again and one after it finds nothing to do.
     */
    @Override
    public boolean run(Ctx ctx, @Nullable MinecraftServer server) throws IOException {
        convert(ctx, Dimensions.without(Dimensions.withData(ctx.world), ctx.deferredAddons));
        scoreboard(ctx);
        return true;
    }

    /** Also for an addon's dimensions that were left in DE's shape when this step ran. */
    public static void convert(Ctx ctx, Map<String, Path> dims) throws IOException {
        for (Map.Entry<String, Path> dim : dims.entrySet()) {
            for (String name : KNOWN) {
                Path file = dim.getValue().resolve("data/" + name + ".dat");
                String where = dim.getKey() + " " + name;
                CompoundTag root = read(ctx, file, where);
                if (root == null) continue;
                CompoundTag original = root.copy();
                CompoundTag outer = root.getCompound("data");
                boolean wrapped = outer.contains("data", Tag.TAG_COMPOUND);
                CompoundTag data = wrapped ? outer.getCompound("data") : outer;
                Unit unit = ctx.begin("saved data " + where);
                boolean reshaped;
                try {
                    reshaped = switch (name) {
                        case "hbm_pollution" -> pollution(data);
                        case "hbm_satellites" -> satellites(ctx, data, where);
                        default -> false;
                    };
                } catch (RuntimeException e) {
                    // left as it is with a copy kept, as an unreadable file is
                    ctx.abandon(unit);
                    ctx.parking.park("saved_data", where, "not converted: " + e, NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()));
                    ctx.report.line(where + " was not converted (" + e + ") and was left as it is");
                    continue;
                }
                if (!wrapped && !reshaped) {
                    ctx.abandon(unit);
                    continue;
                }
                ctx.parking.park("saved_data", where, name, original);
                root.put("data", data);
                ctx.commit(unit);
                AtomicNbt.write(root, file);
                ctx.report.count("saved data unwrapped");
            }
            for (String name : PARKED) {
                Path file = dim.getValue().resolve("data/" + name + ".dat");
                String where = dim.getKey() + " " + name;
                CompoundTag root = read(ctx, file, where);
                // Continuum saves its own annihilator under the same name, without DE's extra level
                if (root == null || !root.getCompound("data").contains("data", Tag.TAG_COMPOUND)) continue;
                Unit unit = ctx.begin("saved data " + where);
                ctx.parking.park("saved_data", where, name, root);
                ctx.commit(unit);
                Files.delete(file);
                ctx.report.line("saved data parked: " + where);
            }
        }
        for (Path dim : dims.values()) keepPowerGraph(ctx, dim);
    }

    /**
     * DE's power graph has Continuum's file name: Continuum reads it as empty and saves its own over it, while a later
     * start converting airships still needs DE's pylon wires from it. Moved aside, it stays DE's.
     */
    private static void keepPowerGraph(Ctx ctx, Path dim) throws IOException {
        Path graph = dim.resolve("data/hbm_power_graph.dat"), kept = dim.resolve(Ctx.DE_POWER_GRAPH);
        if (Files.exists(kept)) return;
        CompoundTag root = read(ctx, graph, "power graph");
        if (root != null && root.getCompound("data").contains("data", Tag.TAG_COMPOUND)) Files.move(graph, kept);
    }

    /**
     * An objective on a stat of a renamed id ({@code minecraft.mined:hbm.ore_uranium}) follows it; vanilla keeps one it
     * cannot resolve, but as a dummy that stops counting.
     */
    private static void scoreboard(Ctx ctx) throws IOException {
        Path file = ctx.world.resolve("data/scoreboard.dat");
        CompoundTag root = read(ctx, file, "scoreboard");
        if (root == null) return;
        Unit unit = ctx.begin("saved data scoreboard");
        boolean changed = false;
        for (Tag t : root.getCompound("data").getList("Objectives", Tag.TAG_COMPOUND)) {
            CompoundTag objective = (CompoundTag) t;
            String criteria = objective.getString("CriteriaName");
            int colon = criteria.indexOf(':'), dot = criteria.indexOf('.', colon + 1);
            if (colon < 0 || dot < 0) continue;
            // a stat criterion spells its id <namespace>.<path>
            String id = criteria.substring(colon + 1, dot) + ":" + criteria.substring(dot + 1);
            if (!DeAddons.isSource(id)) continue;
            String stat = criteria.substring(0, colon);
            Map<String, String> table = switch (stat) {
                case "minecraft.mined" -> WorldTables.get().blocks;
                case "minecraft.killed", "minecraft.killed_by" -> Tables.get().entities;
                default -> Tables.get().items;
            };
            String rule = table.getOrDefault(DeAddons.key(id), table.get(id));
            if (rule == null && !id.startsWith("hbm:")) rule = DeAddons.rename(id);
            if (rule == null) continue;
            if (!DeAddons.isTarget(rule)) {
                ctx.parking.park("scoreboard_objective", "scoreboard", objective.getString("Name"), objective.copy());
                continue;
            }
            objective.putString("CriteriaName", stat + ":" + rule.split("\\[")[0].replace(':', '.'));
            changed = true;
        }
        if (!changed) {
            ctx.abandon(unit);
            return;
        }
        ctx.commit(unit);
        AtomicNbt.write(root, file);
        ctx.report.count("scoreboard objectives renamed");
    }

    /** DE lists each region's pollution by type ordinal; Continuum names the types. Both keep 1.7's type order. */
    private static boolean pollution(CompoundTag data) {
        boolean changed = false;
        for (Tag t : data.getList("entries", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            if (!entry.contains("values", Tag.TAG_LIST)) continue;
            ListTag values = entry.getList("values", Tag.TAG_FLOAT);
            CompoundTag named = entry.getCompound("pollution");
            for (int i = 0; i < Math.min(values.size(), POLLUTION.length); i++) {
                if (values.getFloat(i) != 0F) named.putFloat(POLLUTION[i], values.getFloat(i));
            }
            entry.put("pollution", named);
            entry.remove("values");
            changed = true;
        }
        return changed;
    }

    /** DE nests each satellite as {@code {sat, state}}; one of a type Continuum lacks would empty the level's list. */
    public static boolean satellites(Ctx ctx, CompoundTag data, String where) {
        boolean changed = false;
        Walker walker = new Walker(ctx);
        ListTag sats = data.getList("sats", Tag.TAG_COMPOUND);
        for (int i = 0; i < sats.size(); i++) {
            CompoundTag entry = sats.getCompound(i);
            CompoundTag outer = entry.getCompound("sat");
            if (!outer.contains("sat", Tag.TAG_COMPOUND)) continue;
            CompoundTag sat = outer.getCompound("sat");
            if (sat.getString("type").equals("death_ray")) sat.putString("type", "laser");
            sat.putString("type", DeAddons.rename(sat.getString("type")));
            String at = where + " freq " + entry.getInt("freq");
            CompoundTag state = outer.getCompound("state");
            for (String key : List.of("targetX", "targetZ", "tx")) move(state, entry, key);
            for (String key : List.of("driveInput", "driveOutput")) {
                if (state.contains(key, Tag.TAG_STRING)) entry.putString(key, state.getString(key).toUpperCase(Locale.ROOT));
                state.remove(key);
            }
            if (state.contains("requestableSlots", Tag.TAG_LIST)) {
                ListTag slots = state.getList("requestableSlots", Tag.TAG_COMPOUND);
                walker.walkList(slots, at);
                entry.put("requestableSlots", slots);
                state.remove("requestableSlots");
            }
            CompoundTag components = state.getCompound("components");
            CompoundTag extra = new CompoundTag();
            for (DeAddons.Addon addon : DeAddons.all()) addon.satellite(sat, components, extra, ctx, at);
            if (!extra.isEmpty()) entry.put("extra", extra);
            if (components.isEmpty()) state.remove("components");
            changed = true;
            if (!ctx.target.hasSatelliteType(sat.getString("type"))) {
                entry.put("sat", outer);
                ctx.parking.park("satellite", at, sat.getString("type"), sats.remove(i--));
                ctx.report.line("satellite " + sat.getString("type") + " in " + at + " has no Continuum type and was parked");
                continue;
            }
            if (!state.isEmpty()) ctx.parking.park("satellite_state", at, sat.getString("type"), state);
            entry.put("sat", sat);
            ctx.report.count("satellites converted");
        }
        return changed;
    }

    private static void move(CompoundTag from, CompoundTag to, String key) {
        if (from.contains(key)) to.put(key, from.get(key));
        from.remove(key);
    }

    /** The file, or null when it is missing or unreadable; an unreadable one is left alone with its bytes parked. */
    private static @Nullable CompoundTag read(Ctx ctx, Path file, String where) throws IOException {
        if (!Files.isRegularFile(file)) return null;
        try {
            return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        } catch (IOException | RuntimeException e) {
            ctx.parking.park("saved_data", where, "unreadable: " + e, new ByteArrayTag(Files.readAllBytes(file)));
            ctx.report.line(where + " cannot be read and was left as it is");
            return null;
        }
    }
}
