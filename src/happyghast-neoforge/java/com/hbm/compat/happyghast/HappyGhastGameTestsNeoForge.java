package com.hbm.compat.happyghast;

import com.hbm.Constants;
import dev.golem.core.gametest.GameTests;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** The happy-ghast GameTests on NeoForge, found by annotation; release jars drop the class. */
@EventBusSubscriber(modid = Constants.MOD_ID)
public final class HappyGhastGameTestsNeoForge {

    private HappyGhastGameTestsNeoForge() {}

    @SubscribeEvent
    public static void construct(FMLConstructModEvent event) {
        HappyGhastGameTests.init();
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        GameTests.register(event, HappyGhastGameTests.TESTS);
    }
}
