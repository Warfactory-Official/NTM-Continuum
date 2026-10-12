package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.inventory.fluid.FluidHazard;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * 1.7 {@code DiamondPronter.pront}: the NFPA 704 hazard diamond of a fluid ({@link FluidHazard}), a unit square in the
 * YZ plane facing +X, drawn on {@code danger_diamond.png} with blending and without lighting (1.7 called it between
 * {@code RenderHelper.disableStandardItemLighting} and its enable). The fluid tank, the barrels, the BAT-9000 and the
 * big tank place it with their own 1.7 transforms. A category value outside 0..5 is not drawn, as in 1.7.
 */
public final class DiamondPronter {

    private static final RenderType TYPE = CompatRenderTypes.entityTranslucentEmissive(
            Ids.of(Constants.MOD_ID, "textures/models/misc/danger_diamond.png"));

    private DiamondPronter() {}

    public static void pront(PoseStack poseStack, SubmitSink collector, FluidHazard hazard) {
        collector.submitCustomGeometry(poseStack, TYPE, (pose, buffer) -> draw(pose, buffer, hazard));
    }

    private static void draw(PoseStack.Pose pose, VertexConsumer buffer, FluidHazard h) {
        float pixel = 1F / 256F;
        float s = 1F / 139F;
        vertex(buffer, pose, 0F, 0.5F, -0.5F, 144 * pixel, 45 * pixel);
        vertex(buffer, pose, 0F, 0.5F, 0.5F, 5 * pixel, 45 * pixel);
        vertex(buffer, pose, 0F, -0.5F, 0.5F, 5 * pixel, 184 * pixel);
        vertex(buffer, pose, 0F, -0.5F, -0.5F, 144 * pixel, 184 * pixel);

        category(buffer, pose, h.poison(), 0F, 33F * s, pixel);
        category(buffer, pose, h.flammability(), 33F * s, 0F, pixel);
        category(buffer, pose, h.reactivity(), 0F, -33F * s, pixel);

        if (h.symbol() != FluidHazard.Symbol.NONE) {
            int x = symbolX(h.symbol());
            int y = symbolY(h.symbol());
            float size = 59F / 2F * s;
            float oY = -33F * s;
            vertex(buffer, pose, 0.01F, size + oY, -size, (x + 59) * pixel, y * pixel);
            vertex(buffer, pose, 0.01F, size + oY, size, x * pixel, y * pixel);
            vertex(buffer, pose, 0.01F, -size + oY, size, x * pixel, (y + 59) * pixel);
            vertex(buffer, pose, 0.01F, -size + oY, -size, (x + 59) * pixel, (y + 59) * pixel);
        }
    }

    private static void category(VertexConsumer buffer, PoseStack.Pose pose, int value, float oY, float oZ, float pixel) {
        if (value < 0 || value >= 6) return;
        float width = 10F / 139F;
        float height = 14F / 139F;
        int x = value == 0 ? 125 : 5 + (value - 1) * 24;
        int y = 5;
        vertex(buffer, pose, 0.01F, height + oY, -width + oZ, (x + 20) * pixel, y * pixel);
        vertex(buffer, pose, 0.01F, height + oY, width + oZ, x * pixel, y * pixel);
        vertex(buffer, pose, 0.01F, -height + oY, width + oZ, x * pixel, (y + 28) * pixel);
        vertex(buffer, pose, 0.01F, -height + oY, -width + oZ, (x + 20) * pixel, (y + 28) * pixel);
    }

    /** 1.7 {@code EnumSymbol.x}. */
    private static int symbolX(FluidHazard.Symbol symbol) {
        return switch (symbol) {
            case NONE -> 0;
            case RADIATION, NOWATER, ACID, ASPHYXIANT -> 195;
            case CRYOGENIC -> 134;
            case ANTIMATTER -> 73;
            case OXIDIZER -> 12;
        };
    }

    /** 1.7 {@code EnumSymbol.y}. */
    private static int symbolY(FluidHazard.Symbol symbol) {
        return switch (symbol) {
            case NONE -> 0;
            case RADIATION -> 2;
            case NOWATER -> 63;
            case ACID -> 124;
            case ASPHYXIANT, CRYOGENIC, ANTIMATTER, OXIDIZER -> 185;
        };
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v) {
        Vertexes.vertex(buffer, pose, x, y, z, 0xFFFFFFFF, u, v, OverlayTexture.NO_OVERLAY, 0x00F000F0, 0F, 1F, 0F);
    }
}
