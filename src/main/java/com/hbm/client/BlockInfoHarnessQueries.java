package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.core.harness.HarnessQueryRegistry;
import dev.golem.core.info.BlockInfo;
import dev.golem.core.info.Gauge;
import dev.golem.core.info.Tank;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The {@code hbm_block_info} harness query: the gauges and tanks {@link BlockInfo} hands an overlay for the block at
 * {@code x,y,z}, read on the integrated server after the folded-cell redirect, as Jade's server data carries them.
 * Answers {@code pending} while the chunk is not loaded.
 */
public final class BlockInfoHarnessQueries {

    private BlockInfoHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_block_info", BlockInfoHarnessQueries::query);
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || server == null) return new JsonObject();
        BlockPos pos = new BlockPos(params.get("x").getAsInt(), params.get("y").getAsInt(), params.get("z").getAsInt());
        CompletableFuture<JsonObject> result = new CompletableFuture<>();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
            result.complete(player == null ? new JsonObject() : read((ServerLevel) player.level(), pos));
        });
        try {
            return result.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.toString());
            return error;
        }
    }

    private static JsonObject read(ServerLevel level, BlockPos pos) {
        JsonObject json = new JsonObject();
        if (!level.isLoaded(pos)) {
            json.addProperty("pending", true);
            return json;
        }
        BlockPos redirect = BlockInfo.redirectOf(level, pos);
        BlockPos target = redirect == null ? pos : redirect;
        Object be = level.getBlockEntity(target);
        json.addProperty("target", target.getX() + "," + target.getY() + "," + target.getZ());
        json.addProperty("block_entity", be == null ? "" : be.getClass().getSimpleName());
        JsonArray gauges = new JsonArray();
        for (Gauge gauge : BlockInfo.gaugesOf(be)) {
            JsonObject entry = new JsonObject();
            entry.addProperty("kind", gauge.kind().name());
            entry.addProperty("label", gauge.label());
            entry.addProperty("value", gauge.value());
            entry.addProperty("max", gauge.max());
            entry.addProperty("unit", gauge.unit());
            gauges.add(entry);
        }
        json.add("gauges", gauges);
        JsonArray tanks = new JsonArray();
        for (Tank tank : BlockInfo.tanksOf(be)) {
            JsonObject entry = new JsonObject();
            entry.addProperty("fluid", tank.fluid() == null ? "" : BuiltInRegistries.FLUID.getKey(tank.fluid()).toString());
            entry.addProperty("mb", tank.millibuckets());
            entry.addProperty("capacity", tank.capacity());
            tanks.add(entry);
        }
        json.add("tanks", tanks);
        return json;
    }
}
