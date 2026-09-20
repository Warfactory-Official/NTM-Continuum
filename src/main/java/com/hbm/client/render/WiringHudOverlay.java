package com.hbm.client.render;

import com.hbm.items.tool.ItemWiring;
import com.hbm.blocks.ModEnergyNetContent;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * The cable drum's readout: 1.7 pushed "{@code <name>: <n>m}" (the distance back to the started pylon)
 * through the info system every tick while a wire was begun. The info system is not ported; this draws
 * that one line where its default position was (15, 15) with the same dim backing.
 */
public final class WiringHudOverlay {

    private WiringHudOverlay() {}

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null || mc.options.hideGui) return;

        ItemStack stack = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof ItemWiring)) stack = mc.player.getItemInHand(InteractionHand.OFF_HAND);
        if (!(stack.getItem() instanceof ItemWiring)) return;
        BlockPos start = ModEnergyNetContent.WIRE_START.get(stack).orElse(null);
        if (start == null) return;

        double dx = mc.player.getX() - start.getX();
        double dy = mc.player.getY() - start.getY();
        double dz = mc.player.getZ() - start.getZ();
        String text = stack.getHoverName().getString() + ": " + ((int) Math.sqrt(dx * dx + dy * dy + dz * dz)) + "m";

        Font font = mc.font;
        int pX = 15, pZ = 15;
        int longest = font.width(text);
        GuiDraw.fill(graphics, pX - 5, pZ - 5, pX + 5 + longest, pZ + 12, 0x80404040);
        GuiDraw.text(graphics, font, text, pX, pZ, 0xFFFFFFFF, false);
    }
}
