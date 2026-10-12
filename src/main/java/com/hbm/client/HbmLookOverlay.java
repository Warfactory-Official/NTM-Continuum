package com.hbm.client;

import com.hbm.interfaces.IItemLookOverlay;
import com.hbm.interfaces.ILookOverlay;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Central dispatcher for {@link ILookOverlay}: each frame it finds the block (or {@link com.hbm.interfaces.IEntityLookOverlay}
 * entity) the player is looking at,
 * obtains its data, and renders that data. The 1.12.2 original ran this from a
 * {@code RenderGameOverlayEvent}; here each loader calls {@link #render} from its HUD-render hook
 * (NeoForge {@code RenderGuiEvent.Post}, Fabric {@code HudElementRegistry}).
 */
public final class HbmLookOverlay {

    private HbmLookOverlay() {}

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null || mc.options.hideGui) return;
        if (!com.hbm.config.ClientConfig.DODD_RBMK_DIAGNOSTIC) return; // 1.7: the whole hook sits behind this toggle

        HitResult hit = mc.hitResult;
        if (hit == null) return;

        // 1.7: an entity looked at (mop.entityHit instanceof ILookOverlay) gives its own readout
        if (hit.getType() == HitResult.Type.ENTITY) {
            ILookOverlay.Data data = com.hbm.interfaces.IEntityLookOverlay.of(
                    ((net.minecraft.world.phys.EntityHitResult) hit).getEntity(), mc.player);
            if (data != null) render(graphics, data);
            return;
        }
        if (hit.getType() != HitResult.Type.BLOCK) return;

        Level level = mc.level;
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof ILookOverlay overlay) {
            ILookOverlay.Data data = overlay.getLookOverlay(level, pos, mc.player);
            if (data != null) {
                render(graphics, data);
                return;
            }
        }
        ILookOverlay.Data provided = ILookOverlay.provided(level, pos, mc.player); // addon readouts (C-26)
        if (provided != null) {
            render(graphics, provided);
            return;
        }

        // 1.7 IItemLookOverlay: the held item may add its own readout (the conveyor wand's creative line break).
        ItemStack held = mc.player.getMainHandItem();
        if (held.getItem() instanceof IItemLookOverlay overlay) {
            ILookOverlay.Data data = overlay.getLookOverlay(level, pos, held, mc.player);
            if (data != null) render(graphics, data);
        }
    }

    private static void render(GuiGraphicsExtractor graphics, ILookOverlay.Data data) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int pX = mc.getWindow().getGuiScaledWidth() / 2 + 8;
        int pZ = mc.getWindow().getGuiScaledHeight() / 2;

        GuiDraw.text(graphics, font, data.title(), pX + 1, pZ - 9, opaque(data.backgroundColor()), false);
        GuiDraw.text(graphics, font, data.title(), pX, pZ - 10, opaque(data.titleColor()), false);

        for (String line : data.lines()) {
            int color = 0xFFFFFF;
            if (line.startsWith("&[")) {
                int end = line.lastIndexOf("&]");
                if (end > 2) {
                    try {
                        color = Integer.parseInt(line.substring(2, end));
                    } catch (NumberFormatException ignored) {}
                    line = line.substring(end + 2);
                }
            }
            GuiDraw.text(graphics, font, line, pX, pZ, opaque(color), true);
            pZ += 10;
        }
    }

    /** Forces a fully-opaque alpha channel for the legacy RGB values. */
    private static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }
}
