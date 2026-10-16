import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 7: item description lines were white where 1.7 drew them gray (1.7's GuiContainer prefixed every line
 * after the name with GRAY), the radiation line read " 11.25 RAD/s" for 1.7's "11.25RAD/s", and a RESET value after a
 * coloured label kept the label's colour where 1.7 drew it white. Reads the client's own tooltip runs through
 * {@code hbm_release mode=tooltip}, then hovers each item in the survival inventory for a screenshot.
 */
public final class Gh7TooltipColours implements Scenario {

    private static final String[] ITEMS = {"hbm:igniter", "hbm:pellet_rtg_radium", "hbm:solinium_core", "hbm:pa_coil_gold"};

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.cmd("gamemode survival");
        c.cmd("clear");
        for (String id : ITEMS) c.cmd("give @s " + id + " 1");
        c.waitTicks(10);
        c.markLog();

        String[] igniter = runs(c, 0);
        Expect.that(igniter[1].equals("gray:(Used by right-clicking the Prototype)"), "igniter description is gray: " + igniter[1]);

        String[] pellet = runs(c, 1);
        Expect.that(pellet[1].startsWith("gray:Great starter pellet"), "pellet description is gray: " + pellet[1]);
        Expect.that(contains(pellet, "gray:Power Level: 3"), "power level is gray: " + String.join(" / ", pellet));
        Expect.that(contains(pellet, "gray:Decays to: "), "decay line is gray: " + String.join(" / ", pellet));
        Expect.that(contains(pellet, "green:[|green:Radioactive"), "radioactive trait is green: " + String.join(" / ", pellet));
        Expect.that(contains(pellet, "yellow:11.25RAD/s"), "radiation reads 1.7's 11.25RAD/s: " + String.join(" / ", pellet));

        String[] core = runs(c, 2);
        Expect.that(core[1].equals("gray:Used in:") && core[2].equals("gray:The Blue Rinse"), "bomb part lines are gray: " + String.join(" / ", core));
        Expect.that(contains(core, "yellow:14.0RAD/s"), "radiation reads 1.7's 14.0RAD/s: " + String.join(" / ", core));
        Expect.that(contains(core, "dark_aqua:[|dark_aqua:Blinding"), "blinding trait is dark aqua: " + String.join(" / ", core));

        String[] coil = runs(c, 3);
        Expect.that(contains(coil, "blue:Quadrupole operational range: |white:"), "the range value after RESET is white: " + String.join(" / ", coil));

        c.key("e");
        c.waitTicks(10);
        for (int i = 0; i < ITEMS.length; i++) {
            hover(c, ITEMS[i]);
            c.screenshot("0" + (i + 1) + "-" + ITEMS[i].substring(4));
        }
        c.key("escape");
        c.waitTicks(3);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    /** Each tooltip line as its styled runs, "colour:text|colour:text". */
    private static String[] runs(Client c, int slot) {
        JsonObject p = new JsonObject();
        p.addProperty("mode", "tooltip");
        p.addProperty("slot", slot);
        JsonArray styled = c.query("hbm_release", p).getAsJsonArray("styled");
        String[] out = new String[styled.size()];
        for (int i = 0; i < out.length; i++) out[i] = styled.get(i).getAsString();
        System.out.println("[Gh7] slot " + slot + ": " + String.join(" / ", out));
        return out;
    }

    private static boolean contains(String[] lines, String prefix) {
        for (String line : lines) if (line.startsWith(prefix)) return true;
        return false;
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
