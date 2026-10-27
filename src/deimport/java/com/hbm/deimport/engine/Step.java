package com.hbm.deimport.engine;

import java.io.IOException;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * One unit of the import. Adding, replacing or removing a step is one class plus one line in {@code Steps.ALL}. A step
 * marked done never runs again, and neither does a new version of it: only terrain keeps a DE stamp, so every other
 * store would be converted twice. A fix for imported worlds is a step with a new name, which runs in worlds already
 * imported and finds what the earlier steps kept: the leftovers and the backup.
 */
public interface Step {
    enum Phase { FILES, LIVE }

    /** {@code name@version}, the key in the state file. */
    String id();

    default Phase phase() {
        return Phase.FILES;
    }

    /** Mods that must be loaded; while one is missing the step defers instead of running. */
    default Set<String> requiresMods() {
        return Set.of();
    }

    /** Cheap and read-only: is there anything for this step in the world? */
    boolean applies(Ctx ctx) throws IOException;

    /** False keeps the step deferred to a later start. {@code server} has no levels yet in the files phase. */
    boolean run(Ctx ctx, @Nullable MinecraftServer server) throws IOException;

    /**
     * After a successful {@link #run}: what the step still waits for (a dimension that does not exist yet), or null when
     * it is done. A waiting step runs again on every start and keeps the world {@code partial}.
     */
    default @Nullable String waitsFor(Ctx ctx) {
        return null;
    }

    /** Called instead of {@link #run} while a required mod is missing: move the data where nothing loads it. */
    default void defer(Ctx ctx) throws IOException {}
}
