package com.hbm.platform.fabric;

/**
 * The {@code fabric-gametest} entrypoint. Fabric API registers this class by reflection and runs its
 * {@code @GameTestGenerator} when the server starts with {@code -Dfabric-api.gametest}
 * ({@code gradlew :<node>-fabric:runGametest}). 26.1 has no generator API; its Fabric API registry hook is forwarded
 * to Golem Core by the loader-specific mixin. The suites are declared by {@code GameTestEntry}.
 */
public final class FabricGameTests {

    public FabricGameTests() {}

    //? if <26.1 {
    /*@net.minecraft.gametest.framework.GameTestGenerator
    public static java.util.Collection<net.minecraft.gametest.framework.TestFunction> generate() {
        for (dev.golem.core.gametest.GameTests.Suite suite : com.hbm.gametest.HbmGameTestSuites.all()) {
            dev.golem.core.gametest.GameTests.register(suite);
        }
        return dev.golem.core.gametest.GameTests.pending();
    }
    *///?}
}
