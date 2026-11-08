package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.gametest.NTMComponentStructureGameTests;
import com.hbm.world.gen.component.BunkerComponents;
import com.hbm.world.gen.component.CivilianFeatures;
import com.hbm.world.gen.component.Component;
import com.hbm.world.gen.component.OfficeFeatures;
import com.hbm.world.gen.component.SiloComponent;
import com.hbm.world.gen.nbt.NBTStructure;
import com.hbm.world.gen.nbt.SpawnCondition;
import com.hbm.world.gen.NTMWorldGenerator;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.tileentity.deco.TileEntityBobble;
import dev.golem.core.harness.HarnessQueryRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
 *       {@code StartingHub}) with its 1.7 rules at x, z in full (or with {@code chunked}, one chunk box at a time in a
 *       shuffled order, as world generation builds it; {@code rot} picks the rotation), and describes it the same way
 *       plus every multiblock core and how many of its cells stand;</li>
 *   <li>{@code mode=nbt}, {@code piece x y z rot}: builds a single-piece NBT structure chunk by chunk, and lists its
 *       multiblocks and every HBM block with a facing, axis, rotation or orientation (local position).</li>
 * </ul>
 */
public final class ComponentHarnessQueries {

    private ComponentHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_wgc", ComponentHarnessQueries::query);
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
        if (!mode.equals("build") && !mode.equals("nbt")) return ServerQueries.answer(mc, "hbm_wgc", params, ComponentHarnessQueries::onServer);
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
            long seed = p.has("seed") ? p.get("seed").getAsLong() : 0L;
            // the constructors draw the rotation first: step the seed until it gives the one asked for
            if (p.has("rot")) while (RandomSource.create(seed).nextInt(4) != p.get("rot").getAsInt()) seed++;
            RandomSource rand = RandomSource.create(seed);
            Component piece = factory.apply(rand);
            piece.move(x - piece.getBoundingBox().minX(), 0, z - piece.getBoundingBox().minZ());
            if (p.has("y")) piece.fixHeight(p.get("y").getAsInt()); // else 1.7's own height rule on the terrain
            BoundingBox b = piece.getBoundingBox();
            BoundingBox all = new BoundingBox(b.minX() - 8, level.getMinY(), b.minZ() - 8, b.maxX() + 8, level.getMaxY() - 1, b.maxZ() + 8);
            if (p.has("chunked")) {
                for (BoundingBox chunk : chunkBoxes(all, rand)) piece.build(level, rand, chunk);
            } else {
                // the whole piece at once: every chunk it touches, full height (1.7's per-chunk boxes, all of them)
                piece.build(level, rand, all);
            }
            JsonObject s = describe(level, name, List.of(piece));
            s.add("multiblocks", multiblocks(level, piece.getBoundingBox()));
            s.add("blocks", facings(level, piece.getBoundingBox(), piece.getCoordMode()));
            json.add("start", s);
            return json;
        }
        if (mode.equals("nbt")) {
            NTMWorldGenerator.registerSpawns();
            SpawnCondition spawn = NBTStructure.getStructure(p.get("piece").getAsString());
            if (spawn == null || spawn.structure == null) {
                json.addProperty("error", "no single-piece structure " + p.get("piece").getAsString());
                return json;
            }
            int rot = p.has("rot") ? p.get("rot").getAsInt() : 0;
            NBTStructure.Component piece = new NBTStructure.Component(spawn, spawn.structure, x, p.get("y").getAsInt(), z, rot);
            BoundingBox b = piece.getBoundingBox();
            RandomSource rand = RandomSource.create(p.has("seed") ? p.get("seed").getAsLong() : 0L);
            BoundingBox all = new BoundingBox(b.minX(), level.getMinY(), b.minZ(), b.maxX(), level.getMaxY() - 1, b.maxZ());
            for (BoundingBox chunk : chunkBoxes(all, rand)) {
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), rand, chunk,
                        new ChunkPos(chunk.minX() >> 4, chunk.minZ() >> 4), b.getCenter());
            }
            JsonObject s = describe(level, spawn.name, List.of(piece));
            s.add("multiblocks", multiblocks(level, b));
            s.add("blocks", facings(level, b));
            json.add("start", s);
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
            JsonObject d = describe(level, spawn, start.getPieces());
            BoundingBox all = StructurePiece.createBoundingBox(start.getPieces().stream());
            if (loaded(level, all)) d.add("multiblocks", multiblocks(level, all));
            starts.add(d);
        }
        if (starts.isEmpty() && p.has("rebuild")) {
            List<StructurePiece> pieces = NTMComponentStructureGameTests.assemble(level, spawn, chunk);
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

    /** Whether every chunk of {@code box} is loaded (a read must never generate one). */
    private static boolean loaded(ServerLevel level, BoundingBox box) {
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                if (ServerQueries.loaded(level, cx, cz) == null) return false;
            }
        }
        return true;
    }

    private static List<BoundingBox> chunkBoxes(BoundingBox all, RandomSource rand) {
        List<BoundingBox> boxes = new ArrayList<>();
        for (int cx = all.minX() >> 4; cx <= all.maxX() >> 4; cx++) {
            for (int cz = all.minZ() >> 4; cz <= all.maxZ() >> 4; cz++) {
                boxes.add(new BoundingBox(cx << 4, all.minY(), cz << 4, (cx << 4) + 15, all.maxY(), (cz << 4) + 15));
            }
        }
        Collections.shuffle(boxes, new Random(rand.nextLong()));
        return boxes;
    }

    /** Every multiblock core in {@code box}: its block, facing, and how many of its cells stand and point home. */
    private static JsonArray multiblocks(ServerLevel level, BoundingBox box) {
        JsonArray out = new JsonArray();
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY() - 30, box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BlockMultiblockCore core) || core.isCellState(state)) continue;
            BlockPos at = pos.immutable();
            Direction facing = state.getValue(BlockMultiblockCore.FACING);
            int[] counts = new int[3];
            core.visitPlacedCells(level, at, facing, (cell, mask) -> {
                counts[0]++;
                BlockState found = level.getBlockState(cell);
                if (core.isCellOf(found)) counts[1]++;
                if (MultiblockCoreIndex.lookup(level, cell) == at.asLong()) counts[2]++;
            });
            JsonObject o = new JsonObject();
            o.addProperty("block", BuiltInRegistries.BLOCK.getKey(core).getPath());
            o.addProperty("pos", at.getX() + " " + at.getY() + " " + at.getZ());
            o.addProperty("facing", facing.getSerializedName());
            o.addProperty("cells", counts[0]);
            o.addProperty("placed", counts[1]);
            o.addProperty("indexed", counts[2]);
            out.add(o);
        }
        return out;
    }

    private static final Set<String> TURNING = Set.of("facing", "axis", "rotation", "orient", "gauge", "wall", "shape");

    /** Every HBM block in {@code box} with a facing, axis, rotation or orient property, as {@code x y z id props}. */
    private static JsonArray facings(ServerLevel level, BoundingBox box) {
        return facings(level, box, -1);
    }

    /**
     * {@link #facings} with positions in a hardcoded piece's own 1.7 frame for {@code coordBaseMode} 0..3 (the inverse
     * of {@code Component.getXWithOffset/getZWithOffset}); -1 keeps them relative to the box corner.
     */
    private static JsonArray facings(ServerLevel level, BoundingBox box, int coordBaseMode) {
        JsonArray out = new JsonArray();
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            BlockState state = level.getBlockState(pos);
            var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (!"hbm".equals(id.getNamespace())) continue;
            StringBuilder props = new StringBuilder();
            for (Property<?> property : state.getProperties()) {
                String n = property.getName();
                if (TURNING.contains(n)) {
                    props.append(n).append('=').append(valueName(state, property)).append(' ');
                }
            }
            if (props.isEmpty()) continue;
            String type = level.getBlockEntity(pos) instanceof TileEntityBobble bobble ? " type=" + bobble.type().name() : "";
            int dx = pos.getX() - box.minX(), dz = pos.getZ() - box.minZ();
            int ex = box.maxX() - pos.getX(), ez = box.maxZ() - pos.getZ();
            int lx = switch (coordBaseMode) { case 1 -> dz; case 2 -> ex; case 3 -> ez; default -> dx; };
            int lz = switch (coordBaseMode) { case 1 -> ex; case 2 -> ez; case 3 -> dx; default -> dz; };
            out.add(lx + " " + (pos.getY() - box.minY()) + " " + lz + " " + id.getPath() + " " + props.toString().trim() + type);
        }
        return out;
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
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
