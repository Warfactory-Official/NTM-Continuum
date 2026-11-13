import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 185 in the viewers: opens the acidizer, liquefaction, solidification, fractioning, refinery and
 * hydrotreating tabs in each of JEI, EMI and REI the run has, hovers the first row's fluids and shoots the tooltip,
 * which must read "N mB". EMI needs {@code ORG_GRADLE_PROJECT_emi.run=true harness up --rebuild} (1.21.1 and 1.20.1; it
 * then replaces JEI), REI {@code ORG_GRADLE_PROJECT_rei.run=true} (beside JEI).
 */
public final class Gh185ViewerTooltips implements Scenario {

    /** Category, then the GUI points of its first row's fluid slots in JEI, EMI and REI (x, y). */
    private static final Object[][] TABS = {
            {"crystallizer", new int[]{292, 125}, new int[]{286, 121}, new int[]{292, 117}},
            {"liquefaction", new int[]{346, 125}, new int[]{340, 121}, new int[]{346, 117}},
            {"solidification", new int[]{292, 125}, new int[]{286, 121}, new int[]{292, 117}},
            {"fractioning", new int[]{292, 125, 346, 125}, new int[]{286, 121, 340, 121}, new int[]{292, 117, 346, 117}},
            {"refinery", new int[]{292, 125, 346, 114}, new int[]{286, 121, 340, 121}, new int[]{292, 117, 346, 104}},
            {"hydrotreating", new int[]{292, 125}, new int[]{286, 121}, new int[]{292, 117}},
    };
    private static final String[] VIEWERS = {"jei", "emi", "rei"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(180_000L);
        c.waitTicks(100);
        int ran = 0;
        for (int v = 0; v < VIEWERS.length; v++) {
            String viewer = VIEWERS[v];
            if (!has(c, viewer)) continue;
            ran++;
            c.key("e");
            c.waitTicks(10);
            for (Object[] tab : TABS) {
                String category = "hbm:" + tab[0];
                JsonObject p = new JsonObject();
                if (!viewer.equals("jei")) p.addProperty("op", "open");
                p.addProperty("category", category);
                JsonObject opened = c.query(viewer, p);
                Expect.that(opened.has("opened") && opened.get("opened").getAsBoolean(),
                        viewer + " opened " + category + ": " + opened);
                c.waitTicks(10);
                int[] points = (int[]) tab[1 + v];
                for (int i = 0; i < points.length; i += 2) {
                    c.mouseMove(points[i], points[i + 1]);
                    c.waitTicks(6);
                    c.screenshot(viewer + "-" + tab[0] + "-" + i / 2);
                }
            }
            for (int i = 0; i < 3 && c.screenClass() != null; i++) {
                c.key("escape");
                c.waitTicks(5);
            }
        }
        Expect.that(ran > 0, "a recipe viewer is in the run");
    }

    private static boolean has(Client c, String query) {
        try {
            c.query(query, new JsonObject());
            return true;
        } catch (RuntimeException e) {
            // the harness answers "unknown query kind" when the viewer is not in this run
            return false;
        }
    }
}
