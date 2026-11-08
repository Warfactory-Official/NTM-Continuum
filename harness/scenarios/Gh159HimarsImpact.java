import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 159, the HIMARS sibling: 1.7 plays the standard rocket's blast effect at {@code mop.blockX + 0.5} (and Y, Z),
 * the struck block's centre, not at the hit point. The blast's flying chunks leave from where the effect plays, so their
 * origin must sit on a block centre (all three coordinates .5).
 */
public final class Gh159HimarsImpact implements Scenario {

    private static final int X = 1730;
    private static final int Y = 100;
    private static final int Z = 1700;
    private static final int RANGE = 60;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z + RANGE / 2, RANGE / 2 + 12);
        Fixtures.fill(c, X - 12, Y - 2, Z - 12, X + 12, Y - 1, Z + RANGE + 12, "minecraft:stone");
        c.cmd("time set 6000");
        c.cmd("gamemode creative");
        Fixtures.setBlock(c, X, Y + 1, Z, "hbm:turret_himars[facing=north]");
        c.cmd("data merge block " + X + " " + (Y + 1) + " " + Z + " {power:100000L,isOn:1b,mode:1s,ammo:6,type:\"SMALL\",crane:0.0f}");
        c.teleport(X + 0.5, Y + 1, Z + RANGE - 16.5);
        c.waitTicks(5);
        c.lookAt(X + 0.5, Y + 2, Z + RANGE + 0.5);
        JsonObject cmd = new JsonObject();
        JsonArray a = new JsonArray();
        for (int v : new int[]{X, Y + 1, Z, X, Y + 1, Z + RANGE}) a.add(v);
        cmd.add("command", a);
        System.out.println("[Gh159] " + c.node() + " himars command " + c.query("hbm_gapsc", cmd).get("command_accepted"));

        JsonObject far = new JsonObject();
        far.addProperty("x", X + 0.5);
        far.addProperty("y", Y + 1);
        far.addProperty("z", Z + RANGE + 0.5);
        JsonArray debris = new JsonArray();
        for (int i = 0; i < 1200 && debris.isEmpty(); i++) {
            debris = c.query("hbm_turret_fx", far).getAsJsonArray("debris");
            if (debris.isEmpty()) c.waitTicks(1);
        }
        c.screenshot("himars-impact");
        c.cmd("data merge block " + X + " " + (Y + 1) + " " + Z + " {isOn:0b}");
        Expect.that(!debris.isEmpty(), "the rocket's blast threw debris");
        JsonArray first = debris.get(0).getAsJsonArray();
        double x = first.get(3).getAsDouble();
        double y = first.get(4).getAsDouble();
        double z = first.get(5).getAsDouble();
        System.out.println("[Gh159] " + c.node() + " himars blast effect at " + x + ", " + y + ", " + z);
        Expect.that(half(x) && half(y) && half(z), "1.7 plays the blast at the struck block's centre: " + x + ", " + y + ", " + z);
        for (JsonElement e : debris) {
            JsonArray d = e.getAsJsonArray();
            Expect.that(d.get(0).getAsDouble() == 0D, "the standard rocket throws no visible chunks (1.7 debris size 0)");
        }
        Expect.logCleanSinceMark(c, "hbm.*Exception|Exception.*hbm|/(ERROR|FATAL)[]].*hbm");
    }

    private static boolean half(double v) {
        return Math.abs(v - Math.floor(v) - 0.5D) < 1.0E-6D;
    }
}
