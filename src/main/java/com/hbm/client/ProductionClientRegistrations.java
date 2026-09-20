package com.hbm.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.tileentity.ModProductionBlockEntities;
import com.hbm.inventory.ModProductionMenus;
import com.hbm.client.render.RenderConveyorPress;
import com.hbm.client.render.RenderMicrowave;
import com.hbm.client.render.RenderRotaryFurnace;
import com.hbm.client.render.RenderThresher;
import com.hbm.inventory.gui.GUIAutocrafter;
import com.hbm.inventory.gui.GUIMachineRotaryFurnace;
import com.hbm.inventory.gui.GUIMicrowave;

/** Screens and block-entity renderers for the production machines (group A). */
public final class ProductionClientRegistrations {
    private ProductionClientRegistrations() {}

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModProductionMenus.ROTARY_FURNACE.get(), GUIMachineRotaryFurnace::new);
        sink.register(ModProductionMenus.MICROWAVE.get(), GUIMicrowave::new);
        sink.register(ModProductionMenus.AUTOCRAFTER.get(), GUIAutocrafter::new);
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        HbmBlockEntityRenderers.regGem(sink, ModProductionBlockEntities.ROTARY_FURNACE.get(), RenderRotaryFurnace::new);
        HbmBlockEntityRenderers.regGem(sink, ModProductionBlockEntities.THRESHER.get(), RenderThresher::new);
        HbmBlockEntityRenderers.regGem(sink, ModProductionBlockEntities.MICROWAVE.get(), RenderMicrowave::new);
        HbmBlockEntityRenderers.regGem(sink, ModProductionBlockEntities.CONVEYOR_PRESS.get(), RenderConveyorPress::new);
    }
}
