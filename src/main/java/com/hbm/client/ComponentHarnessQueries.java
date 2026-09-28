package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.world.gen.component.BunkerComponents;
import com.hbm.world.gen.component.CivilianFeatures;
import com.hbm.world.gen.component.Component;
import com.hbm.world.gen.component.OfficeFeatures;
import com.hbm.world.gen.component.SiloComponent;
import com.hbm.world.gen.nbt.NBTStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Harness query {@code hbm_wgc} (WG-C, dev clients with the harness on), on the integrated server thread in the
 * player's dimension:
 * <ul>
 *   <li>{@code mode=start}, {@code x z spawn} (+ {@code rebuild}): the {@code features}/{@code bunker}/{@code meteor_dungeon}
 *       start at that chunk - each piece's class (or jigsaw piece name), box and rotation; over the start's box, the
 *       containers and how many hold items, spawners, and the block-entity types. {@code rebuild} assembles the start
 *       again as {@code /place structure} did (that command records no start);</li>
 *   <li>{@code mode=build}, {@code piece x z seed} (+ {@code y}, the box floor): builds one hardcoded piece ({@code NTMHouse1} ... {@code SiloComponent},
 *       {@code StartingHub}) with its 1.7 rules at x, z in full, and describes it the same way.</li>
 * </ul>
 */
public final class ComponentHarnessQueries {

    private ComponentHarnessQueries() {}

    public static void register() {
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_wgc", ComponentHarnessQueries::query);
    }

    private static final Map<String, Function<RandomSource, Component>> PIECES = new TreeMap<>();

    static {
        PIECES.put("NTMHouse1", r -> new CivilianFeatures.NTMHouse1(r, 0, 0));
        PIECES.put("NTMHouse2", r -> new CivilianFeatures.NTMHouse2(r, 0, 0));
        PIECES.put("NTMLab1", r -> new CivilianFeatures.NTMLab1(r, 0, 0));
        PIECES.put("NTMLab2", r -> new CivilianFeatures.NTMLab2(r, 0, 0));
        PIECES.put("RuralHouse1", r -> new CivilianFeatures.RuralHouse1(r, 0, 0));
        PIECES.put("LargeOffice", r -> new OfficeFeatures.LargeOffice(r, 0, 0));
        PIECES.put("LargeOfficeCorner", r -> new OfficeFeatures.LargeOfficeCorner(r, 0, 0));
        PIECES.put("SiloComponent", r -> new SiloComponent(r, 0, 0));
        PIECES.put("StartingHub", r -> new BunkerComponents.StartingHub(r, 0, 0));
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        MinecraftServer server = mc.getSingleplayerServer();
        JsonObject json = new JsonObject();
        if (server == null || mc.player == null) {
            json.addProperty("error", "no integrated server");
            return json;
        }
        var dimension = mc.player.level().dimension();
        String mode = params.has("mode") ? params.get("mode").getAsString() : "start";
        // start: a read, polled by the scenario, so it never waits on the server thread or generates (see ServerQueries);
        // build writes a piece into the world on purpose and keeps its blocking answer
        if (!mode.equals("build")) return ServerQueries.answer(mc, "hbm_wgc", params, ComponentHarnessQueries::onServer);
        try {
            return server.submit(() -> onServer(server.getLevel(dimension), params)).get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            json.addProperty("error", e.toString());
            return json;
        }
    }

    private static JsonObject onServer(ServerLevel level, JsonObject p) {
        String mode = p.has("mode") ? p.get("mode").getAsString() : "start";
        int x = p.get("x").getAsInt();
        int z = p.get("z").getAsInt();
        JsonObject json = new JsonObject();
        if (mode.equals("build")) {
            String name = p.get("piece").getAsString();
            Function<RandomSource, Component> factory = PIECES.get(name);
            if (factory == null) {
                json.addProperty("error", "no piece " + name + ", known: " + PIECES.keySet());
                return json;
            }
            RandomSource rand = RandomSource.create(p.has("seed") ? p.get("seed").getAsLong() : 0L);
            Component piece = factory.apply(rand);
            piece.move(x - piece.getBoundingBox().minX(), 0, z - piece.getBoundingBox().minZ());
            if (p.has("y")) piece.fixHeight(p.get("y").getAsInt()); // else 1.7's own height rule on the terrain
            BoundingBox b = piece.getBoundingBox();
            // the whole piece at once: every chunk it touches, full height (1.7's per-chunk boxes, all of them)
            piece.build(level, rand, new BoundingBox(b.minX() - 8, level.getMinY(), b.minZ() - 8, b.maxX() + 8, level.getMaxY() - 1, b.maxZ() + 8));
            json.add("start", describe(level, name, List.of(piece)));
            return json;
        }

        String spawn = p.get("spawn").getAsString();
        ChunkPos chunk = new ChunkPos(x >> 4, z >> 4);
        JsonArray starts = new JsonArray();
        List<StructureStart> found = ServerQueries.loadedStarts(level, x >> 4, z >> 4, 0,
                s -> s instanceof NBTStructure.GenStructure g && g.spawnName().equals(spawn));
        if (found == null) {
            json.addProperty("pending", "the chunk at " + x + ", " + z + " or its start chunk is not loaded yet");
            json.add("starts", starts);
            return json;
        }
        for (StructureStart start : found) {
            starts.add(describe(level, spawn, start.getPieces()));
        }
        if (starts.isEmpty() && p.has("rebuild")) {
            List<StructurePiece> pieces = com.hbm.gametest.NTMComponentStructureGameTests.assemble(level, spawn, chunk);
            if (!pieces.isEmpty()) {
                JsonObject s = describe(level, spawn, pieces);
                s.addProperty("rebuilt", true);
                starts.add(s);
            }
        }
        json.add("starts", starts);
        return json;
    }

    private static JsonObject describe(ServerLevel level, String spawn, List<StructurePiece> pieceList) {
        JsonObject s = new JsonObject();
        s.addProperty("spawn", spawn);
        BoundingBox box = StructurePiece.createBoundingBox(pieceList.stream());
        s.add("box", box(box));
        JsonArray pieces = new JsonArray();
        Map<String, Integer> kinds = new TreeMap<>();
        for (StructurePiece piece : pieceList) {
            JsonObject o = new JsonObject();
            String kind;
            if (piece instanceof NBTStructure.Component c) {
                kind = c.getPiece() == null ? "NULL" : c.getPiece().name;
                o.addProperty("rot", c.getCoordBaseMode());
            } else {
                kind = piece.getClass().getSimpleName();
                if (piece instanceof Component c) o.addProperty("rot", c.getCoordMode());
            }
            o.addProperty("piece", kind);
            o.add("box", box(piece.getBoundingBox()));
            pieces.add(o);
            kinds.merge(kind, 1, Integer::sum);
        }
        s.add("pieces", pieces);
        JsonObject k = new JsonObject();
        kinds.forEach(k::addProperty);
        s.add("kinds", k);

        int containers = 0, filled = 0;
        Map<String, Integer> entities = new TreeMap<>();
        JsonArray firstContainers = new JsonArray();
        for (int bx = box.minX() >> 4; bx <= box.maxX() >> 4; bx++) {
            for (int bz = box.minZ() >> 4; bz <= box.maxZ() >> 4; bz++) {
                var loaded = ServerQueries.loaded(level, bx, bz);
                if (loaded == null) continue;
                for (BlockEntity be : loaded.getBlockEntities().values()) {
                    BlockPos at = be.getBlockPos();
                    if (!box.isInside(at)) continue;
                    if (be instanceof Container container) {
                        containers++;
                        if (!container.isEmpty()) {
                            filled++;
                            if (firstContainers.size() < 6) {
                                JsonArray pos = new JsonArray();
                                pos.add(at.getX());
                                pos.add(at.getY());
                                pos.add(at.getZ());
                                firstContainers.add(pos);
                            }
                        }
                    }
                    entities.merge(String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType())), 1, Integer::sum);
                }
            }
        }
        s.addProperty("containers", containers);
        s.addProperty("filled_containers", filled);
        s.add("filled_at", firstContainers);
        JsonObject types = new JsonObject();
        entities.forEach(types::addProperty);
        s.add("block_entities", types);
        return s;
    }

    private static JsonArray box(BoundingBox box) {
        JsonArray a = new JsonArray();
        a.add(box.minX());
        a.add(box.minY());
        a.add(box.minZ());
        a.add(box.maxX());
        a.add(box.maxY());
        a.add(box.maxZ());
        return a;
    }
}
