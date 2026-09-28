package com.hbm.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.HbmEntityRenderers;
import com.hbm.client.render.SatDockRenderer;
import com.hbm.client.render.entity.RenderOrbitalLaser;
import com.hbm.entity.ModSatelliteEntities;
import com.hbm.inventory.ModSatelliteMenus;
import com.hbm.inventory.gui.GUIMachineSatLinker;
import com.hbm.inventory.gui.GUISatDock;
import com.hbm.inventory.gui.GUIScreenSatCoord;
import com.hbm.items.tool.ItemSatInterface;
import com.hbm.tileentity.ModSatelliteBlockEntities;

/** Screens, block-entity renderers and entity renderers of the satellite family. */
public final class SatelliteClientRegistrations {

    private SatelliteClientRegistrations() {}

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModSatelliteMenus.SATLINKER.get(), GUIMachineSatLinker::new);
        sink.register(ModSatelliteMenus.SAT_DOCK.get(), GUISatDock::new);
        // 1.7 ItemSatInterface.provideGUI: a client-only screen with no container
        ItemSatInterface.OPEN_SCREEN = player -> net.minecraft.client.Minecraft.getInstance()
                .setScreen(new GUIScreenSatCoord(player));
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        HbmBlockEntityRenderers.regGem(sink, ModSatelliteBlockEntities.SAT_DOCK.get(), SatDockRenderer::new);
    }

    public static void registerEntityRenderers(HbmEntityRenderers.Sink sink) {
        sink.register(ModSatelliteEntities.ORBITAL_LASER.get(), RenderOrbitalLaser::new);
    }
}
