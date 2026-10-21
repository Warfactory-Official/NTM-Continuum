package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.tileentity.machine.rbmk.BlockEntityCraneConsole;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKConsole;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKDisplay;
import com.hbm.tileentity.machine.rbmk.RBMKColumn;
import dev.golem.core.harness.HarnessQueryRegistry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The {@code hbm_rbmk_console} harness query: the scanned columns and screen readouts of the RBMK console or display at
 * {@code x,y,z}, once as the integrated server holds them and once as the client received them.
 */
public final class RbmkConsoleHarnessQueries {

    private RbmkConsoleHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_rbmk_console", RbmkConsoleHarnessQueries::query);
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        MinecraftServer server = mc.getSingleplayerServer();
        JsonObject json = new JsonObject();
        if (mc.player == null || mc.level == null || server == null) return json;
        BlockPos pos = new BlockPos(params.get("x").getAsInt(), params.get("y").getAsInt(), params.get("z").getAsInt());
        json.add("client", describe(mc.level.getBlockEntity(pos)));
        CompletableFuture<JsonObject> result = new CompletableFuture<>();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
            result.complete(player == null ? new JsonObject() : describe(((ServerLevel) player.level()).getBlockEntity(pos)));
        });
        try {
            json.add("server", result.get(10, TimeUnit.SECONDS));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            json.addProperty("error", e.toString());
        }
        return json;
    }

    private static JsonObject describe(BlockEntity be) {
        JsonObject json = new JsonObject();
        if (be instanceof BlockEntityRBMKConsole console) {
            json.addProperty("kind", "console");
            json.add("columns", columns(console.columns));
            JsonArray screens = new JsonArray();
            for (BlockEntityRBMKConsole.RBMKScreen screen : console.screens) {
                screens.add(screen.type.name() + "=" + screen.display);
            }
            json.add("screens", screens);
        } else if (be instanceof BlockEntityCraneConsole crane) {
            json.addProperty("kind", "crane");
            json.addProperty("setUp", crane.setUpCrane);
            json.addProperty("front", Math.round(crane.posFront * 1000D) / 1000D);
            json.addProperty("left", Math.round(crane.posLeft * 1000D) / 1000D);
            json.addProperty("progress", Math.round(crane.progress * 1000D) / 1000D);
            json.addProperty("loaded", crane.hasItemLoaded());
            json.addProperty("enrichment", Math.round(crane.loadedEnrichment * 1000D) / 1000D);
            json.addProperty("spans", crane.spanF + "," + crane.spanB + "," + crane.spanL + "," + crane.spanR + " h" + crane.height);
            json.addProperty("center", crane.centerX + "," + crane.centerY + "," + crane.centerZ);
            json.addProperty("above", crane.isAboveValidTarget());
        } else if (be instanceof BlockEntityRBMKDisplay display) {
            json.addProperty("kind", "display");
            json.add("columns", columns(display.columns));
        }
        return json;
    }

    private static JsonArray columns(RBMKColumn[] columns) {
        JsonArray out = new JsonArray();
        for (int i = 0; i < columns.length; i++) {
            RBMKColumn column = columns[i];
            if (column == null) continue;
            StringBuilder text = new StringBuilder(i + ":" + column.type.name() + ":heat=" + Math.round(column.heat * 10D) / 10D);
            if (column instanceof RBMKColumn.FuelColumn fuel) {
                text.append(":enr=").append(Math.round(fuel.enrichment * 1000D) / 1000D)
                        .append(":xenon=").append(Math.round(fuel.xenon * 100D) / 100D)
                        .append(":skin=").append(Math.round(fuel.c_heat * 10D) / 10D)
                        .append(":core=").append(Math.round(fuel.c_coreHeat * 10D) / 10D)
                        .append(":max=").append(fuel.c_maxHeat);
            } else if (column instanceof RBMKColumn.ControlColumn control) {
                text.append(":level=").append(Math.round(control.level * 1000D) / 1000D).append(":color=").append(control.color);
            } else if (column instanceof RBMKColumn.BoilerColumn boiler) {
                text.append(":water=").append(boiler.water).append('/').append(boiler.maxWater)
                        .append(":steam=").append(boiler.steam).append('/').append(boiler.maxSteam);
            }
            out.add(text.toString());
        }
        return out;
    }
}
