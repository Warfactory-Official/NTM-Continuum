package com.hbm.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.tileentity.ModSecurityBlockEntities;
import com.hbm.inventory.ModSecurityMenus;
import com.hbm.client.render.SecurityObjRenderer;
import com.hbm.inventory.gui.GUIMachineKeyForge;

/** Client hooks for the security family: the locksmith screen and the barbed wire / spikes OBJ renderer. */
public final class SecurityClientRegistrations {
    private SecurityClientRegistrations() {}

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModSecurityMenus.MACHINE_KEYFORGE.get(), GUIMachineKeyForge::new);
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        HbmBlockEntityRenderers.regGem(sink, ModSecurityBlockEntities.SECURITY_OBJ.get(), SecurityObjRenderer::new);
    }
}
