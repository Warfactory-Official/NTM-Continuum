package com.hbm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class HbmLookOverlay {

    private HbmLookOverlay() {}

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null || mc.options.hideGui) return;

        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        Level level = mc.level;
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof ILookOverlay overlay) {
            overlay.printHook(graphics, level, pos);
        }
    }
}
