package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.client.particle.AmatFlashParticles;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModSingularityItems;
import com.hbm.items.special.ItemCell;
import com.hbm.items.weapon.ItemCrucible;
import dev.golem.core.world.ItemStacks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * The {@code hbm_gapsa} harness query of gap batch A (§19ed-gaps-a), read by {@code FinalGapsA}: the server player's
 * held stack (id, variant, crucible charge, whether its modifiers are suppressed), attack damage and health, every
 * cell in the inventory with its model variant; the client player's hurt time/duration (the knife's jolt) and the
 * live antimatter flashes. {@code {"give":"oldcell:<FLUID>"}} hands the player a cell saved the pre-2026-09-25 way
 * (model variant = fluid id), which no /give can spell identically on every node. Inert unless the harness runs.
 */
public final class GapsAHarnessQueries {

    private GapsAHarnessQueries() {}

    public static void register() {
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_gapsa", GapsAHarnessQueries::query);
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || server == null) return json;
        json.addProperty("hurt_time", mc.player.hurtTime);
        json.addProperty("hurt_duration", mc.player.hurtDuration);
        json.addProperty("amat_flashes", AmatFlashParticles.active());
        json.addProperty("client_alt_down", altDown(mc));
        CompletableFuture<JsonObject> result = new CompletableFuture<>();
        server.execute(() -> {
            try {
                ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
                result.complete(player == null ? new JsonObject() : onServer(player, params));
            } catch (Throwable t) {
                JsonObject error = new JsonObject();
                error.addProperty("error", t.toString());
                result.complete(error);
            }
        });
        try {
            JsonObject server1 = result.get(5, TimeUnit.SECONDS);
            for (var e : server1.entrySet()) json.add(e.getKey(), e.getValue());
        } catch (Exception e) {
            json.addProperty("error", e.toString());
        }
        return json;
    }

    private static boolean altDown(Minecraft mc) {
        for (var mapping : mc.options.keyMappings) {
            // the state the copy tool itself reads (UtilityToolClientRegistrations.tick): key.hbm.abilityAlt is on
            // left Alt too (as in 1.7), and Fabric below 26.1 dispatches a key to one mapping only, so isDown()
            // of the losing mapping stays false while the key is held; Core's isHeld() reads the winner as well
            if (mapping.getName().equals("key.hbm.copyToolAlt")) {
                return mapping instanceof dev.golem.core.client.KeyMappings.Mapping held ? held.isHeld() : mapping.isDown();
            }
        }
        return false;
    }

    /**
     * {@code {"custom_machines":"stage","machines":[…],"recipes":{…}}} loads the real {@code hbmCustomMachines.json}
     * plus the scenario's test machines, and the scenario's custom machine recipe file, from a staging folder beside
     * the config ({@code config/hbmHarnessStaging}); {@code "restore"} reloads the real files. The real config files
     * are never written, so a scenario that dies half-way leaves nothing behind but the staging folder. Machines
     * are read once at startup in 1.7; the running game shares one static list between the integrated server and
     * the client, so the reload is seen by both (and by JEI on the next world join).
     */
    private static int customMachines(JsonObject params) {
        String mode = params.get("custom_machines").getAsString();
        java.nio.file.Path real = com.hbm.config.CustomMachineConfigJSON.configFile();
        if (mode.equals("restore")) {
            com.hbm.config.CustomMachineConfigJSON.reloadFrom(real);
            com.hbm.inventory.recipes.CustomMachineRecipes.reload();
            return com.hbm.config.CustomMachineConfigJSON.niceList.size();
        }
        try {
            java.nio.file.Path dir = dev.golem.core.platform.ConfigFiles.configDir().resolve("hbmHarnessStaging");
            java.nio.file.Path recipes = dir.resolve("hbmRecipes");
            java.nio.file.Files.createDirectories(recipes);
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            JsonObject machines;
            try (java.io.Reader reader = java.nio.file.Files.newBufferedReader(real, java.nio.charset.StandardCharsets.UTF_8)) {
                machines = gson.fromJson(reader, JsonObject.class);
            }
            for (var machine : params.getAsJsonArray("machines")) machines.getAsJsonArray("machines").add(machine);
            java.nio.file.Path staged = dir.resolve("hbmCustomMachines.json");
            java.nio.file.Files.writeString(staged, gson.toJson(machines), java.nio.charset.StandardCharsets.UTF_8);
            java.nio.file.Files.writeString(recipes.resolve(com.hbm.inventory.recipes.CustomMachineRecipes.getFileName()),
                    gson.toJson(params.getAsJsonObject("recipes")), java.nio.charset.StandardCharsets.UTF_8);
            com.hbm.config.CustomMachineConfigJSON.reloadFrom(staged);
            com.hbm.inventory.recipes.CustomMachineRecipes.loadFrom(recipes);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        return com.hbm.config.CustomMachineConfigJSON.niceList.size();
    }

    private static JsonObject onServer(ServerPlayer player, JsonObject params) {
        JsonObject json = new JsonObject();
        if (params.has("clearrads")) {
            // between scenario sections: the radioactive test items (cells, crucible) are lethal over a long run
            com.hbm.capability.HbmLivingProps.setRadiation(player, 0);
            com.hbm.capability.HbmLivingProps.setNeutron(player, 0);
            json.addProperty("cleared", true);
            return json;
        }
        if (params.has("custom_machines")) {
            json.addProperty("custom_machines", customMachines(params));
            return json;
        }
        if (params.has("give")) {
            String[] give = params.get("give").getAsString().split(":", 2);
            if (give[0].equals("oldcell")) {
                var fluid = Fluids.fromName(give[1]);
                ItemStack cell = new ItemStack(ModSingularityItems.cell.get());
                ModDataComponents.fluidId(cell, fluid.getID());
                ModDataComponents.fluidAmount(cell, ItemCell.CAPACITY);
                ModDataComponents.ITEM_VARIANT.set(cell, fluid.getID());
                json.addProperty("given", player.getInventory().add(cell));
                player.inventoryMenu.broadcastChanges();
            }
            return json;
        }
        ItemStack held = player.getMainHandItem();
        json.addProperty("held", held.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
        json.addProperty("held_variant", ModDataComponents.ITEM_VARIANT.getOr(held, -1));
        json.addProperty("crucible_spent", held.getItem() instanceof ItemCrucible ? ItemCrucible.spent(held) : -1);
        json.addProperty("modifiers_suppressed", ItemStacks.attributeModifiersSuppressed(held));
        json.addProperty("attack_damage", player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        json.addProperty("tool_alt", com.hbm.handler.ToolKeybinds.getKeyPressed(player,
                com.hbm.handler.ToolKeybinds.EnumToolKey.TOOL_ALT));
        json.addProperty("tool_ctrl", com.hbm.handler.ToolKeybinds.getKeyPressed(player,
                com.hbm.handler.ToolKeybinds.EnumToolKey.TOOL_CTRL));
        // the settings tool's paste selection, read in the same server step as tool_alt: FinalGapsA polls both
        // while Alt is held, so it can release the key before 1.7's 5-tick repeat cycles the selection again
        net.minecraft.nbt.CompoundTag settings = com.hbm.items.ModUtilityToolItems.SETTINGS.getOr(held, null);
        json.addProperty("settings_copy_index", settings == null ? -1
                : dev.golem.core.data.Tags.getIntOr(settings, "copyIndex", 0));
        json.addProperty("health", player.getHealth());
        json.addProperty("max_health", player.getMaxHealth());
        JsonArray cells = new JsonArray();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!(stack.getItem() instanceof ItemCell)) continue;
            JsonObject c = new JsonObject();
            c.addProperty("slot", i);
            c.addProperty("count", stack.getCount());
            c.addProperty("fluid", ModDataComponents.fluidId(stack));
            c.addProperty("variant", ModDataComponents.ITEM_VARIANT.getOr(stack, -1));
            c.addProperty("name", stack.getHoverName().getString());
            cells.add(c);
        }
        json.add("cells", cells);
        return json;
    }
}
