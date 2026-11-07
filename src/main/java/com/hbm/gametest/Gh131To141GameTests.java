package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModConveyorBlocks;
import com.hbm.blocks.network.BlockCraneBase;
import com.hbm.config.DungeonConfig;
import com.hbm.config.GeneralConfig;
import com.hbm.config.StructureConfig;
import com.hbm.inventory.recipes.loader.RecipeConditions;
import com.hbm.items.ModFoodItems;
import com.hbm.items.ModToolItems;
import com.hbm.world.feature.DungeonBlockFeature;
import com.hbm.world.feature.FallenMeteoriteFeature;
import com.hbm.world.feature.GlyphidHiveFeature;
import com.hbm.world.gen.ConfigSpreadPlacement;
import com.hbm.world.gen.LegacyDungeonBlocks;
import com.hbm.world.gen.LegacyDungeonDeclarations;
import com.hbm.world.gen.LegacyDungeonWorld;
import com.hbm.world.gen.NTMStructureDeclarations;
import com.hbm.world.gen.ScatteredStructure;
import com.hbm.world.gen.component.LegacyBlock;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.RegistryLookups;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** GitHub issues 130, 131, 134 and 141: config gates, structure forms, the tool path through a crane, the smoker. */
public final class Gh131To141GameTests {

    private Gh131To141GameTests() {}

    private static final int HEIGHT = 64;
    private static final int SEEDS = 10_000;

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function CRANE =
            TESTS.function("gh134_screwdriver_sets_crane_input", 20, () -> Gh131To141GameTests::crane);
    public static final GameTests.Function SMOKER =
            TESTS.function("gh141_smoker_grills_glyphid_meat", 160, () -> Gh131To141GameTests::smoker);
    public static final GameTests.Function GRID =
            TESTS.function("gh131_blueprint_chest_grid", 20, () -> Gh131To141GameTests::grid);
    public static final GameTests.Function RATES =
            TESTS.function("gh131_scattered_rates_and_toggles", 20, () -> Gh131To141GameTests::rates);
    public static final GameTests.Function WATER =
            TESTS.function("gh131_water_columns_per_feature", 20, () -> Gh131To141GameTests::water);
    public static final GameTests.Function CHEST =
            TESTS.function("gh131_blueprint_chest_config", 20, () -> Gh131To141GameTests::chest);
    public static final GameTests.Function FORMS =
            TESTS.function("gh131_structures_registered", 20, () -> Gh131To141GameTests::forms);
    public static final GameTests.Function CONSOLE =
            TESTS.function("gh130_console_gate_is_528_mode_only", 20, () -> Gh131To141GameTests::console);

    public static void init() {}

    public static void crane(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(pos, ModConveyorBlocks.CRANE_INSERTER.get().defaultBlockState()
                .setValue(BlockCraneBase.INPUT, Direction.NORTH).setValue(BlockCraneBase.OUTPUT, Direction.SOUTH));
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        ItemStack screwdriver = new ItemStack(ModToolItems.screwdriver.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, screwdriver);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        player.gameMode.useItemOn(player, level, screwdriver, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "the screwdriver opened the crane's GUI");
        helper.assertTrue(BlockCraneBase.inputSide(level.getBlockState(pos)) == Direction.UP,
                "the clicked face is the input: " + level.getBlockState(pos));
        helper.succeed();
    }

    public static void smoker(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, Blocks.SMOKER);
        Container smoker = (Container) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        smoker.setItem(0, new ItemStack(ModFoodItems.glyphid_meat.get()));
        smoker.setItem(1, new ItemStack(Items.COAL));
        // vanilla's smoker cooks in 100 ticks
        helper.runAfterDelay(110, () -> {
            ItemStack out = smoker.getItem(2);
            helper.assertTrue(out.is(ModFoodItems.glyphid_meat_grilled.get()), "smoker result: " + out);
            helper.succeed();
        });
    }

    private static ConfigSpreadPlacement placement(String key) {
        return new ConfigSpreadPlacement(Vec3i.ZERO, StructurePlacement.FrequencyReductionMethod.DEFAULT, 1F, 1_700_016, Optional.empty(), key);
    }

    /** 1.7 {@code genBlueprintChest}: chunks with {@code abs(i) % 5000 < 16} on both axes. */
    public static void grid(GameTestHelper helper) {
        ConfigSpreadPlacement grid = placement("grid:" + LegacyDungeonDeclarations.BLUEPRINT_CHEST);
        long seed = 4815162342L;
        int grid17 = DungeonConfig.blueprintGrid;
        helper.assertTrue(grid17 == 5000 && grid.spacing() == 312, "5000 blocks are 312 chunks and a bit: " + grid.spacing());
        int sites = 0;
        for (int x = -700; x <= 700; x++) {
            boolean lineX = Math.abs(x * 16) % grid17 < 16;
            for (int z = -700; z <= 700; z++) {
                boolean rule = lineX && Math.abs(z * 16) % grid17 < 16;
                ChunkPos candidate = grid.getPotentialStructureChunk(seed, x, z);
                helper.assertTrue(rule == candidate.equals(new ChunkPos(x, z)), "grid chunk " + x + " " + z + " against 1.7's test");
                helper.assertTrue(rule == grid.isCandidate(seed, x, z), "the placement keeps every grid chunk, the roll is the feature's");
                if (rule) sites++;
            }
        }
        // chunk lines 0, 313 and 625 either way: 5 x 5 sites
        helper.assertTrue(sites == 25, "25 sites in reach, got " + sites);
        try {
            DungeonConfig.blueprintGrid = 0;
            helper.assertTrue(!grid.isCandidate(seed, 313, 313) && !grid.isCandidate(seed, 313, 0), "4.26 = 0 turns the chests off");
        } finally {
            DungeonConfig.blueprintGrid = grid17;
        }
        helper.succeed();
    }

    private static <C> C config(Codec<C> codec) {
        return codec.parse(JsonOps.INSTANCE, new JsonObject()).result().orElseThrow();
    }

    /** {@code depth} blocks of water with their top at {@code HEIGHT - 1}, stone below, as the noise terrain answers. */
    private static LegacyDungeonWorld.Terrain water(int depth) {
        return new LegacyDungeonWorld.Terrain() {
            @Override
            public LegacyBlock block(int x, int y, int z) {
                return y >= HEIGHT ? LegacyDungeonBlocks.Blocks.air : y >= HEIGHT - depth ? LegacyDungeonBlocks.Blocks.water : LegacyDungeonBlocks.Blocks.stone;
            }

            @Override
            public int height(int x, int z) {
                return HEIGHT;
            }

            @Override
            public int minY() {
                return -64;
            }
        };
    }

    private static int deepest(FeatureConfiguration config) {
        int depth = -1;
        while (depth < 40 && ScatteredStructure.buildable(config, water(depth + 1), 0, HEIGHT, 0, 0L)) depth++;
        return depth;
    }

    /** Per seed the share of meteorites that stand in this water; by hand from 1.7, {@code (11 - depth) / 10}. */
    private static double meteorites(int depth) {
        FeatureConfiguration config = config(FallenMeteoriteFeature.Config.CODEC);
        LegacyDungeonWorld.Terrain terrain = water(depth);
        int built = 0;
        for (long seed = 0; seed < SEEDS; seed++) if (ScatteredStructure.buildable(config, terrain, 0, HEIGHT, 0, seed)) built++;
        return built / (double) SEEDS;
    }

    /** The water each feature's 1.7 branch stands in: none for a torch top on y - 1, more for the rest. */
    public static void water(GameTestHelper helper) {
        helper.assertTrue(deepest(config(DungeonBlockFeature.LockedSafe.Config.CODEC)) == 0, "the safe tests y - 1");
        helper.assertTrue(deepest(config(DungeonBlockFeature.BrokenLantern.Config.CODEC)) == 0, "the lantern tests y - 1");
        helper.assertTrue(deepest(config(DungeonBlockFeature.Broadcaster.Config.CODEC)) == 0, "the broadcaster tests y - 1");
        helper.assertTrue(deepest(config(DungeonBlockFeature.LandedCapsule.Config.CODEC)) == 2, "the capsule tests height - 3");
        helper.assertTrue(deepest(config(GlyphidHiveFeature.Config.CODEC)) == 1, "the hive's scan reaches y - 2");
        helper.assertTrue(meteorites(1) == 1D, "any sink clears one block of water");
        for (int depth : new int[] {3, 5, 10}) {
            double share = meteorites(depth), hand = (11 - depth) / 10D;
            helper.assertTrue(Math.abs(share - hand) < 0.03D, "meteorites in " + depth + " of water: " + share + ", by hand " + hand);
        }
        helper.assertTrue(meteorites(11) == 0D, "the sink reaches 9 blocks down and 2 below that");
        helper.succeed();
    }

    /** The chest's codec fields as the structure reads them; {@code min_distance} is 1.7's 100 on both axes. */
    public static void chest(GameTestHelper helper) {
        var c = config(DungeonBlockFeature.BlueprintChest.Config.CODEC);
        var always = new DungeonBlockFeature.BlueprintChest.Config(c.minDistance(), c.roll(), 0, c.pool(), c.rolls(), c.container(), c.shell());
        var never = new DungeonBlockFeature.BlueprintChest.Config(c.minDistance(), c.roll(), c.roll(), c.pool(), c.rolls(), c.container(), c.shell());
        var near = new DungeonBlockFeature.BlueprintChest.Config(0, c.roll(), 0, c.pool(), c.rolls(), c.container(), c.shell());
        RandomSource rand = RandomSource.create(1L);
        helper.assertTrue(!DungeonBlockFeature.BlueprintChest.passes(always, 64, 64, rand), "1.7 refuses chunk 4 4 on a 64 block grid");
        helper.assertTrue(!DungeonBlockFeature.BlueprintChest.passes(always, 0, 0, rand), "1.7 refuses the origin");
        helper.assertTrue(DungeonBlockFeature.BlueprintChest.passes(always, 112, 0, rand), "one axis past 100 is enough");
        helper.assertTrue(DungeonBlockFeature.BlueprintChest.passes(near, 64, 64, rand), "min_distance is read");
        helper.assertTrue(!DungeonBlockFeature.BlueprintChest.passes(never, 5008, 5008, rand), "skip is read");
        int kept = 0;
        for (int n = 0; n < SEEDS; n++) if (DungeonBlockFeature.BlueprintChest.passes(c, 5008, 5008, rand)) kept++;
        helper.assertTrue(Math.abs(kept / (double) SEEDS - 0.5D) < 0.03D, "1.7 nextInt(20) < 10 skips half, kept " + kept);
        helper.succeed();
    }

    private static int sites(ConfigSpreadPlacement placement) {
        int sites = 0;
        for (int x = 0; x < 800; x++) for (int z = 0; z < 800; z++) if (placement.isCandidate(4815162342L, x, z)) sites++;
        return sites;
    }

    /** 1 in n per chunk over 640,000 chunks, within sampling error, and n = 0 or the switch off places none. */
    public static void rates(GameTestHelper helper) {
        ConfigSpreadPlacement safe = placement("dungeon:" + LegacyDungeonDeclarations.LOCKED_SAFE);
        ConfigSpreadPlacement lantern = placement("dungeon:" + LegacyDungeonDeclarations.LANTERN);
        int safes = sites(safe), lanterns = sites(lantern);
        helper.assertTrue(safes > 216 && safes < 296, "640000 / 2500 = 256 safes, got " + safes);
        helper.assertTrue(lanterns > 270 && lanterns < 370, "640000 / 2000 = 320 lanterns, got " + lanterns);
        boolean vaults = DungeonConfig.enableVaults;
        int lanternChance = DungeonConfig.lanternStructure, meteor = StructureConfig.meteorDungeonSpawnWeight;
        try {
            DungeonConfig.enableVaults = false;
            helper.assertTrue(sites(safe) == 0, "1.15 off: no safes");
            DungeonConfig.lanternStructure = 0;
            helper.assertTrue(sites(lantern) == 0, "4.25 = 0: no lanterns");
            helper.assertTrue(NTMStructureDeclarations.runtimeWeight("meteor_dungeon") == 1, "5.50 defaults to 1.7's weight 1");
            StructureConfig.meteorDungeonSpawnWeight = 0;
            helper.assertTrue(NTMStructureDeclarations.runtimeWeight("meteor_dungeon") == 0, "5.50 = 0: no meteor dungeon");
        } finally {
            DungeonConfig.enableVaults = vaults;
            DungeonConfig.lanternStructure = lanternChance;
            StructureConfig.meteorDungeonSpawnWeight = meteor;
        }
        helper.assertTrue(NTMStructureDeclarations.runtimeWeight(NTMStructureDeclarations.SILO) == 0, "the silo adds no weight to the region pick");
        helper.succeed();
    }

    public static void forms(GameTestHelper helper) {
        for (String id : List.of(NTMStructureDeclarations.SILO, "meteor_dungeon", LegacyDungeonDeclarations.LOCKED_SAFE, LegacyDungeonDeclarations.LANTERN,
                LegacyDungeonDeclarations.BROADCASTER, LegacyDungeonDeclarations.CAPSULE, LegacyDungeonDeclarations.HIVE, LegacyDungeonDeclarations.METEORITE,
                LegacyDungeonDeclarations.BLUEPRINT_CHEST)) {
            RegistryLookups.holder(helper.getLevel().registryAccess(), Registries.STRUCTURE,
                    ResourceKey.create(Registries.STRUCTURE, Ids.of(Constants.MOD_ID, id)));
        }
        helper.succeed();
    }

    /** 1.7 {@code if(!GeneralConfig.enable528)}: {@code X528_forceReasimBoilers}, true by default in the file, is not in it. */
    public static void console(GameTestHelper helper) {
        boolean mode = GeneralConfig.enable528, force = GeneralConfig.enable528ReasimBoilers;
        try {
            GeneralConfig.enable528 = false;
            GeneralConfig.enable528ReasimBoilers = true;
            helper.assertTrue(RecipeConditions.holds(new String[] {"!enable_528"}, "rbmk_console"), "the force key alone gates nothing");
            GeneralConfig.enable528 = true;
            helper.assertTrue(!RecipeConditions.holds(new String[] {"!enable_528"}, "rbmk_console"), "528 mode gates the consoles");
        } finally {
            GeneralConfig.enable528 = mode;
            GeneralConfig.enable528ReasimBoilers = force;
        }
        helper.succeed();
    }
}
