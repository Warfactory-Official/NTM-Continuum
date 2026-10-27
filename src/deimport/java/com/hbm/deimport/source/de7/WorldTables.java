package com.hbm.deimport.source.de7;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** The generated DE data version 7 tables for blocks and player files; stacks use the legacy layer's tables. */
public final class WorldTables {
    public final Map<String, String> blocks = load("blocks");
    public final Map<String, String> props = load("props");
    public final Map<String, String> advancements = load("advancements");

    private static WorldTables instance;

    public static synchronized WorldTables get() {
        if (instance == null) instance = new WorldTables();
        return instance;
    }

    private static Map<String, String> load(String name) {
        Map<String, String> out = new HashMap<>();
        try (InputStream in = WorldTables.class.getResourceAsStream("/hbm_deimport/de7/" + name + ".tsv")) {
            if (in == null) throw new IllegalStateException("missing table " + name);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = reader.readLine()) != null; ) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                int tab = line.indexOf('	');
                out.put(line.substring(0, tab), line.substring(tab + 1));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }
}
