package com.hbm.client;

import com.hbm.inventory.ModRBMKMenus;
import com.hbm.inventory.gui.*;

/** RBMK client registrations stay with the RBMK feature instead of growing the global registry. */
public final class RBMKClientScreens {
    private RBMKClientScreens() {}
    public static void register(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModRBMKMenus.RBMK_ROD.get(), ScreenRBMKRod::new);
        sink.register(ModRBMKMenus.RBMK_CONTROL.get(), ScreenRBMKControl::new);
        sink.register(ModRBMKMenus.RBMK_CONTROL_AUTO.get(), ScreenRBMKControlAuto::new);
        sink.register(ModRBMKMenus.RBMK_BOILER.get(), ScreenRBMKBoiler::new);
        sink.register(ModRBMKMenus.RBMK_STORAGE.get(), ScreenRBMKStorage::new);
        sink.register(ModRBMKMenus.RBMK_HEATER.get(), ScreenRBMKHeater::new);
        sink.register(ModRBMKMenus.RBMK_OUTGASSER.get(), ScreenRBMKOutgasser::new);
        sink.register(ModRBMKMenus.RBMK_CONSOLE.get(), ScreenRBMKConsole::new);
        sink.register(ModRBMKMenus.RBMK_AUTOLOADER.get(), ScreenRBMKAutoloader::new);
        // The ROR panels are screwdriver-opened client screens, not menus.
        com.hbm.blocks.machine.rbmk.RBMKKeyPad.OPEN_SCREEN = panel -> net.minecraft.client.Minecraft.getInstance().setScreen(new ScreenRBMKKeyPad(panel));
        com.hbm.blocks.machine.rbmk.RBMKLever.OPEN_SCREEN = panel -> net.minecraft.client.Minecraft.getInstance().setScreen(new ScreenRBMKLever(panel));
        com.hbm.blocks.machine.rbmk.RBMKNumitron.OPEN_SCREEN = panel -> net.minecraft.client.Minecraft.getInstance().setScreen(new ScreenRBMKNumitron(panel));
        com.hbm.blocks.machine.rbmk.RBMKGraph.OPEN_SCREEN = panel -> net.minecraft.client.Minecraft.getInstance().setScreen(new ScreenRBMKGraph(panel));
        com.hbm.blocks.machine.rbmk.RBMKIndicator.OPEN_SCREEN = panel -> net.minecraft.client.Minecraft.getInstance().setScreen(new ScreenRBMKIndicator(panel));
    }
}
