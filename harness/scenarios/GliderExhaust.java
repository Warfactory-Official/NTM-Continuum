import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * The worn glider jetpack with its exhaust (development {@code /hbm jetpack_smoke} session: glider worn, fuelled,
 * hovering), seen from behind and in front, by night and by day, then with a charged crucible in first person on top.
 *
 * <p>Positive signals: {@code hbm_static_models} reports live exhaust effects (plasma, heat distortion, brightness)
 * while hovering; Core's {@code golem_static_meshes} reports the glider's 17 Collada meshes (and the crucible's 5)
 * resident and drawn from the GPU copy with no new fallback; and no shader or render error since the mark.
 */
public final class GliderExhaust implements Scenario {

    private static final int X = 340;
    private static final int Y = 100;
    private static final int Z = 340;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 8);
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("gamerule doMobSpawning false");
        c.cmd("weather clear");
        c.cmd("gamemode creative");
        String charged = c.node().startsWith("1.20") ? "hbm:crucible{\"hbm:charges\":16}" : "hbm:crucible[hbm:charges=16]";
        try {
            JsonObject start = meshes(c);
            boolean resident = start.get("active").getAsBoolean();
            for (String time : new String[] {"night", "day"}) {
                c.cmd("time set " + (time.equals("night") ? 18000 : 6000));
                c.cmd("hbm jetpack_smoke stop");
                c.cmd("clear");
                c.waitTicks(5);
                c.teleport(X + 0.5, Y + 1, Z + 0.5);
                c.setOption("camera", "third_person_back");
                c.cmd("hbm jetpack_smoke");
                c.waitTicks(5);
                c.look(0, 10);
                c.waitTicks(40);
                c.look(0, 10);
                JsonObject before = meshes(c);
                c.screenshot(time + "-glider-back");
                c.setOption("camera", "third_person_front");
                c.waitTicks(5);
                c.screenshot(time + "-glider-front");
                c.setOption("camera", "third_person_back");
                c.waitTicks(5);
                JsonObject exhaust = c.query("hbm_static_models", new JsonObject());
                int effects = exhaust.get("exhaust_plasma").getAsInt() + exhaust.get("exhaust_heat").getAsInt()
                        + exhaust.get("exhaust_brightness").getAsInt();
                Expect.that(effects > 0, time + ": exhaust effects live while hovering: " + exhaust);
                JsonObject after = meshes(c);
                if (resident) {
                    Expect.that(after.get("meshes").getAsInt() >= 17, time + ": glider meshes resident: " + after);
                    Expect.that(after.get("draws").getAsLong() > before.get("draws").getAsLong(),
                            time + ": resident draws advance: " + before + " -> " + after);
                    Expect.equal(before.get("fallbacks").getAsLong(), after.get("fallbacks").getAsLong(),
                            time + ": no resident-draw fallback");
                }

                // glider + charged crucible in first person: the crucible's hilt, guards and arms join the resident set
                Fixtures.giveOnly(c, charged, 1);
                c.setOption("camera", "first_person");
                c.waitTicks(40);
                c.screenshot(time + "-glider-crucible-fp");
                JsonObject withCrucible = meshes(c);
                if (resident) {
                    Expect.that(withCrucible.get("meshes").getAsInt() >= 22, time + ": glider + crucible meshes: " + withCrucible);
                    Expect.equal(after.get("fallbacks").getAsLong(), withCrucible.get("fallbacks").getAsLong(),
                            time + ": no resident-draw fallback with the crucible");
                }
            }
            JsonObject end = meshes(c);
            Expect.that(!end.get("failed").getAsBoolean(), "resident meshes did not fail: " + end);
            Expect.equal(0, end.get("rejected").getAsInt(), "no geometry rejected");
        } finally {
            c.cmd("hbm jetpack_smoke stop");
            c.setOption("camera", "first_person");
            c.cmd("clear");
            c.cmd("time set 6000");
        }
        List<String> bad = c.logSinceMark("/(ERROR|FATAL)[]]|Exception|static_mesh|screen_(bloom|distortion|blur|downsample|upsample)");
        List<String> relevant = new ArrayList<>();
        for (String line : bad) if (!line.contains("Loaded ") && !line.contains("composited")) relevant.add(line);
        Expect.that(relevant.isEmpty(), "log clean since the mark: " + relevant);
    }

    private static JsonObject meshes(Client c) {
        return c.query("golem_static_meshes", new JsonObject());
    }
}
