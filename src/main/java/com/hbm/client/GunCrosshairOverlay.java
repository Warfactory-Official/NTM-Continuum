package com.hbm.client;

import com.hbm.Constants;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** The Sedna crosshair replacement from the old {@code ItemGunBaseNT.renderHUD}. */
public final class GunCrosshairOverlay {

    private static final Identifier MISC =
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/misc/overlay_misc.png");

    private GunCrosshairOverlay() {}

    /** Returns true when a held gun owns the crosshair layer, even when aiming hides its graphic. */
    public static boolean render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null
                || minecraft.options.hideGui) {
            return false;
        }

        ItemStack stack = minecraft.player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemGunBaseNT)) stack = minecraft.player.getOffhandItem();
        if (!(stack.getItem() instanceof ItemGunBaseNT gun)) return false;

        GunConfig config = gun.getConfig(stack, 0);
        if (config.getHideCrosshair(stack) && ItemGunBaseNT.aimingProgress >= 1F) return true;

        Crosshair crosshair = config.getCrosshair(stack);
        if (crosshair == null || crosshair == Crosshair.NONE) return true;

        int x = minecraft.getWindow().getGuiScaledWidth() / 2 - crosshair.size / 2;
        int y = minecraft.getWindow().getGuiScaledHeight() / 2 - crosshair.size / 2;
        dev.golem.core.gui.GuiDraw.crosshair(graphics, MISC, x, y, crosshair.x, crosshair.y,
                crosshair.size, crosshair.size, 256, 256);
        return true;
    }
}
