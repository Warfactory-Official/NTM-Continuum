import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 12: the blast tooltip Fat Man shows ("[Nuclear Bomb] Radius" and "[Fallout] Radius") was missing on the
 * other bombs and the nuclear missiles. Each line is read through {@code hbm_release mode=tooltip} with the default
 * config, against the radius each 1.7 explosion is given and 1.7 MK5's fallout size (radius × 2.5 + extra), both
 * rounded to the nearest 5.
 */
public final class Gh12BombTooltips implements Scenario {

    private static final String[][] CASES = {
            {"hbm:nuke_fleija", "aqua:[|aqua:Schrabidium Bomb", "Radius: 50m", null},
            {"hbm:nuke_prototype", "aqua:[|aqua:Schrabidium Bomb", "Radius: 150m", null},
            {"hbm:nuke_solinium", "dark_aqua:[|dark_aqua:Solinium Bomb", "Radius: 150m", null},
            {"hbm:nuke_fstbmb", "green:[|green:Balefire Bomb", "Radius: 250m", null},
            {"hbm:crashed_bomb_salted", "dark_green:[|dark_green:Nuclear Bomb", "Radius: 25m", "Radius: 85m"},
            {"hbm:det_nuke", "dark_green:[|dark_green:Nuclear Bomb", "Radius: 100m", "Radius: 250m"},
            {"hbm:fissure_bomb", "dark_green:[|dark_green:Nuclear Bomb", "Radius: 20m", null},
            {"hbm:mine_fat", "dark_green:[|dark_green:Nuclear Bomb", "Radius: 10m", null},
            {"hbm:missile_doomsday", "dark_green:[|dark_green:Nuclear Bomb", "Radius: 200m", "Radius: 600m"},
            {"hbm:nuke_man", "dark_green:[|dark_green:Nuclear Bomb", "Radius: 175m", "Radius: 435m"},
    };

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.cmd("spreadplayers ~ ~ 0 1 false @s");
        c.waitTicks(5);
        c.cmd("gamemode survival");
        c.cmd("clear");
        for (String[] k : CASES) c.cmd("give @s " + k[0] + " 1");
        c.waitTicks(10);
        c.markLog();

        for (int slot = 0; slot < CASES.length; slot++) {
            String[] k = CASES[slot];
            String[] lines = runs(c, slot);
            String all = k[0] + ": " + String.join(" / ", lines);
            int heading = indexOf(lines, k[1]);
            Expect.that(heading > 0, "blast heading " + k[1] + " on " + all);
            Expect.that(heading + 1 < lines.length && lines[heading + 1].startsWith("yellow:")
                    && text(lines[heading + 1]).contains(k[2]), "yellow " + k[2] + " under the heading on " + all);
            int fallout = indexOf(lines, "dark_green:[|dark_green:Fallout");
            if (k[3] == null) {
                Expect.that(fallout < 0, "no fallout line on " + all);
            } else {
                Expect.that(fallout > heading && text(lines[fallout + 1]).contains(k[3]), "fallout " + k[3] + " on " + all);
            }
        }

        c.key("e");
        c.waitTicks(10);
        for (int i = 0; i < CASES.length; i++) {
            hover(c, CASES[i][0]);
            c.screenshot(String.format("%02d-%s", i + 1, CASES[i][0].substring(4)));
        }
        c.key("escape");
        c.waitTicks(3);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    private static String[] runs(Client c, int slot) {
        JsonObject p = new JsonObject();
        p.addProperty("mode", "tooltip");
        p.addProperty("slot", slot);
        JsonArray styled = c.query("hbm_release", p).getAsJsonArray("styled");
        String[] out = new String[styled.size()];
        for (int i = 0; i < out.length; i++) out[i] = styled.get(i).getAsString();
        System.out.println("[Gh12] slot " + slot + ": " + String.join(" / ", out));
        return out;
    }

    /** A styled line's text without its "colour:" run prefixes. */
    private static String text(String styled) {
        StringBuilder out = new StringBuilder();
        for (String run : styled.split("[|]")) out.append(run.substring(run.indexOf(':') + 1));
        return out.toString();
    }

    private static int indexOf(String[] lines, String prefix) {
        for (int i = 0; i < lines.length; i++) if (lines[i].startsWith(prefix)) return i;
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
