package com.hbm.deimport.source.de7;

import com.hbm.delegacy.DeAddons;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

/** The generated DE data version 7 tables for blocks and player files; stacks use the legacy layer's tables. */
public final class WorldTables {
    public final Map<String, String> blocks = load("blocks");
    public final Map<String, String> props = load("props");
    public final Map<String, String> advancements = load("advancements");
    /** Biome and structure renames; DE has none of its own, a DE addon's are its rows. */
    public final Map<String, String> biomes = DeAddons.rows("biomes");
    public final Map<String, String> structures = DeAddons.rows("structures");

    private static WorldTables instance;

    public static synchronized WorldTables get() {
        if (instance == null) instance = new WorldTables();
        return instance;
    }

    private static Map<String, String> load(String name) {
        Map<String, String> out = new HashMap<>();
        try (InputStream in = WorldTables.class.getResourceAsStream("/hbm_deimport/de7/" + name + ".tsv")) {
            if (in == null) throw new IllegalStateException("missing table " + name);
            DeAddons.read(in, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        out.putAll(DeAddons.rows(name));
        return out;
    }
}
