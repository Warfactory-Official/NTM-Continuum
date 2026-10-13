package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.capability.HbmLivingProps;
import com.hbm.handler.ArmorModEvents;
import com.hbm.handler.ArmorModHandler;
import dev.golem.core.harness.HarnessQueryRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The {@code hbm_armor} harness query: the integrated server's view of the local player's armor-module
 * state (attribute values, shield, radiation, effects, installed modules per slot), so a scenario can
 * check a module's effect by value instead of by eye. Inert unless the harness listener is running.
 */
public final class ArmorModHarnessQueries {

    private ArmorModHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_armor", (mc, params) -> query(mc));
    }

    private static JsonObject query(Minecraft mc) {
        JsonObject json = new JsonObject();
        if (mc.player == null || mc.getSingleplayerServer() == null) return json;
        Player player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
        if (player == null) return json;

        json.addProperty("health", player.getHealth());
        json.addProperty("max_health", player.getMaxHealth());
        json.addProperty("alive", player.isAlive());
        json.addProperty("fall_flying", player.isFallFlying());
        json.addProperty("client_fall_flying", mc.player.isFallFlying());

        json.addProperty("knockback_resistance", attribute(player, "knockback_resistance"));
        json.addProperty("attack_damage", attribute(player, "attack_damage"));
        json.addProperty("movement_speed", attribute(player, "movement_speed"));
        json.addProperty("shield", HbmLivingProps.getData(player).shield);
        json.addProperty("max_shield", ArmorModEvents.effectiveMaxShield(player));
        json.addProperty("radiation", HbmLivingProps.getRadiation(player));
        json.addProperty("digamma", HbmLivingProps.getDigamma(player));

        JsonArray effects = new JsonArray();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            effects.add(effect.getDescriptionId() + "@" + effect.getAmplifier());
        }
        json.add("effects", effects);

        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET}) {
            ItemStack armor = player.getItemBySlot(slot);
            JsonObject piece = new JsonObject();
            piece.addProperty("id", armor.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(armor.getItem()).toString());
            piece.addProperty("damage", armor.getDamageValue());
            JsonArray mods = new JsonArray();
            if (!armor.isEmpty()) {
                ItemStack[] installed = ArmorModHandler.pryMods(armor);
                for (int i = 0; i < installed.length; i++) {
                    if (!installed[i].isEmpty()) {
                        mods.add(i + ":" + BuiltInRegistries.ITEM.getKey(installed[i].getItem())
                                + ":" + installed[i].getDamageValue());
                    }
                }
            }
            piece.add("mods", mods);
            json.add(slot.getName(), piece);
        }
        return json;
    }

    private static double attribute(Player player, String name) {
        var instance = switch (name) {
            case "knockback_resistance" ->
                    player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            case "attack_damage" -> player.getAttribute(Attributes.ATTACK_DAMAGE);
            default -> player.getAttribute(Attributes.MOVEMENT_SPEED);
        };
        return instance == null ? -1 : instance.getValue();
    }
}
