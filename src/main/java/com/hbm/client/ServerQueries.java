package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * The shared rules for harness queries that read structure starts on the integrated server (dev clients with the harness
 * on): {@code hbm_wgd}, {@code hbm_structure} and {@code hbm_wgc} start mode.
 *
 * <p><b>Never wait for the server on the client thread.</b> A query runs on the client (render) thread, which also answers
 * every other harness request. Right after a scenario's {@code /forceload add}, the server thread is inside that command,
 * which generates each forced chunk synchronously ({@code ServerLevel.setChunkForced} calls {@code getChunk}); on 1.21.1
 * Fabric that takes well over 20-60 s. A timed-out poll leaves its task queued, so polls pile up behind the forceload,
 * the client stops answering, and the world never finishes saving when the scenario is cut off. So a query is
 * submitted once per distinct request and the client waits for it at most {@link #CLIENT_WAIT_MS}; until it has
 * finished, polls of the same request answer {@code pending} without submitting again.
 *
 * <p><b>Never generate a chunk from a query.</b> {@code ServerLevel.hasChunk} asks whether a chunk is <em>ticketed</em> to FULL,
 * not whether it is loaded, so a freshly force-loaded chunk still generating passes it and the next block read generates it
 * synchronously; vanilla's {@code StructureManager.startsForStructure} likewise loads each referenced start chunk to
 * STRUCTURE_STARTS synchronously. Everything here reads only chunks {@code getChunkNow} returns (present at FULL), and
 * reports what it could not read so the scenario polls again.
 */
public final class ServerQueries {

    private ServerQueries() {}

    /** The longest a query blocks the client thread; a request still running after that answers "pending". */
    static final long CLIENT_WAIT_MS = 2_000L;
    /** A finished answer nobody collected is dropped after this long rather than handed out stale. */
    private static final long STALE_MS = 5_000L;

    private record InFlight(CompletableFuture<JsonObject> answer, long[] doneAt) {}

    private static final Map<String, InFlight> IN_FLIGHT = new ConcurrentHashMap<>();

    /**
     * Answers {@code params} for {@code kind} from the server thread without holding the client thread longer than
     * {@link #CLIENT_WAIT_MS}. A pending answer carries {@code pending} and an empty {@code starts}, which every caller's
     * poll loop already treats as "not yet".
     */
    static JsonObject answer(Minecraft mc, String kind, JsonObject params,
                             BiFunction<ServerLevel, JsonObject, JsonObject> onServer) {
        MinecraftServer server = mc.getSingleplayerServer();
        JsonObject json = new JsonObject();
        if (server == null || mc.player == null) {
            json.addProperty("error", "no integrated server");
            return json;
        }
        var dimension = mc.player.level().dimension();
        String key = kind + "@" + dimension + params;
        long now = System.currentTimeMillis();
        InFlight flight = IN_FLIGHT.compute(key, (k, old) -> {
            if (old != null && !(old.answer.isDone() && now - old.doneAt[0] > STALE_MS)) return old;
            long[] doneAt = {Long.MAX_VALUE};
            CompletableFuture<JsonObject> answer = server.submit(() -> onServer.apply(server.getLevel(dimension), params));
            answer.whenComplete((r, e) -> doneAt[0] = System.currentTimeMillis());
            return new InFlight(answer, doneAt);
        });
        try {
            JsonObject result = flight.answer.get(CLIENT_WAIT_MS, TimeUnit.MILLISECONDS);
            IN_FLIGHT.remove(key, flight);
            return result;
        } catch (TimeoutException e) {
            json.addProperty("pending", "the server thread has not reached this query yet");
            json.add("starts", new JsonArray());
            return json;
        } catch (ExecutionException | InterruptedException | RuntimeException e) {
            IN_FLIGHT.remove(key, flight);
            json.addProperty("error", String.valueOf(e.getCause() != null ? e.getCause() : e));
            return json;
        }
    }

    /** The chunk if it is present at FULL, else null. Never loads or generates. Server thread only. */
    static LevelChunk loaded(ServerLevel level, int chunkX, int chunkZ) {
        return level.getChunkSource().getChunkNow(chunkX, chunkZ);
    }

    /** How many chunks under {@code box} are not present at FULL. */
    static int unloadedChunks(ServerLevel level, BoundingBox box) {
        int unloaded = 0;
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                if (loaded(level, cx, cz) == null) unloaded++;
            }
        }
        return unloaded;
    }

    /**
     * The valid starts of the structures {@code wanted} accepts that the chunks {@code radius} around
     * ({@code chunkX}, {@code chunkZ}) reference: vanilla's {@code startsForStructure}, reading only loaded chunks. Returns
     * null when a referencing chunk or a referenced start chunk is not loaded yet.
     */
    static List<StructureStart> loadedStarts(ServerLevel level, int chunkX, int chunkZ, int radius, Predicate<Structure> wanted) {
        Set<StructureStart> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<StructureStart> starts = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = loaded(level, chunkX + dx, chunkZ + dz);
                if (chunk == null) return null;
                for (var references : chunk.getAllReferences().entrySet()) {
                    Structure structure = references.getKey();
                    if (!wanted.test(structure)) continue;
                    for (long reference : references.getValue()) {
                        LevelChunk startChunk = loaded(level, ChunkPos.getX(reference),
                                ChunkPos.getZ(reference));
                        if (startChunk == null) return null;
                        StructureStart start = startChunk.getStartForStructure(structure);
                        if (start != null && start.isValid() && seen.add(start)) starts.add(start);
                    }
                }
            }
        }
        return starts;
    }
}
