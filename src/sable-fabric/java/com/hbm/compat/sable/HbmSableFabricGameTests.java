package com.hbm.compat.sable;

import dev.golem.core.gametest.GameTests;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;

import java.util.Collection;
import java.util.List;

/**
 * The {@code hbm:sable_*} GameTests on Fabric: a second {@code fabric-gametest} entrypoint the build appends only on
 * nodes that declare Sable. It hands over the Sable suite alone ({@link GameTests#functionsOf}); HBM's own generator
 * hands over everything else, so nothing is registered twice.
 */
public final class HbmSableFabricGameTests {

    public HbmSableFabricGameTests() {}

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        if (!FabricLoader.getInstance().isModLoaded("sable")) return List.of();
        return GameTests.functionsOf(HbmSableIntegration.gameTests());
    }
}
