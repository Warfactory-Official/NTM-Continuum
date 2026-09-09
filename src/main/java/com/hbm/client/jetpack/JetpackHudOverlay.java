package com.hbm.client.jetpack;

import dev.golem.core.Ids;
import dev.golem.core.client.FrameTime;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

/** The old 50px compact and 80px large jetpack HUD layouts, including their needle transforms. */
public final class JetpackHudOverlay {

    private static final Identifier LARGE = Ids.of("hbm", "textures/gui/hud/jetpack_hud_large.png");
    private static final Identifier SMALL = Ids.of("hbm", "textures/gui/hud/jetpack_hud_small.png");
    private static final Identifier SMALL_TEXT = Ids.of("hbm", "textures/gui/hud/jetpack_hud_small_text.png");

    private JetpackHudOverlay() {}

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui) return;
        String kind = JetpackClientVisuals.kind(player);
        if (!JetpackClientVisuals.GLIDER.equals(kind)) return;
        JetpackClientBridge.State state = JetpackClientBridge.state(player);
        if (state == null) return;
        JetpackClientBridge.Fuel fuel = JetpackClientVisuals.fuel(player);
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        if (state.useCompactHUD()) compact(graphics, player, state, fuel, width, height);
        else large(graphics, player, state, fuel, width, height);
    }

    private static void compact(GuiGraphicsExtractor graphics, Player player,
            JetpackClientBridge.State state, JetpackClientBridge.Fuel fuel, int width, int height) {
        int hudYOffset = 45;
        float maxHeight = 0.78125F;
        int maxHeightPixels = Math.round(50F * maxHeight);
        int top = Math.round(height - maxHeightPixels - hudYOffset);
        GuiDraw.blitUv(graphics, SMALL, 0, top, 50, maxHeightPixels,
                0F, 1F - maxHeight, 1F, 1F, 256, 256);

        float originX = 31F / 256F * 50F + 40.5F * 50F / 256F;
        float originY = height - (maxHeightPixels - maxHeightPixels * 8F / 256F) - hudYOffset
                + 14.5F * maxHeightPixels / 256F;
        float thrust = interpolatedThrust(state);
        float thrustDegrees = Mth.clamp(thrust * 100F - 27F, -27F, 200F);
        GuiDraw.pushMatrix(graphics);
        GuiDraw.translate(graphics, originX + 117F / 256F * 50F, originY + 76F / 256F * maxHeightPixels);
        GuiDraw.rotate(graphics, (float) Math.toRadians(thrustDegrees));
        GuiDraw.translate(graphics, -originX, -originY);
        GuiDraw.blitUv(graphics, SMALL, Math.round(31F / 256F * 50F), Math.round(top + maxHeightPixels * 8F / 256F),
                Math.round((88F - 31F) / 256F * 50F), Math.round((31F - 8F) / 256F * 50F),
                31F / 256F, 8F / 256F, 88F / 256F, 31F / 256F, 256, 256);
        GuiDraw.popMatrix(graphics);

        float fuelDegrees = fraction(fuel) * 227F - 27F;
        GuiDraw.pushMatrix(graphics);
        GuiDraw.translate(graphics, originX, originY + 76F / 256F * maxHeightPixels);
        GuiDraw.rotate(graphics, (float) Math.toRadians(fuelDegrees));
        GuiDraw.translate(graphics, -originX, -originY);
        GuiDraw.blitUv(graphics, SMALL, Math.round(31F / 256F * 50F), Math.round(top + maxHeightPixels * 8F / 256F),
                Math.round((88F - 31F) / 256F * 50F), Math.round((31F - 8F) / 256F * 50F),
                31F / 256F, 8F / 256F, 88F / 256F, 31F / 256F, 256, 256);
        GuiDraw.popMatrix(graphics);

        float yOffset;
        float yPosition;
        float yHeight;
        if (state.failureTicks() > 0) {
            yOffset = 76F / 128F;
            yHeight = 104F / 128F - yOffset;
            yPosition = -0.5F;
        } else if (state.active()) {
            if (state.hover()) {
                yOffset = 25F / 128F;
                yHeight = 46F / 128F - yOffset;
                yPosition = -10.25F;
            } else {
                yOffset = 1F / 128F;
                yHeight = 23F / 128F - yOffset;
                yPosition = -14.75F;
            }
        } else {
            yOffset = 47F / 128F;
            yHeight = 75F / 128F - yOffset;
            yPosition = -6F;
        }
        GuiDraw.blitUv(graphics, SMALL_TEXT, 0,
                Math.round(height - 25F - (yOffset - yOffset * 25F) - yPosition - hudYOffset),
                50, Math.round(25F * yHeight), 0F, yOffset, 1F, yOffset + yHeight, 128, 128);
    }

    private static void large(GuiGraphicsExtractor graphics, Player player,
            JetpackClientBridge.State state, JetpackClientBridge.Fuel fuel, int width, int height) {
        int hudYOffset = 45;
        int top = height - 80 - hudYOffset;
        GuiDraw.blitUv(graphics, LARGE, 0, top, 80, 80, 0F, 0F, 0.5F, 1F, 512, 512);
        if (state.active()) GuiDraw.blitUv(graphics, LARGE, 6, height - 40 - hudYOffset,
                Math.round((172F - 40F) / 512F * 80F), 40,
                0.5F + 40F / 512F * 0.5F, 0.5F, 0.5F + 172F / 512F * 0.5F, 1F, 1024, 512, true);
        if (state.hover()) GuiDraw.blitUv(graphics, LARGE, Math.round(172F / 512F * 80F), height - 40 - hudYOffset,
                Math.round((282F - 172F) / 512F * 80F), 40,
                0.5F + 172F / 512F * 0.5F, 0.5F, 0.5F + 282F / 512F * 0.5F, 1F, 1024, 512, true);
        if (!state.hover()) GuiDraw.blitUv(graphics, LARGE, Math.round(282F / 512F * 80F), height - 40 - hudYOffset,
                Math.round((383F - 282F) / 512F * 80F), 40,
                0.5F + 282F / 512F * 0.5F, 0.5F, 0.5F + 383F / 512F * 0.5F, 1F, 1024, 512, true);
        if (state.failureTicks() > 0) GuiDraw.blitUv(graphics, LARGE, Math.round(383F / 512F * 80F), height - 40 - hudYOffset,
                Math.round((512F - 383F) / 512F * 80F), 40,
                0.5F + 383F / 512F * 0.5F, 0.5F, 1F, 1F, 1024, 512, true);

        int needleX = Math.round(49F / 512F * 80F);
        int needleY = Math.round(height - (80F - 148F / 512F * 80F) - hudYOffset);
        float originX = needleX + 61.5F * 80F / 512F;
        float originY = needleY + 11.5F * 80F / 512F;
        float thrustDegrees = Mth.clamp(interpolatedThrust(state) * 100F - 27F, -27F, 200F);
        GuiDraw.pushMatrix(graphics);
        GuiDraw.translate(graphics, originX, originY);
        GuiDraw.rotate(graphics, (float) Math.toRadians(thrustDegrees));
        GuiDraw.translate(graphics, -originX, -originY);
        GuiDraw.blitUv(graphics, LARGE, needleX, needleY,
                Math.round((134F - 49F) / 512F * 80F), Math.round((171F - 148F) / 512F * 80F),
                0.5F + 49F / 512F * 0.5F, 148F / 512F,
                0.5F + 134F / 512F * 0.5F, 171F / 512F, 512, 512);
        GuiDraw.popMatrix(graphics);

        float fuelDegrees = fraction(fuel) * 227F - 27F;
        GuiDraw.pushMatrix(graphics);
        GuiDraw.translate(graphics, originX + 80F * 179F / 512F, originY);
        GuiDraw.rotate(graphics, (float) Math.toRadians(fuelDegrees));
        GuiDraw.translate(graphics, -originX, -originY);
        GuiDraw.blitUv(graphics, LARGE, needleX, needleY,
                Math.round((134F - 49F) / 512F * 80F), Math.round((171F - 148F) / 512F * 80F),
                0.5F + 49F / 512F * 0.5F, 148F / 512F,
                0.5F + 134F / 512F * 0.5F, 171F / 512F, 512, 512);
        GuiDraw.popMatrix(graphics);

        if (fuel != null && fuel.type() != null && !fuel.type().isEmpty()) {
            Identifier sprite = Ids.of("hbm", "textures/gui/fluids/" + fuel.type().toLowerCase() + ".png");
            GuiDraw.blit(graphics, sprite, 67, height - 71 - hudYOffset, 0F, 0F, 7, 7, 16, 16);
        }
    }

    private static float interpolatedThrust(JetpackClientBridge.State state) {
        float partial = FrameTime.partialTick();
        return state.prevThrust() + (state.thrust() - state.prevThrust()) * partial;
    }

    private static float fraction(JetpackClientBridge.Fuel fuel) {
        if (fuel == null || fuel.capacity() <= 0) return 0F;
        return Mth.clamp(fuel.amount() / (float) fuel.capacity(), 0F, 1F);
    }
}
