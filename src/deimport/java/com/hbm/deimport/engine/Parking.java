package com.hbm.deimport.engine;

import com.hbm.delegacy.AtomicNbt;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * {@code <world>/data/hbm_de_leftovers.dat}: everything the importer could not convert, kept with where it was and its
 * raw NBT so a later step can still convert it. Nothing unconvertible is deleted. While an import runs, each unit's
 * records sit in its shard under {@link #SHARDS}; the shards are folded into the file once the live phase is done.
 */
public final class Parking {
    public static final String FILE = "data/hbm_de_leftovers.dat";
    public static final String SHARDS = "data/hbm_de_pending";
    /** On a record parked by live-phase work: that work's id, so doing the work again replaces it. */
    static final String LIVE = "live";
    private final Path file;
    private final Path shards;
    private final ListTag records = new ListTag();
    private final Map<String, Integer> counts = new TreeMap<>();
    /** Shards read at start, for the context to take its pending lists from. */
    final List<Unit> loaded = new ArrayList<>();
    /** Shards already folded into the leftovers file by a live phase that was cut short before deleting them. */
    final Set<String> merged = new HashSet<>();
    /** Units with records from an earlier run; only theirs can be replaced. */
    final Set<String> earlier = new HashSet<>();
    /** Every unit with a shard on disk, read at start or written since. */
    final Set<String> units = new HashSet<>();
    /** Records parked by live work, by its id, so doing it again finds them without a scan of every record. */
    private final Map<String, List<CompoundTag>> byLive = new HashMap<>();
    private @Nullable String liveId;
    private boolean globalChanged;

    public Parking(Path world) throws IOException {
        this.file = world.resolve(FILE);
        this.shards = world.resolve(SHARDS);
        CompoundTag root = AtomicNbt.read(file);
        if (root != null) {
            records.addAll(root.getList("records", Tag.TAG_COMPOUND));
            root.getList("merged", Tag.TAG_STRING).forEach(t -> merged.add(t.getAsString()));
        }
        if (!Files.isDirectory(shards)) return;
        try (Stream<Path> list = Files.list(shards)) {
            for (Path shard : list.filter(p -> p.getFileName().toString().endsWith(".dat")).sorted().toList()) {
                CompoundTag tag = AtomicNbt.read(shard);
                if (tag == null) continue;
                Unit unit = Unit.fromTag(tag);
                loaded.add(unit);
                earlier.add(unit.name);
                units.add(unit.name);
                if (merged.contains(unit.name)) continue;
                for (Tag t : unit.parked) {
                    CompoundTag record = (CompoundTag) t;
                    record.putString("unit", unit.name);
                    records.add(record);
                    if (record.contains(LIVE)) byLive.computeIfAbsent(record.getString(LIVE), k -> new ArrayList<>()).add(record);
                }
            }
        }
    }

    /** Parks into the unit being converted on this thread, else straight into the records. */
    public synchronized void park(String kind, String where, String id, @Nullable Tag nbt) {
        CompoundTag record = new CompoundTag();
        record.putString("kind", kind);
        record.putString("where", where);
        record.putString("id", id);
        if (nbt != null) record.put("nbt", nbt.copy());
        counts.merge(kind, 1, Integer::sum);
        Unit unit = Unit.current();
        if (unit != null) {
            unit.addParked(record);
        } else {
            record.putString("unit", Unit.GLOBAL);
            if (liveId != null) {
                record.putString(LIVE, liveId);
                byLive.computeIfAbsent(liveId, k -> new ArrayList<>()).add(record);
            }
            records.add(record);
            globalChanged = true;
        }
    }

    /** Live-phase work {@code id} runs now: drops what an earlier attempt at it parked, and tags what it parks. */
    public synchronized void during(@Nullable String id) {
        liveId = id;
        List<CompoundTag> before = id == null ? null : byLive.remove(id);
        if (before == null || before.isEmpty()) return;
        Set<Tag> drop = Collections.newSetFromMap(new IdentityHashMap<>());
        drop.addAll(before);
        records.removeIf(drop::contains);
        globalChanged = true;
    }

    /** The unit's earlier records this run keeps: those of parts it did not convert again. */
    synchronized List<CompoundTag> kept(Unit unit) {
        List<CompoundTag> out = new ArrayList<>();
        if (!earlier.contains(unit.name)) return out;
        for (Tag t : records) {
            CompoundTag record = (CompoundTag) t;
            if (unit.name.equals(record.getString("unit")) && !unit.replaces(record)) out.add(record);
        }
        return out;
    }

    /** A committed unit's records replace whatever an earlier, interrupted run kept for its converted parts. */
    synchronized void replace(Unit unit) {
        units.add(unit.name);
        if (earlier.contains(unit.name))
            records.removeIf(t -> unit.name.equals(((CompoundTag) t).getString("unit")) && unit.replaces((CompoundTag) t));
        for (Tag t : unit.parked) {
            CompoundTag record = ((CompoundTag) t).copy();
            record.putString("unit", unit.name);
            records.add(record);
        }
    }

    synchronized ListTag global() {
        ListTag out = new ListTag();
        for (Tag t : records) if (Unit.GLOBAL.equals(((CompoundTag) t).getString("unit"))) out.add(t.copy());
        return out;
    }

    /** True once, after records no unit owns were added or dropped. */
    synchronized boolean takeGlobalChange() {
        boolean changed = globalChanged;
        globalChanged = false;
        return changed;
    }

    public synchronized Map<String, Integer> counts() {
        return Map.copyOf(counts);
    }

    /** Records per kind across every run, for {@code /hbm deimport status}. */
    public synchronized Map<String, Integer> countsFromRecords() {
        Map<String, Integer> out = new TreeMap<>();
        for (Tag t : records) out.merge(((CompoundTag) t).getString("kind"), 1, Integer::sum);
        return out;
    }

    public ListTag records() {
        return records;
    }

    /** Writes every record to the leftovers file, naming the shards it now holds so a crash before they go skips them. */
    synchronized void writeMain(Collection<String> merged) throws IOException {
        if (records.isEmpty() && !Files.isRegularFile(file)) return;
        CompoundTag root = new CompoundTag();
        root.put("records", records);
        ListTag names = new ListTag();
        for (String name : merged) names.add(StringTag.valueOf(name));
        if (!names.isEmpty()) root.put("merged", names);
        AtomicNbt.write(root, file);
    }

    /** Once the records are in the leftovers file, no unit owns them any more. */
    synchronized void settle() {
        for (Tag t : records) {
            ((CompoundTag) t).remove("unit");
            ((CompoundTag) t).remove(Unit.PART);
            ((CompoundTag) t).remove(LIVE);
        }
        earlier.clear();
        units.clear();
        byLive.clear();
    }

    Path shards() {
        return shards;
    }
}
