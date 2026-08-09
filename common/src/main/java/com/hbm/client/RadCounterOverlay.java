package com.hbm.client;

import com.hbm.Constants;
import com.hbm.items.ModGearItems;
import com.hbm.util.ContaminationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The radiation-survey HUD — a 94×18 bar (scaled to 1000 RAD accumulated) with warning icons + a RAD/s
 * readout, shown while the local player carries a geiger counter or dosimeter. Ported from 1.12.2's
 * {@code RenderScreenOverlay.renderRadCounter}: the bar geometry, the 74-px scale, the 1-second survey
 * sampling and the 2.5/10/25 RAD/s warning thresholds are copied. Registered per loader next to
 * {@link HbmLookOverlay} (NeoForge {@code RegisterGuiLayersEvent} / Fabric {@code HudElementRegistry}).
 */
public final class RadCounterOverlay {

    private RadCounterOverlay() {}

    private static final Identifier MISC = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/misc/overlay_misc.png");

    private static float lastRadResult, prevRadResult;
    private static long lastRadSurvey;

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null || mc.options.hideGui) return;
        if (!carriesCounter(mc.player)) return;

        float in = (float) ContaminationUtil.getActualPlayerRads(mc.player);

        // 1-second survey delta = RAD/s rate.
        float radiation = lastRadResult - prevRadResult;
        long now = System.currentTimeMillis();
        if (now >= lastRadSurvey + 1000) {
            lastRadSurvey = now;
            prevRadResult = lastRadResult;
            lastRadResult = in;
        }

        int length = 74;
        int bar = getScaled(in, 1000, length);

        int posX = 16;
        int posY = mc.getWindow().getGuiScaledHeight() - 20;

        // frame + fill
        graphics.blit(RenderPipelines.GUI_TEXTURED, MISC, posX, posY, 0, 0, 94, 18, 256, 256);
        if (bar > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, MISC, posX + 1, posY + 1, 1, 19, bar, 16, 256, 256);

        // warning icons
        int iconX = posX + length + 2 + 18;
        if (radiation >= 25) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, MISC, iconX, posY, 36, 36, 18, 18, 256, 256);
        } else if (radiation >= 10) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, MISC, iconX, posY, 18, 36, 18, 18, 256, 256);
        } else if (radiation >= 2.5) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, MISC, iconX, posY, 0, 36, 18, 18, 256, 256);
        }

        Font font = mc.font;
        if (radiation > 1000) {
            graphics.text(font, ">1000 RAD/s", posX, posY - 8, 0xFFFF0000, false);
        } else if (radiation >= 1) {
            graphics.text(font, Math.round(radiation) + " RAD/s", posX, posY - 8, 0xFFFFFF00, false);
        } else if (radiation > 0) {
            graphics.text(font, "<1 RAD/s", posX, posY - 8, 0xFF00FF00, false);
        }
    }

    private static boolean carriesCounter(Player player) {
        Item geiger = ModGearItems.geiger_counter.get();
        Item dosimeter = ModGearItems.dosimeter.get();
        for (ItemStack stack : player.getInventory()) {
            if (stack.is(geiger) || stack.is(dosimeter)) return true;
        }
        return false;
    }

    private static int getScaled(float value, int max, int length) {
        int scaled = (int) (value / max * length);
        if (scaled < 0) return 0;
        return Math.min(scaled, length);
    }
}
