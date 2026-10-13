package com.hbm.gametest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Expected gun, receiver, magazine and bullet values of 1.7, interpreted from the source's
 * {@code XFactory*} declarations. The data lives in
 * {@code /hbm/gametest/gun17_expectations.txt}; {@link #ACCEPTED} lists the keys where the port
 * differs, each with its reason.
 */
public final class Gun17Expectations {

    private Gun17Expectations() {}

    public static final String RESOURCE = "/hbm/gametest/gun17_expectations.txt";

    /** Key to reason. Every entry is a port spelling or a documented deviation, never an unexplained number. */
    public static final Map<String, String> ACCEPTED = new LinkedHashMap<>();

    private static List<String[]> rows;

    public static synchronized List<String[]> rows() {
        if (rows != null) return rows;
        List<String[]> out = new ArrayList<>();
        try (InputStream in = Gun17Expectations.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException("missing " + RESOURCE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                out.add(new String[] {line.substring(0, eq), line.substring(eq + 1)});
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + RESOURCE, e);
        }
        rows = out;
        return rows;
    }
}
