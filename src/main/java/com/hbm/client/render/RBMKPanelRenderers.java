package com.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.gui.Font;
import net.minecraft.core.Direction;
import net.minecraft.util.FormattedCharSequence;

/** Pieces every 1.7 RBMK console panel renderer repeated: the facing turn and the scaled label. */
final class RBMKPanelRenderers {
    private RBMKPanelRenderers() {}

    /** The 1.7 metadata switch: meta 2 (north) 90, 4 (west) 180, 3 (south) 270, 5 (east) 0. */
    static float yaw(Direction facing) {
        return switch (facing) { case NORTH -> 90F; case WEST -> 180F; case SOUTH -> 270F; default -> 0F; };
    }

    /** {@code glColor3f(r * mult, g * mult, b * mult)} as an opaque ARGB tint. */
    static int scaled(int rgb, float mult) {
        int r = (int) ((rgb >> 16 & 255) * mult), g = (int) ((rgb >> 8 & 255) * mult), b = (int) ((rgb & 255) * mult);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /**
     * The shared label pass: {@code scale = min(0.0125, fit / width)}, flipped Y, turned 90 degrees onto
     * the panel's face and centred on the current point.
     */
    static void label(SubmitSink sink, PoseStack pose, Font font, FormattedCharSequence text, float fit, int color, int light) {
        int width = font.width(text);
        float scale = Math.min(.0125F, fit / Math.max(width, 1));
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.YP.rotationDegrees(90F));
        sink.submitText(pose, font, -width / 2F, -font.lineHeight / 2F, text, false, Font.DisplayMode.NORMAL, light, color, 0);
    }
}
