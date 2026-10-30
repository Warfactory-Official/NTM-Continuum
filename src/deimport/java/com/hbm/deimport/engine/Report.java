package com.hbm.deimport.engine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

/** {@code <world>/hbm_de_import_report.txt}: counts per step plus every refund, drop and deferral with its position. */
public final class Report {
    public static final String FILE = "hbm_de_import_report.txt";
    private final Map<String, AtomicLong> counts = new TreeMap<>();
    private final List<String> lines = new ArrayList<>();

    public void count(String key, long n) {
        synchronized (counts) {
            counts.computeIfAbsent(key, k -> new AtomicLong()).addAndGet(n);
        }
    }

    public void count(String key) {
        count(key, 1);
    }

    public long get(String key) {
        synchronized (counts) {
            AtomicLong v = counts.get(key);
            return v == null ? 0 : v.get();
        }
    }

    public synchronized void line(String text) {
        lines.add(text);
    }

    /** Writes {@code text} to the report file at once, for what a crash before the run's end must not hide. */
    public synchronized void lineNow(Path world, String text) throws IOException {
        Files.write(world.resolve(FILE), List.of(text), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public String summary() {
        StringBuilder out = new StringBuilder();
        synchronized (counts) {
            counts.forEach((k, v) -> out.append(out.isEmpty() ? "" : ", ").append(k).append(' ').append(v.get()));
        }
        return out.toString();
    }

    public synchronized void append(Path world, String heading) throws IOException {
        List<String> text = new ArrayList<>();
        text.add("== " + heading);
        text.add(summary());
        text.addAll(lines);
        text.add("");
        Files.write(world.resolve(FILE), text, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
