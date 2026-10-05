package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.config.Configuration;
import com.hbm.config.GeneralConfig;
import com.hbm.config.MobConfig;
import com.hbm.config.PotionConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.config.WorldConfig;
import com.hbm.main.MainRegistry;
import com.hbm.world.gen.ConfigPlacement;
import com.hbm.world.gen.WorldgenKeys;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Fix wave phase 8 (History §19ej-8): {@code hbm.cfg}. A freshly written file holds every applicable 1.7 key with its
 * 1.7 effective default and none of the not-applicable ones ({@link Config17Table}, generated from the 1.7 source), and
 * a sample of keys is flipped to show it reaches the code it gates.
 */
public final class ConfigGameTests {

    private ConfigGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function KEYS = TESTS.function(
            "fix8_config_keys_match_17", 20, () -> ConfigGameTests::keys);
    public static final GameTests.Function FORMAT = TESTS.function(
            "fix8_config_file_format", 20, () -> ConfigGameTests::format);
    public static final GameTests.Function GATES = TESTS.function(
            "fix8_config_keys_gate_behaviour", 20, () -> ConfigGameTests::gates);
    public static final GameTests.Function PLACEMENT = TESTS.function(
            "fix8_config_placement_reads_cfg", 20, () -> ConfigGameTests::placement);

    public static void init() {}

    /** Writes a fresh {@code hbm.cfg} (every loader, 1.7 order) into a temp file and reads it back. */
    private static Configuration fresh() throws Exception {
        Path dir = Files.createTempDirectory("hbmcfg");
        Path file = dir.resolve("hbm.cfg");
        MainRegistry.loadConfig(new Configuration(file));
        Configuration read = new Configuration(file);
        read.load();
        return read;
    }

    /** Puts the running config back to the real file (the test server's own {@code hbm.cfg}). */
    private static void restore() {
        MainRegistry.loadConfig(new Configuration(MainRegistry.configFile()));
    }

    private static void keys(GameTestHelper helper) {
        try {
            Configuration cfg = fresh();
            int written = 0;
            int absent = 0;
            Set<String> expected = new HashSet<>();
            for (Config17Table.Key k : Config17Table.KEYS) {
                Configuration.Property p = cfg.peek(k.category(), k.key());
                if (k.notApplicable() != null) {
                    helper.assertTrue(p == null, k.key() + " is not applicable (" + k.notApplicable() + ") but was written");
                    absent++;
                    continue;
                }
                expected.add(k.category() + "/" + k.key());
                helper.assertTrue(p != null, "1.7 key " + k.category() + "/" + k.key() + " missing from hbm.cfg");
                boolean list = k.type().endsWith("L");
                helper.assertTrue(p.isList() == list, k.key() + " list-ness differs from 1.7");
                String value = list ? String.join("|", p.getStringList()) : p.getString();
                helper.assertTrue(k.def().equals(value), k.key() + " default must be 1.7's " + k.def() + ", got " + value);
                written++;
            }
            // nothing beyond the 1.7 keys
            for (String category : cfg.categoryNames()) {
                for (String key : cfg.properties(category).keySet()) {
                    helper.assertTrue(expected.contains(category + "/" + key), "hbm.cfg holds a key 1.7 did not have: " + category + "/" + key);
                }
            }
            // 248 (§19ej-8) + 57 since §19ek-hooks (6.04, 10.04, RADWORLD_03, POL_01/03/06/07, 5.01/5.02/5.05-5.43, 9 dungeon
            // chances) + 1 (1.31_enableSkyboxes, §19ek-visual) = 306 written, 33 not applicable (§19ek-final). Exact on
            // purpose: a key added to or dropped from the writer without its table row is drift and must fail here.
            helper.assertTrue(written + absent == 339 && written == 306 && absent == 33, "339 1.7 keys = 306 written + 33 not applicable, got "
                    + written + " + " + absent);
            // the parsed values are the 1.7 effective defaults too (spot checks of the reported mismatches)
            helper.assertTrue(MobConfig.scoutThreshold == 1D && MobConfig.scoutSwarmSpawnChance == 3 && MobConfig.largeHiveThreshold == 20
                    && MobConfig.rampantScoutSpawnThresh == 13D && MobConfig.pollutionMult == 1D && MobConfig.swarmCooldown == 2400,
                    "MobConfig 1.7 defaults");
            helper.assertTrue(MobConfig.glyphidChance[1] == -45 && MobConfig.brawlerChance[0] == 10 && MobConfig.brawlerChance[1] == 30
                    && MobConfig.blasterChance[0] == -5, "glyphid chance lists must be 1.7's");
            helper.assertTrue(RadiationConfig.fogCh == 20, "FOG_01_threshold 20");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            restore();
        }
        helper.succeed();
    }

    /** Forge's file format, and Forge's handling of edited and broken values. */
    private static void format(GameTestHelper helper) {
        try {
            Path dir = Files.createTempDirectory("hbmcfg");
            Path file = dir.resolve("hbm.cfg");
            MainRegistry.loadConfig(new Configuration(file));
            String text = Files.readString(file);
            helper.assertTrue(text.startsWith("# Configuration file"), "Forge header");
            helper.assertTrue(text.contains("01_general {") && text.contains("\"LESS BULLSHIT MODE\" {") && text.contains("528 {"),
                    "1.7 category names (quoted when they hold a space)");
            helper.assertTrue(text.contains("    B:2.D00_overworldOres=true") && text.contains("    I:2.00_uraniumSpawnrate=7")
                    && text.contains("    D:AMBIENT_00_nether=0.1") && text.contains("    S:1.03_enableDungeonSpawn=flag"),
                    "typed B:/I:/D:/S: entries");
            helper.assertTrue(text.contains("    I:12.GC01_glyphidChance <\n        50\n        -45\n        0\n     >")
                    || text.contains("    I:12.GC01_glyphidChance <\r\n        50\r\n        -45\r\n        0\r\n     >"), "Forge list block");
            helper.assertTrue(text.indexOf("01_general {") < text.indexOf("02_ores {") && text.indexOf("18_weapons {") < text.indexOf("528 {"),
                    "categories in natural string order");
            helper.assertTrue(text.contains("# 528 Mode: Please proceed with caution!"), "category comment box");

            // a user edit survives a load/save, a broken value falls back to the default, an unknown key is kept
            String edited = text.replace("I:2.00_uraniumSpawnrate=7", "I:2.00_uraniumSpawnrate=3")
                    .replace("I:2.01_titaniumSpawnrate=8", "I:2.01_titaniumSpawnrate=banana")
                    .replace("01_general {", "01_general {\n    B:zz_userKey=true");
            Files.writeString(file, edited);
            MainRegistry.loadConfig(new Configuration(file));
            helper.assertTrue(WorldConfig.uraniumSpawn == 3, "edited uranium rate must load as 3, got " + WorldConfig.uraniumSpawn);
            helper.assertTrue(WorldConfig.titaniumSpawn == 8, "an unreadable value must fall back to 1.7's 8");
            String saved = Files.readString(file);
            helper.assertTrue(saved.contains("I:2.00_uraniumSpawnrate=3") && saved.contains("I:2.01_titaniumSpawnrate=8")
                    && saved.contains("B:zz_userKey=true"), "save keeps the edit, repairs the broken value and keeps unknown keys");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            restore();
        }
        helper.succeed();
    }

    /** Flips a sample of keys through a real file and observes the gated code. */
    private static void gates(GameTestHelper helper) {
        try {
            Path dir = Files.createTempDirectory("hbmcfg");
            Path file = dir.resolve("hbm.cfg");
            MainRegistry.loadConfig(new Configuration(file));
            String text = Files.readString(file)
                    .replace("B:12.R01_rampantMode=false", "B:12.R01_rampantMode=true")
                    .replace("S:8.S0_potionSickness=OFF", "S:8.S0_potionSickness=TERRARIA")
                    .replace("B:enableLessBullshitMode=false", "B:enableLessBullshitMode=true")
                    .replace("I:LBSM_schrabOreRate=20", "I:LBSM_schrabOreRate=33")
                    .replace("B:9.00_scaleRTGPower=false", "B:9.00_scaleRTGPower=true");
            Files.writeString(file, text);
            MainRegistry.loadConfig(new Configuration(file));

            // 1.7 rampant mode switches on every rampant feature and raises the soot multiplier
            helper.assertTrue(MobConfig.rampantMode && MobConfig.rampantDig && MobConfig.rampantExtendedTargetting
                    && MobConfig.rampantGlyphidGuidance && MobConfig.rampantNaturalScoutSpawn, "rampant mode enables its features");
            helper.assertTrue(MobConfig.pollutionMult == 3D && MobConfig.scoutThreshold == 0.1D && MobConfig.scoutSwarmSpawnChance == 1
                    && MobConfig.bombardierChance[2] == 0 && RadiationConfig.sootFogThreshold == 105D, "rampant overrides (pollutionMult 3, smog 35*3)");
            helper.assertTrue(MobConfig.trueRam(), "trueRam() with rampant mode");
            // the chimney reads the rampant override
            helper.assertTrue(new com.hbm.tileentity.machine.TileEntityChimneyBrick(BlockPos.ZERO,
                    com.hbm.smoke.SmokeChainRegistration.chimney_brick.get().defaultBlockState()).getPollutionMod() == 0.4D,
                    "brick chimney under rampant mode = 12.R06_rampantSmokeStackOverride 0.4");

            helper.assertTrue(PotionConfig.potionSickness == 2, "8.S0_potionSickness TERRARIA = 2");
            helper.assertTrue(com.hbm.explosion.ExplosionNukeGeneric.getSchrabOreChance() == 33, "LBSM schrab ore rate 33");
            helper.assertTrue(com.hbm.inventory.recipes.StorageDrumRecipes.longDecayChance() == 15 * 60 * 20,
                    "LBSM_shortDecay: long decay 15 minutes");
            helper.assertTrue(com.hbm.config.MachineConfig.scaleRTGPowerActive(), "9.00_scaleRTGPower read");
            // 528 is off: its sub-switches stay forced off whatever the file says (1.7 loadFromConfig)
            helper.assertTrue(!GeneralConfig.enable528NetherBurn && !GeneralConfig.enable528ReasimBoilers
                    && !com.hbm.config.DungeonConfig.enable528BosniaSimulator, "X528 switches forced off without 528 mode");

            // 528 on forces LBSM off and lets its sub-switches through
            Files.writeString(file, Files.readString(file).replace("B:enable528Mode=false", "B:enable528Mode=true"));
            MainRegistry.loadConfig(new Configuration(file));
            helper.assertTrue(GeneralConfig.enable528 && !GeneralConfig.enableLBSM && GeneralConfig.enable528NetherBurn
                    && GeneralConfig.enable528ReasimBoilers && com.hbm.tileentity.machine.rbmk.RBMKDials.getReasimBoilers(helper.getLevel()),
                    "528 mode: LBSM off, X528 switches on, the RBMK ReaSim dial forced on");
            helper.assertTrue(com.hbm.explosion.ExplosionNukeGeneric.getSchrabOreChance() == 250, "LBSM off again: 250");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            restore();
        }
        helper.assertTrue(!MobConfig.rampantMode && MobConfig.pollutionMult == 1D && !GeneralConfig.enable528,
                "the real hbm.cfg is restored");
        helper.succeed();
    }

    /** {@code hbm:config} reads the current value, and the shipped placed features use it. */
    private static void placement(GameTestHelper helper) {
        int uranium = WorldConfig.uraniumSpawn;
        boolean overworld = WorldConfig.overworldOre;
        int meteorite = WorldConfig.meteoriteSpawn;
        try {
            ConfigPlacement gneissUranium = new ConfigPlacement(WorldgenKeys.URANIUM, ConfigPlacement.Mode.COUNT, 3);
            RandomSource random = RandomSource.create(1L);
            helper.assertTrue(gneissUranium.getPositions(null, random, BlockPos.ZERO).count() == 21, "7 * 3 gneiss uranium veins");
            WorldConfig.uraniumSpawn = 0;
            helper.assertTrue(gneissUranium.getPositions(null, random, BlockPos.ZERO).count() == 0, "0 places nothing");
            WorldConfig.uraniumSpawn = 2;
            helper.assertTrue(gneissUranium.getPositions(null, random, BlockPos.ZERO).count() == 6, "2 * 3");

            ConfigPlacement ores = new ConfigPlacement(WorldgenKeys.OVERWORLD_ORES, ConfigPlacement.Mode.SWITCH, 1);
            helper.assertTrue(ores.getPositions(null, random, BlockPos.ZERO).count() == 1, "2.D00_overworldOres on");
            WorldConfig.overworldOre = false;
            helper.assertTrue(ores.getPositions(null, random, BlockPos.ZERO).count() == 0, "2.D00_overworldOres off");

            ConfigPlacement fallen = new ConfigPlacement(WorldgenKeys.METEORITE, ConfigPlacement.Mode.CHANCE, 1);
            WorldConfig.meteoriteSpawn = 1;
            helper.assertTrue(fallen.getPositions(null, random, BlockPos.ZERO).count() == 1, "chance 1 always keeps");
            WorldConfig.meteoriteSpawn = 0;
            helper.assertTrue(fallen.getPositions(null, random, BlockPos.ZERO).count() == 0, "chance 0 = off (1.7 > 0 guard)");
            WorldConfig.meteoriteSpawn = 200;
            int kept = 0;
            for (int i = 0; i < 20000; i++) kept += (int) fallen.getPositions(null, random, BlockPos.ZERO).count();
            helper.assertTrue(kept > 60 && kept < 140, "1 in 200 over 20000 draws ~ 100, got " + kept);

            // every key a placed feature names is bound
            for (Map.Entry<String, java.util.function.IntSupplier> e : WorldgenKeys.all().entrySet()) {
                helper.assertTrue(e.getValue() != null, "binding for " + e.getKey());
            }
            // the shipped placed feature starts with the master switch and counts by the config key
            com.google.gson.JsonObject json = resource(helper, "worldgen/placed_feature/ore_gneiss_uranium.json");
            com.google.gson.JsonArray placement = json.getAsJsonArray("placement");
            com.google.gson.JsonObject first = placement.get(0).getAsJsonObject();
            com.google.gson.JsonObject second = placement.get(1).getAsJsonObject();
            helper.assertTrue("hbm:config".equals(first.get("type").getAsString()) && "2.D00_overworldOres".equals(first.get("key").getAsString())
                    && "switch".equals(first.get("mode").getAsString()), "ore_gneiss_uranium begins with the 2.D00 switch");
            helper.assertTrue("2.00_uraniumSpawnrate".equals(second.get("key").getAsString()) && second.get("multiplier").getAsInt() == 3,
                    "ore_gneiss_uranium counts 3 x 2.00_uraniumSpawnrate");
        } finally {
            WorldConfig.uraniumSpawn = uranium;
            WorldConfig.overworldOre = overworld;
            WorldConfig.meteoriteSpawn = meteorite;
        }
        helper.succeed();
    }

    /** A file of the loaded data pack, from the server's resource manager. */
    private static com.google.gson.JsonObject resource(GameTestHelper helper, String path) {
        var resource = helper.getLevel().getServer().getResourceManager().getResource(dev.golem.core.Ids.of("hbm", path))
                .orElseThrow(() -> new IllegalStateException("missing data/hbm/" + path));
        try (java.io.Reader reader = resource.openAsReader()) {
            return com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
