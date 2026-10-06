import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * Pre-release client wave (History §19es-client): the impacts of two newly launchable missiles on a real client, with
 * the sounds they play (Core's {@code [harness-sound]} log). Each missile is summoned 20 blocks above a stone target
 * with a straight-down motion (its flight from the pad is ReleaseMissilePad), so only the impact is under test.
 * <ul>
 *   <li>Drill (1.7 EntityMissileDrill.onMissileImpact): 30 stacked ExplosionNT(10, ERRODE) at posY - i, each plays
 *       vanilla's "random.explode" once ({@code doExplosionB}), so the client must start exactly 30
 *       {@code entity.generic.explode} sounds, at 30 different heights one block apart.</li>
 *   <li>Volcano (1.7 EntityMissileVolcano + TileEntityVolcanoCore): the impact places {@code volcano_core} with a 3x3x3
 *       of volcanic lava; the static active core then blasts and spews every 10 ticks (its explosions are NOSOUND in
 *       1.7) — the core must still stand after 20 s, keep its lava cube and spew lava blobs (shrapnel), and no explosion
 *       sound may come from the volcano's own blasts (only the missile's impact, ExplosionLarge.explode).</li>
 * </ul>
 * The world is reset afterwards.
 */
public final class ReleaseMissileImpacts implements Scenario {

    private static final int X = 0, Y = 100, Z = 0, L = 101;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        for (int i = 0; i < 3 && c.screenClass() != null; i++) { c.key("escape"); c.waitTicks(5); }
        c.useFlatWorld();
        Fixtures.platform(c, X, Y, Z, 6);
        c.markLog();
        c.setOption("sound_capture", "true");
        try {
            drill(c);
            volcano(c);
        } finally {
            c.setOption("sound_capture", "false");
            c.cmd("kill @e[type=!player]");
        }
        Expect.logCleanSinceMark(c, "(?i)(exception|unregistered hbm:|is not registered).*");
        c.resetWorld();
    }

    private static void target(Client c, int x, int z) {
        c.cmd("forceload add " + (x - 32) + " " + (z - 32) + " " + (x + 32) + " " + (z + 32));
        c.cmd("fill " + (x - 4) + " " + Y + " " + (z - 4) + " " + (x + 4) + " " + Y + " " + (z + 4) + " minecraft:stone");
        c.cmd("fill " + (x - 4) + " " + (Y + 1) + " " + (z - 4) + " " + (x + 4) + " " + (Y + 30) + " " + (z + 4) + " minecraft:air");
        c.waitTicks(20);
    }

    private static void summonDown(Client c, String entity, int x, int z) {
        double y = Y + 21;
        c.cmd("summon " + entity + " " + x + ".5 " + y + " " + z + ".5 {moX:0.0d,moY:-1.0d,moZ:0.0d,veloc:2.0d,"
                + "decel:0.0d,accel:0.0d,tX:" + x + ",tZ:" + z + ",sX:" + x + ",sZ:" + z
                + ",poX:" + x + ".5d,poY:" + y + "d,poZ:" + z + ".5d}");
    }

    private void drill(Client c) {
        int tx = X + 40, tz = Z;
        target(c, tx, tz);
        c.teleport(X + 15.5, L, Z + 0.5);
        c.lookAt(tx + 0.5, Y + 5.0, tz + 0.5);
        c.waitTicks(10);
        c.markLog();
        summonDown(c, "hbm:missile_drill", tx, tz);
        List<String> booms = List.of();
        for (int i = 0; i < 20; i++) {
            c.waitTicks(5);
            if (i == 3) c.screenshot("10-drill-impact");
            booms = c.logSinceMark("harness-sound.*id=minecraft:entity.generic.explode");
            if (booms.size() >= 30) break;
        }
        c.waitTicks(20);
        booms = c.logSinceMark("harness-sound.*id=minecraft:entity.generic.explode");
        java.util.Set<String> heights = new java.util.TreeSet<>();
        for (String line : booms) heights.add(line.replaceAll(".* y=([-0-9.]+) .*", "$1"));
        System.out.println(c.node() + " drill: " + booms.size() + " explode sounds at " + heights.size() + " heights " + heights);
        c.screenshot("11-drill-crater");
        Expect.that(booms.size() == 30, "the drill's 30 stacked ExplosionNT each play random.explode once: " + booms.size());
        Expect.that(heights.size() == 30, "one sound per blast height (posY - i): " + heights);
    }

    private void volcano(Client c) {
        int tx = X - 40, tz = Z;
        target(c, tx, tz);
        c.teleport(tx + 16.5, Y + 4, tz + 0.5);
        c.lookAt(tx + 0.5, Y + 3.0, tz + 0.5);
        c.waitTicks(10);
        c.markLog();
        summonDown(c, "hbm:missile_volcano", tx, tz);
        int[] core = null;
        for (int i = 0; i < 20 && core == null; i++) {
            c.waitTicks(5);
            core = findCore(c, tx, tz);
        }
        Expect.that(core != null, "the volcano missile placed hbm:volcano_core");
        List<String> impact = c.logSinceMark("harness-sound.*id=minecraft:entity.generic.explode");
        int lava0 = countLava(c, core);
        c.screenshot("20-volcano-impact");
        c.markLog();
        int shrapnelMax = 0;
        for (int i = 0; i < 8; i++) {
            c.waitTicks(50);
            if (i % 2 == 1) c.screenshot("2" + (i / 2 + 1) + "-volcano-" + ((i / 2 + 1) * 5) + "s");
            int n = 0;
            for (JsonElement e : c.entities(80)) {
                if (e.getAsJsonObject().get("type").getAsString().contains("shrapnel")) n++;
            }
            shrapnelMax = Math.max(shrapnelMax, n);
        }
        int[] still = findCore(c, tx, tz);
        int lava1 = countLava(c, core);
        List<String> later = c.logSinceMark("harness-sound.*id=minecraft:entity.generic.explode");
        int shrapnel = shrapnelMax;
        System.out.println(c.node() + " volcano: core " + java.util.Arrays.toString(core) + " -> " + java.util.Arrays.toString(still)
                + ", lava " + lava0 + " -> " + lava1 + ", impact booms " + impact.size() + ", later booms " + later.size()
                + ", shrapnel max " + shrapnel);
        Expect.that(still != null, "the static volcano core still stands after 20 s");
        Expect.that(lava0 >= 26, "the impact left the 3x3x3 lava cube around the core: " + lava0);
        Expect.that(lava1 >= 26, "the core keeps its 3x3x3 lava (1.7 surroundLava every 10 ticks): " + lava1);
        Expect.that(shrapnel >= 10, "the active core spews volcanic blobs (1.7 spawnBlobs every 10 ticks): " + shrapnel);
        Expect.that(later.isEmpty(), "the volcano's own blasts are NOSOUND in 1.7: " + later.size());
    }

    private static int[] findCore(Client c, int tx, int tz) {
        for (int y = Y + 3; y >= Y - 3; y--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    JsonObject b = c.block(tx + dx, y, tz + dz);
                    if (b.toString().contains("hbm:volcano_core")) return new int[]{tx + dx, y, tz + dz};
                }
            }
        }
        return null;
    }

    private static int countLava(Client c, int[] core) {
        int n = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (c.block(core[0] + dx, core[1] + dy, core[2] + dz).toString().contains("volcanic_lava")) n++;
                }
            }
        }
        return n;
    }
}
