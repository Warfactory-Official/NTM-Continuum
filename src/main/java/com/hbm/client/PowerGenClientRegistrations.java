package com.hbm.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.tileentity.ModPowerGenBlockEntities;
import com.hbm.inventory.ModPowerGenMenus;
import com.hbm.packet.SirenPayload;
import dev.golem.core.net.GolemNetwork;
import com.hbm.client.render.RenderChungus;
import com.hbm.client.render.RenderHephaestus;
import com.hbm.client.render.RenderIGenerator;
import com.hbm.client.render.RenderLPW2;
import com.hbm.client.render.SirenClient;
import com.hbm.inventory.gui.GUIMachineSiren;

/** Screens, block-entity renderers and the siren packet handler for the large power-generation family. */
public final class PowerGenClientRegistrations {
    private PowerGenClientRegistrations() {}

    public static void registerScreens(dev.golem.core.client.ClientRegistry.ScreenSink sink) {
        sink.register(ModPowerGenMenus.SIREN.get(), GUIMachineSiren::new);
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        HbmBlockEntityRenderers.regGem(sink, ModPowerGenBlockEntities.CHUNGUS.get(), RenderChungus::new);
        HbmBlockEntityRenderers.regGem(sink, ModPowerGenBlockEntities.LPW2.get(), RenderLPW2::new);
        HbmBlockEntityRenderers.regGem(sink, ModPowerGenBlockEntities.HEPHAESTUS.get(), RenderHephaestus::new);
        HbmBlockEntityRenderers.regGem(sink, ModPowerGenBlockEntities.INDUSTRIAL_GENERATOR.get(), RenderIGenerator::new);
    }

    /** 1.7 {@code TESirenPacket.Handler}. */
    public static void registerPacketHandlers() {
        GolemNetwork.registerPlayToClient(SirenPayload.CODEC, SirenClient::handle);
    }
}
