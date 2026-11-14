package com.hbm.client;

import com.hbm.Constants;
import com.hbm.config.ClientConfig;
import com.hbm.config.GeneralConfig;
import com.hbm.config.MobConfig;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** 1.7's mode badges ({@code RenderScreenOverlay.renderBadges}): 528, expensive, rampant and all three (328), top left. */
public final class BadgeOverlay {

    private BadgeOverlay() {}

    private static final Identifier MISC = Ids.of(Constants.MOD_ID, "textures/misc/overlay_misc.png");

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientConfig.BADGES_HUD || mc.player == null || mc.options.hideGui) return;

        boolean true528 = GeneralConfig.true528();
        boolean trueExp = GeneralConfig.trueExp();
        boolean trueRam = MobConfig.trueRam();
        boolean true328 = true528 && trueExp && trueRam;

        int offsetX = 2;
        if (true528) offsetX = badge(graphics, offsetX, 218);
        if (trueExp) offsetX = badge(graphics, offsetX, 226);
        if (trueRam) offsetX = badge(graphics, offsetX, 234);
        if (true328) badge(graphics, offsetX, 242);
    }

    private static int badge(GuiGraphicsExtractor graphics, int x, int v) {
        GuiDraw.blit(graphics, MISC, x, 2, 0, v, 24, 8, 256, 256);
        return x + 26;
    }
}
