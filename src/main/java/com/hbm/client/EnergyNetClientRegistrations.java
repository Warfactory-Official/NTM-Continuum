package com.hbm.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.Constants;
import com.hbm.blocks.network.BlockNetworkSeparator;
import com.hbm.client.render.MachineObjRenderer;
import com.hbm.client.render.MachineObjRenderer.ModelDef;
import com.hbm.tileentity.network.TileEntityNetworkSeparator;
import dev.golem.core.Ids;
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

/** Client hooks for the energy-network family: the separator screen, the pylon/wire renderers, the drum readout. */
public final class EnergyNetClientRegistrations {

    private EnergyNetClientRegistrations() {}

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        BlockNetworkSeparator.OPEN_SCREEN = separator -> Minecraft.getInstance().setScreen(new GUIDiode(separator));
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
        // 1.7 turns N=90, W=180, S=270, E=0: the standard map + 90
        HbmBlockEntityRenderers.regGem(sink, ModEnergyNetBlockEntities.NETWORK_SEPARATOR.get(),
                () -> new MachineObjRenderer<TileEntityNetworkSeparator>(90F, 1F,
                        new ModelDef(Ids.of(Constants.MOD_ID, "models/network/network_separator.obj"),
                                Ids.of(Constants.MOD_ID, "textures/models/network/network_separator.png"))));
    }

    public static void registerHud(HbmClientRegistrations.HudSink sink) {
        sink.register("wiring_readout", WiringHudOverlay::render);
    }
}
