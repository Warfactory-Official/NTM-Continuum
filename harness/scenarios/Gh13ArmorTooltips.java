import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 13: the NCR power armor helmet showed a green charge line and purple resistance lines in another order,
 * and no gas mask or helmet showed 1.7's Shift-gated protection classes. Reads the client's tooltip runs for the helmet,
 * a gas mask and a filter with and without Shift through {@code hbm_release mode=tooltip}, then hovers each for
 * screenshots.
 */
public final class Gh13ArmorTooltips implements Scenario {

    private static final String[] ITEMS = {"hbm:ncrpa_helmet", "hbm:gas_mask", "hbm:gas_mask_filter"};
    private static final String HINT = "dark_gray/i:Hold <|yellow/i:LSHIFT|dark_gray/i:> to display protection info";

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        // Radiation left by earlier scenarios kills a survival player within seconds; a respawn resets it.
        String screen = c.screenClass();
        if (screen == null || !screen.contains("Death")) {
            c.cmd("kill @s");
            c.waitTicks(20);
        }
        c.clickWidget("Respawn");
        c.waitTicks(40);
        c.cmd("gamemode survival");
        c.cmd("clear");
        for (String id : ITEMS) c.cmd("give @s " + id + " 1");
        c.waitTicks(10);
        c.markLog();

        String[] ncr = runs(c, 0);
        Expect.that(ncr[1].equals("gray:Charge: 2.5M / 2.5M"), "the charge line is gray: " + ncr[1]);
        Expect.that(ncr[2].equals("gold:Full Set Bonus:"), "full set bonus is gold: " + ncr[2]);
        int set = indexOf(ncr, "dark_purple:Resistances when worn in set:");
        Expect.that(set > 0, "set header is dark purple: " + String.join(" / ", ncr));
        String[] stats = {"gray:Fire: 10.0/90%", "gray:Physical: 25.0/65%", "gray:Energy: 10.0/50%",
                "gray:Explosion: 15.0/25%", "gray:Fall damage: 0.0/100%", "gray:Other: 15.0/25%"};
        for (int i = 0; i < stats.length; i++) {
            Expect.that(ncr[set + 5 + i].equals(stats[i]), "resistance line " + i + " is " + stats[i] + ": " + ncr[set + 5 + i]);
        }
        int hint = indexOf(ncr, HINT);
        int rad = indexOf(ncr, "yellow:Radiation resistance: 0.34");
        Expect.that(hint > set && rad == hint + 1, "protection hint just before the radiation line: " + String.join(" / ", ncr));

        String[] mask = runs(c, 1);
        Expect.that(mask[1].equals("red:No filter installed!") && mask[2].equals("red:Will never protect against:")
                && mask[3].equals("dark_red: -Corrosive Fumes"), "gas mask filter lines: " + String.join(" / ", mask));
        Expect.that(indexOf(mask, HINT) > 3, "gas mask protection hint: " + String.join(" / ", mask));

        String[] filter = runs(c, 2);
        Expect.that(indexOf(filter, HINT) > 0, "filter protection hint: " + String.join(" / ", filter));

        c.keyDown("lshift");
        c.waitTicks(2);
        String[] ncrShift = runs(c, 0);
        int prot = indexOf(ncrShift, "gold:Protects against hazards:");
        Expect.that(prot > 0 && ncrShift[prot + 1].equals("yellow:  Airborne Particles")
                && ncrShift[prot + 8].equals("yellow:  Eye Irritants")
                && ncrShift[prot + 9].equals("yellow:Radiation resistance: 0.34"),
                "helmet protection classes with Shift: " + String.join(" / ", ncrShift));
        String[] maskShift = runs(c, 1);
        int maskProt = indexOf(maskShift, "gold:Protects against hazards:");
        Expect.that(maskProt > 3 && maskShift[maskProt + 1].equals("yellow:  Eye Irritants")
                && maskShift[maskProt + 2].equals("yellow:  Bright Lights"),
                "gas mask protection classes with Shift: " + String.join(" / ", maskShift));
        String[] filterShift = runs(c, 2);
        int filterProt = indexOf(filterShift, "gold:Protects against hazards:");
        Expect.that(filterProt > 0 && filterShift[filterProt + 1].equals("yellow:  Airborne Particles"),
                "filter protection classes with Shift: " + String.join(" / ", filterShift));
        c.keyUp("lshift");

        c.key("e");
        c.waitTicks(10);
        for (int i = 0; i < ITEMS.length; i++) {
            String name = ITEMS[i].substring(4);
            hover(c, ITEMS[i]);
            c.screenshot("0" + (2 * i + 1) + "-" + name);
            c.keyDown("lshift");
            c.waitTicks(3);
            c.screenshot("0" + (2 * i + 2) + "-" + name + "-shift");
            c.keyUp("lshift");
        }
        c.key("escape");
        c.waitTicks(3);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    /** Each tooltip line as "colour:text" runs, neighbouring runs of one style merged. */
    private static String[] runs(Client c, int slot) {
        JsonObject p = new JsonObject();
        p.addProperty("mode", "tooltip");
        p.addProperty("slot", slot);
        JsonArray styled = c.query("hbm_release", p).getAsJsonArray("styled");
        String[] out = new String[styled.size()];
        for (int i = 0; i < out.length; i++) out[i] = merge(styled.get(i).getAsString());
        System.out.println("[Gh13] slot " + slot + ": " + String.join(" / ", out));
        return out;
    }

    private static String merge(String line) {
        StringBuilder out = new StringBuilder();
        String last = null;
        for (String run : line.split("\\|")) {
            int colon = run.indexOf(':');
            if (colon < 0) {
                out.append('|').append(run);
                continue;
            }
            String style = run.substring(0, colon);
            if (!style.equals(last)) out.append(last == null ? "" : "|").append(style).append(':');
            out.append(run.substring(colon + 1));
            last = style;
        }
        return out.toString();
    }

    private static int indexOf(String[] lines, String line) {
        for (int i = 0; i < lines.length; i++) if (lines[i].equals(line)) return i;
        return -1;
    }

    private static void hover(Client c, String id) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(id)) {
                c.mouseMove(s.get("center_x").getAsDouble(), s.get("center_y").getAsDouble());
                c.waitTicks(5);
                return;
            }
        }
        throw new AssertionError(id + " is not in the inventory screen");
    }
}
