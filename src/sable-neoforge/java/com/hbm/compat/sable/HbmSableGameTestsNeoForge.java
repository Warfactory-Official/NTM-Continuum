package com.hbm.compat.sable;

import com.hbm.Constants;
import dev.golem.core.gametest.GameTests;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** The {@code hbm:sable_*} GameTests on NeoForge, found by annotation; release jars drop the class. */
@EventBusSubscriber(modid = Constants.MOD_ID)
public final class HbmSableGameTestsNeoForge {

    private HbmSableGameTestsNeoForge() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        if (ModList.get().isLoaded("sable")) GameTests.register(event, SableGameTests.TESTS);
    }
}
