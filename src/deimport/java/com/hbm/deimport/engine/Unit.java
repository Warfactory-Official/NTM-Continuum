package com.hbm.deimport.engine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * One store converted as a whole (a region file, a player file, a saved-data file): what it parks and leaves for the
 * live phase is held here until {@link Ctx#commit} writes it to the store's own shard, right before the store is
 * replaced. A crash before that converts the store again and overwrites the shard, so nothing is kept twice. A store
 * converted in parts (chunks, ships) replaces only the records of the parts it converted again.
 */
public final class Unit {
    static final String GLOBAL = "global";
    public static final String PART = "part";
    private static final ThreadLocal<Unit> CURRENT = new ThreadLocal<>();
    public final String name;
    final ListTag parked = new ListTag();
    final Map<String, ListTag> pending = new TreeMap<>();
    /** The parts converted or parked this run; null for a store converted as a whole. */
    @Nullable Set<String> touched;
    private @Nullable String part;
    private int parkedMark;
    private final Map<String, Integer> pendingMark = new HashMap<>();

    Unit(String name) {
        this.name = name;
    }

    static @Nullable Unit current() {
        return CURRENT.get();
    }

    static void set(@Nullable Unit unit) {
        if (unit == null) CURRENT.remove();
        else CURRENT.set(unit);
    }

    public int pendingCount(String kind) {
        ListTag list = pending.get(kind);
        return list == null ? 0 : list.size();
    }

    ListTag pending(String kind) {
        return pending.computeIfAbsent(kind, k -> new ListTag());
    }

    void addPending(String kind, CompoundTag tag) {
        if (part != null) tag.putString(PART, part);
        pending(kind).add(tag);
    }

    void addParked(CompoundTag record) {
        if (part != null) record.putString(PART, part);
        parked.add(record);
    }

    /**
     * Starts one part; what is recorded until {@link #endPart} belongs to it. Starting the part that is still open
     * drops what it recorded so far, for a conversion that failed halfway.
     */
    public void part(String key) {
        if (touched == null) touched = new HashSet<>();
        if (key.equals(part)) {
            while (parked.size() > parkedMark) parked.remove(parked.size() - 1);
            for (Map.Entry<String, ListTag> e : pending.entrySet()) {
                int mark = pendingMark.getOrDefault(e.getKey(), 0);
                while (e.getValue().size() > mark) e.getValue().remove(e.getValue().size() - 1);
            }
            return;
        }
        part = key;
        parkedMark = parked.size();
        pendingMark.clear();
        for (Map.Entry<String, ListTag> e : pending.entrySet()) pendingMark.put(e.getKey(), e.getValue().size());
    }

    /** Ends the open part; a part that was converted or recorded anything replaces its earlier records. */
    public void endPart(boolean converted) {
        if (part == null) return;
        boolean recorded = parked.size() > parkedMark
                || pending.entrySet().stream().anyMatch(e -> e.getValue().size() > pendingMark.getOrDefault(e.getKey(), 0));
        if (converted || recorded) touched.add(part);
        part = null;
    }

    /** True once a part was converted or recorded anything, so the shard must be written even if nothing else changed. */
    public boolean touchedAny() {
        return touched != null && !touched.isEmpty();
    }

    /** Whether this run's records replace an earlier record of the same unit. */
    boolean replaces(CompoundTag earlier) {
        return touched == null || touched.contains(earlier.getString(PART));
    }

    /** The shard file's name: the unit's name made safe for a file name, and its hash so two names never share one. */
    String file() {
        return name.replaceAll("[^A-Za-z0-9.-]", "_") + "-" + Integer.toHexString(name.hashCode()) + ".dat";
    }

    CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("unit", name);
        tag.put("parked", parked);
        CompoundTag lists = new CompoundTag();
        for (Map.Entry<String, ListTag> e : pending.entrySet()) lists.put(e.getKey(), e.getValue());
        tag.put("pending", lists);
        return tag;
    }

    static Unit fromTag(CompoundTag tag) {
        Unit unit = new Unit(tag.getString("unit"));
        unit.parked.addAll(tag.getList("parked", Tag.TAG_COMPOUND));
        CompoundTag lists = tag.getCompound("pending");
        for (String kind : lists.getAllKeys()) unit.pending(kind).addAll(lists.getList(kind, Tag.TAG_COMPOUND));
        return unit;
    }
}
