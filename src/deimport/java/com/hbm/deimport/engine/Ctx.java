package com.hbm.deimport.engine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.deimport.source.de7.BlockEntities;
import com.hbm.deimport.source.de7.BlockStates;
import com.hbm.delegacy.AtomicNbt;
import com.hbm.delegacy.ContinuumStacks;
import com.hbm.delegacy.de7.StackContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * Shared state of one import run: the target, the parking file, the report and what the live phase must finish. Work
 * for the live phase is collected per {@link Unit} and kept in the unit's shard until the live phase has done it.
 */
public final class Ctx implements StackContext {
    /** The single pending file an earlier importer wrote; read once more, then replaced by the shards. */
    public static final String PENDING = "data/hbm_de_pending.dat";
    static final List<String> KINDS = List.of("cores", "plotcells", "radiation", "conductors", "piles", "entities", "loot");
    /** The unit holding live work that waits for a dimension. */
    static final String WAITING = "live waiting";
    /** Where DE's power graph is kept once moved out of Continuum's way, relative to its dimension. */
    public static final String DE_POWER_GRAPH = "data/hbm_de_power_graph.dat";
    private static final String RBMK_DIALS = "data/hbm/hbm/machine_config/rbmk.json";
    public final Path world;
    public final Target target;
    public final Parking parking;
    public final Report report;
    public final Journal journal;
    /** Multiblock cores to rebuild once the levels are loaded: {dim, pos, block}. */
    public final List<CompoundTag> cores = new ArrayList<>();
    /** An airship's multiblock cells, for the core index the cells find their machine through: {dim, pos, cells}. */
    public final List<CompoundTag> plotCells = new ArrayList<>();
    /** Radiation to set once the levels are loaded: {dim, pos, density}, one per DE pocket. */
    public final List<CompoundTag> radiation = new ArrayList<>();
    /** Conductors to announce to their network once the levels are loaded: {dim, pos}. */
    public final List<CompoundTag> conductors = new ArrayList<>();
    /** DE pile cores and their member boxes, to link the members once the levels are loaded: {dim, pos, box}. */
    public final List<CompoundTag> piles = new ArrayList<>();
    /** Converted entities with HBM data, checked against Continuum's own save once a level exists: {dim, where, nbt}. */
    public final List<CompoundTag> entities = new ArrayList<>();
    /** Lazy loot on DE containers Continuum keeps none on, rolled into them once the levels are loaded: {dim, pos, table, seed}. */
    public final List<CompoundTag> loot = new ArrayList<>();

    private final Map<String, Map<Long, String>> ductFluids = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, long[]>> wires = new ConcurrentHashMap<>();
    private final ThreadLocal<String> location = new ThreadLocal<>();
    private boolean globalChanged;

    public Ctx(Path world, Target target, Parking parking, Report report, Journal journal) throws IOException {
        this.world = world;
        this.journal = journal;
        this.target = target;
        this.parking = parking;
        this.report = report;
        CompoundTag old = AtomicNbt.read(world.resolve(PENDING));
        if (old != null) for (String kind : KINDS) old.getList(kind, Tag.TAG_COMPOUND).forEach(t -> add(kind, Unit.GLOBAL, (CompoundTag) t));
        for (Unit unit : parking.loaded) {
            // a merged shard's work was done and saved before the leftovers file named it
            if (parking.merged.contains(unit.name)) continue;
            for (Map.Entry<String, ListTag> e : unit.pending.entrySet()) {
                for (Tag t : e.getValue()) {
                    // work of a kind this importer no longer does is kept, not dropped
                    if (KINDS.contains(e.getKey())) add(e.getKey(), unit.name, (CompoundTag) t);
                    else parking.park("live_record", unit.name, e.getKey(), t);
                }
            }
        }
    }

    public List<CompoundTag> list(String kind) {
        return switch (kind) {
            case "cores" -> cores;
            case "plotcells" -> plotCells;
            case "radiation" -> radiation;
            case "conductors" -> conductors;
            case "piles" -> piles;
            case "entities" -> entities;
            case "loot" -> loot;
            default -> throw new IllegalArgumentException(kind);
        };
    }

    private void add(String kind, String unit, CompoundTag tag) {
        tag.putString("unit", unit);
        list(kind).add(tag);
        if (unit.equals(Unit.GLOBAL)) globalChanged = true;
    }

    /** Live-phase work: into the unit converting on this thread, else straight into the lists. */
    public synchronized void add(String kind, CompoundTag tag) {
        Unit unit = Unit.current();
        if (unit != null) unit.addPending(kind, tag);
        else add(kind, Unit.GLOBAL, tag);
    }

    /** Starts collecting this thread's records for one store; nothing it parks or leaves counts until {@link #commit}. */
    public Unit begin(String name) {
        Unit unit = new Unit(name);
        Unit.set(unit);
        return unit;
    }

    /**
     * Writes the unit's shard, then lets its records replace an interrupted run's. Called right before the store itself
     * is replaced, so a crash in between converts the store again into the same shard.
     */
    public void commit(Unit unit) throws IOException {
        Unit.set(null);
        boolean earlier = parking.earlier.contains(unit.name);
        Unit shard = new Unit(unit.name);
        shard.parked.addAll(parking.kept(unit));
        shard.parked.addAll(unit.parked);
        synchronized (this) {
            for (String kind : KINDS) {
                if (earlier)
                    for (CompoundTag t : list(kind)) if (unit.name.equals(t.getString("unit")) && !unit.replaces(t)) shard.pending(kind).add(t);
                shard.pending(kind).addAll(unit.pending(kind));
            }
        }
        AtomicNbt.write(shard.toTag(), parking.shards().resolve(unit.file()));
        synchronized (this) {
            for (String kind : KINDS) {
                if (earlier) list(kind).removeIf(t -> unit.name.equals(t.getString("unit")) && unit.replaces(t));
                for (Tag t : unit.pending(kind)) add(kind, unit.name, (CompoundTag) t);
            }
        }
        parking.replace(unit);
    }

    /** Drops what the unit collected: its store was not replaced, so an earlier run's shard still stands. */
    public void abandon(Unit unit) {
        Unit.set(null);
    }

    /** Writes the records no unit owns to their own shard, when they changed since the last write. */
    public synchronized void flushGlobal() throws IOException {
        boolean changed = parking.takeGlobalChange() | globalChanged;
        globalChanged = false;
        if (!changed) return;
        Unit global = new Unit(Unit.GLOBAL);
        parking.units.add(Unit.GLOBAL);
        global.parked.addAll(parking.global());
        for (String kind : KINDS)
            for (CompoundTag t : list(kind)) if (Unit.GLOBAL.equals(t.getString("unit"))) global.pending(kind).add(t);
        Path file = parking.shards().resolve(global.file());
        if (global.parked.isEmpty() && global.pending.isEmpty()) Files.deleteIfExists(file);
        else AtomicNbt.write(global.toTag(), file);
    }

    /**
     * The live phase is done, its levels saved: every record goes into the leftovers file, then the shards and the lists
     * go. {@code waiting} (work for a dimension that does not exist yet) is written to its own shard first, so no crash
     * point loses it, and stays the only work left.
     */
    public synchronized void finishLive(Map<String, List<CompoundTag>> waiting) throws IOException {
        Path dir = parking.shards();
        Unit kept = new Unit(WAITING);
        waiting.forEach((kind, records) -> kept.pending(kind).addAll(records));
        Path keptFile = dir.resolve(kept.file());
        if (!kept.pending.isEmpty()) AtomicNbt.write(kept.toTag(), keptFile);
        // the shards on disk are the ones this run read or wrote, so their names need no second read
        List<String> names = new ArrayList<>(parking.units);
        if (!kept.pending.isEmpty()) names.remove(WAITING);
        parking.writeMain(names);
        if (Files.isDirectory(dir)) {
            try (Stream<Path> list = Files.list(dir)) {
                for (Path p : list.toList()) {
                    String name = p.getFileName().toString();
                    if (!name.contains(".damaged-") && (kept.pending.isEmpty() || !p.equals(keptFile))) Files.delete(p);
                }
            }
        }
        Files.deleteIfExists(world.resolve(PENDING));
        parking.settle();
        parking.writeMain(List.of());
        for (String kind : KINDS) list(kind).clear();
        parking.earlier.add(WAITING);
        if (!kept.pending.isEmpty()) parking.units.add(WAITING);
        waiting.forEach((kind, records) -> records.forEach(t -> add(kind, WAITING, t)));
        globalChanged = false;
    }

    /** Where this thread is converting, for parks made deep in a mapper that knows only a key. */
    public void at(@Nullable String where) {
        if (where == null) location.remove();
        else location.set(where);
    }

    public String where(String key) {
        String at = location.get();
        return at == null ? key : at + " " + key;
    }
    private byte @Nullable [] lockedKey;
    private @Nullable Long gameTime;
    private boolean lockedKeyRead;
    private @Nullable JsonObject rbmkDials;
    private boolean rbmkDialsRead;

    /**
     * DE's duct fluids by position from its pipe graph ({@code <dim>/data/hbm_fluid_pipe_graph.dat}): every node and
     * every cell of a straight segment carries the fluid of the data entry it points at.
     */
    /** DE's pylon wires by position from its power graph ({@code <dim>/data/hbm_power_graph.dat}): each node's remote links. */
    public Map<Long, long[]> wires(String dim) {
        return wires.computeIfAbsent(dim, d -> {
            Map<Long, long[]> out = new HashMap<>();
            try {
                Path folder = Dimensions.all(world).get(d);
                Path file = folder == null ? null : folder.resolve(DE_POWER_GRAPH);
                if (file != null && !Files.isRegularFile(file)) file = folder.resolve("data/hbm_power_graph.dat");
                if (file == null || !Files.isRegularFile(file)) return out;
                CompoundTag graph = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()).getCompound("data").getCompound("data");
                for (Tag t : graph.getList("nodes", Tag.TAG_COMPOUND)) {
                    CompoundTag node = (CompoundTag) t;
                    long[] remote = node.getLongArray("r");
                    if (remote.length == 0) remote = node.getList("r", Tag.TAG_LONG).stream().mapToLong(l -> ((LongTag) l).getAsLong()).toArray();
                    if (remote.length > 0) out.put(node.getLong("p"), remote);
                }
            } catch (IOException | RuntimeException e) {
                // an unreadable graph leaves the wires unlinked, as a missing one does; the file stays in the backup
                parking.park("power_graph", d, "hbm_power_graph", null);
            }
            return out;
        });
    }

    public Map<Long, String> ductFluids(String dim) {
        return ductFluids.computeIfAbsent(dim, d -> {
            Map<Long, String> out = new HashMap<>();
            try {
                Path folder = Dimensions.all(world).get(d);
                Path file = folder == null ? null : folder.resolve("data/hbm_fluid_pipe_graph.dat");
                if (file == null || !Files.isRegularFile(file)) return out;
                CompoundTag graph = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()).getCompound("data").getCompound("data");
                ListTag data = graph.getList("data", Tag.TAG_COMPOUND);
                for (Tag t : graph.getList("nodes", Tag.TAG_COMPOUND)) {
                    CompoundTag node = (CompoundTag) t;
                    out.put(node.getLong("p"), data.getCompound(node.getInt("d")).getString("fluid"));
                }
                for (Tag t : graph.getList("segs", Tag.TAG_COMPOUND)) {
                    CompoundTag seg = (CompoundTag) t;
                    String fluid = data.getCompound(seg.getInt("d")).getString("fluid");
                    for (long cell : seg.getLongArray("cells")) out.put(cell, fluid);
                    for (Tag c : seg.getList("cells", Tag.TAG_LONG)) out.put(((LongTag) c).getAsLong(), fluid);
                }
            } catch (IOException | RuntimeException e) {
                // an unreadable graph leaves the ducts untyped, as a missing one does; the file stays in the backup
                parking.park("pipe_graph", d, "hbm_fluid_pipe_graph", null);
            }
            return out;
        });
    }

    /** DE's AES key for sealed crate contents ({@code data/hbm_locked_contents_key.dat}), or null. */
    @Override
    public synchronized byte @Nullable [] lockedContentsKey() {
        if (!lockedKeyRead) {
            lockedKeyRead = true;
            lockedKey = ContinuumStacks.lockedContentsKey(world);
        }
        return lockedKey;
    }

    @Override
    public void park(String kind, String where, String id, @Nullable Tag nbt) {
        parking.park(kind, where, id, nbt);
    }

    @Override
    public void count(String key, long n) {
        report.count(key, n);
    }

    @Override
    public boolean hasComponent(String id) {
        return target.hasComponent(id);
    }

    @Override
    public @Nullable CompoundTag filledComponents(String item, String fluid, int amount) {
        return target.filledComponents(item, fluid, amount);
    }

    @Override
    public @Nullable List<String> undecodable(CompoundTag stack) {
        return target.undecodable(stack);
    }

    @Override
    public boolean hasEntity(String id) {
        return target.hasEntity(id);
    }

    @Override
    public void entityConverted(CompoundTag entity, String where) {
        add("entities", entityRecord(entity, where));
    }

    private static CompoundTag entityRecord(CompoundTag entity, String where) {
        CompoundTag record = new CompoundTag();
        record.putString("where", where);
        record.put("nbt", entity.copy());
        return record;
    }

    @Override
    public synchronized long gameTime() {
        if (gameTime == null) {
            try {
                Path file = world.resolve("level.dat");
                gameTime = Files.isRegularFile(file)
                        ? NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()).getCompound("Data").getLong("Time") : 0L;
            } catch (IOException e) {
                // a world whose level.dat cannot be read never got this far; fuses then count from zero
                gameTime = 0L;
            }
        }
        return gameTime;
    }

    /** The built-in definition, else {@code recipe_key} from the definition in this world's datapacks that vanilla uses. */
    @Override
    public synchronized @Nullable String customMachineName(String id) {
        String builtIn = StackContext.super.customMachineName(id);
        if (builtIn != null || !id.contains(":")) return builtIn;
        String entry = "data/" + id.substring(0, id.indexOf(':')) + "/hbm/custom_machine/" + id.substring(id.indexOf(':') + 1) + ".json";
        try {
            List<Path> packs = Datapacks.ordered(world);
            for (int i = packs.size() - 1; i >= 0; i--) {
                String json = Datapacks.read(packs.get(i), entry);
                if (json != null) return JsonParser.parseString(json).getAsJsonObject().get("recipe_key").getAsString();
            }
        } catch (IOException | RuntimeException e) {
            // an unreadable pack leaves the machine's type parked, as an unknown definition does
            return null;
        }
        return null;
    }

    /** DE's RBMK settings from the highest-priority world datapack that overrides them, or null. */
    public synchronized @Nullable JsonObject rbmkDials() {
        if (rbmkDialsRead) return rbmkDials;
        rbmkDialsRead = true;
        try {
            for (Path pack : Datapacks.ordered(world)) {
                try {
                    String json = Datapacks.read(pack, RBMK_DIALS);
                    if (json != null) rbmkDials = JsonParser.parseString(json).getAsJsonObject();
                } catch (IOException | RuntimeException e) {
                    // vanilla skips a pack it cannot open; its RBMK settings stay at Continuum's defaults
                    report.line("datapack " + pack.getFileName() + " cannot be read; its RBMK settings were not carried over");
                }
            }
        } catch (IOException e) {
            report.line("the world's datapacks cannot be listed; RBMK settings were not carried over");
        }
        return rbmkDials;
    }

    /** The RBMK column height DE's datapack sets, core included, or null for the default. */
    public @Nullable Integer rbmkColumnHeight() {
        JsonObject dials = rbmkDials();
        JsonElement height = dials == null ? null : dials.get("column_height");
        return height != null && height.isJsonPrimitive() && height.getAsJsonPrimitive().isNumber() ? height.getAsInt() : null;
    }

    @Override
    public CompoundTag blockState(CompoundTag state) {
        return BlockStates.convert(state, target).entry();
    }

    @Override
    public boolean blockEntity(CompoundTag be, CompoundTag state, String where) {
        String type = target.blockEntityType(state.getString("Name"), state.getCompound("Properties"));
        if (type == null) {
            park("block_entity", where, be.getString("id"), be);
            return false;
        }
        BlockEntities.convert(be, type, this, null, where);
        return true;
    }

    /** A player file's HBM attachments, checked like an entity's. */
    public void playerConverted(CompoundTag player, String where) {
        if (player.getCompound("neoforge:attachments").getAllKeys().stream().noneMatch(k -> k.startsWith("hbm:"))) return;
        CompoundTag record = entityRecord(player, where);
        record.putBoolean("player", true);
        add("entities", record);
    }

    @Override
    public @Nullable String weaponModName(int id) {
        return target.weaponModName(id);
    }

    public void core(String dim, long pos, String block) {
        CompoundTag tag = new CompoundTag();
        tag.putString("dim", dim);
        tag.putLong("pos", pos);
        tag.putString("block", block);
        add("cores", tag);
    }

    /** {@code plot}: in an airship, joined to the graph without loading the ship. */
    public void conductor(String dim, long pos, boolean plot) {
        CompoundTag tag = new CompoundTag();
        tag.putString("dim", dim);
        tag.putLong("pos", pos);
        if (plot) tag.putBoolean("plot", true);
        add("conductors", tag);
    }

    public void radiation(String dim, long pos, double density) {
        CompoundTag tag = new CompoundTag();
        tag.putString("dim", dim);
        tag.putLong("pos", pos);
        tag.putDouble("density", density);
        add("radiation", tag);
    }
}
