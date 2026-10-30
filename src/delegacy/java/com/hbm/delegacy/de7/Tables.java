package com.hbm.delegacy.de7;

import com.hbm.delegacy.DeAddons;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

/** The generated DE data version 7 tables for stacks and entities ({@code port-tools/de_import/gen_de_tables.py}); keys are DE ids. */
public final class Tables {
    public final Map<String, String> items = load("items");
    public final Map<String, String> fluids = load("fluids");
    public final Map<String, String> entities = load("entities");
    /** DE's 1.7 numeric material ids to Continuum's material names. */
    public final Map<String, String> materials = load("materials");
    /** DE's runtime BulletConfig ids to the ammo names Continuum saves in a gun's magazine. */
    public final Map<String, String> magtypes = load("magtypes");
    /** DE chest loot tables Continuum lacks, to the copies served for them under {@code hbm_de}. */
    public final Map<String, String> lootTables = load("loot_tables");

    private static Tables instance;

    public static synchronized Tables get() {
        if (instance == null) instance = new Tables();
        return instance;
    }

    /** A DE fluid id as Continuum names it. */
    public String fluid(String de) {
        return fluids.getOrDefault(de, DeAddons.rename(de));
    }

    /** A DE material id as Continuum names it, or null. */
    public String material(int id) {
        return materials.get(Integer.toString(id));
    }

    private static Map<String, String> load(String name) {
        Map<String, String> out = new HashMap<>();
        try (InputStream in = Tables.class.getResourceAsStream("/hbm_delegacy/de7/" + name + ".tsv")) {
            if (in == null) throw new IllegalStateException("missing table " + name);
            DeAddons.read(in, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        out.putAll(DeAddons.rows(name));
        return out;
    }
}
