package com.hbm.util;

import com.hbm.Constants;
import com.hbm.world.gen.nbt.LegacyStructurePalette;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.NbtFiles;
import dev.golem.core.data.Tags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Saving identities without numeric ids.
 *
 * <p>A global block-state id ({@code Block.getId}), a registry raw id or an enum {@code ordinal()} is only meaningful
 * for one game session and one mod set: the next version, a new block or a reordered enum silently turns the saved
 * number into something else. Everything a block entity, an entity, an item component or a copied setting keeps is
 * therefore written by name here: block states as {@code namespace:block[prop=value,...]} (the same state string the
 * structure wands already save, {@link LegacyStructurePalette#stateString}), enums by {@code name()}. Fluids have
 * their own pair in {@code Fluids.write/read}.
 *
 * <p>Pre-release, no world migration is done: a reader that meets an int where a name belongs falls back to the
 * caller's default and logs one WARN per key and session, instead of failing the load.
 */
public final class SavedIds {

    private SavedIds() {}

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    /**
     * A saved name (enum constant, registry name) as an item-component codec. Decoding is tolerant: an int left by an old save
     * reads as {@code ""} (the caller's default) with one WARN, so the stack still loads.
     */
    public static final Codec<String> NAME_CODEC =
            Codec.either(Codec.STRING, Codec.INT)
                    .xmap(either -> either.map(name -> name, number -> {
                        warnNumeric("ordinal/id", "item component");
                        return "";
                    }), Either::left);

    /** One WARN per (what, key) per session for an old numeric value where a name is saved. */
    public static void warnNumeric(String what, String key) {
        if (WARNED.add(what + ":" + key)) {
            Constants.LOG.warn("[HBM] Found a numeric {} under '{}' in saved data; numeric ids are no longer read, "
                    + "using the default", what, key);
        }
    }

    // block states

    public static String stateName(BlockState state) {
        return LegacyStructurePalette.stateString(state);
    }

    public static BlockState stateFromName(String name, BlockState fallback) {
        if (name == null || name.isEmpty()) return fallback;
        BlockState state = LegacyStructurePalette.parseState(name);
        return state.isAir() && !name.startsWith("minecraft:air") && !name.startsWith("minecraft:cave_air")
                && !name.startsWith("minecraft:void_air") ? fallback : state;
    }

    public static void putState(CompoundTag tag, String key, BlockState state) {
        tag.putString(key, stateName(state));
    }

    public static BlockState getState(CompoundTag tag, String key, BlockState fallback) {
        if (Tags.hasInt(tag, key)) {
            warnNumeric("block state id", key);
            return fallback;
        }
        return stateFromName(Tags.getStringOr(tag, key, ""), fallback);
    }

    public static void putState(DataOut out, String key, BlockState state) {
        out.putString(key, stateName(state));
    }

    public static BlockState getState(DataIn in, String key, BlockState fallback) {
        String name = in.getStringOr(key, null);
        if (name == null) {
            if (in.getIntOr(key, Integer.MIN_VALUE) != Integer.MIN_VALUE) warnNumeric("block state id", key);
            return fallback;
        }
        return stateFromName(name, fallback);
    }

    public static ListTag stateList(Iterable<BlockState> states) {
        ListTag list = new ListTag();
        for (BlockState state : states) list.add(StringTag.valueOf(stateName(state)));
        return list;
    }

    /** A list written by {@link #stateList}; unknown entries are dropped, an old int array reads as empty. */
    public static List<BlockState> readStateList(CompoundTag tag, String key) {
        List<BlockState> states = new ArrayList<>();
        if (Tags.getIntArrayOr(tag, key, null) != null) {
            warnNumeric("block state id array", key);
            return states;
        }
        ListTag list = NbtFiles.list(tag, key);
        for (int i = 0; i < list.size(); i++) {
            BlockState state = stateFromName(NbtFiles.stringAt(list, i), null);
            if (state != null) states.add(state);
        }
        return states;
    }

    // choices from unnamed recipe lists

    /**
     * A selection from a list of unnamed recipes, saved as {@code <signature>#<k>}: the recipe's own content signature
     * (its output, its inputs) and which of the recipes sharing that signature it is. A list index would silently pick
     * another recipe as soon as one is added before it.
     */
    public static String listKey(List<String> signatures, int index) {
        if (index < 0 || index >= signatures.size()) return "";
        String signature = signatures.get(index);
        int k = 0;
        for (int i = 0; i < index; i++) if (signatures.get(i).equals(signature)) k++;
        return signature + "#" + k;
    }

    /** Inverse of {@link #listKey}; -1 when the key is empty or does not name a recipe. */
    public static int fromListKey(List<String> signatures, String key) {
        int hash = key == null ? -1 : key.lastIndexOf('#');
        if (hash < 0) return -1;
        String signature = key.substring(0, hash);
        int k;
        try {
            k = Integer.parseInt(key.substring(hash + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
        for (int i = 0; i < signatures.size(); i++) {
            if (signatures.get(i).equals(signature) && k-- == 0) return i;
        }
        return -1;
    }

    // enums

    /** Writes an enum by {@code name()}. */
    public static void putEnum(DataOut out, String key, Enum<?> value) {
        out.putString(key, value.name().toLowerCase(Locale.ROOT));
    }

    public static void putEnum(CompoundTag tag, String key, Enum<?> value) {
        tag.putString(key, value.name().toLowerCase(Locale.ROOT));
    }

    /** Reads an enum written by {@link #putEnum}; absent, unknown or an old ordinal reads as {@code fallback}. */
    public static <E extends Enum<E>> E getEnum(DataIn in, String key, E fallback) {
        String name = in.getStringOr(key, null);
        if (name == null) {
            if (in.getIntOr(key, Integer.MIN_VALUE) != Integer.MIN_VALUE) warnNumeric("enum ordinal", key);
            return fallback;
        }
        return byName(fallback.getDeclaringClass(), name, fallback);
    }

    public static <E extends Enum<E>> E getEnum(CompoundTag tag, String key, E fallback) {
        if (Tags.hasInt(tag, key)) {
            warnNumeric("enum ordinal", key);
            return fallback;
        }
        String name = Tags.getStringOr(tag, key, null);
        return name == null ? fallback : byName(fallback.getDeclaringClass(), name, fallback);
    }

    /** As {@link #getEnum(DataIn, String, Enum)} for a nullable value: absent or {@code "none"} is null. */
    public static <E extends Enum<E>> E getEnumOrNull(DataIn in, String key, Class<E> type) {
        String name = in.getStringOr(key, null);
        if (name == null) {
            if (in.getIntOr(key, Integer.MIN_VALUE) != Integer.MIN_VALUE) warnNumeric("enum ordinal", key);
            return null;
        }
        return byName(type, name, null);
    }

    public static <E extends Enum<E>> E byName(Class<E> type, String name, E fallback) {
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(name)) return constant;
        }
        if (WARNED.add("enum:" + type.getSimpleName() + ":" + name)) {
            Constants.LOG.warn("[HBM] Unknown {} '{}' in saved data, using the default", type.getSimpleName(), name);
        }
        return fallback;
    }
}
