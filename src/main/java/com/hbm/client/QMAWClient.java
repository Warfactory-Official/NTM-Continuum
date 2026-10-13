package com.hbm.client;

import com.hbm.Constants;
import com.hbm.qmaw.GuiQMAW;
import com.hbm.qmaw.QMAWLoader;
import com.hbm.qmaw.QuickManualAndWiki;
import dev.golem.core.client.ClientResourceReloads;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.KeyMappings;
import dev.golem.core.client.Tooltips;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Client registration and the short tooltip-to-F1 hand-off used by QMAW. */
public final class QMAWClient {
    private static QuickManualAndWiki lastManual;
    private static Screen lastManualScreen;
    private static boolean qmawWasDown;
    private static boolean reloadPending;

    private QMAWClient() {}

    public static void register() {
        ClientResourceReloads.register(Constants.MOD_ID, "qmaw",
                () -> {
                    lastManual = null;
                    lastManualScreen = null;
                    qmawWasDown = false;
                    // NeoForge fires this listener before deferred items are visible through the built-in
                    // registry. Resolve trigger stacks on the following client tick, after registration.
                    reloadPending = true;
                });
        Tooltips.onItemTooltip(QMAWClient::addTooltip);
        ClientTicks.onEndTick(QMAWClient::openPending);
    }

    private static void addTooltip(ItemStack stack, List<Component> lines) {
        QuickManualAndWiki manual = QMAWLoader.find(stack);
        if (manual == null || HbmKeybindsClient.qmawKey == null) {
            lastManual = null;
            lastManualScreen = null;
            return;
        }
        // 1.7 ModEventHandlerClient: EnumChatFormatting.YELLOW (green is the cannery "presentation" line)
        lines.add(Component.translatable("qmaw.tab", HbmKeybindsClient.qmawKey.getTranslatedKeyMessage())
                .withStyle(ChatFormatting.YELLOW));
        lastManual = manual;
        lastManualScreen = Minecraft.getInstance().screen;
    }

    private static void openPending() {
        Minecraft mc = Minecraft.getInstance();
        if (reloadPending && mc.player != null) {
            reloadPending = false;
            QMAWLoader.reload(mc.getResourceManager());
        }
        KeyMappings.Mapping key = HbmKeybindsClient.qmawKey;
        if (key == null) return;
        // F1 is also vanilla's hide-GUI key. Its KeyMapping dispatch state is not reliable while an
        // inventory screen owns input, so poll the mapping's current physical binding directly.
        boolean down = KeyMappings.isHeld(key);
        if (!down) {
            qmawWasDown = false;
            return;
        }
        // Polling the physical mapping keeps F1 working on 1.20.1, where the vanilla dispatch map only
        // retains one mapping when two actions share a key. The edge is the old key-down debounce.
        if (qmawWasDown) return;
        qmawWasDown = true;
        // 1.7 opened the manual only without Left Shift; with it the key opens a presentation (CanneryClient).
        if (CanneryClient.shiftHeld()) return;
        if (mc.screen == null || mc.screen != lastManualScreen || mc.player == null || lastManual == null) return;
        mc.player.closeContainer();
        mc.setScreen(new GuiQMAW(lastManual));
    }
}
