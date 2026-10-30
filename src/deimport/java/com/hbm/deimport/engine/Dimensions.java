package com.hbm.deimport.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** Every dimension folder in a world, installed mod or not: vanilla's three plus {@code dimensions/<ns>/<path>}. */
public final class Dimensions {
    private Dimensions() {}

    public static Map<String, Path> all(Path world) throws IOException {
        Map<String, Path> out = new LinkedHashMap<>();
        out.put("minecraft:overworld", world);
        out.put("minecraft:the_nether", world.resolve("DIM-1"));
        out.put("minecraft:the_end", world.resolve("DIM1"));
        Path dims = world.resolve("dimensions");
        if (Files.isDirectory(dims)) {
            try (Stream<Path> namespaces = Files.list(dims)) {
                for (Path ns : namespaces.filter(Files::isDirectory).toList()) {
                    try (Stream<Path> paths = Files.walk(ns)) {
                        for (Path p : paths.filter(p -> Files.isDirectory(p.resolve("region"))).toList())
                            out.put(ns.getFileName() + ":" + ns.relativize(p).toString().replace('\\', '/'), p);
                    }
                }
            }
        }
        out.values().removeIf(p -> !Files.isDirectory(p));
        return out;
    }

    /** {@link #all} and every dimension that saved data but never a chunk (a satellite's orbit nobody landed on). */
    public static Map<String, Path> withData(Path world) throws IOException {
        Map<String, Path> out = all(world);
        Path dims = world.resolve("dimensions");
        if (!Files.isDirectory(dims)) return out;
        try (Stream<Path> namespaces = Files.list(dims)) {
            for (Path ns : namespaces.filter(Files::isDirectory).toList()) {
                try (Stream<Path> paths = Files.walk(ns)) {
                    for (Path p : paths.filter(p -> Files.isDirectory(p.resolve("data"))).toList())
                        out.putIfAbsent(ns.getFileName() + ":" + ns.relativize(p).toString().replace('\\', '/'), p);
                }
            }
        }
        return out;
    }

    /** {@code dims} without the dimensions of DE addons this import leaves in DE's shape. */
    public static Map<String, Path> without(Map<String, Path> dims, Set<String> deferred) {
        dims.keySet().removeIf(id -> deferred.contains(id.substring(0, id.indexOf(':'))));
        return dims;
    }
}
