package com.hbm.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.tileentity.ModNuclearMiscBlockEntities;
import com.hbm.inventory.ModNuclearMiscMenus;
import com.hbm.client.render.RenderBreeder;
import com.hbm.client.render.RenderRadiolysis;
import com.hbm.client.render.RenderSmallReactor;
import com.hbm.inventory.gui.GUIMachineReactorBreeding;
import com.hbm.inventory.gui.GUIRadiolysis;
import com.hbm.inventory.gui.GUIReactorControl;
import com.hbm.inventory.gui.GUIReactorResearch;

/** Screens and block-entity renderers for the miscellaneous nuclear machines. */
public final class NuclearMiscClientRegistrations {
    private NuclearMiscClientRegistrations() {}

    public static void registerScreens(dev.golem.core.client.ClientRegistry.ScreenSink sink) {
        sink.register(ModNuclearMiscMenus.RADIOLYSIS.get(), GUIRadiolysis::new);
        sink.register(ModNuclearMiscMenus.REACTOR_BREEDING.get(), GUIMachineReactorBreeding::new);
        sink.register(ModNuclearMiscMenus.REACTOR_RESEARCH.get(), GUIReactorResearch::new);
        sink.register(ModNuclearMiscMenus.REACTOR_CONTROL.get(), GUIReactorControl::new);
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        HbmBlockEntityRenderers.regGem(sink, ModNuclearMiscBlockEntities.RADIOLYSIS.get(), RenderRadiolysis::new);
        HbmBlockEntityRenderers.regGem(sink, ModNuclearMiscBlockEntities.REACTOR_BREEDING.get(), RenderBreeder::new);
        HbmBlockEntityRenderers.regGem(sink, ModNuclearMiscBlockEntities.REACTOR_RESEARCH.get(), RenderSmallReactor::new);
    }
}
