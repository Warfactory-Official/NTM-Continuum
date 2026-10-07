package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.blocks.generic.BlockWandLogic;
import com.hbm.blocks.generic.BlockWandLoot;
import com.hbm.world.gen.nbt.NBTStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.Map;
import java.util.TreeMap;
import java.util.List;

/**
 * Harness query {@code hbm_structure} (dev clients with the harness on): the HBM NBT structure starts referenced by the
 * chunk at {@code x, z} in the player's dimension, read on the integrated server thread. Per start: the 1.7 spawn name,
 * each piece with its box, rotation and {@link NBTStructure.Component#verify} score, and over the start's box the
 * remaining loot/logic placeholders, the containers (and how many hold items) and the block entities by type.
 */
public final class StructureHarnessQueries {

    private StructureHarnessQueries() {}

    public static void register() {
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_structure", StructureHarnessQueries::query);
        LegacyBiomeHarnessQueries.register(); // hbm_legacy_biome, hbm_modded_biome: the 1.7 biome model on modern/modded biomes
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
        return ServerQueries.answer(mc, "hbm_structure", request, (level, p) -> onServer(level, p.get("x").getAsInt(),
                p.get("z").getAsInt(), p.has("rebuild") ? p.get("rebuild").getAsString() : null));
    }

    private static JsonObject onServer(ServerLevel level, int x, int z, String rebuild) {
        JsonObject json = new JsonObject();
        JsonArray starts = new JsonArray();
        ChunkPos chunk = new ChunkPos(x >> 4, z >> 4);
        // Never generate from here (see ServerQueries): read only chunks present at FULL, answer "pending" otherwise
        List<StructureStart> found = ServerQueries.loadedStarts(level, x >> 4, z >> 4, 0,
                structure -> structure instanceof NBTStructure.GenStructure);
        if (found == null) {
            json.addProperty("pending", "the chunk at " + x + ", " + z + " or its start chunk is not loaded yet");
            json.add("starts", starts);
            return json;
        }
        for (StructureStart start : found) {
            starts.add(describe(level, ((NBTStructure.GenStructure) start.getStructure()).spawnName(), start.getPieces()));
        }
        if (starts.isEmpty() && rebuild != null) {
            // /place structure builds a start without recording it in any chunk: assemble the same start again (same
            // seed, chunk and random, biome check off, as the command does) and describe that
            NBTStructure.GenStructure structure = new NBTStructure.GenStructure(new net.minecraft.world.level.levelgen.structure.Structure.StructureSettings(
                    net.minecraft.core.HolderSet.direct(java.util.List.of()), java.util.Map.of(),
                    net.minecraft.world.level.levelgen.GenerationStep.Decoration.SURFACE_STRUCTURES,
                    net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE), rebuild);
            var generator = level.getChunkSource().getGenerator();
            var context = new net.minecraft.world.level.levelgen.structure.Structure.GenerationContext(level.registryAccess(), generator,
                    generator.getBiomeSource(), level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(),
                    chunk, level, biome -> true);
            var stub = structure.findValidGenerationPoint(context);
            if (stub.isPresent()) {
                JsonObject s = describe(level, rebuild, stub.get().getPiecesBuilder().build().pieces());
                s.addProperty("rebuilt", true);
                starts.add(s);
            }
        }
        json.add("starts", starts);
        return json;
    }

    private static JsonObject describe(ServerLevel level, String spawn, java.util.List<StructurePiece> pieceList) {
        {
            JsonObject s = new JsonObject();
            s.addProperty("spawn", spawn);
            BoundingBox box = StructurePiece.createBoundingBox(pieceList.stream());
            s.add("box", box(box));
            JsonArray pieces = new JsonArray();
            for (StructurePiece piece : pieceList) {
                JsonObject p = new JsonObject();
                p.add("box", box(piece.getBoundingBox()));
                if (piece instanceof NBTStructure.Component component) {
                    p.addProperty("piece", component.getPiece() == null ? "NULL" : component.getPiece().name);
                    p.addProperty("rot", component.getCoordBaseMode());
                    // verify reads every column of the piece; a chunk that is not there yet would be generated by the read
                    int unloaded = ServerQueries.unloadedChunks(level, piece.getBoundingBox());
                    p.addProperty("unloaded", unloaded);
                    Map<String, Integer> misses = new java.util.HashMap<>();
                    int[] score = unloaded > 0 ? new int[] {0, 0} : component.verify(level, misses);
                    p.addProperty("matched", score[0]);
                    p.addProperty("total", score[1]);
                    JsonObject top = new JsonObject();
                    misses.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(8)
                            .forEach(e -> top.addProperty(e.getKey(), e.getValue()));
                    p.add("misses", top);
                }
                pieces.add(p);
            }
            s.add("pieces", pieces);

            int loot = 0, logic = 0, containers = 0, filled = 0;
            Map<String, Integer> entities = new TreeMap<>();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int bx = box.minX() >> 4; bx <= box.maxX() >> 4; bx++) {
                for (int bz = box.minZ() >> 4; bz <= box.maxZ() >> 4; bz++) {
                    var loaded = ServerQueries.loaded(level, bx, bz);
                    if (loaded == null) continue;
                    for (BlockEntity be : loaded.getBlockEntities().values()) {
                        BlockPos at = be.getBlockPos();
                        if (!box.isInside(at)) continue;
                        if (be instanceof BlockWandLoot.TileEntityWandLoot) loot++;
                        if (be instanceof BlockWandLogic.TileEntityWandLogic) logic++;
                        if (be instanceof Container container) {
                            containers++;
                            if (!container.isEmpty()) filled++;
                        }
                        String id = String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()));
                        entities.merge(id, 1, Integer::sum);
                    }
                }
            }
            // Waterloggable blocks over the box: how many hold water, and how many stand dry beside a water source (a
            // structure block that replaced water must hold it; 2026-10-07, chains/bars/scaffold of the oil rig were dry)
            int waterlogged = 0, dryInWater = 0;
            JsonArray dry = new JsonArray();
            for (int bx = box.minX() >> 4; bx <= box.maxX() >> 4; bx++) {
                for (int bz = box.minZ() >> 4; bz <= box.maxZ() >> 4; bz++) {
                    var loaded = ServerQueries.loaded(level, bx, bz);
                    if (loaded == null) continue;
                    for (int px = Math.max(box.minX(), bx << 4); px <= Math.min(box.maxX(), (bx << 4) + 15); px++) {
                        for (int pz = Math.max(box.minZ(), bz << 4); pz <= Math.min(box.maxZ(), (bz << 4) + 15); pz++) {
                            for (int py = box.minY(); py <= box.maxY(); py++) {
                                var state = loaded.getBlockState(pos.set(px, py, pz));
                                if (!state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) continue;
                                if (state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) {
                                    waterlogged++;
                                    continue;
                                }
                                for (var side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                                    BlockPos next = pos.relative(side);
                                    var beside = ServerQueries.loaded(level, next.getX() >> 4, next.getZ() >> 4);
                                    if (beside == null) continue;
                                    if (beside.getFluidState(next).getType() == net.minecraft.world.level.material.Fluids.WATER) {
                                        dryInWater++;
                                        if (dry.size() < 8) dry.add(BuiltInRegistries.BLOCK.getKey(state.getBlock()) + " @ " + px + "," + py + "," + pz);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            s.addProperty("waterlogged", waterlogged);
            s.addProperty("dry_in_water", dryInWater);
            s.add("dry_in_water_sample", dry);
            s.addProperty("wand_loot", loot);
            s.addProperty("wand_logic", logic);
            s.addProperty("containers", containers);
            s.addProperty("filled_containers", filled);
            JsonObject types = new JsonObject();
            entities.forEach(types::addProperty);
            s.add("block_entities", types);
            return s;
        }
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
