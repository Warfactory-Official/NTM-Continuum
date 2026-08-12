package com.hbm.client;

import com.hbm.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * The two screen-space effects of a nearby nuclear detonation, driven by timestamps the
 * {@code TorexRenderer} latches (mirroring 1.12.2's {@code ModEventHandlerClient}):
 * <ul>
 *   <li><b>Flash</b> — a full-screen white-out fading over {@link #FLASH_DURATION} ms, drawn here as a
 *       HUD layer ({@link #render}).</li>
 *   <li><b>Shake</b> — a HUD/camera wobble over {@link #SHAKE_DURATION} ms; the offsets ({@link #shakeOffsetX}/
 *       {@link #shakeOffsetY}) are applied by {@code MixinGui} wrapping {@code Gui.render}.</li>
 * </ul>
 * Both are gated by {@link ClientConfig}. Timestamps are epoch-millis so they decay in real time exactly
 * like the original (independent of tick rate).
 */
public final class NukeScreenEffects {

    private NukeScreenEffects() {}

    public static final int FLASH_DURATION = 5_000;
    public static final int SHAKE_DURATION = 1_500;

    /** Set by the renderer during the flash window; drives the white-out fade. */
    public static long flashTimestamp;
    /** Set once by the renderer when the shockwave hits; drives the HUD shake. */
    public static long shakeTimestamp;

    // ---- Flash (HUD layer) ----

    public static void render(GuiGraphicsExtractor graphics) {
        if (!ClientConfig.NUKE_HUD_FLASH) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

        long remaining = flashTimestamp + FLASH_DURATION - System.currentTimeMillis();
        if (remaining <= 0) return;

        // Plain translucent white fade (user-preferred over the additive/below-HUD variants tried
        // against the old (SRC_ALPHA, ONE) overlay — the additive versions read worse in playtests).
        float brightness = remaining / (float) FLASH_DURATION;
        int alpha = (int) (Mth.clamp(brightness, 0F, 1F) * 255F);
        int color = (alpha << 24) | 0xFFFFFF;

        // Overfill by 200px each side so the shake (which also moves this layer) never bares an edge.
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        graphics.fill(-200, -200, w + 200, h + 200, color);
    }

    // ---- Shake (applied by MixinGui) ----

    public static boolean isShakeActive() {
        if (!ClientConfig.NUKE_HUD_SHAKE) return false;
        return shakeTimestamp + SHAKE_DURATION - System.currentTimeMillis() > 0;
    }

    private static double shakeMult() {
        return (shakeTimestamp + SHAKE_DURATION - System.currentTimeMillis()) / (double) SHAKE_DURATION * 2;
    }

    public static double shakeOffsetX() {
        double now = System.currentTimeMillis();
        return Mth.clamp(Math.sin(now * 0.02), -0.7, 0.7) * 15 * shakeMult();
    }

    public static double shakeOffsetY() {
        double now = System.currentTimeMillis();
        return Mth.clamp(Math.sin(now * 0.01 + 2), -0.7, 0.7) * 3 * shakeMult();
    }
}
