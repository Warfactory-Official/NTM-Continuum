package com.hbm.client;

import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The old full-screen scope pass, kept separate from the crosshair layer.
 *
 * <p>The scope sits in a fixed 16:9 window of its texture: the screen's shorter side always shows
 * 9/16 of the image, and the longer side shows however much more its aspect ratio asks for. The
 * result is a UV rectangle, not a texel one, so it goes through
 * {@link GuiDraw#blitUvFullscreen} rather than a blit that would need the texture's own size.
 */
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
        double narrow = 9D / 16D;
        double wide = (double) Math.max(width, height) / Math.min(width, height) * narrow;
        double uSpan = width < height ? narrow : wide;
        double vSpan = height < width ? narrow : wide;

        GuiDraw.blitUvFullscreen(graphics, texture,
                (float) (0.5D - uSpan / 2D), (float) (0.5D - vSpan / 2D),
                (float) (0.5D + uSpan / 2D), (float) (0.5D + vSpan / 2D));
    }
}
