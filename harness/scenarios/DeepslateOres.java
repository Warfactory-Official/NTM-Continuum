import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * The deepslate ore twins: each stands behind its stone ore, photographed from the front, from behind (the twin's
 * side) and from above (the deepslate end), so a missing model or a mismatched mineral pattern shows next to its twin.
 */
public final class DeepslateOres implements Scenario {

    private static final int X = 860;
    private static final int Y = 100;
    private static final int Z = 860;
    private static final int L = Y + 1;

    private static final List<String> ORES = List.of("uranium", "thorium", "titanium", "sulfur", "aluminium", "fluorite",
            "niter", "tungsten", "lead", "beryllium", "rare", "cinnabar", "cobalt", "lignite", "asbestos", "coltan");

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String forceload = (X - 16) + " " + (Z - 16) + " " + (X + 48) + " " + (Z + 16);
        String hideGui = null;
        try {
            c.cmd("forceload add " + forceload);
            Fixtures.platform(c, X + 16, Y, Z, 24);
            c.cmd("time set 6000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("weather clear");
            hideGui = c.setOption("hide_gui", "true");

            for (int i = 0; i < ORES.size(); i++) {
                Fixtures.setBlock(c, X + i * 2, L, Z, "hbm:ore_" + ORES.get(i));
                Fixtures.setBlock(c, X + i * 2, L, Z + 1, "hbm:ore_" + ORES.get(i) + "_deepslate");
            }
            c.waitTicks(10);
            List<String> missing = new ArrayList<>();
            for (int i = 0; i < ORES.size(); i++) {
                String id = "hbm:ore_" + ORES.get(i) + "_deepslate";
                JsonObject block = c.block(X + i * 2, L, Z + 1);
                if (!id.equals(block.get("block").getAsString())) missing.add(id + " " + block);
            }
            Expect.that(missing.isEmpty(), "every deepslate twin placed: " + missing);

            c.cmd("gamemode spectator");
            for (int part = 0; part < 4; part++) {
                double cx = X + part * 8 + 3.5;
                c.teleport(cx, L + 2.2, Z - 3.5);
                c.lookAt(cx, L + 0.5, Z + 1.0);
                c.waitTicks(15);
                c.screenshot(String.format("%d-front", part));
            }
            for (int part = 0; part < 4; part++) {
                double cx = X + part * 8 + 3.5;
                c.teleport(cx, L + 1.2, Z + 5.5);
                c.lookAt(cx, L + 0.5, Z + 1.0);
                c.waitTicks(15);
                c.screenshot(String.format("%d-back", part));
            }
            for (int part = 0; part < 2; part++) {
                double cx = X + part * 16 + 7.5;
                c.teleport(cx, L + 9, Z + 1.0);
                c.lookAt(cx, L, Z + 1.0);
                c.waitTicks(15);
                c.screenshot(String.format("%d-top", part + 4));
            }
        } finally {
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            c.cmd("forceload remove " + forceload);
        }
    }
}
