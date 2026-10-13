package com.hbm.items.weapon.sedna;

import com.hbm.Constants;
import dev.golem.core.Ids;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * The registry of every declared {@link BulletConfig}, keyed by {@link Identifier}.
 *
 * <p>This replaces upstream's mutable {@code static List<BulletConfig> BulletConfig.configs}, whose
 * <i>list index</i> was written into gun NBT and bullet-entity synched data. That made declaration
 * order save-critical and network-critical at once; inserting a round anywhere but the end silently
 * reinterpreted every loaded magazine and every bullet in flight. Upstream flags this itself with a
 * {@code //TODO: replace with named tags to avoid ID shifting}; this is that fix.
 *
 * <p>Ids are the durable form used in item components. A numeric index is still handed out for the
 * entity's synched data, where a per-tick int is worth the lookup, but it is derived at runtime from
 * registration order and never persisted.
 */
public final class BulletConfigs {

    private static final Map<Identifier, BulletConfig> BY_ID = new LinkedHashMap<>();
    private static final List<BulletConfig> BY_INDEX = new ArrayList<>();

    private BulletConfigs() {}

    /**
     * Registers {@code config} under {@code hbm:<name>} and returns it for chaining.
     *
     * @throws IllegalStateException if the name is already taken; a duplicate is always a copy-paste
     *         bug in an XFactory, and silently replacing the first one would be far harder to find.
     */
    public static BulletConfig register(String name, BulletConfig config) {
        return register(Ids.of(Constants.MOD_ID, name), config);
    }

    /**
     * Registers {@code config} under {@code id}: the form an addon uses, so its rounds live in its own namespace
     * ({@code ntmspace:worldwar}) and can never collide with HBM's. Same duplicate rule as {@link #register(String,
     * BulletConfig)}. Register during the addon's {@code declare()} (after HBM's, see {@code HbmEntry}); the client
     * tracer audit only insists on HBM's own rounds, an addon gives its rounds a tracer with
     * {@code TracerRegistry.setBulletRenderer}/{@code setBeamRenderer}.
     */
    public static synchronized BulletConfig register(Identifier id, BulletConfig config) {
        if (BY_ID.containsKey(id)) {
            throw new IllegalStateException("Duplicate bullet config id: " + id);
        }
        config.id = id;
        BY_ID.put(id, config);
        BY_INDEX.add(config);
        return config;
    }

    /** The config registered under {@code id}, or null if none is. */
    public static BulletConfig get(Identifier id) {
        return id == null ? null : BY_ID.get(id);
    }

    /** The config registered under a parsed id string, or null for an unknown or malformed id. */
    public static BulletConfig get(String id) {
        if (id == null || id.isEmpty()) return null;
        try {
            return get(Ids.parse(id));
        } catch (Exception e) {
            return null;
        }
    }

    /** The runtime index of {@code config}, or -1. Valid for this session only; never persist it. */
    public static int indexOf(BulletConfig config) {
        return BY_INDEX.indexOf(config);
    }

    /** The config at a runtime index, or null when out of range. */
    public static BulletConfig byIndex(int index) {
        return index >= 0 && index < BY_INDEX.size() ? BY_INDEX.get(index) : null;
    }

    public static List<BulletConfig> all() {
        return Collections.unmodifiableList(BY_INDEX);
    }
}
