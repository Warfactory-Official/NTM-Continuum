package com.hbm.deimport.engine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@code <world>/hbm_de_import.journal}: one line per file a step has finished, written right after the file is
 * replaced. A run that crashed resumes past those files, so no store is converted twice.
 */
public final class Journal {
    public static final String FILE = "hbm_de_import.journal";
    private final Path file;
    private final Set<String> done = new HashSet<>();

    public Journal(Path world) throws IOException {
        this.file = world.resolve(FILE);
        if (Files.isRegularFile(file)) done.addAll(Files.readAllLines(file, StandardCharsets.UTF_8));
    }

    public synchronized boolean done(String step, Path world, Path path) {
        return done.contains(key(step, world, path));
    }

    public synchronized void mark(String step, Path world, Path path) throws IOException {
        String key = key(step, world, path);
        done.add(key);
        Files.write(file, List.of(key), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String key(String step, Path world, Path path) {
        return step + " " + world.relativize(path).toString().replace('\\', '/');
    }
}
