package com.hbm.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.tileentity.ModProduction2BlockEntities;
import com.hbm.inventory.ModProduction2Menus;
import com.hbm.client.render.RenderAnnihilator;
import com.hbm.client.render.RenderExcavator;
import com.hbm.client.render.RenderOreSlopper;
import com.hbm.inventory.gui.GUIMachineAnnihilator;
import com.hbm.inventory.gui.GUIMachineDiFurnaceRTG;
import com.hbm.inventory.gui.GUIMachineExcavator;
import com.hbm.inventory.gui.GUIOreSlopper;
import com.hbm.inventory.gui.GUIRtgFurnace;

/** Screens and block-entity renderers for the production machines (group B). */
public final class Production2ClientRegistrations {
    private Production2ClientRegistrations() {}

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModProduction2Menus.EXCAVATOR.get(), GUIMachineExcavator::new);
        sink.register(ModProduction2Menus.ORE_SLOPPER.get(), GUIOreSlopper::new);
        sink.register(ModProduction2Menus.ANNIHILATOR.get(), GUIMachineAnnihilator::new);
        sink.register(ModProduction2Menus.RTG_FURNACE.get(), GUIRtgFurnace::new);
        sink.register(ModProduction2Menus.DIFURNACE_RTG.get(), GUIMachineDiFurnaceRTG::new);
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        HbmBlockEntityRenderers.regGem(sink, ModProduction2BlockEntities.EXCAVATOR.get(), RenderExcavator::new);
        HbmBlockEntityRenderers.regGem(sink, ModProduction2BlockEntities.ORE_SLOPPER.get(), RenderOreSlopper::new);
        HbmBlockEntityRenderers.regGem(sink, ModProduction2BlockEntities.ANNIHILATOR.get(), RenderAnnihilator::new);
    }
}
