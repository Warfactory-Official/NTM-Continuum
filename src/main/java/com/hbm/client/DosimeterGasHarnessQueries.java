package com.hbm.client;

import com.google.gson.JsonObject;
import com.hbm.blocks.generic.BlockGasBase;
import com.hbm.capability.HbmLivingProps;
import com.hbm.client.particle.ParticleGasCloud;
import com.hbm.items.tool.ItemDosimeter;
import dev.golem.core.harness.HarnessQueryRegistry;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

/**
 * The {@code hbm_dosimeter_gas} harness query (1.7 dosimeter clicks and ash-glasses gas clouds):
 * <ul>
 *   <li>client: {@code revealed}, the ash-glasses gas puffs built since start ({@link ParticleGasCloud#REVEALED}), and
 *       {@code chance}, the display-tick gate ({@link BlockGasBase#ashGlassesParticleChance}, 1 = 1.7; {@code set_chance}
 *       changes it for the running game, for a scenario that compares the two rates);</li>
 *   <li>server: the player's {@code rad_buf} (what the dosimeter reads) and {@code gate}
 *       ({@link ItemDosimeter#probabilisticClicks}).</li>
 * </ul>
 * The clicks themselves are read from the {@code [harness-sound]} log ({@code hbm:item.geiger1..3}). Inert unless the
 * harness listener is running.
 */
public final class DosimeterGasHarnessQueries {

    private DosimeterGasHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_dosimeter_gas", DosimeterGasHarnessQueries::query);
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        // test hook: lets a scenario compare the two cloud rates on one client (restore it afterwards)
        if (params.has("set_chance")) BlockGasBase.ashGlassesParticleChance = params.get("set_chance").getAsInt();
        json.addProperty("revealed", ParticleGasCloud.REVEALED.get());
        json.addProperty("chance", BlockGasBase.ashGlassesParticleChance);
        var server = mc.getSingleplayerServer();
        if (mc.player == null || server == null) return json;
        UUID id = mc.player.getUUID();
        CompletableFuture<JsonObject> result = new CompletableFuture<>();
        server.execute(() -> {
            JsonObject s = new JsonObject();
            try {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null) s.addProperty("rad_buf", HbmLivingProps.getRadBuf(player));
                s.addProperty("gate", ItemDosimeter.probabilisticClicks);
            } catch (Throwable t) {
                s.addProperty("error", t.toString());
            }
            result.complete(s);
        });
        try {
            json.add("server", result.get(10, TimeUnit.SECONDS));
        } catch (Exception e) {
            json.addProperty("error", e.toString());
        }
        return json;
    }
}
