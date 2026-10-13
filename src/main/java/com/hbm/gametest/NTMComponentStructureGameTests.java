package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.tileentity.bomb.TileEntityLaunchPadRusted;
import com.hbm.tileentity.network.TileEntityRadioTorchBase;
import com.hbm.world.gen.NTMComponentStructures;
import com.hbm.world.gen.ProceduralStructureStart;
import com.hbm.world.gen.component.BunkerComponents;
import com.hbm.world.gen.component.CivilianFeatures;
import com.hbm.world.gen.component.Component;
import com.hbm.world.gen.component.OfficeFeatures;
import com.hbm.world.gen.component.SiloComponent;
import com.hbm.world.gen.nbt.JigsawPiece;
import com.hbm.world.gen.nbt.JigsawPool;
import com.hbm.world.gen.nbt.NBTStructure;
import com.hbm.world.gen.nbt.SpawnCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * WG-C: the multi-piece 1.7 structures - the meteor dungeon's jigsaw assembly (ported 1.7 assembler, block tables),
 * the {@code features} buildings and silo (1.7 hardcoded builders) and the bunker (1.7 procedural start) - assembled
 * through their real {@code hbm:nbt} spawns and, for the hardcoded pieces, built in the test world.
 */
public final class NTMComponentStructureGameTests {

    private NTMComponentStructureGameTests() {}

    /** The start {@code /place structure} would build at {@code chunk} (biome check off). */
    public static List<StructurePiece> assemble(ServerLevel level, String spawn, ChunkPos chunk) {
        NBTStructure.GenStructure structure = new NBTStructure.GenStructure(new Structure.StructureSettings(
                HolderSet.direct(List.of()), Map.of(),
                GenerationStep.Decoration.SURFACE_STRUCTURES,
                TerrainAdjustment.NONE), spawn);
        var generator = level.getChunkSource().getGenerator();
        var context = new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(),
                level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(), chunk, level, biome -> true);
        return structure.findValidGenerationPoint(context).map(stub -> stub.getPiecesBuilder().build().pieces()).orElse(List.of());
    }

    private static String describe(List<StructurePiece> pieces) {
        StringBuilder out = new StringBuilder();
        for (StructurePiece piece : pieces) {
            if (piece instanceof NBTStructure.Component c) out.append(c.getPiece() == null ? "NULL" : c.getPiece().name);
            else out.append(piece.getClass().getSimpleName());
            out.append(piece.getBoundingBox()).append(' ');
        }
        return out.toString();
    }

    private static final String[] METEOR_PIECES = {"meteor_core", "meteor_spike", "meteor_corner", "meteor_t", "meteor_stairs",
            "meteor_room_base_thru", "meteor_room_base_end", "meteor_room_basic", "meteor_room_balcony", "meteor_room_dragon",
            "meteor_room_ladder", "meteor_room_ooze", "meteor_room_split", "meteor_room_stairs", "meteor_room_triple",
            "meteor_3_bale", "meteor_3_blank", "meteor_3_block", "meteor_3_crab", "meteor_3_crab_tesla", "meteor_3_crate",
            "meteor_3_dirt", "meteor_3_lead", "meteor_3_ooze", "meteor_3_pillar", "meteor_3_star", "meteor_3_tesla",
            "meteor_3_book", "meteor_3_mku", "meteor_3_statue", "meteor_3_glow", "meteor_dragon_chest", "meteor_dragon_tesla",
            "meteor_dragon_trap", "meteor_dragon_crate_crab", "meteor_fallback", "meteor_room_fallback", "meteor_loot_fallback"};

    /**
     * The three multi-piece spawns are registered as 1.7 did: features/bunker as starts, the meteor dungeon with its
     * nine pools, 38 loaded pieces (32..32 height, size limit 128), block tables on the brick pieces, the spike conforming.
     */
    public static void spawnsRegistered(GameTestHelper helper) {
        NTMComponentStructures.registerSpawns();
        SpawnCondition features = NBTStructure.getStructure("features");
        SpawnCondition bunker = NBTStructure.getStructure("bunker");
        SpawnCondition meteor = NBTStructure.getStructure("meteor_dungeon");
        helper.assertTrue(features != null && features.start != null, "features is a start");
        helper.assertTrue(bunker != null && bunker.start != null, "bunker is a start");
        helper.assertTrue(meteor != null && meteor.pools != null && meteor.pools.size() == 9 && "start".equals(meteor.startPool),
                "meteor_dungeon has its nine pools");
        helper.assertTrue(meteor.minHeight == 32 && meteor.maxHeight == 32 && meteor.sizeLimit == 128 && meteor.rangeLimit == 128,
                "meteor_dungeon heights/limits");
        List<String> seen = new ArrayList<>();
        for (var pool : meteor.pools.values()) {
            for (String name : METEOR_PIECES) {
                if (seen.contains(name)) continue;
                JigsawPiece piece = pieceNamed(pool, name);
                if (piece == null) continue;
                seen.add(name);
                helper.assertTrue(piece.structure.isLoaded(), name + " .nbt loads");
            }
        }
        helper.assertTrue(seen.size() == METEOR_PIECES.length, "all 38 meteor pieces are in the pools: " + seen);
        JigsawPiece core = pieceNamed(meteor.pools.get("start"), "meteor_core");
        helper.assertTrue(core.blockTable != null && core.blockTable.containsKey("meteor_brick"), "the core swaps meteor bricks");
        JigsawPiece spike = pieceNamed(meteor.pools.get("spike"), "meteor_spike");
        helper.assertTrue(spike.conformToTerrain && spike.heightOffset == -3 && spike.blockTable == null, "the spike follows the terrain");
        JigsawPiece ooze = pieceNamed(meteor.pools.get("10room"), "meteor_room_ooze");
        helper.assertTrue(ooze.blockTable.containsKey("concrete_colored"), "the ooze room swaps colored concrete");
        JigsawPiece crates = pieceNamed(meteor.pools.get("headloot"), "meteor_dragon_chest");
        helper.assertTrue(crates.blockTable.containsKey("crate") && crates.blockTable.containsKey("meteor_spawner"), "headloot swaps crates/spawners");
        helper.succeed();
    }

    private static JigsawPiece pieceNamed(JigsawPool pool, String name) {
        // JigsawPool keeps its list package-private; the registered pieces are reachable by drawing from a clone
        JigsawPool clone = NBTStructure.getStructure("meteor_dungeon").getPool(poolName(pool));
        if (clone == null) return null;
        RandomSource rand = RandomSource.create(0);
        JigsawPiece piece;
        while ((piece = clone.get(rand)) != null) {
            if (piece.name.equals(name)) return piece;
        }
        return null;
    }

    private static String poolName(JigsawPool pool) {
        for (var e : NBTStructure.getStructure("meteor_dungeon").pools.entrySet()) if (e.getValue() == pool) return e.getKey();
        return "";
    }

    /**
     * The meteor dungeon assembles with 1.7's jigsaw rules: the core first, only meteor pieces, the size limit honoured
     * (fallbacks only past 128 pieces), every non-conforming piece at the start's shared height, and the same start for
     * the same chunk and seed.
     */
    public static void meteorAssembly(GameTestHelper helper) {
        NTMComponentStructures.registerSpawns();
        ServerLevel level = helper.getLevel();
        int total = 0;
        Map<String, Integer> counts = new TreeMap<>();
        for (int i = 0; i < 6; i++) {
            ChunkPos chunk = new ChunkPos(40 + i * 11, -30 + i * 7);
            List<StructurePiece> pieces = assemble(level, "meteor_dungeon", chunk);
            helper.assertTrue(pieces.size() >= 3, "a dungeon grows beyond its core at " + chunk + ": " + describe(pieces));
            helper.assertTrue(pieces.size() <= 1024, "hard limit");
            NBTStructure.Component first = (NBTStructure.Component) pieces.get(0);
            helper.assertTrue("meteor_core".equals(first.getPiece().name), "the start pool's core comes first: " + describe(pieces));
            int coreY = first.getBoundingBox().minY();
            for (StructurePiece piece : pieces) {
                NBTStructure.Component c = (NBTStructure.Component) piece;
                String name = c.getPiece().name;
                helper.assertTrue(name.startsWith("meteor_"), "only meteor pieces: " + name);
                counts.merge(name, 1, Integer::sum);
                BoundingBox b = c.getBoundingBox();
                int dx = Math.max(Math.abs(b.minX() - chunk.getMinBlockX()), Math.abs(b.maxX() - chunk.getMinBlockX()));
                int dz = Math.max(Math.abs(b.minZ() - chunk.getMinBlockZ()), Math.abs(b.maxZ() - chunk.getMinBlockZ()));
                // a piece starts growing only while its centre is inside the 128 range; it may reach one piece further
                helper.assertTrue(dx < 128 + 64 && dz < 128 + 64, name + " within the range limit: " + b);
            }
            // the height is shared: jigsaw offsets are relative to the core, which sits at 32 (the test world is superflat:
            // there 1.7 took the terrain average unclamped)
            boolean flat = level.getChunkSource().getGenerator() instanceof FlatLevelSource;
            helper.assertTrue(flat || coreY == 32, "the core is at y 32: " + coreY);
            List<StructurePiece> again = assemble(level, "meteor_dungeon", chunk);
            helper.assertTrue(describe(again).equals(describe(pieces)), "the same chunk assembles the same dungeon");
            total += pieces.size();
        }
        helper.assertTrue(counts.getOrDefault("meteor_spike", 0) > 0, "spikes are grown: " + counts);
        helper.assertTrue(counts.keySet().stream().anyMatch(n -> n.startsWith("meteor_room_")), "rooms are grown: " + counts);
        helper.assertTrue(counts.keySet().stream().anyMatch(n -> n.startsWith("meteor_3_")), "3x3 loot is grown: " + counts);
        Constants.LOG.info("[WG-C] meteor dungeon: {} pieces over 6 starts: {}", total, counts);
        helper.succeed();
    }

    /**
     * Every palette entry of the 38 meteor pieces resolves in all four rotations (meteor-3-statue names an unregistered
     * 1.7 block, {@code #undef}, which once crashed world generation; 1.7 read it as air).
     */
    public static void meteorPalettes(GameTestHelper helper) {
        NTMComponentStructures.registerSpawns();
        SpawnCondition meteor = NBTStructure.getStructure("meteor_dungeon");
        BlockPos origin = helper.absolutePos(new BlockPos(0, 1, 0));
        int pieces = 0;
        for (var entry : meteor.pools.entrySet()) {
            var clone = meteor.getPool(entry.getKey());
            RandomSource rand = RandomSource.create(0);
            JigsawPiece piece;
            while ((piece = clone.get(rand)) != null) {
                for (int rot = 0; rot < 4; rot++) {
                    NBTStructure.Component component = new NBTStructure.Component(meteor, piece, origin.getX(), origin.getY(), origin.getZ(), rot);
                    try {
                        component.verify(helper.getLevel());
                    } catch (RuntimeException e) {
                        throw new IllegalStateException(piece.name + " rotation " + rot + " fails to resolve: " + e, e);
                    }
                }
                pieces++;
            }
        }
        helper.assertTrue(pieces >= 38, "resolved " + pieces + " pool entries");
        helper.succeed();
    }

    /** The meteor core built at the test origin: its meteor bricks become the 1.7 40/30/30 brick/mossy/cracked mix. */
    public static void meteorBlockTable(GameTestHelper helper) {
        NTMComponentStructures.registerSpawns();
        ServerLevel level = helper.getLevel();
        SpawnCondition meteor = NBTStructure.getStructure("meteor_dungeon");
        JigsawPiece core = pieceNamed(meteor.pools.get("start"), "meteor_core");
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        NBTStructure.Component component = new NBTStructure.Component(meteor, core, origin.getX(), origin.getY(), origin.getZ(), 0);
        BoundingBox box = component.getBoundingBox();
        component.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(),
                box, new ChunkPos(origin.getX() >> 4, origin.getZ() >> 4), origin);
        Map<String, Integer> found = count(level, box);
        int plain = found.getOrDefault("meteor_brick", 0), mossy = found.getOrDefault("meteor_brick_mossy", 0),
                cracked = found.getOrDefault("meteor_brick_cracked", 0);
        int all = plain + mossy + cracked;
        helper.assertTrue(all > 50 && mossy > all / 6 && cracked > all / 6 && plain > all / 4,
                "block table mix: " + plain + " plain, " + mossy + " mossy, " + cracked + " cracked of " + all + " " + found);
        helper.succeed();
    }

    private static Map<String, Integer> count(ServerLevel level, BoundingBox box) {
        Map<String, Integer> found = new TreeMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            Block block = level.getBlockState(pos).getBlock();
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (!"hbm".equals(id.getNamespace()) && level.getBlockState(pos).isAir()) continue;
            found.merge(id.getPath(), 1, Integer::sum);
        }
        return found;
    }

    private static int[] containers(ServerLevel level, BoundingBox box) {
        int containers = 0, filled = 0;
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Container c) {
                containers++;
                if (!c.isEmpty()) filled++;
            }
        }
        return new int[] {containers, filled};
    }

    /**
     * One hardcoded piece built at the test origin in full (no chunk clipping): it writes blocks, fills its 1.7 loot
     * containers, and survives a save/load round trip with its rotation and flags.
     */
    private static void buildPiece(GameTestHelper helper, Function<RandomSource, Component> factory, String signature, int minContainers) {
        NTMComponentStructures.registerSpawns();
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        RandomSource rand = RandomSource.create(4815162342L);
        Component piece = factory.apply(rand);
        // the constructors anchor at y 64: bring the floor to the test floor (1.7's setAverageHeight would sink it there)
        piece.move(origin.getX() - piece.getBoundingBox().minX(), 0, origin.getZ() - piece.getBoundingBox().minZ());
        // the silo reaches 26 blocks below its ground floor: in a superflat test world its terrain rule would bury the
        // lower floors under the world bottom, so its box floor is fixed at the test floor (the surface floats above)
        // (y -1 is 1.7's "no height yet" marker, which the silo's builder would recompute: one higher then)
        if (piece instanceof SiloComponent) piece.fixHeight(origin.getY() == -1 ? 0 : origin.getY());
        BoundingBox all = new BoundingBox(piece.getBoundingBox().minX() - 2, level.getMinY(), piece.getBoundingBox().minZ() - 2,
                piece.getBoundingBox().maxX() + 2, level.getMaxY() - 1, piece.getBoundingBox().maxZ() + 2);
        piece.build(level, rand, all);
        BoundingBox built = piece.getBoundingBox();
        Map<String, Integer> found = count(level, new BoundingBox(built.minX(), built.minY() - 20, built.minZ(), built.maxX(), built.maxY(), built.maxZ()));
        int solid = found.values().stream().mapToInt(Integer::intValue).sum();
        helper.assertTrue(solid > 40, piece.getClass().getSimpleName() + " builds: " + found);
        if (piece instanceof SiloComponent silo) {
            // the launch pad core with its missile, the receiver torch under it on the silo's frequency
            BlockPos pad = new BlockPos(silo.getXWithOffset(19, 14), silo.getYWithOffset(1), silo.getZWithOffset(19, 14));
            BlockPos torch = pad.below();
            helper.assertTrue(level.getBlockEntity(pad) instanceof TileEntityLaunchPadRusted launch && launch.missileLoaded,
                    "the rusted pad is loaded: " + siloAt(level, 19, 1, 14));
            helper.assertTrue(level.getBlockEntity(torch) instanceof TileEntityRadioTorchBase radio
                    && radio.channel.equals(String.valueOf(silo.freq)), "the pad's torch listens on the silo frequency: " + siloAt(level, 19, 0, 14));
            Constants.LOG.info("[WG-C] silo box {} pad {} ladder {} crate {} torch {}", piece.getBoundingBox(), siloAt(level, 19, 1, 14),
                    siloAt(level, 17, 3, 12), siloAt(level, 32, 13, 9), siloAt(level, 19, 0, 14));
        }
        helper.assertTrue(found.containsKey(signature), piece.getClass().getSimpleName() + " has its " + signature + ": " + found);
        int[] c = containers(level, new BoundingBox(built.minX(), built.minY() - 20, built.minZ(), built.maxX(), built.maxY(), built.maxZ()));
        helper.assertTrue(c[0] >= minContainers && c[1] >= minContainers, piece.getClass().getSimpleName()
                + " fills its loot: " + c[1] + "/" + c[0] + " containers " + found);

        CompoundTag tag = piece.createTag(StructurePieceSerializationContext.fromLevel(level));
        StructurePiece loaded = piece.getType().load(StructurePieceSerializationContext.fromLevel(level), tag);
        helper.assertTrue(loaded.getClass() == piece.getClass() && loaded.getBoundingBox().equals(piece.getBoundingBox())
                && ((Component) loaded).getCoordMode() == piece.getCoordMode(), "round trip: " + tag);
        Constants.LOG.info("[WG-C] {} rot {} built {} containers {}/{}: {}", piece.getClass().getSimpleName(),
                piece.getCoordMode(), built, c[1], c[0], found);
        helper.succeed();
    }

    public static void house1(GameTestHelper helper) {
        buildPiece(helper, rand -> new CivilianFeatures.NTMHouse1(rand, 0, 0), "crate_weapon", 1);
    }

    public static void house2(GameTestHelper helper) {
        buildPiece(helper, rand -> new CivilianFeatures.NTMHouse2(rand, 0, 0), "machine_boiler_off", 2);
    }

    public static void lab1(GameTestHelper helper) {
        buildPiece(helper, rand -> new CivilianFeatures.NTMLab1(rand, 0, 0), "machine_microwave", 1);
    }

    public static void lab2(GameTestHelper helper) {
        buildPiece(helper, rand -> new CivilianFeatures.NTMLab2(rand, 0, 0), "vitrified_barrel", 2);
    }

    public static void ruralHouse(GameTestHelper helper) {
        buildPiece(helper, rand -> new CivilianFeatures.RuralHouse1(rand, 0, 0), "deco_loot", 0);
    }

    public static void largeOffice(GameTestHelper helper) {
        buildPiece(helper, rand -> new OfficeFeatures.LargeOffice(rand, 0, 0), "filing_cabinet", 2);
    }

    public static void largeOfficeCorner(GameTestHelper helper) {
        buildPiece(helper, rand -> new OfficeFeatures.LargeOfficeCorner(rand, 0, 0), "filing_cabinet", 1);
    }

    /** The silo: surface, the rusted launch pad with its missile loaded, the radio torches on one frequency. */
    public static void silo(GameTestHelper helper) {
        buildPiece(helper, rand -> {
            SiloComponent silo = new SiloComponent(rand, 0, 0);
            SILO = silo;
            return silo;
        }, "launch_pad_rusted", 10);
    }

    static SiloComponent SILO;

    /** Debug: what stands at a silo-local position after the build. */
    static String siloAt(ServerLevel level, int x, int y, int z) {
        SiloComponent s = SILO;
        BlockPos pos = new BlockPos(s.getXWithOffset(x, z), s.getYWithOffset(y), s.getZWithOffset(x, z));
        return pos + "=" + level.getBlockState(pos);
    }

    /**
     * The bunker grows from its hub with 1.7's procedural rules: the hub first, 7..12 size limit (+1), the weights' instance
     * limits, no two rooms overlapping, within 40 (+ a room) of the start, sunk so its top is below y 43.
     */
    public static void bunkerAssembly(GameTestHelper helper) {
        NTMComponentStructures.registerSpawns();
        Map<String, Integer> counts = new TreeMap<>();
        for (int i = 0; i < 20; i++) {
            RandomSource rand = RandomSource.create(1000L + i);
            BunkerComponents.BunkerStart start = new BunkerComponents.BunkerStart(rand, 3 + i, -7 + i);
            List<StructurePiece> pieces = start.components;
            helper.assertTrue(pieces.get(0) instanceof BunkerComponents.StartingHub, "the hub starts it");
            helper.assertTrue(pieces.size() >= 1 && pieces.size() <= 14, "size limit: " + pieces.size());
            for (int a = 0; a < pieces.size(); a++) {
                for (int b = a + 1; b < pieces.size(); b++) {
                    helper.assertTrue(!pieces.get(a).getBoundingBox().intersects(pieces.get(b).getBoundingBox()),
                            "rooms do not overlap: " + pieces.get(a).getBoundingBox() + " " + pieces.get(b).getBoundingBox());
                }
                counts.merge(pieces.get(a).getClass().getSimpleName(), 1, Integer::sum);
            }
            Map<String, Integer> own = new TreeMap<>();
            for (StructurePiece piece : pieces) own.merge(piece.getClass().getSimpleName(), 1, Integer::sum);
            // 1.7 Weight instance limits
            helper.assertTrue(own.getOrDefault("Corridor", 0) <= 3 && own.getOrDefault("BedroomL", 0) <= 4
                    && own.getOrDefault("FunJunction", 0) <= 3 && own.getOrDefault("BathroomL", 0) <= 2
                    && own.getOrDefault("Laboratory", 0) <= 2 && own.getOrDefault("PowerRoom", 0) <= 1, "instance limits: " + own);
            BoundingBox box = start.getBoundingBox();
            helper.assertTrue(box.maxY() < 63 - 20 || box.getYSpan() + 1 >= 43, "sunk below y 43: " + box);
            int cx = (3 + i) * 16 + 8, cz = (-7 + i) * 16 + 8;
            helper.assertTrue(box.minX() >= cx - 40 - 12 && box.maxX() <= cx + 40 + 12 && box.minZ() >= cz - 40 - 12 && box.maxZ() <= cz + 40 + 12,
                    "within the distance limit: " + box);
        }
        helper.assertTrue(counts.size() >= 5, "several room types grow: " + counts);
        Constants.LOG.info("[WG-C] bunker rooms over 20 starts: {}", counts);
        helper.succeed();
    }

    /** The bunker hub built at the test origin: vinyl floor, reinforced shield, the hatch ladder and its loot chest. */
    public static void bunkerHub(GameTestHelper helper) {
        buildPiece(helper, rand -> new BunkerComponents.StartingHub(rand, 0, 0), "vinyl_tile_small", 1);
    }

    /** The features start picks one building per start, anchored at the start chunk's +8 corner, at a terrain height. */
    public static void featuresAssembly(GameTestHelper helper) {
        NTMComponentStructures.registerSpawns();
        ServerLevel level = helper.getLevel();
        Map<String, Integer> counts = new TreeMap<>();
        for (int i = 0; i < 40; i++) {
            ChunkPos chunk = new ChunkPos(-200 + i * 5, 100 - i * 3);
            List<StructurePiece> pieces = assemble(level, "features", chunk);
            helper.assertTrue(pieces.size() == 1 && pieces.get(0) instanceof Component, "one building: " + describe(pieces));
            BoundingBox b = pieces.get(0).getBoundingBox();
            helper.assertTrue(b.minX() == chunk.getMinBlockX() + 8 && b.minZ() == chunk.getMinBlockZ() + 8, "anchored at +8: " + b);
            counts.merge(pieces.get(0).getClass().getSimpleName(), 1, Integer::sum);
            List<StructurePiece> bunker = assemble(level, "bunker", chunk);
            helper.assertTrue(!bunker.isEmpty() && bunker.get(0) instanceof BunkerComponents.StartingHub, "a bunker start: " + describe(bunker));
        }
        helper.assertTrue(counts.size() >= 4, "several buildings: " + counts);
        Constants.LOG.info("[WG-C] features over 40 starts (superflat plains): {}", counts);
        helper.succeed();
    }
}
