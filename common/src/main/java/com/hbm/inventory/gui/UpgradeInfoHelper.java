package com.hbm.inventory.gui;

import com.hbm.tileentity.IUpgradeInfoProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

/**
 * CLIENT-ONLY: resolves the {@link IUpgradeInfoProvider} of the currently open machine screen, so
 * {@code ItemMachineUpgrade} tooltips can print machine-specific numbers (the 1.12.2
 * {@code addInformation} screen probe). Referenced only from tooltip code paths, which never run on a
 * dedicated server — keeping the {@link Minecraft} touch lazily classloaded.
 */
public final class UpgradeInfoHelper {

    private UpgradeInfoHelper() {}

    public static @Nullable IUpgradeInfoProvider resolveProvider() {
        Screen open = Minecraft.getInstance().screen;
        if (open instanceof IUpgradeInfoProviderSource source) return source.getUpgradeInfoProvider();
        return null;
    }
}
