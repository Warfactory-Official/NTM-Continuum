package com.hbm.client.chemicalfactory;

import com.hbm.chemicalfactory.ChemicalFactoryRegistrations;
import com.hbm.client.HbmClientRegistrations;
import com.hbm.client.render.ChemicalFactoryRenderer;
import com.hbm.inventory.gui.GUIMachineChemicalFactory;

/** Client-only registration bridge for the factory leaf. */
public final class ChemicalFactoryClientRegistrations {

    private ChemicalFactoryClientRegistrations() {}

    public static void registerScreen(dev.golem.core.client.ClientRegistry.ScreenSink sink) {
        sink.register(ChemicalFactoryRegistrations.MACHINE_CHEMICAL_FACTORY_MENU.get(),
                GUIMachineChemicalFactory::new);
    }

    public static void registerRenderer(com.hbm.client.render.HbmBlockEntityRenderers.Sink sink) {
        com.hbm.client.render.HbmBlockEntityRenderers.regGem(sink,
                ChemicalFactoryRegistrations.MACHINE_CHEMICAL_FACTORY_BE.get(), ChemicalFactoryRenderer::new);
    }
}
