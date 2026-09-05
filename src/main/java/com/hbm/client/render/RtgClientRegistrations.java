package com.hbm.client.render;

import com.hbm.client.HbmClientRegistrations;
import com.hbm.inventory.ModRtgMenus;
import com.hbm.inventory.gui.GUIMachineRTG;
import com.hbm.tileentity.ModRtgBlockEntities;

/** Client registration bridge kept with the feature instead of the shared registration tables. */
public final class RtgClientRegistrations {
    private RtgClientRegistrations() {}

    public static void registerScreen(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModRtgMenus.MACHINE_RTG.get(), GUIMachineRTG::new);
    }

    public static void registerRenderer(HbmBlockEntityRenderers.Sink sink) {
        sink.register(ModRtgBlockEntities.MACHINE_RTG.get(), context -> new RTGRenderer<>());
    }
}
