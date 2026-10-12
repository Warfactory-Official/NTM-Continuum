package com.hbm.api.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * A registry of records keyed by name, for HBM content kinds that 1.7 kept in a closed enum or a static table and
 * that addons add to (satellites, bomber payloads, bedrock ore types, ...). The key is the name saves already carry:
 * HBM's own entries keep their pre-registry name (often a bare {@code "mapper"}), an addon's entries use its
 * namespace ({@code "ntmspace:dyson_relay"}), so nothing already saved changes and two addons cannot collide.
 *
 * <p>Iteration order is registration order: HBM registers its 1.7 entries first, in 1.7 order, so loops that 1.7 ran
 * over {@code values()} keep their order. Register at mod construction (or from a class initialiser that runs then);
 * a duplicate name is a startup error naming the name, never a silent replacement. Reads are lock-free snapshots.
 */
public final class NamedRegistry<T> {

    private final String kind;
    private final Function<T, String> nameOf;
    private volatile Map<String, T> byName = Map.of();
    private volatile List<T> values = List.of();

    /**
     * @param kind   what the entries are, for error messages ("satellite type")
     * @param nameOf the name an entry was registered under (entries carry their own name)
     */
    public NamedRegistry(String kind, Function<T, String> nameOf) {
        this.kind = kind;
        this.nameOf = nameOf;
    }

    /** A registry holding {@code values} in order (for an interface constant, which cannot run a static block). */
    @SafeVarargs
    public static <T> NamedRegistry<T> of(String kind, Function<T, String> nameOf, T... values) {
        NamedRegistry<T> registry = new NamedRegistry<>(kind, nameOf);
        for (T value : values) registry.register(value);
        return registry;
    }

    /** Registers {@code value} under its own name and returns it. */
    public synchronized <V extends T> V register(V value) {
        String name = Objects.requireNonNull(nameOf.apply(value), "name");
        if (byName.containsKey(name)) throw new IllegalStateException("The " + kind + " '" + name + "' is registered twice");
        Map<String, T> next = new LinkedHashMap<>(byName);
        next.put(name, value);
        byName = Collections.unmodifiableMap(next);
        values = List.copyOf(next.values());
        return value;
    }

    /** The entry registered as {@code name}, or null. */
    public T get(String name) {
        return name == null ? null : byName.get(name);
    }

    /** The name {@code value} is registered under. */
    public String name(T value) {
        return nameOf.apply(value);
    }

    public boolean contains(String name) {
        return name != null && byName.containsKey(name);
    }

    /** Every entry, in registration order (HBM's 1.7 order first). */
    public List<T> values() {
        return values;
    }

    public Collection<String> names() {
        return byName.keySet();
    }

    public int size() {
        return values.size();
    }

    /** The entries as a codec of their names; an unknown name is a decode error. */
    public Codec<T> codec() {
        return Codec.STRING.comapFlatMap(name -> {
            T value = get(name);
            return value != null ? DataResult.success(value) : DataResult.error(() -> "Unknown " + kind + ": " + name);
        }, nameOf);
    }
}
