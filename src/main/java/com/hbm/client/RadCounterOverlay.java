package com.hbm.client;

import com.hbm.Constants;
import com.hbm.capability.HbmLivingProps;
import com.hbm.config.ClientConfig;
import com.hbm.items.ModGearItems;
import com.hbm.items.armor.ArmorFSB;
import dev.golem.core.Ids;
import dev.golem.core.data.Inventories;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7's geiger counter HUD ({@code RenderScreenOverlay.renderRadCounter}): the bar is the player's accumulated
 * radiation out of 1000 RAD, the RAD/s readout its change over the last one-second survey.
 */
public final class RadCounterOverlay {

    private RadCounterOverlay() {}

    private static final Identifier MISC = Ids.of(Constants.MOD_ID, "textures/misc/overlay_misc.png");

    private static float lastResult, prevResult;
    private static long lastSurvey;

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null || mc.options.hideGui) return;
        if (!carriesCounter(mc.player) || hasCustomGeiger(mc.player)) return;

        float in = (float) HbmLivingProps.getRadiation(mc.player);

        float radiation = lastResult - prevResult;
        long now = System.currentTimeMillis();
        if (now >= lastSurvey + 1000) {
            lastSurvey = now;
            prevResult = lastResult;
            lastResult = in;
        }

        int length = 74;
        int bar = getScaled(in, 1000, length);

        int posX = 16 + ClientConfig.GEIGER_OFFSET_HORIZONTAL;
        int posY = mc.getWindow().getGuiScaledHeight() - 20 - ClientConfig.GEIGER_OFFSET_VERTICAL;

        GuiDraw.blit(graphics, MISC, posX, posY, 0, 0, 94, 18, 256, 256);
        if (bar > 0) GuiDraw.blit(graphics, MISC, posX + 1, posY + 1, 1, 19, bar, 16, 256, 256);

        if (radiation >= 25) GuiDraw.blit(graphics, MISC, posX + length + 2, posY - 18, 36, 36, 18, 18, 256, 256);
        else if (radiation >= 10) GuiDraw.blit(graphics, MISC, posX + length + 2, posY - 18, 18, 36, 18, 18, 256, 256);
        else if (radiation >= 2.5) GuiDraw.blit(graphics, MISC, posX + length + 2, posY - 18, 0, 36, 18, 18, 256, 256);

        Font font = mc.font;
        if (radiation > 1000) GuiDraw.text(graphics, font, ">1000 RAD/s", posX, posY - 8, 0xFFFF0000, false);
        else if (radiation >= 1) GuiDraw.text(graphics, font, Math.round(radiation) + " RAD/s", posX, posY - 8, 0xFFFF0000, false);
        else if (radiation > 0) GuiDraw.text(graphics, font, "<1 RAD/s", posX, posY - 8, 0xFFFF0000, false);
    }

    private static boolean carriesCounter(Player player) {
        Item geiger = ModGearItems.geiger_counter.get();
        for (ItemStack stack : Inventories.main(player.getInventory())) if (stack.is(geiger)) return true;
        return false;
    }

    /** A full set with its own geiger display (the HEV) hides this one. */
    private static boolean hasCustomGeiger(Player player) {
        return ArmorFSB.hasFSBArmor(player)
                && player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmorFSB chest && chest.customGeiger;
    }

    private static int getScaled(double cur, double max, double scale) {
        return (int) Math.min(cur / max * scale, scale);
    }
}
