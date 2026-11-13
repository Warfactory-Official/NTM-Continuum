import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * GitHub issue 181: a running 1.7 turbofan pulls a survival player standing in front of its intake into the blades
 * (its client repeats the 0.2 push for its own player, since the server never sends a player its motion), kills them
 * with {@code death.attack.blender} ("chopped in small, bite-sized pieces") and gibs whatever lies dead in the blades
 * every tick, five giblets a burst for a player (0.6 x 1.8: 2 x 1.5 x 7 = 21, divided by 5 rounded up).
 */
public final class Gh181TurbofanIntake implements Scenario {

    private static final int Y = 100;
    private static final int L = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        String hideGui = null;
        try {
            Fixtures.platform(c, 0, Y, 0, 16);
            c.cmd("gamemode creative");
            c.cmd("gamerule sendCommandFeedback false");
            c.cmd("gamerule showDeathMessages true");
            c.cmd("gamerule show_death_messages true");
            c.cmd("gamerule doImmediateRespawn false");
            c.cmd("gamerule immediate_respawn false");
            c.cmd("time set 6000");
            String core = place(c, "hbm:machine_turbofan[facing=north]", "TileEntityMachineTurbofan");
            String[] p = core.split(" ");
            double x = Integer.parseInt(p[0]) + 7.5, z = Integer.parseInt(p[2]) + 0.5;
            c.cmd("data merge block " + core + " {tank:24000,tank_type:\"hbm:kerosene\"}");
            c.waitTicks(40);
            c.teleport(x, L, z);
            c.lookAt(Integer.parseInt(p[0]) + 0.5, L + 1.5, z);
            hideGui = c.setOption("hide_gui", "false");
            c.send("clear_chat");
            c.waitTicks(2);
            c.cmd("gamemode survival");
            double nearest = x;
            boolean dead = false;
            for (int i = 0; i < 60 && !dead; i++) {
                c.waitTicks(1);
                JsonObject me = c.player();
                boolean inLane = Math.abs(me.get("z").getAsDouble() - z) <= 1D;
                dead = me.get("health").getAsDouble() <= 0D || !inLane;
                if (inLane) nearest = Math.min(nearest, me.get("x").getAsDouble());
                if (i == 1) c.screenshot("intake-pulled");
            }
            c.waitTicks(20);
            c.screenshot("intake-dead");
            int giblets = c.query("hbm_giblets", new JsonObject()).getAsJsonArray("giblets").size();
            List<String> chat = c.logSinceMark("chopped in small, bite-sized pieces|shredded by a sawmill");
            System.out.println("[Gh181] " + c.node() + " start x " + x + ", nearest x " + nearest + ", dead " + dead
                    + ", giblets 20 ticks after death " + giblets + ", death lines " + chat);
            Expect.that(x - nearest > 3D, "the intake pulls the player into the blades: x " + x + " to " + nearest);
            Expect.that(dead, "the blades kill the player");
            Expect.that(!chat.isEmpty() && chat.get(chat.size() - 1).contains("chopped in small, bite-sized pieces"),
                    "1.7's death message: " + chat);
            // a dead player stays in the blades until respawn: one burst of 5 a tick
            Expect.that(giblets > 20, "the dead player is gibbed every tick, not once: " + giblets);
            Expect.logCleanSinceMark(c, "Exception|/(ERROR|FATAL)[]].*hbm");
        } finally {
            if (c.screen().toString().contains("Respawn")) c.clickWidget("Respawn");
            c.waitTicks(10);
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            c.cmd("gamemode creative");
            c.cmd("fill -6 " + L + " -6 6 " + (L + 6) + " 6 minecraft:air");
            c.teleport(0.5, L, 0.5);
        }
    }

    /** Sets the block and answers the coordinates of the block entity it made. */
    private static String place(Client c, String block, String cls) {
        c.cmd("setblock 0 " + L + " 0 " + block);
        c.waitTicks(8);
        for (int dy = 0; dy <= 2; dy++) for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            JsonObject be = c.blockEntity(dx, L + dy, dz);
            if (be.has("block_entity") && !be.get("block_entity").isJsonNull()
                    && be.get("block_entity").getAsString().endsWith(cls)) {
                return dx + " " + (L + dy) + " " + dz;
            }
        }
        throw new AssertionError("no " + cls + " placed by " + block);
    }
}
