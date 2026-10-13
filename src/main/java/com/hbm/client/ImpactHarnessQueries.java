package com.hbm.client;

import com.google.gson.JsonObject;
import com.hbm.capability.HbmLivingProps;
import com.hbm.client.render.world.NTMSkyboxes;
import com.hbm.handler.ImpactWorldHandler;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.saveddata.TomSaveData;
import dev.golem.core.harness.HarnessQueryRegistry;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LightLayer;

/**
 * The {@code hbm_impact} harness query: reads and optionally sets the overworld's
 * Tom impact data on the integrated server ({@code set_dust}, {@code set_fire}, {@code set_impact}), and reports
 * the server's and the client's view of it plus both sides' sky darkening and the chunk radiation at the player.
 * Inert unless the harness listener is running.
 */
public final class ImpactHarnessQueries {

    private ImpactHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_impact", ImpactHarnessQueries::impact);
    }

    private static JsonObject impact(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        var server = mc.getSingleplayerServer();
        if (mc.player == null || server == null || mc.level == null) return json;
        UUID id = mc.player.getUUID();
        CompletableFuture<JsonObject> result = new CompletableFuture<>();
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
                    s.addProperty("chunk_rad", ChunkRadiationManager.proxy.getRadiation(level, pos));
                    s.addProperty("player_rads", HbmLivingProps.getData(player).getRads());
                    s.addProperty("biome", level.getBiome(pos).unwrapKey().map(k -> k.toString()).orElse("?"));
                    s.addProperty("sky_light", level.getBrightness(LightLayer.SKY, pos));
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
            json.add("server", result.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            json.addProperty("error", e.toString());
        }
        JsonObject c = new JsonObject();
        c.addProperty("dust", ImpactWorldHandler.getDustForClient(mc.level));
        c.addProperty("fire", ImpactWorldHandler.getFireForClient(mc.level));
        c.addProperty("impact", ImpactWorldHandler.getImpactForClient(mc.level));
        c.addProperty("sky_darken", mc.level.getSkyDarken());
        // sky renderer slot: none / chainloader / chainloader_impact (stable while dust or fire > 0).
        c.addProperty("skybox", NTMSkyboxes.state(mc.level));
        c.addProperty("lode_star", NTMSkyboxes.renderLodeStar);
        json.add("client", c);
        return json;
    }
}
