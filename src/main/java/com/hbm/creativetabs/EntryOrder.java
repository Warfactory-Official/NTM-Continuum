package com.hbm.creativetabs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Anchored ordering of existing entries, retaining all values that share one key in their original order. */
public final class EntryOrder<K> {
    private final Map<K, List<K>> before = new LinkedHashMap<>();
    private final Map<K, List<K>> after = new LinkedHashMap<>();
    private final List<K> first = new ArrayList<>();
    private final Set<K> removed = new HashSet<>();

    public void insertBefore(K anchor, List<K> entries) { insert(before, anchor, entries); }
    public void insertAfter(K anchor, List<K> entries) { insert(after, anchor, entries); }
    public void insertFirst(List<K> entries) {
        for (var entry : entries) if (!first.contains(entry)) first.add(entry);
    }
    public void remove(K key) { removed.add(key); }

    private void insert(Map<K, List<K>> positions, K anchor, List<K> entries) {
        var group = positions.computeIfAbsent(anchor, ignored -> new ArrayList<>());
        for (var entry : entries) {
            if (anchor.equals(entry)) throw new IllegalArgumentException("An entry cannot anchor itself: " + entry);
            if (!group.contains(entry)) group.add(entry);
        }
    }

    public <T> List<T> apply(List<T> entries, Function<T, K> key) {
        if (before.isEmpty() && after.isEmpty() && first.isEmpty() && removed.isEmpty()) return entries;
        var selected = new HashMap<K, List<T>>();
        var groups = new ArrayList<List<K>>();
        groups.add(first);
        for (var positions : List.of(before, after)) groups.addAll(positions.values());
        for (var group : groups) for (var member : group) {
                if (selected.put(member, new ArrayList<>()) != null)
                    throw new IllegalStateException("Creative entry has multiple positions: " + member);
                if (removed.contains(member))
                    throw new IllegalStateException("Creative entry is both moved and removed: " + member);
        }
        var baseline = new ArrayList<T>();
        var dropped = new HashSet<K>();
        for (var entry : entries) {
            var id = key.apply(entry);
            if (removed.contains(id)) dropped.add(id);
            var moved = selected.get(id);
            if (moved == null) baseline.add(entry);
            else moved.add(entry);
        }
        for (var selection : selected.entrySet()) if (selection.getValue().isEmpty())
            throw new IllegalStateException("Creative tab contains no moved entry " + selection.getKey());
        if (!dropped.containsAll(removed)) {
            var missing = new HashSet<>(removed);
            missing.removeAll(dropped);
            throw new IllegalStateException("Creative tab contains no removed entries " + missing);
        }
        var result = new ArrayList<T>(entries.size());
        var ahead = new HashSet<K>();
        var behind = new HashSet<K>();
        for (var member : first) for (var value : selected.get(member))
            emit(value, key, selected, ahead, behind, result);
        for (var entry : baseline) emit(entry, key, selected, ahead, behind, result);
        if (ahead.size() != before.size() || behind.size() != after.size()) {
            var missing = new HashSet<K>();
            for (var anchor : before.keySet()) if (!ahead.contains(anchor)) missing.add(anchor);
            for (var anchor : after.keySet()) if (!behind.contains(anchor)) missing.add(anchor);
            throw new IllegalStateException("Creative anchors are absent or cyclic: " + missing);
        }
        return result;
    }

    private <T> void emit(T entry, Function<T, K> key, Map<K, List<T>> selected,
                         Set<K> ahead, Set<K> behind, List<T> result) {
        var id = key.apply(entry);
        var preceding = before.get(id);
        if (preceding != null && ahead.add(id)) {
            for (var member : preceding) for (var value : selected.get(member))
                emit(value, key, selected, ahead, behind, result);
        }
        if (!removed.contains(id)) result.add(entry);
        var following = after.get(id);
        if (following != null && behind.add(id)) {
            for (var member : following) for (var value : selected.get(member))
                emit(value, key, selected, ahead, behind, result);
        }
    }
}
