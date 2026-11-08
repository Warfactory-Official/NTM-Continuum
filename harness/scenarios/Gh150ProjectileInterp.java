import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 150, siblings: 1.7 moves its {@code EntityThrowableInterp} projectiles on the client only inside the
 * client tick, towards the server's positions, so a frame never sees one jump between ticks. Each is launched at a
 * slow tick rate and every frame's position and angles are compared with the last frame of the same tick.
 */
public final class Gh150ProjectileInterp implements Scenario {

    private static final int Y = 100;

    private record Shot(String type, String nbt, int ticks) {}

    private static final Shot[] SHOTS = {
            new Shot("hbm:entity_missile_generic", "{moX:0.0d,moY:2.0d,moZ:0.0d,veloc:0.5d,decel:0.01d,accel:0.005d,"
                    + "tX:0,tZ:-300,sX:0,sZ:-12}", 12),
            new Shot("hbm:entity_missile_ab", "{}", 12),
            new Shot("hbm:entity_himars_rocket", "{Motion:[0.0d,0.8d,-0.6d]}", 12),
            new Shot("hbm:entity_coin", "{Motion:[0.0d,0.7d,-0.3d]}", 12),
            new Shot("hbm:entity_sawblade", "{Motion:[0.2d,0.6d,-0.3d]}", 12),
    };

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        List<String> failures = new ArrayList<>();
        try {
            Fixtures.platform(c, 0, Y, 0, 8);
            c.cmd("gamemode creative");
            c.cmd("gamerule sendCommandFeedback false");
            c.cmd("time set 6000");
            c.teleport(0.5, Y + 1, 0.5);
            c.look(180, -30);
            c.window(false);
            for (Shot shot : SHOTS) {
                JsonObject start = new JsonObject();
                start.addProperty("type", shot.type());
                c.query("hbm_frame_moves", start);
                float rate = c.tickRate(4F);
                c.cmd("summon " + shot.type() + " 0.5 " + (Y + 3) + " -6.5 " + shot.nbt());
                c.waitTicks(3);
                c.screenshot(shot.type().substring(4));
                c.waitTicks(shot.ticks());
                JsonObject result = c.query("hbm_frame_moves", new JsonObject());
                c.tickRate(rate);
                c.cmd("kill @e[type=" + shot.type() + "]");
                c.waitTicks(10);
                int frames = result.get("frames").getAsInt();
                int moves = result.get("moves").getAsInt();
                double travel = result.get("travel").getAsDouble();
                System.out.println("[Gh150] " + c.node() + " " + shot.type() + " frames " + frames + ", moves between ticks "
                        + moves + ", largest " + result.get("largest").getAsDouble() + ", travel " + travel);
                if (frames < 10) failures.add(shot.type() + " only " + frames + " frames");
                if (travel < 2D) failures.add(shot.type() + " travelled only " + travel + " blocks on the client");
                if (moves > 0) failures.add(shot.type() + " moved between ticks in " + moves + " frames");
            }
            Expect.that(failures.isEmpty(), "projectiles move only in the client tick, as 1.7's: " + failures);
            Expect.logCleanSinceMark(c, "Exception|/(ERROR|FATAL)[]].*hbm");
        } finally {
            c.tickRate(20F);
            c.window(true);
            c.teleport(0.5, Y + 1, 0.5);
        }
    }
}
