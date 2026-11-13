import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * GitHub issue 188 on generated terrain: in the seeded world nothing is placed by hand. At each spot the survey scanner
 * reports what lies there, then a neutrino lens on a scanner satellite sweeps it; every deposit the scanner reported
 * (oil, bedrock oil, schist, bedrock ore) must come back as a lens marker, and natural bedrock oil must turn up. The game runs at 100 ticks a second so that
 * the 1-in-300 and 1-in-5000 rolls get enough sweeps.
 */
public final class Gh188LensNatural implements Scenario {

    /** Spots in the seeded world with oil and schist, bedrock ore, bedrock oil, and schist and bedrock ore. */
    private static final int[][] SPOTS = {{-1200, -600}, {1500, 900}, {2400, -1500}, {-2600, 1800}};
    /** Scanner chat line, lens label. */
    private static final String[][] KINDS = {{"Found OIL!", "Oil"}, {"Found BEDROCK OIL!", "Bedrock Oil"},
            {"Found SCHIST!", "Schist"}, {"Found BEDROCK ORE", "Bedrock Ore"}};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(180_000L);
        c.useSeededWorld();
        c.cmd("gamemode creative");
        boolean lensOil = false;
        Set<String> lensAll = new LinkedHashSet<>();
        Set<String> wanted = new LinkedHashSet<>();
        Set<String> seen = new LinkedHashSet<>();
        List<String> report = new ArrayList<>();
        for (int[] spot : SPOTS) {
            c.cmd("item replace entity @s armor.head with minecraft:air");
            c.cmd("spreadplayers " + spot[0] + " " + spot[1] + " 0 1 false @s");
            c.waitServerTicks(200);
            c.cmd("clear @s");
            c.cmd("item replace entity @s weapon.mainhand with hbm:survey_scanner");
            c.waitTicks(10);
            c.markLog();
            c.use();
            c.waitTicks(20);
            String chat = String.join("\n", c.logSinceMark("Found"));
            Set<String> here = new LinkedHashSet<>();
            for (String[] kind : KINDS) if (chat.contains(kind[0])) here.add(kind[1]);
            JsonObject lens = new JsonObject();
            lens.add("lens", new JsonPrimitive(4817));
            c.query("hbm_gapsc", lens);
            c.tickRate(100F);
            Set<String> marked = new LinkedHashSet<>();
            for (int t = 0; t < 80 && !(marked.containsAll(here) && marked.contains("Bedrock Oil")); t++) {
                c.waitTicks(20);
                for (var label : c.query("hbm_gapsc", new JsonObject()).getAsJsonArray("marker_labels")) {
                    for (String[] kind : KINDS) if (label.getAsString().startsWith(kind[1])) marked.add(kind[1]);
                }
            }
            c.tickRate(20F);
            c.screenshot("lens-" + spot[0] + "_" + spot[1]);
            report.add(spot[0] + "," + spot[1] + ": scanner " + here + ", lens " + marked);
            wanted.addAll(here);
            for (String kind : here) if (marked.contains(kind)) seen.add(kind);
            if (marked.contains("Bedrock Oil")) lensOil = true;
            lensAll.addAll(marked);
        }
        c.cmd("item replace entity @s armor.head with minecraft:air");
        c.cmd("clear @s");
        System.out.println(c.node() + " lens on generated terrain: " + report);
        // the scanner reads one chunk centre, the lens 7x7 chunks: where deposits fall differs per version
        Expect.that(lensAll.contains("Bedrock Ore") && lensAll.contains("Oil") && lensAll.contains("Schist"),
                "the lens marked natural bedrock ore, oil and schist: " + report);
        Expect.that(seen.containsAll(wanted), "the lens marked every natural deposit the scanner found: " + report);
        Expect.that(lensOil, "the lens marked natural bedrock oil somewhere: " + report);
        c.useFlatWorld();
    }
}
