import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Additive effects seen from every side (2026-10-06): the FEnSU's arcs and the Maxwell's microwave spirals had been
 * drawn through culled render types ({@code dragonRays()} / {@code lightning()}), while 1.7 drew them with
 * {@code glDisable(GL_CULL_FACE)}. BeamPronter's prism faces are not consistently wound, so the beams lost faces by
 * view direction and faded out as the camera walked round them. Both now use Core's {@code additive*()} types.
 *
 * <p>A powered FEnSU (REDD) and a powered Maxwell firing at an invulnerable, AI-less zombie are each photographed from
 * eight compass points at one radius. Every shot must show the effect at about the same brightness; before the fix,
 * one half of the circle showed thin or missing beams.
 */
public final class AdditiveBeamAngles implements Scenario {

    private static final int X = 700;
    private static final int Y = 100;
    private static final int Z = 700;
    private static final int L = Y + 1;
    private static final int REDD_X = X - 12;
    private static final int MAX_X = X + 12;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String forceload = (X - 40) + " " + (Z - 40) + " " + (X + 40) + " " + (Z + 40);
        String hideGui = null, fov = null;
        try {
            c.cmd("forceload add " + forceload);
            Fixtures.platform(c, X, Y, Z, 34);
            c.cmd("time set 18000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("weather clear");
            c.cmd("difficulty easy");
            hideGui = c.setOption("hide_gui", "true");
            fov = c.setOption("fov", "70");

            Fixtures.setBlock(c, REDD_X, L, Z, "hbm:machine_battery_redd");
            c.cmd("data merge block " + REDD_X + " " + L + " " + Z + " {power:\"1000000000000000000\"}");

            Fixtures.setBlock(c, MAX_X, L, Z, "hbm:turret_maxwell");
            c.cmd("data merge block " + MAX_X + " " + L + " " + Z
                    + " {power:10000000L,isOn:1b,targetMobs:1b,targetPlayers:0b}");
            c.cmd("summon minecraft:zombie " + (MAX_X + 0.5) + " " + L + " " + (Z + 9.5)
                    + " {NoAI:1b,Invulnerable:1b,PersistenceRequired:1b,Silent:1b,Tags:[\"beam_target\"]}");

            c.cmd("gamemode spectator");
            c.waitTicks(60);

            JsonObject redd = c.block(REDD_X, L, Z);
            Expect.that(redd.get("block").getAsString().equals("hbm:machine_battery_redd"), "REDD placed: " + redd);
            JsonObject maxwell = c.block(MAX_X, L, Z);
            Expect.that(maxwell.get("block").getAsString().equals("hbm:turret_maxwell"), "Maxwell placed: " + maxwell);

            // The REDD picks which arcs to draw at random every frame: two passes so a gap is not mistaken for chance.
            ring(c, "redd", REDD_X + 0.5, L + 5.5, Z + 0.5, 13.0, 3.0, null);
            ring(c, "redd-b", REDD_X + 0.5, L + 5.5, Z + 0.5, 13.0, 3.0, null);
            // 10 M buffer at 100 k/tick lasts five seconds: refilled before every shot.
            String refill = "data merge block " + MAX_X + " " + L + " " + Z + " {power:10000000L}";
            // A slow first client can photograph an idle turret and still pass: require the buffer to drain first.
            c.cmd(refill);
            c.waitTicks(20);
            String nbt = c.blockEntity(MAX_X, L, Z).get("nbt").getAsString();
            Expect.that(!nbt.contains("power: 10000000L"), "Maxwell is firing (power drains): " + nbt);
            ring(c, "maxwell", MAX_X + 1.0, L + 2.0, Z + 5.0, 9.0, 4.0, refill);
        } finally {
            c.cmd("kill @e[tag=beam_target]");
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            if (fov != null) c.setOption("fov", fov);
            c.cmd("forceload remove " + forceload);
        }
    }

    /** Eight shots on a circle round (cx, cy, cz), each looking at the centre from {@code up} blocks above it. */
    private static void ring(Client c, String name, double cx, double cy, double cz, double radius, double up,
                             String beforeShot) {
        String[] points = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45.0);
            double x = cx + Math.sin(angle) * radius;
            double z = cz - Math.cos(angle) * radius;
            if (beforeShot != null) c.cmd(beforeShot);
            c.teleport(x, cy + up, z);
            c.lookAt(cx, cy, cz);
            c.waitTicks(10);
            c.screenshot(name + "-" + i + "-" + points[i]);
        }
    }
}
