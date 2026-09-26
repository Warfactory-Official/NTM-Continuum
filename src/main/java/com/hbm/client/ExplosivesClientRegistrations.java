package com.hbm.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.tileentity.ModExplosivesBlockEntities;
import com.hbm.inventory.ModExplosivesMenus;
import com.hbm.client.render.BombMultiRenderer;
import com.hbm.client.render.ChargeRenderer;
import com.hbm.client.render.CrashedBombRenderer;
import com.hbm.client.render.DetCordRenderer;
import com.hbm.client.render.LandmineRenderer;
import com.hbm.inventory.gui.GUIBombMulti;

/** Client hooks for the conventional explosives: the multi purpose bomb's screen and the five renderers. */
public final class ExplosivesClientRegistrations {
    private ExplosivesClientRegistrations() {}

    public static void registerScreens(dev.golem.core.client.ClientRegistry.ScreenSink sink) {
        sink.register(ModExplosivesMenus.BOMB_MULTI.get(), GUIBombMulti::new);
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        HbmBlockEntityRenderers.regGem(sink, ModExplosivesBlockEntities.LANDMINE.get(), LandmineRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, ModExplosivesBlockEntities.CHARGE.get(), ChargeRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, ModExplosivesBlockEntities.DET_CORD.get(), DetCordRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, ModExplosivesBlockEntities.CRASHED_BOMB.get(), CrashedBombRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, ModExplosivesBlockEntities.BOMB_MULTI.get(), BombMultiRenderer::new);
    }
}
