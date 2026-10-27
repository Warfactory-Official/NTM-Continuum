package com.hbm.gametest.deimport;

import com.hbm.Constants;
import dev.golem.core.gametest.GameTests;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** The {@code hbm:deimport_*} GameTests, found by annotation; release jars drop this package. */
@EventBusSubscriber(modid = Constants.MOD_ID)
public final class DeImportGameTestsNeoForge {
    private DeImportGameTestsNeoForge() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        GameTests.register(event, DeImportGameTests.TESTS);
    }
}
