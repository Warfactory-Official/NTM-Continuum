package com.hbm.client;

import com.hbm.Constants;
import com.hbm.client.gun.GluonGunClient;
import com.hbm.client.gun.JadeShotgunClient;
import com.hbm.items.ModGunItems;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.impl.ItemGunEgon;
import com.hbm.items.weapon.sedna.impl.ItemGunStinger;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** The Sedna crosshair replacement from the source gun HUD path. */
public final class GunCrosshairOverlay {

    private static final Identifier MISC =
            Ids.of(Constants.MOD_ID, "textures/misc/overlay_misc.png");

    private GunCrosshairOverlay() {}

    /** Returns true when the main-hand gun owns the layer, even when it draws nothing. */
    public static boolean render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || (minecraft.options.hideGui && minecraft.screen == null)) return false;

        ItemStack stack = minecraft.player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemGunBaseNT gun)) return false;

        GunConfig config = gun.getConfig(stack, 0);
        if (gun instanceof ItemGunEgon) {
            GluonGunClient.renderCrosshair(graphics);
            return true;
        }
        if (gun == ModGunItems.gun_jshotgun.get()) {
            JadeShotgunClient.renderCrosshair(graphics);
            return true;
        }
        if (gun instanceof ItemGunStinger) {
            if (ItemGunBaseNT.aimingProgress < 1F) return true;
            renderCrosshair(graphics, config.getCrosshair(stack));
            renderStingerLockon(graphics, stack);
            return true;
        }
        if (config.getHideCrosshair(stack) && ItemGunBaseNT.aimingProgress >= 1F) return true;

        renderCrosshair(graphics, config.getCrosshair(stack));
        return true;
    }

    private static void renderCrosshair(GuiGraphicsExtractor graphics, Crosshair crosshair) {
        if (crosshair == null || crosshair == Crosshair.NONE) return;
        int x = graphics.guiWidth() / 2 - crosshair.size / 2;
        int y = graphics.guiHeight() / 2 - crosshair.size / 2;
        GuiDraw.crosshair(graphics, MISC, x, y, crosshair.x, crosshair.y,
                crosshair.size, crosshair.size, 256, 256);
    }

    private static void renderStingerLockon(GuiGraphicsExtractor graphics, ItemStack stack) {
        int progress = Math.max(0, Math.min(28,
                Math.round(ItemGunStinger.getLockonProgress(stack) * 28F / 60F)));
        int x = graphics.guiWidth() / 2 - 15;
        int y = graphics.guiHeight() / 2 + 18;
        GuiDraw.blit(graphics, MISC, x, y, 146, 18, 30, 10, 256, 256);
        GuiDraw.blit(graphics, MISC, x + 1, y + 1, 147, 29, progress, 8, 256, 256);
    }
}
