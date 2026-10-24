package com.hbm.gametest;

import com.google.gson.JsonParser;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.ModExplosivesBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.ModOilBlocks;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.config.DungeonConfig;
import com.hbm.config.GeneralConfig;
import com.hbm.config.WorldConfig;
import com.hbm.itempool.ItemPools;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.items.ModFuelItems;
import com.hbm.world.feature.AustraliumFeature;
import com.hbm.world.feature.BedrockDepositFeature;
import com.hbm.world.feature.ColtanDepositFeature;
import com.hbm.world.feature.CrustColumnFeature;
import com.hbm.world.feature.DungeonBlockFeature;
import com.hbm.world.feature.FallenMeteoriteFeature;
import com.hbm.world.feature.FeatureCodecs;
import com.hbm.world.feature.Geyser;
import com.hbm.world.feature.GlyphidHiveFeature;
import com.hbm.world.feature.ImpactAftermathFeature;
import com.hbm.world.feature.LegacyFlowerFeature;
import com.hbm.world.feature.OreCaveFeature;
import com.hbm.world.feature.OreLayer3DFeature;
import com.hbm.world.feature.OreVeinPlacer;
import com.hbm.world.feature.PinkTreeFeature;
import com.hbm.world.feature.SchistStratumFeature;
import com.hbm.world.gen.ModFeatures;
import com.hbm.world.gen.terrain.MapGenBedrockOil;
import com.hbm.world.gen.terrain.MapGenBubble;
import com.hbm.world.gen.terrain.MapGenCrater;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import java.util.List;
import java.util.function.Predicate;

/**
 * The feature configurations: {@code "config": {}} decodes to the constants the features had before they were
 * configurable, the default host rules match exactly the blocks the old predicates did, and a datapack-style override
 * changes what is generated.
 */
public final class WorldgenConfigGameTests {

    private WorldgenConfigGameTests() {}

    private static <C> C d(Codec<C> codec) {
        return FeatureCodecs.defaults(codec);
    }

    private static boolean is(BlockState state, Block block) {
        return state.getBlock() == block && state == block.defaultBlockState();
    }

    /** {@code rule} accepts every block state of every registered block exactly when {@code old} does. */
    private static String sameAs(RuleTest rule, Predicate<BlockState> old) {
        RandomSource rand = RandomSource.create(0L);
        for (Block block : BuiltInRegistries.BLOCK) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                if (rule.test(state, rand) != old.test(state)) return String.valueOf(state);
            }
        }
        return null;
    }

    private static void rule(GameTestHelper helper, String what, RuleTest rule, Predicate<BlockState> old) {
        String diff = sameAs(rule, old);
        helper.assertTrue(diff == null, what + " host differs from the old predicate at " + diff);
    }

    public static void defaults(GameTestHelper helper) {
        Predicate<BlockState> stone = OreVeinPlacer.STONE;

        var schist = d(SchistStratumFeature.Config.CODEC);
        helper.assertTrue(is(schist.state(), ModBlocks.stone_gneiss.get()) && schist.scale() == 0.01D && schist.threshold() == 5D
                && schist.rangeMult() == 3 && schist.maxRange() == 4 && schist.yLevel() == 30 && schist.octaves() == 4
                && schist.seedOffset() == 0L, "schist_stratum " + schist);
        rule(helper, "schist_stratum", schist.host(), stone);

        var caves = d(OreCaveFeature.Config.CODEC).caves();
        helper.assertTrue(caves.size() == 2, "ore_caves: sulfur and asbestos");
        var sulfur = caves.get(0);
        var asbestos = caves.get(1);
        helper.assertTrue(is(sulfur.ore(), ModOreBlocks.stone_resource_sulfur.get())
                && is(sulfur.stalactite().orElseThrow(), ModOreBlocks.stalactite_sulfur.get())
                && is(sulfur.stalagmite().orElseThrow(), ModOreBlocks.stalagmite_sulfur.get())
                && is(sulfur.fluid().orElseThrow(), ModFluidNetBlocks.sulfuric_acid_block.get())
                && sulfur.threshold() == 1.5D && sulfur.rangeMult() == 20 && sulfur.maxRange() == 20 && sulfur.yLevel() == 30
                && sulfur.scale() == 0.01D && sulfur.octaves() == 2 && sulfur.seedOffset() == 0x5FL * 31 + 30
                && sulfur.enabled().get() == (WorldConfig.enableSulfurCave ? 1 : 0), "sulfur cave " + sulfur);
        helper.assertTrue(is(asbestos.ore(), ModOreBlocks.stone_resource_asbestos.get()) && asbestos.fluid().isEmpty()
                && asbestos.threshold() == 1.75D && asbestos.yLevel() == 25 && asbestos.seedOffset() == 0xA5BL * 31 + 25
                && asbestos.enabled().get() == (WorldConfig.enableAsbestosCave ? 1 : 0), "asbestos cave " + asbestos);
        rule(helper, "sulfur cave", sulfur.host(), stone);

        var layers = d(OreLayer3DFeature.Config.CODEC);
        List<OreLayer3DFeature.Layer> l = layers.layers();
        helper.assertTrue(layers.minY() == 6 && layers.maxY() == 64 && l.size() == 3
                && is(l.get(0).state(), ModOreBlocks.stone_resource_hematite.get()) && l.get(0).scaleH() == 0.04D
                && l.get(0).scaleV() == 0.25D && l.get(0).threshold() == 230D && l.get(0).seedOffset() == 0
                && is(l.get(1).state(), ModOreBlocks.stone_resource_bauxite.get()) && l.get(1).scaleH() == 0.03D
                && l.get(1).scaleV() == 0.15D && l.get(1).threshold() == 300D && l.get(1).seedOffset() == 1
                && is(l.get(2).state(), ModOreBlocks.stone_resource_malachite.get()) && l.get(2).scaleH() == 0.10D
                && l.get(2).scaleV() == 0.15D && l.get(2).threshold() == 275D && l.get(2).seedOffset() == 2
                && l.get(0).enabled().get() == (WorldConfig.enableHematite ? 1 : 0)
                && l.get(1).enabled().get() == (WorldConfig.enableBauxite ? 1 : 0)
                && l.get(2).enabled().get() == (WorldConfig.enableMalachite ? 1 : 0), "ore_layers " + layers);
        rule(helper, "ore_layers", layers.host(), stone);

        var coltan = d(ColtanDepositFeature.Config.CODEC);
        helper.assertTrue(is(coltan.ore(), ModBlocks.ore_coltan.get()) && coltan.size() == 4 && coltan.attempts() == 2
                && coltan.rings() == 5 && coltan.baseRange() == 750 && coltan.spread() == 1500D && coltan.seedOffset() == 5L
                && coltan.minY() == 15 && coltan.yRange() == 25
                && coltan.enabled().get() == (GeneralConfig.enable528ColtanDeposit ? 1 : 0), "coltan_deposit " + coltan);
        rule(helper, "coltan_deposit", coltan.host(), stone);

        var aus = d(AustraliumFeature.Config.CODEC);
        helper.assertTrue(is(aus.ore(), ModBlocks.ore_australium.get()) && aus.size() == 50 && aus.attempts() == 4
                && aus.minY() == 15 && aus.yRange() == 15 && aus.minX() == -450 && aus.maxX() == -350 && aus.minZ() == -450
                && aus.maxZ() == -350, "australium " + aus);
        rule(helper, "australium", aus.host(), stone);

        var crust = CrustColumnFeature.Config.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"state\": {\"Name\": \"hbm:geysir_nether\"}}")).result().orElseThrow();
        helper.assertTrue(crust.depth() == 5, "crust_column depth " + crust.depth());
        rule(helper, "crust_column", crust.host(), s -> s.is(Blocks.NETHERRACK));

        var flowers = LegacyFlowerFeature.Config.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"state\": {\"Name\": \"hbm:plant_reeds\"}}")).result().orElseThrow();
        helper.assertTrue(flowers.tries() == 64 && flowers.xzSpread() == 8 && flowers.ySpread() == 4, "legacy_flowers " + flowers);

        var geyser = d(Geyser.Config.CODEC);
        helper.assertTrue(is(geyser.wall(), Blocks.STONE) && geyser.fluid() == Blocks.WATER.defaultBlockState()
                && is(geyser.ore(), ModTerrainBlocks.block_yellowcake.get()) && geyser.cap() == Blocks.GRASS_BLOCK.defaultBlockState()
                && is(geyser.capAlt(), Blocks.GRAVEL) && is(geyser.vent(), ModTerrainBlocks.geysir_chlorine.get()), "geyser " + geyser);
        rule(helper, "geyser surface", geyser.surface(), s -> s.is(Blocks.GRASS_BLOCK));

        var meteorite = d(FallenMeteoriteFeature.Config.CODEC);
        helper.assertTrue(meteorite.maxSink() == 10 && !meteorite.safe() && !meteorite.specials() && !meteorite.damage(),
                "fallen_meteorite " + meteorite);

        var impact = d(ImpactAftermathFeature.Config.CODEC);
        helper.assertTrue(impact.recoveryChance() == 9 && impact.wipeDust() == 0.25F, "impact_aftermath " + impact);

        var pink = d(PinkTreeFeature.Config.CODEC);
        helper.assertTrue(is(pink.state(), ModDecoCrateBlocks.pink_log.get()), "pink_tree " + pink);
        rule(helper, "pink_tree target", pink.target(), s -> s == Blocks.OAK_LOG.defaultBlockState());

        var hive = d(GlyphidHiveFeature.Config.CODEC);
        helper.assertTrue(hive.infestedChance() == 10 && hive.spawnerChance() == 3 && hive.scanUp() == 3 && hive.scanDown() == 1
                && hive.lootPool().equals(ItemPools.POOL_PILE_HIVE) && hive.bonesPool().equals(ItemPools.POOL_PILE_BONES),
                "glyphid_hive " + hive);

        var oil = d(MapGenBubble.OIL_CODEC);
        helper.assertTrue(oil.frequency().get() == WorldConfig.oilSpawn && oil.minSize() == 8 && oil.maxSize() == 16
                && oil.minY() == 15 && oil.yRange() == 25 && !oil.fuzzy() && is(oil.ore(), ModOilBlocks.ore_oil.get())
                && oil.minTemperature().isEmpty() && !oil.noLightning() && oil.spotCount() == 150 && oil.spotWidth() == 7,
                "oil_bubble " + oil);
        rule(helper, "oil_bubble", oil.host(), s -> s.is(Blocks.STONE));
        var sand = d(MapGenBubble.OIL_SAND_CODEC);
        helper.assertTrue(sand.frequency().get() == 200 && sand.minSize() == 16 && sand.maxSize() == 48 && sand.minY() == 56
                && sand.yRange() == 16 && sand.fuzzy() && is(sand.ore(), ModFallingBlocks.ore_oil_sand.get())
                && sand.minTemperature().orElseThrow() == 1.5F && sand.noLightning(), "oil_sand_bubble " + sand);
        rule(helper, "oil_sand_bubble", sand.host(), s -> s.is(Blocks.SAND) || s.is(Blocks.RED_SAND));

        var bedrockOil = d(MapGenBedrockOil.Config.CODEC);
        helper.assertTrue(bedrockOil.frequency().get() == WorldConfig.bedrockOilSpawn && is(bedrockOil.ore(), ModBlocks.ore_bedrock_oil.get())
                && bedrockOil.spotCount() == 50 && bedrockOil.spotWidth() == 5, "bedrock_oil " + bedrockOil);
        rule(helper, "bedrock_oil", bedrockOil.host(), s -> s.is(BlockTags.BASE_STONE_OVERWORLD));

        var crater = d(MapGenCrater.Config.CODEC);
        helper.assertTrue(crater.frequency().get() == WorldConfig.radfreq && crater.biomes().equals(List.of(Biomes.DESERT))
                && crater.minSize() == 8 && crater.maxSize() == 64 && crater.depthFactor() == 0.35D
                && is(crater.regolith(), ModFalloutBlocks.sellafield_slaked.get()) && is(crater.rock(), ModFalloutBlocks.sellafield_slaked.get()),
                "sellafield_crater " + crater);

        var bedrock = d(BedrockDepositFeature.Overworld.Config.CODEC);
        helper.assertTrue(bedrock.depthRock() == ModOreBlocks.stone_depth.get() && bedrock.color() == 0xD78A16, "bedrock_ore " + bedrock);
        rule(helper, "bedrock_ore", bedrock.host(), s -> s.is(BlockTags.BASE_STONE_OVERWORLD));
        var nether = d(BedrockDepositFeature.Nether.Config.CODEC);
        var e = nether.entries();
        helper.assertTrue(nether.depthRock() == ModOreBlocks.stone_depth_nether.get() && e.size() == 3
                && e.get(0).item() == Items.GLOWSTONE_DUST && e.get(0).count() == 4 && e.get(0).color() == 0xF9FF4D
                && e.get(0).weight().get() == WorldConfig.bedrockGlowstoneSpawn
                && e.get(1).item() == ModFuelItems.powder_fire.get() && e.get(1).color() == 0xD7341F
                && e.get(1).weight().get() == WorldConfig.bedrockPhosphorusSpawn
                && e.get(2).item() == Items.QUARTZ && e.get(2).color() == 0xF0EFDD
                && e.get(2).weight().get() == WorldConfig.bedrockQuartzSpawn, "bedrock_ore_nether " + nether);

        var safe = d(DungeonBlockFeature.LockedSafe.Config.CODEC);
        helper.assertTrue(safe.tiers().equals(List.of(
                new DungeonBlockFeature.LockedSafe.Tier(4, 1.0D, ItemPools.POOL_VAULT_RUSTY, 3, 6),
                new DungeonBlockFeature.LockedSafe.Tier(3, 0.1D, ItemPools.POOL_VAULT_STANDARD, 2, 4),
                new DungeonBlockFeature.LockedSafe.Tier(2, 0.02D, ItemPools.POOL_VAULT_REINFORCED, 1, 3),
                new DungeonBlockFeature.LockedSafe.Tier(1, 0.0D, ItemPools.POOL_VAULT_UNBREAKABLE, 1, 2)))
                && safe.spiderChance() == 3 && safe.spiderBound() == 10, "locked_safe " + safe);
        var ap = d(DungeonBlockFeature.Landmine.AP_CODEC);
        var he = d(DungeonBlockFeature.Landmine.HE_CODEC);
        helper.assertTrue(is(ap.mine(), ModExplosivesBlocks.mine_ap.get()) && ap.scanUp() == 2
                && is(he.mine(), ModExplosivesBlocks.mine_he.get()) && he.scanUp() == 0, "landmines " + ap + " " + he);
        helper.assertTrue(d(DungeonBlockFeature.BrokenLantern.Config.CODEC).lootChance() == 2, "broken_lantern");
        var capsule = d(DungeonBlockFeature.LandedCapsule.Config.CODEC);
        helper.assertTrue(capsule.depth() == 4 && capsule.item() == ModFinalConsumableItems.record_glass.get() && capsule.rusted(),
                "landed_capsule " + capsule);
        helper.assertTrue(is(d(DungeonBlockFeature.Broadcaster.Config.CODEC).state(), ModDecoCrateBlocks.broadcaster_pc.get()),
                "corrupted_broadcaster");
        var chest = d(DungeonBlockFeature.BlueprintChest.Config.CODEC);
        helper.assertTrue(chest.grid() == DungeonConfig.blueprintGrid && chest.minDistance() == 100 && chest.roll() == 20
                && chest.skip() == 10 && chest.pool().equals(ItemPools.POOL_BLUEPRINTS) && chest.rolls() == 50
                && chest.container() == Blocks.CHEST.defaultBlockState() && chest.shell() == Blocks.OBSIDIAN.defaultBlockState(),
                "blueprint_chest " + chest);
        helper.succeed();
    }

    /** A schist band written like a datapack file: diamond, one layer at the scratch height, every column. */
    public static void override(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 60, 0));
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(15, 0, 15))) level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);

        String json = "{\"state\": {\"Name\": \"minecraft:diamond_block\"}, \"y_level\": " + origin.getY()
                + ", \"threshold\": -1000, \"range_mult\": 0, \"max_range\": 1000}";
        var config = SchistStratumFeature.Config.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result().orElseThrow();
        ModFeatures.SCHIST_STRATUM.get().place(config, level, level.getChunkSource().getGenerator(), RandomSource.create(1L), origin);

        int diamonds = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin.below(), origin.offset(15, 1, 15))) {
            if (level.getBlockState(pos).is(Blocks.DIAMOND_BLOCK)) diamonds++;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        helper.assertTrue(diamonds == 256, "the overridden band replaced " + diamonds + " of 256 stone blocks");

        var caves = OreCaveFeature.Config.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"caves\": [{\"ore\": {\"Name\": \"minecraft:gold_block\"}, \"enabled\": \"2.C00_enableSulfurCave\"}]}"))
                .result().orElseThrow();
        helper.assertTrue(caves.caves().size() == 1 && caves.caves().get(0).stalactite().isEmpty()
                && caves.caves().get(0).enabled().get() == (WorldConfig.enableSulfurCave ? 1 : 0), "cave list override " + caves);
        helper.succeed();
    }

    /** A zero random bound or an empty size range fails the pack load instead of the chunk generation. */
    public static void bounds(GameTestHelper helper) {
        String[][] rejected = {
                {"australium attempts", "{\"attempts\": 0}"},
                {"flower spread", "{\"state\": {\"Name\": \"minecraft:poppy\"}, \"xz_spread\": 0}"},
                {"crater sizes", "{\"min_size\": 20, \"max_size\": 20}"},
                {"bubble sizes", "{\"min_size\": 30, \"max_size\": 10}"},
        };
        Codec<?>[] codecs = {AustraliumFeature.Config.CODEC, LegacyFlowerFeature.Config.CODEC, MapGenCrater.Config.CODEC,
                MapGenBubble.OIL_CODEC};
        for (int i = 0; i < codecs.length; i++) {
            boolean failed = codecs[i].parse(JsonOps.INSTANCE, JsonParser.parseString(rejected[i][1])).error().isPresent();
            helper.assertTrue(failed, rejected[i][0] + " was accepted");
        }
        helper.assertTrue(AustraliumFeature.Config.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"attempts\": 1}"))
                .result().isPresent(), "australium attempts 1 was refused");
        helper.succeed();
    }
}
