package com.hbm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.LivingEntity;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 1.7's {@code BossStatus}, which HBM's boss renderers (Quackos, UFO, Maskman, hunter chopper, Balls-O-Tron head)
 * fed instead of a server boss event: each frame a boss is <em>rendered</em> its renderer calls
 * {@link #set}, which stores the name and health and resets {@code statusBarTime} to 100; the HUD draws the one
 * bar while {@code statusBarTime > 0} and counts it down by one per HUD frame. So the bar shows only while a boss
 * is on screen, disappears about 100 frames after it leaves view, unloads or dies, and there is only one bar (the
 * last boss rendered wins).
 *
 * <p>The port used server boss events tied to entity tracking until History §19ek-small. A boss the client no
 * longer drew (Quackos cannot be killed and keeps teleporting back from the void) kept its bar for as long as the
 * server tracked it. The bar itself is still vanilla's overlay: one fixed id is added, replaced and removed
 * client-side through {@code BossHealthOverlay.update}, so it looks like every other boss bar.
 */
public final class BossStatus {

    private BossStatus() {}

    private static final UUID BAR = UUID.nameUUIDFromBytes("hbm:boss_status".getBytes(StandardCharsets.UTF_8));

    private static Component bossName;
    private static float healthScale;
    private static BossEvent.BossBarColor color = BossEvent.BossBarColor.PINK;
    private static int statusBarTime;

    /** What vanilla's overlay currently holds under {@link #BAR}; null when nothing. */
    private static Component shownName;
    private static float shownHealth;
    private static BossEvent.BossBarColor shownColor;
    private static ClientLevel shownLevel;

    /** 1.7 {@code BossStatus.setBossStatus(boss, true)}, called from the boss's renderer for every frame it draws. */
    public static void set(LivingEntity boss, BossEvent.BossBarColor barColor) {
        float max = boss.getMaxHealth();
        healthScale = max > 0F ? Math.max(0F, Math.min(1F, boss.getHealth() / max)) : 0F;
        bossName = boss.getDisplayName();
        color = barColor;
        statusBarTime = 100;
    }

    /** 1.7 {@code GuiIngame.renderBossHealth}: one HUD frame. */
    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        // vanilla clears its overlay on login and respawn (a new ClientLevel); forget what it held
        if (mc.level != shownLevel) {
            shownLevel = mc.level;
            shownName = null;
        }
        if (bossName != null && statusBarTime > 0 && mc.level != null) {
            --statusBarTime;
            if (shownName == null || !bossName.equals(shownName) || healthScale != shownHealth || color != shownColor) {
                Bar bar = new Bar(bossName, healthScale, color);
                mc.gui.getBossOverlay().update(ClientboundBossEventPacket.createAddPacket(bar));
                shownName = bossName;
                shownHealth = healthScale;
                shownColor = color;
            }
        } else if (shownName != null) {
            mc.gui.getBossOverlay().update(ClientboundBossEventPacket.createRemovePacket(BAR));
            shownName = null;
        }
    }

    /** True while the HBM bar is in vanilla's overlay (harness query). */
    public static boolean showing() {
        return shownName != null;
    }

    /** The name on the HBM bar, or null (harness query). */
    public static Component shownName() {
        return shownName;
    }

    private static final class Bar extends BossEvent {
        Bar(Component name, float progress, BossBarColor color) {
            super(BAR, name, color, BossBarOverlay.PROGRESS);
            setProgress(progress);
        }
    }
}
