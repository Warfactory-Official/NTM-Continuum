package com.hbm.radio.client;

import com.hbm.Constants;
import com.hbm.client.HbmClientRegistrations;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.MachineObjRenderer.ModelDef;
import com.hbm.client.render.MachineObjRenderer;
import com.hbm.radio.ModRadioBlockEntities;
import com.hbm.radio.ModRadioMenus;

public final class RadioClientRegistrations {
    private RadioClientRegistrations() {}

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModRadioMenus.RADIO_TORCH.get(), RadioTorchScreen::new);
        sink.register(ModRadioMenus.RADIO_READER.get(), RadioReaderScreen::new);
        sink.register(ModRadioMenus.RADIO_LOGIC.get(), RadioLogicScreen::new);
        sink.register(ModRadioMenus.COUNTER.get(), RadioCounterScreen::new);
        sink.register(ModRadioMenus.CONTROLLER.get(), RadioControllerScreen::new);
        sink.register(ModRadioMenus.TELEX.get(), RadioTelexScreen::new);
        sink.register(ModRadioMenus.AUTOCAL.get(), RadioAutocalScreen::new);
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
         HbmBlockEntityRenderers.regGem(sink, ModRadioBlockEntities.RADIO_TELEX.get(),
                 () -> new MachineObjRenderer<com.hbm.tileentity.network.TileEntityRadioTelex>(90F, 1F,
                         new ModelDef(dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/telex.obj"),
                                 dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/telex.png"), true)));
         HbmBlockEntityRenderers.regGem(sink, ModRadioBlockEntities.RADIO_AUTOCAL.get(),
                 () -> new MachineObjRenderer<com.hbm.tileentity.network.TileEntityRadioAUTOCAL>(90F, 1F,
                         new ModelDef(dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/autocal.obj"),
                                 dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/autocal.png"), true)));
    }
}
