import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 175, sibling: the tram trailer's cargo is 1.7's {@code RenderTrainCargoTramTrailer} pile of framed wooden
 * crates at twice their size. A trailer holding 18 stacks (four crates) is shot from the side and from above.
 */
public final class Gh175TramCrates implements Scenario {

    private static final int X = 2030;
    private static final int Y = 100;
    private static final int Z = 2030;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 10);
        c.cmd("time set 6000");
        c.cmd("kill @e[type=hbm:entity_cargo_tram_trailer]");
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < 18; i++) {
            if (i > 0) items.append(',');
            items.append("{Slot:").append(i).append("b,id:\"minecraft:stone\",count:1,Count:1b}");
        }
        c.cmd("summon hbm:entity_cargo_tram_trailer " + (X + 0.5) + " " + (Y + 1) + " " + (Z + 0.5)
                + " {Items:[" + items + "]}");
        String hide = c.setOption("hide_gui", "true");
        try {
            c.teleport(X + 4.5, Y + 2.5, Z + 0.5);
            c.lookAt(X + 0.5, Y + 1.5, Z + 0.5);
            c.waitTicks(10);
            c.screenshot("trailer-side");
            c.teleport(X + 2.5, Y + 4.5, Z + 2.5);
            c.lookAt(X + 0.5, Y + 1.5, Z + 0.5);
            c.waitTicks(5);
            c.screenshot("trailer-above");
        } finally {
            if (hide != null) c.setOption("hide_gui", hide);
        }
        JsonObject find = new JsonObject();
        find.addProperty("radius", 30);
        find.addProperty("type", "cargo_tram_trailer");
        String seen = c.query("entities", find).toString();
        System.out.println("[Gh175Tram] " + c.node() + " " + seen);
        c.cmd("kill @e[type=hbm:entity_cargo_tram_trailer]");
        Expect.that(seen.contains("cargo_tram_trailer"), "the trailer stayed: " + seen);
        Expect.logCleanSinceMark(c, "/(Exception|ERROR|FATAL)[]]?");
    }
}
