package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.handler.ArmorDashHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.armor.ArmorEuphemium;
import com.hbm.items.armor.ArmorHEV;
import com.hbm.items.armor.ArmorTrenchmaster;
import com.hbm.items.armor.ArmorFSB;
import com.hbm.items.armor.ArmorFSBPowered;
import com.hbm.packet.KeybindPayload;
import com.mojang.blaze3d.platform.InputConstants;
import dev.golem.core.Ids;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.KeyMappings;
import dev.golem.core.client.VanillaHud;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.harness.HarnessQueryRegistry;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.world.StepHeight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Client half of the final armour sets (part A): the dash key and dash bar (bismuth), and the HEV suit's
 * replacement of the vanilla health and armour rows.
 */
public final class ArmorSetClient {

    private ArmorSetClient() {}

    private static final Identifier MISC = Ids.of("hbm", "textures/misc/overlay_misc.png");

    public static KeyMappings.Mapping dashKey;
    private static boolean lastDash;
    /** 1.7 {@code RenderScreenOverlay.fadeOut}: the "just recharged" flash, shared across frames. */
    private static float fadeOut = 0F;

    /** 1.7 {@code HbmKeybinds.dashKey}: "hbm.key.dash" on left Shift. */
    public static void registerKeys() {
        dashKey = KeyMappings.register("hbm", "key.hbm.dash", InputConstants.KEY_LSHIFT, "hbm");
        ClientTicks.onEndTick(ArmorSetClient::onEndTick);
        HarnessQueryRegistry.register("hbm_armor_set", (mc, params) -> query(mc));
    }

    /**
     * The {@code hbm_armor_set} harness query: set detection, bonuses and per-piece charge from both the
     * integrated server's and the client's player, so a scenario checks a set bonus by value. Inert unless
     * the harness listener runs.
     */
    private static JsonObject query(Minecraft mc) {
        JsonObject json = new JsonObject();
        if (mc.player == null || mc.getSingleplayerServer() == null) return json;
        Player server =
                mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
        if (server == null) return json;
        json.addProperty("health", server.getHealth());
        json.addProperty("max_health", server.getMaxHealth());
        json.addProperty("armor_value", server.getArmorValue());
        json.addProperty("armor_toughness", server.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        json.addProperty("fsb", ArmorFSB.hasFSBArmor(server));
        json.addProperty("fsb_ignore_charge", ArmorFSB.hasFSBArmorIgnoreCharge(server));
        json.addProperty("trenchmaster", ArmorTrenchmaster.isTrenchMaster(server));
        json.addProperty("euphemium", ArmorEuphemium.hasFullSet(server));
        json.addProperty("digamma_protected", ArmorUtil.checkForDigamma(server));
        json.addProperty("heavy_hazmat", ArmorUtil.checkForHaz2(server));
        json.addProperty("step_height_server", StepHeight.get(server));
        json.addProperty("step_height_client", StepHeight.get(mc.player));
        json.addProperty("hev_hud", ArmorHEV.replacesHud(mc.player));
        json.addProperty("vanilla_hud_suppressible", VanillaHud.supported());
        ArmorDashHandler.State dash = ArmorDashHandler.state(mc.player);
        json.addProperty("dash_count", dash.totalDashCount);
        json.addProperty("dash_stamina", dash.stamina);
        json.addProperty("dash_cooldown", dash.dashCooldown);
        json.addProperty("fall_distance_client", mc.player.fallDistance);
        JsonArray effects = new JsonArray();
        JsonArray durations = new JsonArray();
        for (MobEffectInstance effect : server.getActiveEffects()) {
            effects.add(effect.getDescriptionId() + "@" + effect.getAmplifier());
            durations.add(effect.getDescriptionId() + "@" + effect.getAmplifier() + ":" + effect.getDuration());
        }
        json.add("effects", effects);
        json.add("effect_ticks", durations);
        for (EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = server.getItemBySlot(slot);
            JsonObject piece = new JsonObject();
            piece.addProperty("id", stack.isEmpty() ? ""
                    : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            piece.addProperty("damageable", stack.isDamageableItem());
            piece.addProperty("max_damage", stack.getMaxDamage());
            if (stack.getItem() instanceof ArmorFSBPowered powered) {
                piece.addProperty("charge", powered.getCharge(stack));
                piece.addProperty("max_charge", powered.getMaxCharge(stack));
            }
            json.add(slot.getName(), piece);
        }
        return json;
    }

    private static void onEndTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            lastDash = false;
            return;
        }
        // isHeld polls the physical key: on 1.20.1 vanilla gives a key to one mapping only, and the default
        // shares left Shift with sneaking, exactly as upstream did.
        boolean dash = mc.screen == null && dashKey.isHeld();
        if (dash != lastDash) {
            // Upstream set the client's own props before sending: the dash impulse is the client's.
            ArmorDashHandler.setKeyPressed(player, dash);
            GolemNetwork.sendToServer(KeybindPayload.CODEC, new KeybindPayload(EnumKeybind.DASH.ordinal(), dash));
            lastDash = dash;
        }
    }

    public static void registerHud(HbmClientRegistrations.HudSink sink) {
        sink.register("dash_bar", ArmorSetClient::renderDashBar);
        VanillaHud.hideHealthAndArmor(() -> {
            Minecraft mc = Minecraft.getInstance();
            return mc.player != null && ArmorHEV.replacesHud(mc.player);
        });
    }

    /** 1.7 {@code RenderScreenOverlay.renderDashBar}, drawn after the hotbar while dashes are available. */
    public static void renderDashBar(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        ArmorDashHandler.State props = ArmorDashHandler.state(mc.player);
        if (props.totalDashCount <= 0) return;

        int width = 30;
        int posX = 16;
        int posY = mc.getWindow().getGuiScaledHeight() - 40 - 2;

        GuiDraw.blit(graphics, MISC, posX - 10, posY, 107, 18, 7, 10, 256, 256);

        int stamina = props.stamina;
        int dashes = props.totalDashCount;
        // Upstream's arithmetic, including that fewer than three dashes draw no row at all.
        int rows = dashes / 3;
        int finalColumns = dashes % 3;

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < 3; x++) {
                if (y == rows && x > finalColumns) break;
                GuiDraw.blit(graphics, MISC, posX + (width + 2) * x, posY - 12 * y, 76, 48, width, 10, 256, 256);
                int staminaDiv = stamina / 30;
                int staminaMod = stamina % 30;
                int barID = (3 * y) + x;
                int barStatus = 1; // 0 = red, 1 = normal, 2 = greyed, 3 = dashed, 4 = ascended
                int barSize = width;
                if (staminaDiv < barID) {
                    barStatus = 3;
                } else if (staminaDiv == barID) {
                    barStatus = 2;
                    barSize = (int) ((float) (stamina % 30) * (width / 30F));
                    if (barID == 0) barStatus = 0;
                }
                if (barSize > 0) {
                    GuiDraw.blit(graphics, MISC, posX + (width + 2) * x, posY - 12 * y, 76, 18 + (10 * barStatus),
                            barSize, 10, 256, 256);
                }

                if (staminaDiv == barID && staminaMod >= 27) fadeOut = 1F;
                if (fadeOut > 0 && staminaDiv - 1 == barID) {
                    int bar = barID;
                    if (stamina % 30 >= 25) bar++;
                    // Upstream bumps the loop's own row counter here; kept, it is what the old bar drew.
                    if (bar / 3 != y) y++;
                    bar = bar % 3;
                    int alpha = Math.round(Math.max(0F, Math.min(1F, fadeOut)) * 255F);
                    GuiDraw.blit(graphics, MISC, posX + (width + 2) * bar, posY - 12 * y, 76, 58, width, 10,
                            256, 256, (alpha << 24) | 0xFFFFFF);
                    fadeOut -= 0.04F;
                }
            }
        }
    }
}
