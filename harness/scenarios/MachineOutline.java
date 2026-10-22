import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * The hovered outline of a folded machine covers the whole machine (ntm-next). Shots: a hand-placed chemical plant
 * aimed at its front corner cell (01) and top middle cell (02), one 3x3x3 outline each; a centrifuge (03), whose cells
 * already report its silhouette, keeps vanilla's 4-tall outline. {@code hbm_outline} reports the drawn bounds.
 */
public final class MachineOutline implements Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 16);
        clear(c);
        c.cmd("gamemode creative");
        c.cmd("gamerule sendCommandFeedback false");
        c.markLog();

        int front = place(c, "hbm:machine_chemical_plant");
        c.cmd("clear");
        c.waitTicks(2);
        JsonObject corner = look(c, 1, G, front, "01-chemplant-corner");
        Expect.equal("1," + G + "," + front, corner.get("target").getAsString(), "crosshair on the corner cell");
        Expect.that(corner.get("replaced").getAsBoolean(), "corner outline is replaced: " + corner);
        expectSize(corner, 3, 3, 3);

        JsonObject top = look(c, 0, G + 2, front, "02-chemplant-top");
        Expect.that(top.get("replaced").getAsBoolean(), "top outline is replaced: " + top);
        expectSize(top, 3, 3, 3);
        clear(c);

        int centrifuge = place(c, "hbm:machine_centrifuge");
        c.cmd("clear");
        c.waitTicks(2);
        JsonObject tall = look(c, 0, G, centrifuge, "03-centrifuge");
        Expect.that(!tall.get("replaced").getAsBoolean(), "a silhouette machine keeps its own outline: " + tall);
        String[] bounds = tall.get("bounds").getAsString().split(" \\.\\. ");
        double height = Double.parseDouble(bounds[1].split(",")[1]) - Double.parseDouble(bounds[0].split(",")[1]);
        Expect.that(Math.abs(height - 4) < 1e-6, "centrifuge outline is 4 tall: " + tall);
        clear(c);

        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    private static void clear(Client c) {
        c.cmd("fill -16 " + G + " -16 16 " + (Y + 12) + " 16 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.waitTicks(3);
    }

    /** Places a machine by hand from south of the origin and answers the z of its front-most cell on x = 0. */
    private static int place(Client c, String item) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(0.5, G, -5.5);
        c.waitTicks(5);
        c.lookAt(0.5, Y + 0.98, -2.5);
        for (int attempt = 0; attempt < 3; attempt++) {
            c.waitTicks(5);
            c.use(2);
            c.waitTicks(10);
            for (int z = -4; z <= 6; z++) {
                if (!c.block(0, G, z).get("block").getAsString().equals("minecraft:air")) return z;
            }
        }
        Expect.that(false, item + " placed by hand");
        return 0;
    }

    /** Looks at the cell's north face from the side, screenshots, and reads the outline. */
    private static JsonObject look(Client c, int x, int y, int z, String shot) {
        c.teleport(x + 2.5, G, z - 3.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, y + 0.5, z - 0.02);
        c.waitTicks(20);
        c.screenshot(shot);
        return c.query("hbm_outline", new JsonObject());
    }

    private static void expectSize(JsonObject outline, double dx, double dy, double dz) {
        String[] corners = outline.get("bounds").getAsString().split(" \\.\\. ");
        String[] min = corners[0].split(","), max = corners[1].split(",");
        double[] size = new double[3];
        for (int i = 0; i < 3; i++) size[i] = Double.parseDouble(max[i]) - Double.parseDouble(min[i]);
        Expect.that(Math.abs(size[0] - dx) < 1e-6 && Math.abs(size[1] - dy) < 1e-6 && Math.abs(size[2] - dz) < 1e-6,
                "outline spans " + dx + "x" + dy + "x" + dz + ": " + outline);
    }
}
