package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.blocks.ModExplosivesBlocks;
import com.hbm.blocks.ModRadarUtilityBlocks;
import com.hbm.blocks.machine.SoyuzCapsule;
import com.hbm.config.DungeonConfig;
import com.hbm.tileentity.bomb.TileEntityLandmine;
import com.hbm.tileentity.deco.TileEntityLanternBehemoth;
import com.hbm.tileentity.machine.storage.TileEntityCrateBase;
import com.hbm.world.feature.DungeonBlockFeature;
import com.hbm.world.gen.LegacyDungeonBlocks.Blocks;
import com.hbm.world.gen.LegacyDungeonBlocks.ModBlocks;
import com.hbm.world.gen.LegacyDungeonStructure;
import com.hbm.world.gen.LegacyDungeonStructure.Kind;
import com.hbm.world.gen.LegacyDungeonWorld;
import com.hbm.world.gen.component.LegacyBlock;
import java.util.EnumMap;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.HashMap;
import java.util.Map;

/**
 * WG-D: the legacy hardcoded dungeons of 1.7 {@code HbmWorldGen} and its scattered world blocks.
 * <ul>
 *   <li>every 1.7 builder runs on a flat test terrain and writes its characteristic blocks and loot, deterministically;
 *       an unfit site (water where the builder wants ground) writes nothing;</li>
 *   <li>a run copied into the level clipped to a box writes nothing outside it and fills its containers;</li>
 *   <li>each scattered-block feature places its block with 1.7's tile-entity setup.</li>
 * </ul>
 */
public final class LegacyDungeonGameTests {

    private LegacyDungeonGameTests() {}

    /** Ground (stone) below {@code surface}, air above; {@code water} columns hold water instead of the top 3 blocks. */
    static LegacyDungeonWorld.Terrain flat(int surface, BiPredicate<Integer, Integer> water) {
        return new LegacyDungeonWorld.Terrain() {
            @Override
            public LegacyBlock block(int x, int y, int z) {
                if (y >= surface) return Blocks.air;
                if (water.test(x, z) && y >= surface - 3) return Blocks.water;
                return Blocks.stone;
            }

            @Override
            public int height(int x, int z) {
                return surface;
            }

            @Override
            public int minY() {
                return -64;
            }
        };
    }

    private static Map<String, Integer> census(LegacyDungeonWorld.Result result) {
        Map<String, Integer> counts = new HashMap<>();
        for (long key : result.positions()) {
            LegacyDungeonWorld.Placed p = result.at(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key));
            counts.merge(p.block().name + (p.block() == ModBlocks.brick_jungle_trap ? ":" + p.meta() : ""), 1, Integer::sum);
        }
        return counts;
    }

    private static int count(Map<String, Integer> census, LegacyBlock block) {
        int n = 0;
        for (var e : census.entrySet()) if (e.getKey().equals(block.name) || e.getKey().startsWith(block.name + ":")) n += e.getValue();
        return n;
    }

    /** Every builder on a flat site writes its landmark blocks, the same way twice; a wet site gets nothing. */
    public static void builders(GameTestHelper helper) {
        LegacyDungeonWorld.Terrain land = flat(70, (x, z) -> false);
        Map<Kind, LegacyBlock> landmark = new EnumMap<>(Kind.class);
        landmark.put(Kind.ANTENNA, ModBlocks.pole_top);
        landmark.put(Kind.DESERT_ATOM, ModBlocks.crashed_balefire);
        landmark.put(Kind.LIBRARY_DUNGEON, Blocks.mob_spawner);
        landmark.put(Kind.DUD, ModBlocks.crashed_balefire);
        landmark.put(Kind.SPACESHIP, ModBlocks.fusion_heater);
        landmark.put(Kind.BARREL, ModBlocks.sellafield);
        landmark.put(Kind.ARCTIC_VAULT, ModBlocks.tape_recorder);
        landmark.put(Kind.ANCIENT_TOMB, ModBlocks.ancient_scrap);
        landmark.put(Kind.JUNGLE_DUNGEON, ModBlocks.brick_jungle_circle);
        StringBuilder sizes = new StringBuilder();
        for (Kind kind : Kind.values()) {
            // the library wants solid ground under and 8 above; the vault wants rock at its floor
            int y = switch (kind) {
                case LIBRARY_DUNGEON -> 30;
                case ARCTIC_VAULT -> 30;
                case JUNGLE_DUNGEON -> 20;
                default -> 70;
            };
            LegacyDungeonWorld.Result a = LegacyDungeonStructure.simulate(kind, 0, y, 0, 42L, land);
            LegacyDungeonWorld.Result b = LegacyDungeonStructure.simulate(kind, 0, y, 0, 42L, land);
            helper.assertTrue(a.size() > 0, kind.id + " builds on flat ground");
            helper.assertTrue(a.size() == b.size() && a.bounds().equals(b.bounds()), kind.id + " is deterministic: " + a.size() + " vs " + b.size());
            Map<String, Integer> census = census(a);
            helper.assertTrue(count(census, landmark.get(kind)) > 0, kind.id + " has its " + landmark.get(kind) + ": " + census);
            // every written 1.7 block resolves to a real port state (air only where 1.7 wrote air)
            for (long key : a.positions()) {
                LegacyDungeonWorld.Placed p = a.at(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key));
                BlockState state = a.stateAt(key);
                boolean legacyAir = p.block() == Blocks.air;
                helper.assertTrue(legacyAir || !state.isAir(), kind.id + ": " + p.block() + ":" + p.meta() + " resolves to air");
            }
            sizes.append(kind.id).append('=').append(a.size()).append(' ').append(a.bounds()).append("; ");
        }
        // details 1.7 fixed
        Map<String, Integer> tomb = census(LegacyDungeonStructure.simulate(Kind.ANCIENT_TOMB, 0, 70, 0, 7L, land));
        helper.assertTrue(count(tomb, ModBlocks.ancient_scrap) == 343, "the tomb core is 7x7x7 ancient scrap: " + tomb);
        Map<String, Integer> jungle = census(LegacyDungeonStructure.simulate(Kind.JUNGLE_DUNGEON, 0, 20, 0, 7L, land));
        helper.assertTrue(count(jungle, ModBlocks.deco_titanium) == 3 && count(jungle, Blocks.redstone_block) == 1, "jungle marker: " + jungle);
        helper.assertTrue(count(jungle, ModBlocks.brick_jungle_trap) > 0 && count(jungle, ModBlocks.brick_jungle_glyph) > 0,
                "jungle traps and glyphs: " + jungle);

        // a site the builder rejects: water on top (the antenna's test column is x+1, z+1)
        LegacyDungeonWorld.Terrain wet = flat(70, (x, z) -> true);
        helper.assertTrue(LegacyDungeonStructure.simulate(Kind.ANTENNA, 0, 70, 0, 42L, wet).size() == 0, "no antenna on water");
        helper.assertTrue(LegacyDungeonStructure.simulate(Kind.DESERT_ATOM, 0, 70, 0, 42L, wet).size() == 0, "no power plant on water");
        Constants.LOG.info("[WG-D] legacy dungeon runs: {}", sizes);
        helper.succeed();
    }

    /** The antenna copied into the level: complete inside the box, nothing outside a clip, its chest filled. */
    public static void apply(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        int surface = origin.getY();
        LegacyDungeonWorld.Terrain land = flat(surface, (x, z) -> false);
        LegacyDungeonWorld.Result result = LegacyDungeonStructure.simulate(Kind.ANTENNA, origin.getX(), surface, origin.getZ(), 99L, land);
        helper.assertTrue(result.size() > 0, "antenna runs");
        BoundingBox full = result.bounds();
        result.apply(level, full);
        int matched = 0, total = 0;
        for (long key : result.positions()) {
            BlockState expected = result.stateAt(key);
            if (expected.isAir()) continue;
            total++;
            if (level.getBlockState(BlockPos.of(key)).is(expected.getBlock())) matched++;
        }
        helper.assertTrue(total > 20 && matched == total, "antenna placed " + matched + "/" + total);
        // 1.7: the chest at x+2, y, z+2 is filled from POOL_ANTENNA
        BlockEntity chest = level.getBlockEntity(origin.offset(2, 0, 2));
        helper.assertTrue(chest instanceof Container c && !c.isEmpty(), "antenna chest filled: " + chest);

        // a second copy clipped to its first two x columns
        BlockPos second = origin.offset(6, 0, 0);
        LegacyDungeonWorld.Result other = LegacyDungeonStructure.simulate(Kind.ANTENNA, second.getX(), surface, second.getZ(), 99L, land);
        BoundingBox box = other.bounds();
        BoundingBox half = new BoundingBox(box.minX(), box.minY(), box.minZ(), box.minX() + 1, box.maxY(), box.maxZ());
        other.apply(level, half);
        int inside = 0, outside = 0;
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (level.getBlockState(pos).isAir()) continue;
            if (pos.getX() <= half.maxX()) inside++; else outside++;
        }
        helper.assertTrue(inside > 0 && outside == 0, "clipped copy: " + inside + " inside, " + outside + " outside");
        helper.succeed();
    }

    private static boolean place(ServerLevel level, Feature<NoneFeatureConfiguration> feature, BlockPos pos, long seed) {
        return feature.place(NoneFeatureConfiguration.INSTANCE, level, level.getChunkSource().getGenerator(), RandomSource.create(seed), pos);
    }

    /** Each scattered block with its 1.7 setup: locked filled safe, armed mines, broken lantern, rusted capsule with a record. */
    public static void features(GameTestHelper helper) {
        // the test server's world has Generate Structures off, which 1.7's enableDungeons flag (2) follows
        int flag = DungeonConfig.enableDungeons;
        DungeonConfig.enableDungeons = 1;
        try {
            featuresEnabled(helper);
        } finally {
            DungeonConfig.enableDungeons = flag;
        }
    }

    private static void featuresEnabled(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockState stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        for (int x = 0; x < 12; x++) for (int z = 0; z < 5; z++) level.setBlock(helper.absolutePos(new BlockPos(x, 1, z)), stone, 3);

        BlockPos safePos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(place(level, DungeonBlockFeature.LOCKED_SAFE.get(), safePos, 3L), "safe placed");
        helper.assertTrue(level.getBlockState(safePos).is(ModDecoCrateBlocks.safe.get()), "a safe stands there");
        helper.assertTrue(level.getBlockEntity(safePos) instanceof TileEntityCrateBase safe && safe.isLocked()
                && safe.getPins() >= 1 && safe.getPins() <= 999 && !safe.isEmpty(), "the safe is locked with pins and loot");

        BlockPos minePos = helper.absolutePos(new BlockPos(3, 2, 1));
        helper.assertTrue(place(level, DungeonBlockFeature.LANDMINE_AP.get(), minePos, 4L), "AP mine placed");
        helper.assertTrue(level.getBlockState(minePos).is(ModExplosivesBlocks.mine_ap.get())
                && level.getBlockEntity(minePos) instanceof TileEntityLandmine mine && mine.waitingForPlayer, "AP mine armed at the surface");

        BlockPos lanternPos = helper.absolutePos(new BlockPos(5, 2, 3));
        helper.assertTrue(place(level, DungeonBlockFeature.BROKEN_LANTERN.get(), lanternPos, 1L), "lantern placed");
        helper.assertTrue(level.getBlockEntity(lanternPos) instanceof TileEntityLanternBehemoth lantern && lantern.isBroken, "the lantern is broken");
        helper.assertTrue(level.getBlockState(lanternPos.above(4)).is(ModDungeonBlocks.lantern_behemoth.get()), "the lantern is five tall");

        BlockPos capPos = helper.absolutePos(new BlockPos(8, 5, 1)); // surface 5: the capsule goes to y 1, under a solid block at y 2
        level.setBlock(capPos.below(3), stone, 3);
        helper.assertTrue(place(level, DungeonBlockFeature.LANDED_CAPSULE.get(), capPos, 5L), "capsule placed");
        BlockPos capsule = capPos.below(4);
        helper.assertTrue(level.getBlockState(capsule).is(ModRadarUtilityBlocks.soyuz_capsule.get())
                && level.getBlockState(capsule).getValue(SoyuzCapsule.RUSTED), "a rusted capsule four blocks down");
        helper.assertTrue(level.getBlockEntity(capsule) instanceof Container cap && !cap.isEmpty()
                && containsRecord(cap), "the capsule holds the glass record");

        BlockPos pcPos = helper.absolutePos(new BlockPos(10, 2, 1));
        helper.assertTrue(place(level, DungeonBlockFeature.CORRUPTED_BROADCASTER.get(), pcPos, 6L), "broadcaster placed");
        helper.assertTrue(level.getBlockState(pcPos).is(ModDecoCrateBlocks.broadcaster_pc.get()), "the corrupted broadcaster stands there");
        helper.succeed();
    }

    private static boolean containsRecord(Container cap) {
        for (int i = 0; i < cap.getContainerSize(); i++) {
            if (String.valueOf(BuiltInRegistries.ITEM.getKey(cap.getItem(i).getItem())).equals("hbm:record_glass")) return true;
        }
        return false;
    }
}
