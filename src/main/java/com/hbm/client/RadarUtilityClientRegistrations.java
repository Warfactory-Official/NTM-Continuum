package com.hbm.client;

import com.hbm.blocks.machine.MachineRadar;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.RenderCapsule;
import com.hbm.client.render.RenderMachineForceField;
import com.hbm.client.render.RenderOrbus;
import com.hbm.client.render.RenderRadar;
import com.hbm.client.render.RenderRadarLarge;
import com.hbm.client.render.RenderRadarScreen;
import com.hbm.inventory.ModRadarUtilityMenus;
import com.hbm.inventory.gui.GUIForceField;
import com.hbm.inventory.gui.GUIMachineRadarNT;
import com.hbm.inventory.gui.GUIMachineRadarNTSlots;
import com.hbm.inventory.gui.GUISoyuzCapsule;
import com.hbm.tileentity.ModRadarUtilityBlockEntities;

/** Screens and block-entity renderers of the radar and utility machines. */
public final class RadarUtilityClientRegistrations {

    private RadarUtilityClientRegistrations() {}

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModRadarUtilityMenus.RADAR.get(), GUIMachineRadarNTSlots::new);
        sink.register(ModRadarUtilityMenus.FORCEFIELD.get(), GUIForceField::new);
        sink.register(ModRadarUtilityMenus.SOYUZ_CAPSULE.get(), GUISoyuzCapsule::new);
        // 1.7 GUI id 0: the radar map is a client-only screen with no menu
        MachineRadar.OPEN_SCREEN = radar -> net.minecraft.client.Minecraft.getInstance().setScreen(new GUIMachineRadarNT(radar));
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        HbmBlockEntityRenderers.regGem(sink, ModRadarUtilityBlockEntities.RADAR.get(), RenderRadar::new);
        HbmBlockEntityRenderers.regGem(sink, ModRadarUtilityBlockEntities.RADAR_LARGE.get(), RenderRadarLarge::new);
        HbmBlockEntityRenderers.regGem(sink, ModRadarUtilityBlockEntities.RADAR_SCREEN.get(), RenderRadarScreen::new);
        HbmBlockEntityRenderers.regGem(sink, ModRadarUtilityBlockEntities.FORCEFIELD.get(), RenderMachineForceField::new);
        HbmBlockEntityRenderers.regGem(sink, ModRadarUtilityBlockEntities.ORBUS.get(), RenderOrbus::new);
        HbmBlockEntityRenderers.regGem(sink, ModRadarUtilityBlockEntities.SOYUZ_CAPSULE.get(), RenderCapsule::new);
    }
}
