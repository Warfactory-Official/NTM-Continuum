package com.hbm.client;

import com.google.gson.JsonObject;
import com.hbm.gametest.ForeignEnergyFixture;
import dev.golem.core.harness.HarnessQueryRegistry;
import dev.golem.core.world.EnergyCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * The {@code hbm_fecell} harness query, read by {@code FxCableForeignEnergy}. Ops: {@code stored} (FE in the
 * {@code test_fe_cell} at x,y,z) and {@code exposes} (whether a foreign handler is found at x,y,z on {@code side}).
 */
public final class FxCablesHarnessQueries {

    private FxCablesHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_fecell", FxCablesHarnessQueries::query);
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || server == null) return new JsonObject();
        CompletableFuture<JsonObject> result = new CompletableFuture<>();
        server.execute(() -> {
            JsonObject json = new JsonObject();
            try {
                ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
                if (player != null) onServer((ServerLevel) player.level(), params, json);
            } catch (RuntimeException e) {
                json.addProperty("error", e.toString());
            }
            result.complete(json);
        });
        try {
            return result.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.toString());
            return error;
        }
    }

    private static void onServer(ServerLevel level, JsonObject p, JsonObject json) {
        BlockPos pos = new BlockPos(p.get("x").getAsInt(), p.get("y").getAsInt(), p.get("z").getAsInt());
        switch (p.get("op").getAsString()) {
            case "stored" -> json.addProperty("stored", ForeignEnergyFixture.stored(level, pos));
            case "exposes" -> json.addProperty("exposes",
                    EnergyCompat.find(level, pos, Direction.byName(p.get("side").getAsString())) != null);
            default -> json.addProperty("error", "unknown op");
        }
    }
}
