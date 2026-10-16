import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 8: 1.7's {@code addStandardInfo} shows a machine's description only while left Shift is held and
 * "Hold <LSHIFT> to display more info" otherwise, ahead of the F1 lines; the port showed it always or not at all.
 * Reads the client's tooltip runs with and without Shift, then hovers both machines in the inventory for screenshots.
 */
public final class Gh8MachineTooltips implements Scenario {

    private static final String[] ITEMS = {"hbm:machine_stirling", "hbm:fusion_torus"};
    private static final String HINT = "dark_gray/i:Hold <|yellow/i:LSHIFT|dark_gray/i:> to display more info";

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

        String[] stirling = runs(c, 0);
        Expect.that(stirling.length > 3 && stirling[1].equals(HINT), "stirling hint: " + String.join(" / ", stirling));
        Expect.that(stirling[2].equals("yellow:[ Press F1 for help ]"), "stirling F1 line after the hint: " + stirling[2]);
        Expect.that(stirling[3].equals("green:[ Press LSHIFT + F1 for a presentation ]"),
                "stirling presentation line names LSHIFT: " + stirling[3]);
        String[] torus = runs(c, 1);
        Expect.that(torus.length > 2 && torus[1].equals(HINT) && torus[2].equals("yellow:[ Press F1 for help ]"),
                "torus hint before F1: " + String.join(" / ", torus));

        c.keyDown("lshift");
        c.waitTicks(2);
        String[] stirlingShift = runs(c, 0);
        Expect.that(stirlingShift.length > 5
                        && stirlingShift[1].equals("yellow:Turns heat into energy. Requires external heat source.")
                        && stirlingShift[4].equals("yellow:Efficiency: 50%")
                        && stirlingShift[5].equals("yellow:[ Press F1 for help ]"),
                "stirling Shift lines: " + String.join(" / ", stirlingShift));
        String[] torusShift = runs(c, 1);
        Expect.that(torusShift.length > 5 && torusShift[1].equals("yellow:Fusion reactor main component.")
                        && torusShift[4].equals("yellow:Requires cooling!")
                        && torusShift[5].equals("yellow:[ Press F1 for help ]"),
                "torus Shift lines: " + String.join(" / ", torusShift));
        c.keyUp("lshift");
        c.waitTicks(2);

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
            c.waitTicks(3);
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
        for (int i = 0; i < out.length; i++) out[i] = merged(styled.get(i).getAsString());
        System.out.println("[Gh8] slot " + slot + ": " + String.join(" / ", out));
        return out;
    }

    /** Joins a line's runs into one "colour:text" when they share a colour (a translated argument splits runs). */
    private static String merged(String line) {
        String[] runs = line.split("[|]");
        String colour = runs[0].substring(0, runs[0].indexOf(':'));
        StringBuilder text = new StringBuilder();
        for (String run : runs) {
            int at = run.indexOf(':');
            if (!run.substring(0, at).equals(colour)) return line;
            text.append(run.substring(at + 1));
        }
        return colour + ":" + text;
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
