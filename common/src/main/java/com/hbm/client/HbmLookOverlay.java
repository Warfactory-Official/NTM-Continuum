package com.hbm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Central dispatcher for {@link ILookOverlay}: each frame it finds the block the player is looking at and,
 * if that block implements {@link ILookOverlay}, lets it draw its info overlay. The 1.12.2 original ran
 * this from a {@code RenderGameOverlayEvent}; here each loader calls {@link #render} from its HUD-render
 * hook (NeoForge {@code RenderGuiEvent.Post}, Fabric {@code HudElementRegistry}).
 */
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
