import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;
import java.util.Map;
import java.util.TreeMap;

/**
 * GitHub issue 69: the Torex shockwave follows the terrain, as 1.7's per-cloud {@code getHeightValue}. A benchmark
 * cloud goes off on the superflat floor beside a 10-block stone plateau; its shock clouds must sit both on the floor
 * and on the plateau top ({@code hbm_torex} shock_y). Then a real Fat Man beside the same plateau, shot at 3 and 6
 * seconds and its shock heights printed.
 */
public final class Gh69ShockwaveTerrain implements Scenario {

    private static final int G = -60;
    private static final int TOP = G + 10;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        for (int i = 0; i < 3 && c.screenClass() != null; i++) { c.key("escape"); c.waitTicks(5); }
        c.useFlatWorld();
        c.markLog();
        c.cmd("gamemode creative");
        c.cmd("gamerule mobGriefing true");
        c.cmd("time set 6000");
        c.cmd("weather clear");
        String hideGui = c.setOption("hide_gui", "true");
        try {
            Map<Integer, Integer> fixture = fixture(c, 1200, 1200);
            System.out.println("[Gh69] benchmark shock clouds by y " + fixture);
            Expect.that(fixture.getOrDefault(G, 0) > 0, "shock clouds lie on the floor: " + fixture);
            Expect.that(fixture.getOrDefault(TOP, 0) > 0, "shock clouds lie on the plateau top: " + fixture);

            Map<Integer, Integer> fatMan = fatMan(c, 4000, 1200);
            System.out.println("[Gh69] Fat Man shock clouds by y " + fatMan);
            Expect.logCleanSinceMark(c, "(?i)(exception|crash report).*");
        } finally {
            c.cmd("hbm torex_benchmark stop");
            c.cmd("kill @e[type=!player]");
            c.cmd("forceload remove all");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
        }
    }

    /** The benchmark cloud spawns 240 blocks south of the player; the plateau lies between them. */
    private static Map<Integer, Integer> fixture(Client c, int x, int z) {
        int gz = z + 240;
        c.cmd("forceload add " + (x - 64) + " " + (gz - 96) + " " + (x + 64) + " " + (gz + 64));
        plateau(c, x, gz);
        c.teleport(x + 0.5, G, z + 0.5);
        c.waitTicks(40);
        c.cmd("hbm torex_benchmark 1");
        c.teleport(x + 0.5, G, gz - 75 + 0.5);
        c.lookAt(x + 0.5, G + 10, gz + 0.5);
        Map<Integer, Integer> seen = watch(c, "fixture");
        c.cmd("hbm torex_benchmark stop");
        return seen;
    }

    private static Map<Integer, Integer> fatMan(Client c, int x, int z) {
        c.cmd("forceload add " + (x - 64) + " " + (z - 96) + " " + (x + 64) + " " + (z + 64));
        plateau(c, x, z);
        c.teleport(x + 0.5, G + 6, z - 4.5);
        c.waitTicks(40);
        c.cmd("setblock " + x + " " + G + " " + z + " hbm:nuke_man");
        c.waitTicks(4);
        String[] fill = {"hbm:man_igniter", "hbm:gadget_explosive8", "hbm:gadget_explosive8", "hbm:gadget_explosive8",
                "hbm:gadget_explosive8", "hbm:man_core"};
        for (int i = 0; i < fill.length; i++) {
            c.cmd("item replace block " + x + " " + G + " " + z + " container." + i + " with " + fill[i] + " 1");
        }
        c.waitTicks(4);
        c.teleport(x + 0.5, G, z - 75 + 0.5);
        c.lookAt(x + 0.5, G + 10, z + 0.5);
        c.waitTicks(10);
        c.cmd("setblock " + (x + 1) + " " + G + " " + z + " minecraft:redstone_block");
        return watch(c, "fatman");
    }

    private static void plateau(Client c, int x, int gz) {
        c.cmd("fill " + (x - 40) + " " + G + " " + (gz - 60) + " " + (x + 40) + " " + (TOP - 1) + " " + (gz - 30)
                + " minecraft:stone");
        c.waitTicks(5);
    }

    /** The largest count of shock clouds seen at each block y over the first six seconds. */
    private static Map<Integer, Integer> watch(Client c, String label) {
        Map<Integer, Integer> most = new TreeMap<>();
        for (int t = 10; t <= 120; t += 10) {
            c.waitTicks(10);
            JsonArray clouds = c.query("hbm_torex", new JsonObject()).getAsJsonArray("clouds");
            for (int i = 0; i < clouds.size(); i++) {
                JsonObject shock = clouds.get(i).getAsJsonObject().getAsJsonObject("shock_y");
                for (String y : shock.keySet()) most.merge(Integer.parseInt(y), shock.get(y).getAsInt(), Math::max);
            }
            if (t == 60 || t == 120) c.screenshot(label + "-" + t);
        }
        return most;
    }
}
