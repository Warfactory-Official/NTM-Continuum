package com.hbm.deimport.engine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/** What of a tag Continuum did not keep: every path in {@code in} that is missing or different in Continuum's save. */
public final class NbtDiff {
    /** Lists saved from a set, whose order depends on hashing: compared as multisets. */
    private static final Set<String> UNORDERED = Set.of("neoforge:cures");

    private NbtDiff() {}

    /** Paths to the original values; numbers compare by value, so a byte saved back as an int is kept. */
    public static Map<String, Tag> lost(CompoundTag in, CompoundTag out, Set<String> ignored) {
        Map<String, Tag> lost = new LinkedHashMap<>();
        for (String key : in.getAllKeys()) {
            if (!ignored.contains(key)) compare(key, in.get(key), out.get(key), lost);
        }
        return lost;
    }

    /**
     * Drops entries Continuum writes by leaving them out: the value a fresh block entity holds at that path, or an
     * empty value (zero, false, an empty string, list or compound, no fluid) where a fresh one writes nothing.
     */
    public static Map<String, Tag> withoutDefaults(Map<String, Tag> lost, CompoundTag fresh) {
        Map<String, Tag> out = new LinkedHashMap<>();
        for (Map.Entry<String, Tag> e : lost.entrySet()) {
            Tag def = at(fresh, e.getKey());
            if (def == null ? !empty(e.getValue()) : !same(e.getValue(), def)) out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    /**
     * Casts every number in {@code tag} to the numeric type {@code shape} holds at the same key, since Continuum's
     * reader takes only the exact tag type and reads anything else as its default. Returns the original of every value
     * the cast changed (a long past an int's range, a fraction cut off), by path.
     */
    public static Map<String, Tag> conformNumbers(CompoundTag tag, CompoundTag shape) {
        Map<String, Tag> changed = new LinkedHashMap<>();
        conform("", tag, shape, changed);
        return changed;
    }

    private static void conform(String prefix, CompoundTag tag, CompoundTag shape, Map<String, Tag> changed) {
        for (String key : List.copyOf(tag.getAllKeys())) {
            Tag value = tag.get(key);
            Tag want = shape.get(key);
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (value instanceof CompoundTag c && want instanceof CompoundTag w) {
                conform(path, c, w, changed);
            } else if (value instanceof NumericTag n && want instanceof NumericTag && value.getId() != want.getId()) {
                NumericTag cast = switch (want.getId()) {
                    case Tag.TAG_BYTE -> ByteTag.valueOf(n.getAsByte());
                    case Tag.TAG_SHORT -> ShortTag.valueOf(n.getAsShort());
                    case Tag.TAG_INT -> IntTag.valueOf(n.getAsInt());
                    case Tag.TAG_LONG -> LongTag.valueOf(n.getAsLong());
                    case Tag.TAG_FLOAT -> FloatTag.valueOf(n.getAsFloat());
                    default -> DoubleTag.valueOf(n.getAsDouble());
                };
                double before = n.getAsDouble(), after = cast.getAsDouble();
                // a double kept as a float only loses precision; anything else that moves is lost data
                boolean moved = want.getId() == Tag.TAG_FLOAT ? Math.abs(after - before) > 1e-6 * Math.max(1, Math.abs(before)) : after != before;
                if (moved) changed.put(path, value);
                tag.put(key, cast);
            }
        }
    }

    /** Every field of {@code tag} but the ignored ones: what is lost when Continuum cannot load it at all. */
    public static Map<String, Tag> everything(CompoundTag tag, Set<String> ignored) {
        Map<String, Tag> out = new LinkedHashMap<>();
        for (String key : tag.getAllKeys()) if (!ignored.contains(key)) out.put(key, tag.get(key));
        return out;
    }

    /** The tag at a path written by {@link #lost}: keys split on dots outside brackets, a bracket an index or an id. */
    static Tag at(CompoundTag root, String path) {
        Tag current = root;
        int i = 0;
        while (i < path.length() && current != null) {
            if (path.charAt(i) == '.') {
                i++;
            } else if (path.charAt(i) == '[') {
                int close = path.indexOf(']', i);
                if (close < 0) return null;
                current = element(current, path.substring(i + 1, close));
                i = close + 1;
            } else {
                int end = i;
                while (end < path.length() && path.charAt(end) != '.' && path.charAt(end) != '[') end++;
                current = current instanceof CompoundTag c ? c.get(path.substring(i, end)) : null;
                i = end;
            }
        }
        return current;
    }

    private static Tag element(Tag list, String index) {
        if (!(list instanceof ListTag l)) return null;
        if (index.matches("\\d+")) return Integer.parseInt(index) < l.size() ? l.get(Integer.parseInt(index)) : null;
        for (Tag t : l) if (t instanceof CompoundTag c && index.equals(c.getString("id"))) return t;
        return null;
    }

    /** Entries all named by a distinct id (attributes, modifiers); stack lists can repeat an id and keep their order. */
    private static boolean keyedById(ListTag list) {
        if (list.isEmpty() || list.getElementType() != Tag.TAG_COMPOUND) return false;
        Set<String> ids = new HashSet<>();
        for (Tag t : list) if (!((CompoundTag) t).contains("id", Tag.TAG_STRING) || !ids.add(((CompoundTag) t).getString("id"))) return false;
        return true;
    }

    private static boolean empty(Tag tag) {
        if (tag instanceof NumericTag n) return n.getAsDouble() == 0;
        if (tag instanceof CollectionTag<?> c) return c.isEmpty();
        if (tag instanceof CompoundTag c) return c.isEmpty();
        String s = tag.getAsString();
        return s.isEmpty() || s.equals("hbm:none") || s.equals("minecraft:empty") || s.equals("NONE");
    }

    private static void compare(String path, Tag a, Tag b, Map<String, Tag> lost) {
        if (a instanceof CompoundTag ca && !ca.isEmpty() && (b == null || b instanceof CompoundTag)) {
            CompoundTag cb = b == null ? new CompoundTag() : (CompoundTag) b;
            for (String key : ca.getAllKeys()) compare(path + "." + key, ca.get(key), cb.get(key), lost);
        } else if (b == null) {
            lost.put(path, a);
        } else if (a instanceof ListTag la && b instanceof ListTag lb && keyedById(la) && keyedById(lb)) {
            // attribute and modifier lists carry no order; match their entries by id
            Map<String, Tag> byId = new LinkedHashMap<>();
            for (Tag t : lb) byId.put(((CompoundTag) t).getString("id"), t);
            for (Tag t : la) {
                String id = ((CompoundTag) t).getString("id");
                compare(path + "[" + id + "]", t, byId.get(id), lost);
            }
        } else if (a instanceof ListTag la && b instanceof ListTag lb && UNORDERED.contains(path.substring(path.lastIndexOf('.') + 1))) {
            if (!multiset(la).equals(multiset(lb))) lost.put(path, a);
        } else if (a instanceof ListTag la && b instanceof ListTag lb && la.size() == lb.size()) {
            for (int i = 0; i < la.size(); i++) compare(path + "[" + i + "]", la.get(i), lb.get(i), lost);
        } else if (!same(a, b)) {
            lost.put(path, a);
        }
    }

    private static Map<Tag, Integer> multiset(ListTag list) {
        Map<Tag, Integer> out = new HashMap<>();
        for (Tag t : list) out.merge(t, 1, Integer::sum);
        return out;
    }

    /** Numbers by value; strings ignoring case, since Continuum reads enum names in any case and saves them lower case. */
    private static boolean same(Tag a, Tag b) {
        if (a instanceof NumericTag na && b instanceof NumericTag nb) return na.getAsDouble() == nb.getAsDouble();
        if (a instanceof StringTag sa && b instanceof StringTag sb) return sa.getAsString().equalsIgnoreCase(sb.getAsString());
        return a.equals(b);
    }
}
