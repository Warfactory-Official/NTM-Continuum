package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.items.ModelState;
import com.hbm.tileentity.machine.TileEntityCargoElevator;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.harness.HarnessQueryRegistry;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.render.WorldEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code hbm_ride}: what a player riding a moving platform goes through. {@code start} clears the record. The answer
 * has the server player's largest fall distance and lowest health since then, and the camera's height per rendered
 * frame: the largest step and how often it reversed against the step before it. {@code hbm_held_state}: the
 * {@code hbm:model_state} of each hotbar stack.
 */
public final class PlayerHarnessQueries {

    private static boolean hooked;
    private static volatile float maxFall;
    private static volatile float minHealth = Float.MAX_VALUE;
    private static int frames;
    private static int reversals;
    private static int airborne;
    private static volatile int serverTicks;
    private static volatile int serverAirborne;
    private static double largestStep;
    private static double lastY = Double.NaN;
    private static double lastStep;
    private static final JsonArray TRACE = new JsonArray();
    private static final JsonArray TICKS = new JsonArray();
    private static final JsonArray SERVER_TICKS = new JsonArray();

    private PlayerHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_ride", PlayerHarnessQueries::query);
        HarnessQueryRegistry.register("hbm_held_state", PlayerHarnessQueries::heldState);
    }

    private static JsonObject heldState(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        JsonArray states = new JsonArray();
        if (mc.player != null) {
            for (int i = 0; i < 9; i++) states.add(ModelState.of(mc.player.getInventory().getItem(i)));
        }
        json.add("hotbar", states);
        return json;
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        if (params.has("start")) {
            if (!hooked) {
                hooked = true;
                WorldEffects.onBegin(camera -> {
                    frame(camera.pos.y);
                    if (Minecraft.getInstance().player != null && !Minecraft.getInstance().player.onGround()) airborne++;
                });
                ClientTicks.onEndTick(PlayerHarnessQueries::tick);
                Lifecycle.onServerTick(server -> {
                    if (server.getPlayerList().getPlayers().isEmpty()) return;
                    ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    maxFall = (float) Math.max(maxFall, player.fallDistance);
                    minHealth = Math.min(minHealth, player.getHealth());
                    serverTicks++;
                    if (!player.onGround()) serverAirborne++;
                    if (SERVER_TICKS.size() < 300) {
                        String lift = "";
                        for (int dy = -8; dy <= 1 && lift.isEmpty(); dy++) {
                            if (player.level().getBlockEntity(player.blockPosition().offset(0, dy, 0))
                                    instanceof TileEntityCargoElevator elevator) {
                                lift = " ext " + Math.round(elevator.extension * 1000D) / 1000D;
                            }
                        }
                        String line = player.level().getGameTime() + " y " + Math.round(player.getY() * 1000D) / 1000D
                                + " ground " + player.onGround() + " pose " + player.getPose() + lift;
                        synchronized (SERVER_TICKS) {
                            SERVER_TICKS.add(line);
                        }
                    }
                });
            }
            maxFall = 0F;
            minHealth = Float.MAX_VALUE;
            frames = 0;
            reversals = 0;
            airborne = 0;
            serverTicks = 0;
            serverAirborne = 0;
            largestStep = 0D;
            lastY = Double.NaN;
            lastStep = 0D;
            while (!TRACE.isEmpty()) TRACE.remove(TRACE.size() - 1);
            while (!TICKS.isEmpty()) TICKS.remove(TICKS.size() - 1);
            synchronized (SERVER_TICKS) {
                while (!SERVER_TICKS.isEmpty()) SERVER_TICKS.remove(SERVER_TICKS.size() - 1);
            }
        }
        JsonObject json = new JsonObject();
        json.addProperty("max_fall", maxFall);
        json.addProperty("min_health", minHealth == Float.MAX_VALUE ? -1F : minHealth);
        json.addProperty("frames", frames);
        json.addProperty("reversals", reversals);
        json.addProperty("airborne_frames", airborne);
        json.addProperty("server_ticks", serverTicks);
        json.addProperty("server_airborne_ticks", serverAirborne);
        json.addProperty("largest_step", largestStep);
        if (params.has("trace")) json.add("trace", TRACE.deepCopy());
        if (params.has("trace")) json.add("ticks", TICKS.deepCopy());
        if (params.has("trace")) {
            synchronized (SERVER_TICKS) {
                json.add("server_ticks_trace", SERVER_TICKS.deepCopy());
            }
        }
        return json;
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || TICKS.size() >= 300) return;
        String lift = "";
        BlockPos at = mc.player.blockPosition();
        for (int dy = -8; dy <= 1 && lift.isEmpty(); dy++) {
            if (mc.level.getBlockEntity(at.offset(0, dy, 0)) instanceof TileEntityCargoElevator elevator) {
                lift = " ext " + Math.round(elevator.extension * 1000D) / 1000D + " sync " + elevator.syncExtension;
            }
        }
        TICKS.add(mc.level.getGameTime() + " y " + Math.round(mc.player.getY() * 1000D) / 1000D
                + " ground " + mc.player.onGround() + " pose " + mc.player.getPose() + " vy " + Math.round(mc.player.getDeltaMovement().y * 1000D) / 1000D + lift);
    }

    private static void frame(double y) {
        frames++;
        if (TRACE.size() < 600 && Minecraft.getInstance().level != null) {
            TRACE.add(Minecraft.getInstance().level.getGameTime() + ":" + Math.round(y * 10000D) / 10000D);
        }
        if (!Double.isNaN(lastY)) {
            double step = y - lastY;
            largestStep = Math.max(largestStep, Math.abs(step));
            // a frame that moves back against the last real step is a visible twitch
            if (Math.abs(step) > 1.0E-4 && Math.abs(lastStep) > 1.0E-4 && Math.signum(step) != Math.signum(lastStep)) {
                reversals++;
            }
            if (Math.abs(step) > 1.0E-4) lastStep = step;
        }
        lastY = y;
    }
}
