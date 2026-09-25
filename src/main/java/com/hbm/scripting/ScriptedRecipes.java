package com.hbm.scripting;

import com.hbm.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Machine-recipe edits from startup scripts, held per registry until it builds.
 *
 * <p>A registry is keyed by its instance (a {@code GenericRecipes}) or its class (a static table) and
 * calls {@link #apply} after each build of its built-ins; every edit replays on every build. An edit
 * queued after a build also runs at once. Registries build lazily, after item registration, so an
 * edit may resolve items when it runs.
 */
public final class ScriptedRecipes {

    private ScriptedRecipes() {}

    private record Op(String description, Runnable body) {}

    private static final Map<Object, List<Op>> CONTENT = new HashMap<>();
    private static final Map<Object, List<Op>> EDITS = new HashMap<>();
    private static final Set<Object> BUILT = new HashSet<>();

    public static void enqueue(Object registry, String description, Runnable op) {
        enqueue(EDITS, registry, description, op);
    }

    /** A recipe owned by minted content: kept across script reloads, applied before script edits. */
    public static void enqueueContent(Object registry, String description, Runnable op) {
        enqueue(CONTENT, registry, description, op);
    }

    private static void enqueue(Map<Object, List<Op>> table, Object registry, String description, Runnable op) {
        Op entry = new Op(description, op);
        synchronized (ScriptedRecipes.class) {
            table.computeIfAbsent(registry, key -> new ArrayList<>()).add(entry);
            if (!BUILT.contains(registry)) return;
        }
        run(entry);
    }

    /** Drops every script edit and forgets which registries built; they rebuild on next use. */
    static synchronized void clear() {
        EDITS.clear();
        BUILT.clear();
    }

    public static void apply(Object registry) {
        List<Op> ops;
        synchronized (ScriptedRecipes.class) {
            BUILT.add(registry);
            ops = new ArrayList<>(CONTENT.getOrDefault(registry, List.of()));
            ops.addAll(EDITS.getOrDefault(registry, List.of()));
        }
        ops.forEach(ScriptedRecipes::run);
    }

    private static void run(Op op) {
        try {
            op.body().run();
        } catch (RuntimeException e) {
            Constants.LOG.error("[scripts] recipe edit {} failed: {}", op.description(), e.getMessage(), e);
        }
    }
}
