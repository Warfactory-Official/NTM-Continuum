package com.hbm.client;

import com.hbm.items.machine.ItemPWRPrinter;
import com.hbm.inventory.ModPwrMenus;
import net.minecraft.client.Minecraft;
import com.hbm.inventory.gui.GUIPWR;
import com.hbm.inventory.gui.GUIScreenSlicePrinter;

/** Client-only registrations for the PWR: the controller screen and the printer's slice screen hook. */
public final class PwrClientRegistrations {
    private PwrClientRegistrations() {}

    public static void registerScreens(dev.golem.core.client.ClientRegistry.ScreenSink sink) {
        sink.register(ModPwrMenus.PWR_CONTROLLER.get(), GUIPWR::new);
        ItemPWRPrinter.OPEN_SCREEN = (x1, y1, z1, x2, y2, z2, dir, whitelist) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;
            if (!(minecraft.player.getMainHandItem().getItem() instanceof ItemPWRPrinter)) return;
            minecraft.setScreen(new GUIScreenSlicePrinter(x1, y1, z1, x2, y2, z2, dir, whitelist));
        };
    }
}
