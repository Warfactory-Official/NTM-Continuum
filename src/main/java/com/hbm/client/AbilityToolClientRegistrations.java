package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.client.render.RenderInfoSystem;
import com.hbm.handler.ability.IBaseAbility;
import com.hbm.handler.ability.IToolAreaAbility;
import com.hbm.handler.ability.ToolPreset;
import com.hbm.inventory.gui.GUIScreenToolAbility;
import com.hbm.items.tool.ItemToolAbility;
import com.hbm.packet.PlayerInformPayload;
import com.mojang.blaze3d.platform.InputConstants;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.KeyMappings;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.harness.HarnessQueryRegistry;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Client half of the ability tool family: the ability-alt key that opens the preset editor, the info HUD
 * (1.7 {@code RenderInfoSystem}) that shows preset changes, the area-ability indicator beside the
 * crosshair (1.7 {@code ItemToolAbility.renderHUD}), and the inform packet.
 */
public final class AbilityToolClientRegistrations {

    private AbilityToolClientRegistrations() {}

    /** 1.7 {@code HbmKeybinds.abilityAlt}, left Alt by default. */
    public static KeyMappings.Mapping abilityAltKey;
    private static boolean lastAlt;

    /** 1.7 {@code ClientConfig.TOOL_HUD_INDICATOR_X/Y}, both 0 by default. */
    public static int toolHudIndicatorX = 0;
    public static int toolHudIndicatorY = 0;

    private static final Map<IBaseAbility, int[]> abilityGui = new HashMap<>();

    static {
        abilityGui.put(IToolAreaAbility.RECURSION, new int[]{0, 138});
        abilityGui.put(IToolAreaAbility.HAMMER, new int[]{16, 138});
        abilityGui.put(IToolAreaAbility.HAMMER_FLAT, new int[]{32, 138});
        abilityGui.put(IToolAreaAbility.EXPLOSION, new int[]{48, 138});
    }

    public static void registerKeys() {
        abilityAltKey = KeyMappings.register("hbm", "key.hbm.abilityAlt", InputConstants.KEY_LALT, "hbm");
        ClientTicks.onEndTick(AbilityToolClientRegistrations::tick);
        HarnessQueryRegistry.register("hbm_info", (mc, params) -> {
            JsonObject json = new JsonObject();
            JsonArray lines = new JsonArray();
            RenderInfoSystem.currentTexts().forEach(lines::add);
            json.add("messages", lines);
            return json;
        });
    }

    public static void registerHud(HbmClientRegistrations.HudSink sink) {
        sink.register("info_system", RenderInfoSystem::render);
        sink.register("tool_ability", AbilityToolClientRegistrations::renderAbilityHud);
    }

    public static void registerPacketHandlers() {
        GolemNetwork.registerPlayToClient(PlayerInformPayload.CODEC,
                (payload, context) -> RenderInfoSystem.displayTooltip(payload.message(), payload.millis(), payload.infoId()));
    }

    private static void tick() {
        RenderInfoSystem.clientTick();

        Minecraft mc = Minecraft.getInstance();
        boolean alt = abilityAltKey != null && mc.player != null && abilityAltKey.isHeld();
        // 1.7 ItemToolAbility.handleKeybindClient: the key going down opens the editor for the held tool.
        if (alt && !lastAlt && mc.screen == null) {
            ItemStack held = mc.player.getMainHandItem();
            if (held.getItem() instanceof ItemToolAbility tool) {
                mc.setScreen(new GUIScreenToolAbility(tool.getAvailableAbilities()));
            }
        }
        lastAlt = alt;
    }

    /** 1.7 {@code ItemToolAbility.renderHUD}: the area ability's icon, inverted like the crosshair. */
    private static void renderAbilityHud(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemToolAbility tool)) return;

        ToolPreset preset = tool.getConfiguration(stack).getActivePreset();
        int[] uv = abilityGui.get(preset.areaAbility);
        if (uv == null) return;

        int size = 16;
        int x = mc.getWindow().getGuiScaledWidth() / 2 - size - 8 + toolHudIndicatorX;
        int y = mc.getWindow().getGuiScaledHeight() / 2 + 8 + toolHudIndicatorY;
        GuiDraw.crosshair(graphics, GUIScreenToolAbility.texture, x, y, uv[0], uv[1], size, size, 256, 256);
    }
}
