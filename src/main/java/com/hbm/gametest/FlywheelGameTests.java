package com.hbm.gametest;

import com.hbm.Constants;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.render.Instancing;
import net.minecraft.gametest.framework.GameTestHelper;

/** The parts of the GemRender wiring a server can check: instancing is never reachable without a client. */
public final class FlywheelGameTests {

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function ENGINE_PRESENT =
            TESTS.function("flywheel_engine_present", 20, () -> FlywheelGameTests::enginePresent);
    public static final GameTests.Function ENGINE_CLIENT_ONLY =
            TESTS.function("flywheel_engine_client_only", 20, () -> FlywheelGameTests::engineClientOnly);

    private FlywheelGameTests() {}

    public static void init() {}

    /** A server level is never visualized, so a caller choosing a draw path is told to draw it itself. */
    public static void enginePresent(GameTestHelper helper) {
        helper.assertTrue(!Instancing.isVisualizing(helper.getLevel()),
                "A server level must never report as visualized");
        helper.assertTrue(!Instancing.isVisualizing(null), "A null level must not report as visualized");
        helper.succeed();
    }

    /** GemRender is a client mod: off-client the facade must not touch its Flywheel types at all. */
    public static void engineClientOnly(GameTestHelper helper) {
        helper.assertTrue(!Instancing.isAvailable(), "Instancing must report unavailable off-client");
        String backend = Instancing.report().backend();
        helper.assertTrue(backend.equals("server"), "report backend off-client: " + backend);
        helper.succeed();
    }
}
