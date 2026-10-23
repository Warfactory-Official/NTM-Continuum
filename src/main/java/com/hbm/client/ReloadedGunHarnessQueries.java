package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.client.gun.GluonGunClient;
import com.hbm.client.gun.JadeShotgunClient;
import com.hbm.client.particle.GluonParticles;
import com.hbm.client.render.item.ItemRenderJShotgun;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.XFactory10ga;
import com.hbm.items.weapon.sedna.impl.ItemGunEgon;
import dev.golem.core.harness.HarnessQueryRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/**
 * The {@code hbm_reloaded_guns} harness query: client-side counters of the gluon gun and the jade shotgun.
 * {@code pin_flash} holds the shotgun's muzzle flash on. Inert unless the harness listener runs.
 */
public final class ReloadedGunHarnessQueries {

    private ReloadedGunHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_reloaded_guns", ReloadedGunHarnessQueries::query);
        HarnessQueryRegistry.register("hbm_lamp_lights", (mc, params) -> LampLights.query());
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        ItemStack held = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
        String mode = params.has("mode") ? params.get("mode").getAsString() : "gluon";
        if (mode.equals("gluon")) {
            json.addProperty("active_ticks", GluonGunClient.activeTicks);
            json.addProperty("beams_drawn", GluonGunClient.beamsDrawn);
            json.addProperty("muzzle", GluonGunClient.MUZZLE.size());
            JsonArray live = new JsonArray();
            for (int n : GluonParticles.live()) live.add(n);
            json.add("live", live);
            json.addProperty("flares_spawned", GluonParticles.flaresSpawned);
            json.addProperty("sparks_spawned", GluonParticles.sparksSpawned);
            json.addProperty("trail_nodes", GluonParticles.trailNodesAdded);
            json.addProperty("cubes_spawned", GluonParticles.cubesSpawned);
            json.addProperty("hits_shown", GluonParticles.hitsShown);
            json.addProperty("firing_flag", held.getItem() instanceof ItemGunEgon && ItemGunEgon.getIsFiring(held));
            json.addProperty("trigger_held", HbmKeybindsClient.isHeld(EnumKeybind.GUN_PRIMARY));
            json.addProperty("local_firing", mc.player != null && GluonGunClient.localFiring(mc.player));
            if (params.has("server")) {
                JsonObject server = ServerQueries.answer(mc, "hbm_reloaded_guns", params.deepCopy(), (level, p) -> {
                    JsonObject out = new JsonObject();
                    var players = level.getServer().getPlayerList().getPlayers();
                    if (players.isEmpty()) return out;
                    ItemStack stack = players.get(0).getMainHandItem();
                    out.addProperty("item", stack.getItem().toString());
                    out.addProperty("primary", ItemGunBaseNT.getPrimary(stack, 0));
                    out.addProperty("firing", ItemGunEgon.getIsFiring(stack));
                    out.addProperty("state", ItemGunBaseNT.getState(stack, 0).name());
                    return out;
                });
                json.add("server", server);
            }
        } else {
            if (params.has("pin_flash")) ItemRenderJShotgun.pinnedFlash = params.get("pin_flash").getAsBoolean();
            json.addProperty("shots_seen", JadeShotgunClient.shotsSeen);
            json.addProperty("smokes", JadeShotgunClient.smokes());
            json.addProperty("smoke_draws", JadeShotgunClient.smokeDraws);
            json.addProperty("loaded", JadeShotgunClient.loaded(held));
            json.addProperty("flashlight", held.getItem() instanceof ItemGunBaseNT && XFactory10ga.getFlashlightActive(held));
        }
        return json;
    }
}
