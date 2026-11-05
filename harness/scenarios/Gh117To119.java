import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Issues 117 and 119 by outcome, in real flights. A missile aimed at a stone pad craters it unless it is shot down (119);
 * a CIWS under the arc spends belt rounds on it (117).
 */
public final class Gh117To119 implements Scenario {

    private static final int Y = 120;
    private static final int TX = 300;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(180_000L);
        c.cmd("gamemode creative");
        c.cmd("kill @e[type=!player]");
        c.teleport(150, Y + 40, 0);
        c.cmd("forceload add -32 -32 340 32");
        c.waitTicks(100);
        // a flight saved by an interrupted run wakes up with its chunk
        c.cmd("kill @e[type=!player]");
        c.waitServerTicks(5);

        pad(c);
        c.cmd(missile());
        waitGone(c);
        int control = padBlocks(c);
        System.out.println("[Gh119] " + c.node() + " unopposed: " + control + " of 441 pad blocks left");
        Expect.that(control < 441, "An unopposed missile must crater the pad");

        // 119: ABM fired at the threat's apex, as a smart-mode radar does, from beside the aim point
        pad(c);
        c.cmd(missile());
        waitDescending(c);
        c.cmd("summon hbm:entity_missile_ab " + TX + " " + Y + " 0");
        waitGone(c);
        int intercepted = padBlocks(c);
        System.out.println("[Gh119] " + c.node() + " with ABM: " + intercepted + " of 441 pad blocks left");
        Expect.that(intercepted == 441, "The ABM must stop the missile before the pad, left " + intercepted);

        int tx = TX - 40;
        int tz = 25;
        c.cmd("setblock " + tx + " " + Y + " " + tz + " hbm:turret_howard");
        c.waitServerTicks(5);
        c.cmd("data merge block " + tx + " " + Y + " " + tz + " {power:50000L,isOn:1b,targetPlayers:1b}");
        c.cmd("item replace block " + tx + " " + Y + " " + tz + " container.1 with hbm:ammo_dgk 4");
        c.cmd("item replace block " + tx + " " + Y + " " + tz + " container.10 with hbm:battery_creative");
        c.waitServerTicks(5);
        c.cmd("gamemode spectator");
        int before = loaded(c, tx, tz);
        pad(c);
        c.cmd(missile());
        waitGone(c);
        int after = loaded(c, tx, tz);
        System.out.println("[Gh117] " + c.node() + " CIWS belt " + before + " -> " + after);
        Expect.that(after < before, "The CIWS must fire on a descending missile, belt " + before + " -> " + after);
        c.cmd("gamemode creative");
        c.cmd("setblock " + tx + " " + Y + " " + tz + " minecraft:air");
    }

    private static void pad(Client c) {
        c.cmd("fill " + (TX - 10) + " " + (Y - 1) + " -10 " + (TX + 10) + " " + (Y - 1) + " 10 minecraft:stone");
    }

    private static int padBlocks(Client c) {
        int left = 0;
        for (int x = TX - 10; x <= TX + 10; x++) {
            for (int z = -10; z <= 10; z++) {
                if (c.block(x, Y - 1, z).toString().contains("minecraft:stone")) left++;
            }
        }
        return left;
    }

    private static int loaded(Client c, int x, int z) {
        Matcher m = Pattern.compile("loaded: (\\d+)").matcher(c.blockEntity(x, Y, z).get("nbt").getAsString());
        Expect.that(m.find(), "CIWS has no belt count");
        return Integer.parseInt(m.group(1));
    }

    private static String missile() {
        return "summon hbm:entity_missile_generic 0 " + Y + " 0 {poX:0d,poY:" + Y + "d,poZ:0d,moY:2d,decel:" + (2D / TX)
                + "d,accel:" + (1D / TX) + "d,tX:" + TX + ",tZ:0,sX:0,sZ:0,veloc:0d}";
    }

    private static boolean any(Client c, boolean descending) {
        JsonObject q = new JsonObject();
        q.addProperty("radius", 1000);
        q.addProperty("server", true);
        for (JsonElement e : c.query("entities", q).getAsJsonArray("entities")) {
            JsonObject o = e.getAsJsonObject();
            if (!o.get("type").getAsString().endsWith("missile_generic")) continue;
            if (!descending || o.get("vy").getAsDouble() < 0) return true;
        }
        return false;
    }

    private static void waitDescending(Client c) {
        for (int i = 0; i < 600 && !any(c, true); i++) c.waitServerTicks(1);
        Expect.that(any(c, true), "The missile never came down");
    }

    private static void waitGone(Client c) {
        c.waitServerTicks(2);
        Expect.that(any(c, false), "The summoned missile never appeared");
        for (int i = 0; i < 1200 && any(c, false); i++) c.waitServerTicks(1);
        Expect.that(!any(c, false), "The missile is still flying");
        c.waitServerTicks(40);
    }
}
