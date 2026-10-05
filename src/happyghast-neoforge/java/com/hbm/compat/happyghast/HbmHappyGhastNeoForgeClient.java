package com.hbm.compat.happyghast;

import com.hbm.Constants;
import com.hbm.compat.happyghast.client.HappyGhastClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;

/**
 * The happy-ghast Fat Man on the NeoForge client: hands its renderer to HBM's entity renderers while mods construct,
 * before {@code EntityRenderersEvent.RegisterRenderers}.
 */
@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public final class HbmHappyGhastNeoForgeClient {

    private HbmHappyGhastNeoForgeClient() {}

    static {
        HappyGhastClient.init();
    }

    @SubscribeEvent
    public static void construct(FMLConstructModEvent event) {
        HappyGhastClient.init();
    }
}
