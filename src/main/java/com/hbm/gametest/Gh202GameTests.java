package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.config.Configuration;
import com.hbm.config.GeneralConfig;
import com.hbm.config.MobConfig;
import com.hbm.main.MainRegistry;
import dev.golem.core.gametest.GameTests;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.gametest.framework.GameTestHelper;

/** GitHub issue 202: the HUD mode badges light up under 1.7's own conditions, read from a real {@code hbm.cfg}. */
public final class Gh202GameTests {

    private Gh202GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function BADGES =
            TESTS.function("gh202_mode_badges_follow_17_conditions", 20, () -> Gh202GameTests::badges);

    public static void init() {}

    private static void badges(GameTestHelper helper) {
        try {
            Path file = Files.createTempDirectory("hbmcfg").resolve("hbm.cfg");
            MainRegistry.loadConfig(new Configuration(file));
            helper.assertTrue(!GeneralConfig.true528() && !GeneralConfig.trueExp() && !MobConfig.trueRam(),
                    "default config lights a badge");

            Files.writeString(file, Files.readString(file)
                    .replace("B:enable528Mode=false", "B:enable528Mode=true")
                    .replace("B:1.99_enableExpensiveMode=false", "B:1.99_enableExpensiveMode=true")
                    .replace("B:12.R01_rampantMode=false", "B:12.R01_rampantMode=true"));
            MainRegistry.loadConfig(new Configuration(file));
            helper.assertTrue(GeneralConfig.true528(), "528 mode with its default switches is not true528");
            helper.assertTrue(GeneralConfig.trueExp(), "expensive mode is not trueExp");
            helper.assertTrue(MobConfig.trueRam(), "rampant mode is not trueRam");

            // 1.7 drops the 528 badge once any switch is relaxed
            Files.writeString(file, Files.readString(file)
                    .replace("B:X528_enableColtanSpawning=false", "B:X528_enableColtanSpawning=true"));
            MainRegistry.loadConfig(new Configuration(file));
            helper.assertTrue(GeneralConfig.enable528 && !GeneralConfig.true528(), "coltan spawning kept the 528 badge");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            MainRegistry.loadConfig(new Configuration(MainRegistry.configFile()));
        }
        helper.succeed();
    }
}
