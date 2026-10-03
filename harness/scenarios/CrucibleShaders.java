import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * The first-person crucible blade with 1.12's shader effects (Golem Core {@code ScreenEffects}): translucent blade,
 * bloom composited after the hand, and the heat-distortion shell. A striped wool wall stands behind the blade so the
 * distortion is visible; shots are taken by day and by night (bloom reads best on a dark background), a few frames
 * apart so the animated noise can be compared, plus the spent crucible (no blade, no layers) as the reference.
 *
 * <p>Positive signal: Core's {@code golem_screen_effects} query counts every bloom and distortion composite, so the
 * check holds whenever the scenario runs. (It used to wait for Core's once-per-session "composited after the hand" log
 * line, which a client that had already drawn the blade earlier, e.g. at world join, wrote before the log mark.)
 * The charged blade must composite both layers, at the main target's size with bloom level 0 at half of it; the spent
 * crucible must composite neither.
 */
public final class CrucibleShaders implements Scenario {

    private static final int X = 260;
    private static final int Y = 100;
    private static final int Z = 260;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 8);
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("gamerule doMobSpawning false");
        c.cmd("weather clear");
        c.cmd("gamemode creative");
        String[] wool = {"white", "black", "red", "yellow", "blue", "lime"};
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = 1; dy <= 5; dy++) {
                Fixtures.setBlock(c, X + dx, Y + dy, Z + 3, "minecraft:" + wool[Math.floorMod(dx + dy, wool.length)] + "_wool");
            }
        }
        c.setOption("camera", "first_person");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, 10);

        c.cmd("time set 6000");
        Fixtures.giveOnly(c, "hbm:crucible", 1); // old-hbm: crafted empty
        c.waitTicks(30);
        c.screenshot("00-day-spent");
        JsonObject before = effects(c);

        Fixtures.giveOnly(c, c.node().startsWith("1.20") ? "hbm:crucible{\"hbm:charges\":16}"
                : "hbm:crucible[hbm:charges=16]", 1);
        c.waitTicks(40);
        c.screenshot("01-day-blade-a");
        c.waitTicks(7);
        c.screenshot("02-day-blade-b");
        c.screenshot("03-day-blade-crop", new int[]{280, 60, 480, 400});
        JsonObject charged = effects(c);
        for (String effect : new String[] {"bloom", "distortion"}) {
            long added = composites(charged, effect) - composites(before, effect);
            Expect.that(added >= 3, effect + " composited while the charged blade is held: +" + added + " (" + charged + ")");
            JsonObject layer = charged.getAsJsonObject(effect);
            Expect.that(layer.get("width").getAsInt() > 0 && layer.get("height").getAsInt() > 0, effect + " layer size: " + layer);
        }
        // bloom chain: built for the layer's (main target's) size, level 0 = ceil(W/2) x ceil(H/2)
        var chain = charged.getAsJsonArray("bloom_chain");
        Expect.that(chain.size() >= 4, "bloom chain built: " + chain);
        int w = chain.get(0).getAsInt(), h = chain.get(1).getAsInt();
        JsonObject bloomLayer = charged.getAsJsonObject("bloom");
        Expect.equal(bloomLayer.get("width").getAsInt() + "x" + bloomLayer.get("height").getAsInt(), w + "x" + h,
                "bloom chain screen size");
        Expect.equal((w + 1) / 2 + "x" + (h + 1) / 2, chain.get(2).getAsInt() + "x" + chain.get(3).getAsInt(),
                "bloom level 0 is half of " + w + "x" + h);

        c.cmd("time set 18000");
        c.waitTicks(10);
        c.screenshot("04-night-blade");
        Fixtures.giveOnly(c, "hbm:crucible", 1); // old-hbm: crafted empty
        c.waitTicks(30);
        c.screenshot("05-night-spent");
        // the spent crucible draws no blade, so neither layer is composited any more
        JsonObject spentA = effects(c);
        c.waitTicks(10);
        JsonObject spentB = effects(c);
        for (String effect : new String[] {"bloom", "distortion"}) {
            Expect.equal(composites(spentA, effect), composites(spentB, effect), effect + " composites with the spent crucible");
        }

        c.cmd("time set 6000");
        c.cmd("clear");
        List<String> bad = c.logSinceMark("/(ERROR|FATAL)[]]|Exception|screen_(bloom|distortion|blur|downsample)");
        List<String> relevant = new ArrayList<>();
        for (String line : bad) if (!line.contains("Loaded ") && !line.contains("composited")) relevant.add(line);
        Expect.that(relevant.isEmpty(), "log clean since the mark: " + relevant);
    }

    private static JsonObject effects(Client c) {
        return c.query("golem_screen_effects", new JsonObject());
    }

    private static long composites(JsonObject effects, String effect) {
        return effects.has(effect) ? effects.getAsJsonObject(effect).get("composites").getAsLong() : 0L;
    }
}
