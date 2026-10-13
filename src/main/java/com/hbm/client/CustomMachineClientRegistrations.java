package com.hbm.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.RenderCustomMachine;
import com.hbm.config.CustomMachineConfigJSON;
import com.hbm.inventory.ModCustomMachineMenus;
import com.hbm.inventory.gui.GUIMachineCustom;
import com.hbm.tileentity.ModCustomMachineBlockEntities;
import net.minecraft.client.Minecraft;

/** The custom machine controller's screen and structure-ghost renderer, and the client language for its names. */
public final class CustomMachineClientRegistrations {

    private CustomMachineClientRegistrations() {}

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModCustomMachineMenus.CUSTOM_MACHINE.get(), GUIMachineCustom::new);
        // 1.7 MainRegistry.proxy.getLanguageCode(), read by the controller item's name and the GUI title
        CustomMachineConfigJSON.languageCode = () -> Minecraft.getInstance().getLanguageManager().getSelected();
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        sink.register(ModCustomMachineBlockEntities.CUSTOM_MACHINE.get(), ctx -> new RenderCustomMachine());
    }
}
