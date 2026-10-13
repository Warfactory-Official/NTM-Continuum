package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.world.gen.LegacyDungeonStructure;
import com.hbm.world.gen.LegacyDungeonWorld;
import dev.golem.core.harness.HarnessQueryRegistry;
import dev.golem.core.registry.BlockEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;

/**
 * Harness query {@code hbm_wgd} (dev clients with the harness on): the WG-D legacy dungeon starts referenced by the chunk
 * at {@code x, z} in the player's dimension, read on the integrated server thread. Per start: the dungeon, its 1.7 site,
 * its box, how many of the blocks the 1.7 builder wrote stand in the world as that block (non-air only; {@code misses}:
 * the most common expected->found pairs), the containers and how many hold items, the spawners' mobs and the block
 * entities by type. {@code rebuild=<dungeon>} assembles the start of that dungeon at the chunk again (for
 * {@code /place structure}, which records no start), with the biome check off, as the command does.
 */
public final class LegacyDungeonHarnessQueries {

    private LegacyDungeonHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_wgd", LegacyDungeonHarnessQueries::query);
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        if (mc.player == null) {
            JsonObject json = new JsonObject();
            json.addProperty("error", "no integrated server");
            return json;
        }
        JsonObject request = params.deepCopy();
        if (!request.has("x")) request.addProperty("x", mc.player.blockPosition().getX());
        if (!request.has("z")) request.addProperty("z", mc.player.blockPosition().getZ());
        return ServerQueries.answer(mc, "hbm_wgd", request, (level, p) -> onServer(level, p.get("x").getAsInt(),
                p.get("z").getAsInt(), p.has("rebuild") ? p.get("rebuild").getAsString() : null));
    }

    private static JsonObject onServer(ServerLevel level, int x, int z, String rebuild) {
        JsonObject json = new JsonObject();
        JsonArray starts = new JsonArray();
        ChunkPos chunk = new ChunkPos(x >> 4, z >> 4);
        // Never generate from here (see ServerQueries): only chunks present at FULL are read, and until the 3 x 3 chunks
        // and the starts they reference are, the answer is "pending" and the scenario polls again.
        // /locate names the start chunk, which a small dungeon (the dud sits 8..23 blocks in) may not reach: look around it
        List<StructureStart> found = ServerQueries.loadedStarts(level, x >> 4, z >> 4, 1, s -> s instanceof LegacyDungeonStructure);
        if (found == null) {
            json.addProperty("pending", "chunks around " + x + ", " + z + " are not loaded yet");
            json.add("starts", starts);
            return json;
        }
        for (StructureStart start : found) describe(level, start.getPieces(), starts, false);
        if (starts.isEmpty() && rebuild != null) {
            LegacyDungeonStructure.Kind kind = LegacyDungeonStructure.Kind.byId(rebuild);
            if (kind != null) {
                LegacyDungeonStructure structure = new LegacyDungeonStructure(new Structure.StructureSettings(
                        HolderSet.direct(List.of()), Map.of(),
                        GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
                        TerrainAdjustment.NONE), kind);
                var generator = level.getChunkSource().getGenerator();
                var context = new Structure.GenerationContext(level.registryAccess(), generator,
                        generator.getBiomeSource(), level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(),
                        chunk, level, biome -> true);
                var stub = structure.findValidGenerationPoint(context);
                stub.ifPresent(s -> describe(level, s.getPiecesBuilder().build().pieces(), starts, true));
            }
        }
        json.add("starts", starts);
        return json;
    }

    private static void describe(ServerLevel level, List<StructurePiece> pieces, JsonArray out, boolean rebuilt) {
        for (StructurePiece piece : pieces) {
            if (!(piece instanceof LegacyDungeonStructure.Piece dungeon)) continue;
            JsonObject s = new JsonObject();
            s.addProperty("dungeon", dungeon.kind().id);
            s.addProperty("rebuilt", rebuilt);
            BlockPos site = dungeon.site();
            JsonArray siteJson = new JsonArray();
            siteJson.add(site.getX());
            siteJson.add(site.getY());
            siteJson.add(site.getZ());
            s.add("site", siteJson);
            BoundingBox box = piece.getBoundingBox();
            JsonArray boxJson = new JsonArray();
            boxJson.add(box.minX()); boxJson.add(box.minY()); boxJson.add(box.minZ());
            boxJson.add(box.maxX()); boxJson.add(box.maxY()); boxJson.add(box.maxZ());
            s.add("box", boxJson);

            LegacyDungeonWorld.Result result = dungeon.result(level, level.getChunkSource().getGenerator());
            int matched = 0, total = 0, unloaded = 0;
            Map<String, Integer> misses = new TreeMap<>();
            Map<String, Integer> expectedBlocks = new TreeMap<>();
            if (result != null) {
                for (long key : result.positions()) {
                    BlockState expected = result.stateAt(key);
                    if (expected.isAir()) continue;
                    BlockPos pos = BlockPos.of(key);
                    var loaded = ServerQueries.loaded(level, pos.getX() >> 4, pos.getZ() >> 4);
                    if (loaded == null) { unloaded++; continue; }
                    total++;
                    String id = BuiltInRegistries.BLOCK.getKey(expected.getBlock()).getPath();
                    expectedBlocks.merge(id, 1, Integer::sum);
                    BlockState found = loaded.getBlockState(pos);
                    if (found.is(expected.getBlock())) matched++;
                    else misses.merge(id + "->" + BuiltInRegistries.BLOCK.getKey(found.getBlock()).getPath(), 1, Integer::sum);
                }
            }
            s.addProperty("matched", matched);
            s.addProperty("total", total);
            s.addProperty("unloaded", unloaded);
            JsonObject top = new JsonObject();
            misses.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(8).forEach(e -> top.addProperty(e.getKey(), e.getValue()));
            s.add("misses", top);
            JsonObject blocks = new JsonObject();
            expectedBlocks.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).forEach(e -> blocks.addProperty(e.getKey(), e.getValue()));
            s.add("blocks", blocks);

            int containers = 0, filled = 0;
            Map<String, Integer> entities = new TreeMap<>();
            JsonArray spawners = new JsonArray();
            JsonArray filledAt = new JsonArray();
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
                                if (filledAt.size() < 12) filledAt.add(at.getX() + " " + at.getY() + " " + at.getZ());
                            }
                        }
                        if (be instanceof SpawnerBlockEntity spawner) {
                            var tag = BlockEntities.saveWithoutMetadata(spawner, level.registryAccess());
                            spawners.add(String.valueOf(tag.get("SpawnData")));
                        }
                        entities.merge(String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType())), 1, Integer::sum);
                    }
                }
            }
            s.addProperty("containers", containers);
            s.addProperty("filled_containers", filled);
            s.add("filled_at", filledAt);
            s.add("spawners", spawners);
            JsonObject types = new JsonObject();
            entities.forEach(types::addProperty);
            s.add("block_entities", types);
            out.add(s);
        }
    }
}
