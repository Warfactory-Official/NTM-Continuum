package com.hbm.client;

import com.hbm.wiaj.GuiWorldInAJar;
import com.hbm.wiaj.cannery.CanneryBase;
import com.hbm.wiaj.cannery.Jars;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.KeyMappings;
import dev.golem.core.client.Tooltips;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The way into a presentation: 1.7 {@code ModEventHandlerClient}'s cannery tooltip line and its
 * Left Shift + QMAW-key check.
 *
 * <p>An item with a presentation gets a green {@code cannery.f1} line naming both keys. While a screen is
 * open, holding Left Shift and pressing the QMAW key opens the presentation of whatever was last hovered
 * in that screen; the plain QMAW key keeps opening the manual ({@link QMAWClient} yields to Shift, as the
 * 1.7 handler did). Both keys are polled physically, because a screen owns key dispatch and 1.20.1 hands
 * a shared key to only one mapping. That is the reason {@link QMAWClient} polls too.
 */
public final class CanneryClient {

    /** 1.7 accepted a tooltip seen within the last 100 ms. */
    private static final long TOOLTIP_WINDOW_MS = 100L;

    private static CanneryBase lastCannery;
    private static Screen lastCanneryScreen;
    private static long canneryTimestamp;

    private CanneryClient() {}

    public static void register() {
        Tooltips.onItemTooltip(CanneryClient::addTooltip);
        ClientTicks.onEndTick(CanneryClient::openPending);
    }

    /** Whether Left Shift is physically held; the QMAW key then belongs to the presentation. */
    public static boolean shiftHeld() {
        return KeyMappings.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT);
    }

    private static void addTooltip(ItemStack stack, List<Component> lines) {
        CanneryBase cannery;
        try {
            cannery = Jars.get(stack);
        } catch (Exception ex) {
            lines.add(Component.literal("Error loading cannery: " + ex.getLocalizedMessage()).withStyle(ChatFormatting.RED));
            return;
        }
        if (cannery == null || HbmKeybindsClient.qmawKey == null) return;
        Component keys = Component.empty()
                .append(Component.translatable("key.keyboard.left.shift"))
                .append(" + ")
                .append(HbmKeybindsClient.qmawKey.getTranslatedKeyMessage());
        lines.add(Component.translatable("cannery.f1", keys).withStyle(ChatFormatting.GREEN));
        lastCannery = cannery;
        lastCanneryScreen = Minecraft.getInstance().screen;
        canneryTimestamp = System.currentTimeMillis();
    }

    private static void openPending() {
        Minecraft mc = Minecraft.getInstance();
        KeyMappings.Mapping key = HbmKeybindsClient.qmawKey;
        if (key == null) return;
        // Checked on every tick both keys are held, as 1.7 did: the hovered stack's tooltip has to have been
        // drawn within the last 100 ms, and at a low frame rate that can take more than the first tick of
        // the press. Opening replaces the screen the tooltip belonged to, so this cannot fire twice.
        if (!KeyMappings.isHeld(key) || !shiftHeld()) return;
        if (mc.screen == null || mc.player == null) return;
        CanneryBase cannery = canneryTimestamp > System.currentTimeMillis() - TOOLTIP_WINDOW_MS
                && mc.screen == lastCanneryScreen ? lastCannery : null;
        if (cannery == null) return;
        mc.player.closeContainer();
        GuiWorldInAJar.open(cannery);
    }
}
