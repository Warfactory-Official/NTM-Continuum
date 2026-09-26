package com.hbm.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.blocks.network.CableDiode;
import com.hbm.tileentity.ModEnergyNetBlockEntities;
import net.minecraft.client.Minecraft;
import com.hbm.client.render.RenderCableDiode;
import com.hbm.client.render.RenderCablePaintable;
import com.hbm.client.render.RenderConnector;
import com.hbm.client.render.RenderHEVBattery;
import com.hbm.client.render.RenderPylon;
import com.hbm.client.render.RenderPylonLarge;
import com.hbm.client.render.RenderPylonMedium;
import com.hbm.client.render.RenderSubstation;
import com.hbm.client.render.RenderTesla;
import com.hbm.client.render.WiringHudOverlay;
import com.hbm.inventory.gui.GUIDiode;

/** Client hooks for the energy-network family: the diode screen, the pylon/wire renderers, the drum readout. */
public final class EnergyNetClientRegistrations {

    private EnergyNetClientRegistrations() {}

    public static void registerScreens(dev.golem.core.client.ClientRegistry.ScreenSink sink) {
        CableDiode.OPEN_SCREEN = diode -> Minecraft.getInstance().setScreen(new GUIDiode(diode));
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        HbmBlockEntityRenderers.regGem(sink, ModEnergyNetBlockEntities.PYLON.get(), RenderPylon::new);
        HbmBlockEntityRenderers.regGem(sink, ModEnergyNetBlockEntities.PYLON_MEDIUM.get(), RenderPylonMedium::new);
        HbmBlockEntityRenderers.regGem(sink, ModEnergyNetBlockEntities.PYLON_LARGE.get(), RenderPylonLarge::new);
        HbmBlockEntityRenderers.regGem(sink, ModEnergyNetBlockEntities.SUBSTATION.get(), RenderSubstation::new);
        HbmBlockEntityRenderers.regGem(sink, ModEnergyNetBlockEntities.CONNECTOR.get(), () -> new RenderConnector<>(false));
        HbmBlockEntityRenderers.regGem(sink, ModEnergyNetBlockEntities.CONNECTOR_SUPER.get(), () -> new RenderConnector<>(true));
        HbmBlockEntityRenderers.regGem(sink, ModEnergyNetBlockEntities.CABLE_DIODE.get(), RenderCableDiode::new);
        HbmBlockEntityRenderers.regGem(sink, ModEnergyNetBlockEntities.RED_CABLE_PAINTABLE.get(), RenderCablePaintable::new);
        HbmBlockEntityRenderers.regGem(sink, ModEnergyNetBlockEntities.HEV_BATTERY.get(), RenderHEVBattery::new);
        HbmBlockEntityRenderers.regGem(sink, ModEnergyNetBlockEntities.TESLA.get(), RenderTesla::new);
    }

    public static void registerHud(HbmClientRegistrations.HudSink sink) {
        sink.register("wiring_readout", WiringHudOverlay::render);
    }
}
