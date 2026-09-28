package com.hbm.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.RenderICF;
import com.hbm.client.render.RenderICFController;
import com.hbm.client.render.StructPreviewRenderer;
import com.hbm.inventory.ModIcfMenus;
import com.hbm.inventory.gui.GUIICF;
import com.hbm.inventory.gui.GUIICFPress;
import com.hbm.tileentity.ModIcfBlockEntities;
import com.hbm.tileentity.machine.TileEntityICFStruct;

/** Screens and block-entity renderers of the ICF. */
public final class IcfClientRegistrations {

    private IcfClientRegistrations() {}

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModIcfMenus.ICF.get(), GUIICF::new);
        sink.register(ModIcfMenus.ICF_PRESS.get(), GUIICFPress::new);
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        HbmBlockEntityRenderers.regGem(sink, ModIcfBlockEntities.ICF.get(), RenderICF::new);
        HbmBlockEntityRenderers.regGem(sink, ModIcfBlockEntities.ICF_CONTROLLER.get(), RenderICFController::new);
        // 1.7 RenderICFMultiblock: the ghost of the vessel the seed still needs
        sink.register(ModIcfBlockEntities.ICF_STRUCT.get(), ctx -> new StructPreviewRenderer<TileEntityICFStruct>());
    }
}
