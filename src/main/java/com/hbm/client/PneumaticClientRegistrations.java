package com.hbm.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.RenderPipeAnchor;
import com.hbm.tileentity.ModPneumaticBlockEntities;
import com.hbm.inventory.ModPneumaticMenus;
import com.hbm.packet.PneumoAccessSyncPayload;
import dev.golem.core.net.GolemNetwork;
import com.hbm.inventory.gui.GUIPneumoStorageAccess;
import com.hbm.inventory.gui.GUIPneumoStorageClutter;
import com.hbm.inventory.gui.GUIPneumoStorageExporter;
import com.hbm.inventory.gui.GUIPneumoStorageImporter;
import com.hbm.inventory.gui.GUIPneumoStorageMono;
import com.hbm.inventory.gui.GUIPneumoTube;

/** Client-only registrations for the pneumatic tube storage system: screens, renderers and the terminal's packet. */
public final class PneumaticClientRegistrations {
    private PneumaticClientRegistrations() {}

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModPneumaticMenus.PNEUMATIC_TUBE.get(), GUIPneumoTube::new);
        sink.register(ModPneumaticMenus.PNEUMATIC_STORAGE_ACCESS.get(), GUIPneumoStorageAccess::new);
        sink.register(ModPneumaticMenus.PNEUMATIC_STORAGE_CLUTTER.get(), GUIPneumoStorageClutter::new);
        sink.register(ModPneumaticMenus.PNEUMATIC_STORAGE_MONO.get(), GUIPneumoStorageMono::new);
        sink.register(ModPneumaticMenus.PNEUMATIC_STORAGE_IMPORTER.get(), GUIPneumoStorageImporter::new);
        sink.register(ModPneumaticMenus.PNEUMATIC_STORAGE_EXPORTER.get(), GUIPneumoStorageExporter::new);
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        HbmBlockEntityRenderers.regGem(sink, ModPneumaticBlockEntities.PNEUMATIC_PIPE_ANCHOR.get(), () -> new RenderPipeAnchor<>("pipe_anchor_pneumatic"));
    }

    /** The access terminal's listing deltas; attached with the other client handlers. */
    public static void registerPacketHandlers() {
        GolemNetwork.registerPlayToClient(PneumoAccessSyncPayload.CODEC, PneumoAccessSyncPayload::handleClient);
    }
}
