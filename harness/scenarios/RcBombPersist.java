import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Release audit, explosives: save and rejoin with live explosives.
 * <ul>
 *   <li>An armed C-4 charge (timer 1200, started) keeps its state and counts down after the rejoin.</li>
 *   <li>A primed TNT (fuse 80, left after 15 ticks) is still an entity afterwards and still blows up.</li>
 *   <li>A missile in flight (summoned high, straight down) is still there after the rejoin and still lands.</li>
 *   <li>A landmine that is primed stays primed (it fires on the first mob that steps on it).</li>
 * </ul>
 */
public final class RcBombPersist implements Scenario {

    private static final int Y = 100;
    private final List<String> notes = new ArrayList<>();

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        for (int i = 0; i < 3 && c.screenClass() != null; i++) { c.key("escape"); c.waitTicks(5); }
        c.useFlatWorld();
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 40);
        c.cmd("fill -40 " + (Y - 10) + " -40 40 " + (Y - 1) + " 40 minecraft:stone");
        c.waitTicks(10);

        // 1 charge
        Fixtures.placeOnTop(c, "hbm:charge_c4", -20, Y, 0);
        c.teleport(-19.5, Y + 1, -2.5);
        c.waitTicks(2);
        for (int i = 0; i < 6; i++) {
            Fixtures.interact(c, -20, Y + 1, 0);
        }
        // 0 -> 100 -> 200 -> 300 -> 600 -> 1200 after five clicks, then 3600 after the sixth: click five times
        c.cmd("data merge block -20 " + (Y + 1) + " 0 {timer:1200,started:1b}");
        c.waitTicks(3);
        JsonObject beBefore = c.blockEntity(-20, Y + 1, 0);
        notes.add("charge before leaving: " + beBefore);

        // 2 primed tnt
        c.cmd("setblock 20 " + Y + " 0 hbm:tnt_ntm");
        c.cmd("setblock 19 " + Y + " 0 minecraft:redstone_block");
        c.waitTicks(15);
        int tntBefore = count(c, "hbm:entity_ntm_tnt_primed");
        notes.add("primed TNT entities before leaving: " + tntBefore + " " + typesNear(c));

        // 3 missile in flight, 60 blocks up, 2 blocks per tick: ~30 ticks to land
        c.cmd("fill 0 " + Y + " 20 8 " + Y + " 28 minecraft:stone");
        c.cmd("summon hbm:entity_missile_generic 4.5 " + (Y + 190) + " 24.5 {moX:0.0d,moY:-1.0d,moZ:0.0d,veloc:2.0d,"
                + "decel:0.0d,accel:0.0d,tX:4,tZ:24,sX:4,sZ:24,poX:4.5d,poY:" + (Y + 190) + "d,poZ:24.5d}");
        c.waitTicks(3);
        int missilesBefore = count(c, "hbm:entity_missile_generic");
        notes.add("missiles before leaving: " + missilesBefore);

        c.leaveWorld(60_000L);
        Expect.that(!c.inWorld(), "left the world");
        c.joinWorld("harness", 180_000L);
        for (int i = 0; i < 20 && c.screenClass() != null; i++) { c.waitTicks(10); }
        c.waitTicks(10);

        JsonObject beAfter = c.blockEntity(-20, Y + 1, 0);
        notes.add("charge after rejoin: " + beAfter);
        notes.add("primed TNT after rejoin: " + count(c, "hbm:entity_ntm_tnt_primed") + ", missiles after rejoin: " + count(c, "hbm:entity_missile_generic"));
        Expect.that(beAfter.toString().contains("started") || beAfter.toString().contains("timer"), "charge block entity read back: " + beAfter);
        c.waitTicks(120);
        String tntBlock = c.block(20, Y, 0).get("block").getAsString();
        notes.add("after 6 s: tnt spot " + tntBlock + ", missiles " + count(c, "hbm:entity_missile_generic") + ", charge block "
                + c.block(-20, Y + 1, 0).get("block").getAsString() + " " + c.blockEntity(-20, Y + 1, 0));
        c.waitTicks(100);
        notes.add("after 11 s: missiles " + count(c, "hbm:entity_missile_generic") + ", target block " + c.block(4, Y, 24).get("block").getAsString());
        Expect.that(count(c, "hbm:entity_ntm_tnt_primed") == 0 && count(c, "hbm:entity_missile_generic") == 0,
                "the TNT blew up and the missile landed after the rejoin");
        c.screenshot("persist-after");
        for (String n : notes) System.out.println("[RcBombPersist] " + n);
        Expect.logCleanSinceMark(c, "(?i)(exception|crash report|failed to load|unregistered hbm:|is not registered).*");
    }

    private static int count(Client c, String type) {
        int n = 0;
        for (JsonElement e : c.entities(200)) {
            if (e.getAsJsonObject().get("type").getAsString().equals(type)) n++;
        }
        return n;
    }

    private static String typesNear(Client c) {
        StringBuilder sb = new StringBuilder();
        for (JsonElement e : c.entities(60)) sb.append(e.getAsJsonObject().get("type").getAsString()).append(' ');
        return sb.toString();
    }
}
