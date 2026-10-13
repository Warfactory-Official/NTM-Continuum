package com.hbm.gametest;

import com.hbm.blocks.generic.BlockWandLoot;
import com.hbm.world.gen.NTMStructureDeclarations;
import com.hbm.world.gen.NTMWorldGenerator;
import com.hbm.world.gen.nbt.NBTStructure;
import com.hbm.world.gen.nbt.SpawnCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

import java.util.Random;

/**
 * The world-generation half of the 1.7 NBT structure system (WG-A): the region pick against 1.7's own arithmetic, every
 * 1.7 spawn registered with a loaded piece, and one generated component built, clipped to a chunk box, serialised and
 * read back, with its loot placeholders replacing themselves.
 */
public final class NTMStructureGameTests {

    private NTMStructureGameTests() {}

    /** 1.7 {@code GenStructure.getSpawnAtCoords}: the chunk of region (x, z) that may hold a structure. */
    private static long legacyRegionChunk(long seed, int chunkX, int chunkZ, int max, int min) {
        int x = chunkX, z = chunkZ;
        if (x < 0) x -= max - 1;
        if (z < 0) z -= max - 1;
        x /= max;
        z /= max;
        Random rand = new Random((long) x * 341873128712L + (long) z * 132897987541L + seed + (long) 996996996 - 0);
        x *= max;
        z *= max;
        x += rand.nextInt(max - min);
        z += rand.nextInt(max - min);
        return pack(x, z);
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /** The {@code random_spread} of {@code hbm:ntm_structures} picks exactly the chunk 1.7 picked, for any seed and region. */
    public static void regionPick(GameTestHelper helper) {
        RandomSpreadStructurePlacement placement = new RandomSpreadStructurePlacement(16, 4, RandomSpreadType.LINEAR, NTMStructureDeclarations.SALT);
        long[] seeds = {0L, 4815162342L, -1L, 123456789012345L, Long.MIN_VALUE + 7};
        int checked = 0;
        for (long seed : seeds) {
            for (int cx = -70; cx <= 70; cx += 7) {
                for (int cz = -70; cz <= 70; cz += 11) {
                    ChunkPos modern = placement.getPotentialStructureChunk(seed, cx, cz);
                    long modernKey = pack(modern.getMinBlockX() >> 4, modern.getMinBlockZ() >> 4);
                    long legacy = legacyRegionChunk(seed, cx, cz, 16, 4);
                    helper.assertTrue(modernKey == legacy, "seed " + seed + " chunk " + cx + "," + cz + ": modern "
                            + (modern.getMinBlockX() >> 4) + "," + (modern.getMinBlockZ() >> 4) + " vs 1.7 "
                            + (int) (legacy >> 32) + "," + (int) legacy);
                    checked++;
                }
            }
        }
        helper.assertTrue(checked > 1000, "checked " + checked);
        helper.succeed();
    }

    private static final String[] SPAWNS = {"spire", "vertibird", "crashed_vertibird", "beached_patrol", "aircraft_carrier",
            "oil_rig", "lighthouse", "dish", "water_pump", "forestchem", "labolatory", "forest_post", "radio", "factory",
            "crane", "broadcaster_tower", "plane1", "plane2", "desert_shack_1", "desert_shack_2", "desert_shack_3",
            "dead_dish_small", "ruinA", "ruinB", "ruinC", "ruinD", "ruinE", "ruinF", "ruinG", "ruinH", "ruinI", "ruinJ",
            "tower_base"};

    /** Every 1.7 spawn is registered with its piece loaded, the two null weights have none, heights are 1.7's. */
    public static void spawnsRegistered(GameTestHelper helper) {
        NTMWorldGenerator.registerSpawns();
        for (String name : SPAWNS) {
            SpawnCondition spawn = NBTStructure.getStructure(name);
            helper.assertTrue(spawn != null && spawn.structure != null, name + " is registered with a piece");
            helper.assertTrue(spawn.structure.structure.isLoaded() && spawn.structure.structure.isLegacy(), name + "'s 1.7 .nbt loads");
        }
        SpawnCondition oilRig = NBTStructure.getStructure("oil_rig");
        helper.assertTrue(oilRig.minHeight == 11 && oilRig.maxHeight == 12 && oilRig.structure.heightOffset == -20, "oil rig heights");
        SpawnCondition ruin = NBTStructure.getStructure("ruinC");
        helper.assertTrue(ruin.structure.conformToTerrain && ruin.structure.heightOffset == -1, "ruins conform to the terrain");
        for (String nul : new String[] {NTMStructureDeclarations.NULL_PLAINS, NTMStructureDeclarations.NULL_OCEAN}) {
            SpawnCondition spawn = NBTStructure.getStructure(nul);
            helper.assertTrue(spawn != null && spawn.structure == null && spawn.pools == null && spawn.start == null, nul + " spawns nothing");
        }
        helper.succeed();
    }

    /**
     * Every palette entry of all 33 structures resolves in all four rotations without throwing (1.21.1 once crashed world
     * generation on the dish: a 1.7 metadata value vanilla's flattening marks "%%FILTER_ME%%").
     */
    public static void palettesResolve(GameTestHelper helper) {
        NTMWorldGenerator.registerSpawns();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 1, 0));
        for (String name : SPAWNS) {
            SpawnCondition spawn = NBTStructure.getStructure(name);
            for (int rot = 0; rot < 4; rot++) {
                NBTStructure.Component component = new NBTStructure.Component(spawn, spawn.structure, origin.getX(), origin.getY(), origin.getZ(), rot);
                try {
                    int[] score = component.verify(helper.getLevel());
                    helper.assertTrue(score[1] > 0, name + " has palette blocks");
                } catch (RuntimeException e) {
                    throw new IllegalStateException(name + " rotation " + rot + " fails to resolve: " + e, e);
                }
            }
        }
        helper.succeed();
    }

    /**
     * desert_shack_1 as a generated component: built through {@code postProcess} it matches its palette block for block;
     * built again clipped to a box it writes nothing outside that box; it survives a save/load round trip; and its two
     * loot placeholders replace themselves with filled containers once they tick.
     */
    public static void componentBuild(GameTestHelper helper) {
        NTMWorldGenerator.registerSpawns();
        ServerLevel level = helper.getLevel();
        SpawnCondition spawn = NBTStructure.getStructure("desert_shack_1");
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        NBTStructure.Component component = new NBTStructure.Component(spawn, spawn.structure, origin.getX(), origin.getY(), origin.getZ(), 1);
        BoundingBox box = component.getBoundingBox();
        helper.assertTrue(box.getXSpan() == 16 && box.getYSpan() == 13 && box.getZSpan() == 18,
                "rotation 1 swaps the 18x13x16 footprint: " + box);

        component.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(),
                box, new ChunkPos(origin.getX() >> 4, origin.getZ() >> 4), origin);
        int[] score = component.verify(level);
        helper.assertTrue(score[1] > 100 && score[0] == score[1], "every palette block placed: " + score[0] + "/" + score[1]);

        // round trip through the piece's own tag
        CompoundTag tag = component.createTag(StructurePieceSerializationContext.fromLevel(level));
        NBTStructure.Component loaded = new NBTStructure.Component(StructurePieceSerializationContext.fromLevel(level), tag);
        helper.assertTrue(loaded.getBoundingBox().equals(box) && loaded.getPiece() == spawn.structure
                        && loaded.getCoordBaseMode() == 1 && "desert_shack_1".equals(loaded.getStructureName()),
                "the component reads back as written: " + tag);

        // a second copy beside the first, clipped at x = its min + 7: nothing may appear past the clip
        BlockPos second = origin.offset(0, 0, 20);
        NBTStructure.Component clipped = new NBTStructure.Component(spawn, spawn.structure, second.getX(), second.getY(), second.getZ(), 1);
        BoundingBox full = clipped.getBoundingBox();
        BoundingBox half = new BoundingBox(full.minX(), full.minY(), full.minZ(), full.minX() + 7, full.maxY(), full.maxZ());
        clipped.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(),
                half, new ChunkPos(second.getX() >> 4, second.getZ() >> 4), second);
        int inside = 0, outside = 0;
        for (BlockPos pos : BlockPos.betweenClosed(full.minX(), full.minY(), full.minZ(), full.maxX(), full.maxY(), full.maxZ())) {
            if (level.getBlockState(pos).isAir()) continue;
            if (pos.getX() <= half.maxX()) inside++; else outside++;
        }
        helper.assertTrue(inside > 0 && outside == 0, "clipped build: " + inside + " inside, " + outside + " outside");

        helper.runAfterDelay(10, () -> {
            int placeholders = 0, filled = 0;
            for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof BlockWandLoot.TileEntityWandLoot) placeholders++;
                if (be instanceof Container container && !container.isEmpty()) filled++;
            }
            // desert_shack_1 carries two wand_loot placeholders; each replaces itself on its first tick
            helper.assertTrue(placeholders == 0, placeholders + " loot placeholders left (" + filled + " filled containers)");
            helper.succeed();
        });
    }

}
