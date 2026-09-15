package com.hbm.client;

import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** The old full-screen scope pass, kept separate from the crosshair layer. */
public final class GunScopeOverlay {

    private GunScopeOverlay() {}

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || (minecraft.options.hideGui && minecraft.screen == null)) return;

        ItemStack stack = minecraft.player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemGunBaseNT gun)) return;
        if (ItemGunBaseNT.prevAimingProgress != 1F || ItemGunBaseNT.aimingProgress != 1F
                || ItemGunBaseNT.prevAimingProgress != ItemGunBaseNT.aimingProgress) return;

        GunConfig config = gun.getConfig(stack, 0);
        Identifier texture = config.getScopeTexture(stack);
        if (texture == null) return;

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        double smallest = Math.min(width, height);
        double divisor = smallest / (9D / 16D);
        double largest = Math.max(width, height) / divisor;
        double vMin = 0.5D - (height < width ? (9D / 16D) : largest) / 2D;
        double vMax = 0.5D + (height < width ? (9D / 16D) : largest) / 2D;
        double uMin = width < height ? vMin : 0.5D - largest / 2D;
        double uMax = width < height ? vMax : 0.5D + largest / 2D;

        GuiDraw.blitUv(graphics, texture, 0, 0, width, height,
                (float) uMin, (float) vMin, (float) uMax, (float) vMax, 1, 1);
    }
}
