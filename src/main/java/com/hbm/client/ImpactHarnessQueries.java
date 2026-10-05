package com.hbm.client;

import com.google.gson.JsonObject;
import com.hbm.handler.ImpactWorldHandler;
import com.hbm.saveddata.TomSaveData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The {@code hbm_impact} harness query (fix wave phase 6, History §19ej-6): reads and optionally sets the overworld's
 * Tom impact data on the integrated server ({@code set_dust}, {@code set_fire}, {@code set_impact}), and reports
 * the server's and the client's view of it plus both sides' sky darkening and the chunk radiation at the player.
 * Inert unless the harness listener is running.
 */
public final class ImpactHarnessQueries {

    private ImpactHarnessQueries() {}

    public static void register() {
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_impact", ImpactHarnessQueries::impact);
    }

    private static JsonObject impact(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        var server = mc.getSingleplayerServer();
        if (mc.player == null || server == null || mc.level == null) return json;
        java.util.UUID id = mc.player.getUUID();
        java.util.concurrent.CompletableFuture<JsonObject> result = new java.util.concurrent.CompletableFuture<>();
        server.execute(() -> {
            try {
                JsonObject s = new JsonObject();
                ServerLevel overworld = server.overworld();
                TomSaveData data = TomSaveData.get(overworld);
                if (params.has("set_dust")) data.dust = params.get("set_dust").getAsFloat();
                if (params.has("set_fire")) data.fire = params.get("set_fire").getAsFloat();
                if (params.has("set_impact")) data.impact = params.get("set_impact").getAsBoolean();
                data.setDirty();
                s.addProperty("dust", data.dust);
                s.addProperty("fire", data.fire);
                s.addProperty("impact", data.impact);
                s.addProperty("sky_darken", overworld.getSkyDarken());
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null && player.level() instanceof ServerLevel level) {
                    BlockPos pos = player.blockPosition();
                    s.addProperty("chunk_rad", com.hbm.handler.radiation.ChunkRadiationManager.proxy.getRadiation(level, pos));
                    s.addProperty("player_rads", com.hbm.capability.HbmLivingProps.getData(player).getRads());
                    s.addProperty("biome", level.getBiome(pos).unwrapKey().map(k -> k.toString()).orElse("?"));
                    s.addProperty("sky_light", level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos));
                    s.addProperty("on_fire", player.getRemainingFireTicks());
                }
                result.complete(s);
            } catch (Throwable t) {
                JsonObject error = new JsonObject();
                error.addProperty("error", t.toString());
                result.complete(error);
            }
        });
        try {
            json.add("server", result.get(5, java.util.concurrent.TimeUnit.SECONDS));
        } catch (Exception e) {
            json.addProperty("error", e.toString());
        }
        JsonObject c = new JsonObject();
        c.addProperty("dust", ImpactWorldHandler.getDustForClient(mc.level));
        c.addProperty("fire", ImpactWorldHandler.getFireForClient(mc.level));
        c.addProperty("impact", ImpactWorldHandler.getImpactForClient(mc.level));
        c.addProperty("sky_darken", mc.level.getSkyDarken());
        // sky renderer slot: none / chainloader / chainloader_impact (stable while dust or fire > 0).
        c.addProperty("skybox", com.hbm.client.render.world.NTMSkyboxes.state(mc.level));
        c.addProperty("lode_star", com.hbm.client.render.world.NTMSkyboxes.renderLodeStar);
        json.add("client", c);
        return json;
    }
}
