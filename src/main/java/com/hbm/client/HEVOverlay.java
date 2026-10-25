package com.hbm.client;

import com.hbm.capability.HbmLivingProps;
import com.hbm.items.armor.ArmorFSBPowered;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code ArmorHEV.renderOverlay}: the Half-Life HEV readout that replaces hearts and armour.
 *
 * <p>Layout, scale and colours are upstream's: health as {@code +HP*5} and suit charge as
 * {@code ||charge*25} at double scale on the hotbar line, the ten-cell radiation gauge above them, and the
 * per-second radiation delta at normal scale. Upstream surveys the dose once per wall-clock second and
 * shows the difference between the last two surveys, so the delta lags by up to a second; this is kept.
 *
 * <p>Text is drawn without a shadow, as 1.7's four-argument {@code drawString} was. Colours carry full
 * alpha because 26.1 treats a zero alpha byte as transparent.
 */
public final class HEVOverlay {

    private HEVOverlay() {}

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    private static long lastSurvey;
    private static float prevResult;
    private static float lastResult;

    public static void render(Object target, Player player) {
        GuiGraphicsExtractor graphics = (GuiGraphicsExtractor) target;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int height = mc.getWindow().getGuiScaledHeight();

        float in = (float) HbmLivingProps.getRadiation(player);
        float radiation = lastResult - prevResult;

        if (System.currentTimeMillis() >= lastSurvey + 1000) {
            lastSurvey = System.currentTimeMillis();
            prevResult = lastResult;
            lastResult = in;
        }

        double scale = 2D;
        GuiDraw.pushMatrix(graphics);
        GuiDraw.scale(graphics, (float) scale, (float) scale);

        int hX = (int) (8 / scale);
        int hY = (int) ((height - 18 - 2) / scale);
        int healthColor = player.getHealth() * 5 > 15 ? 0xFFFF8000 : 0xFFFF0000;
        GuiDraw.text(graphics, font, "+" + (int) (player.getHealth() * 5), hX, hY, healthColor, false);

        double c = 0D;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.getItem() instanceof ArmorFSBPowered item) {
                c += (double) item.getCharge(armor) / (double) item.getMaxCharge(armor);
            }
        }

        int aX = (int) (70 / scale);
        int aY = (int) ((height - 18 - 2) / scale);
        int armorColor = c * 25 > 15 ? 0xFFFF8000 : 0xFFFF0000;
        GuiDraw.text(graphics, font, "||" + (int) (c * 25), aX, aY, armorColor, false);

        StringBuilder rad = new StringBuilder("☢ [");
        for (int i = 0; i < 10; i++) {
            if (in / 100 > i) {
                int mid = (int) (in - i * 100);
                if (mid < 33) rad.append("..");
                else if (mid < 67) rad.append("|.");
                else rad.append("||");
            } else {
                rad.append(' ');
            }
        }
        rad.append(']');

        int rX = (int) (8 / scale);
        int rY = (int) ((height - 40) / scale);
        int radColor = in < 800 ? 0xFFFF8000 : 0xFFFF0000;
        GuiDraw.text(graphics, font, rad.toString(), rX, rY, radColor, false);
        GuiDraw.popMatrix(graphics);

        if (radiation > 0) {
            int dX = 32;
            int dY = height - 55;
            String delta = "" + Math.round(radiation);
            if (radiation > 1000) delta = ">1000";
            else if (radiation < 1) delta = "<1";
            GuiDraw.text(graphics, font, delta + " RAD/s", dX, dY, 0xFFFF0000, false);
        }
    }
}
