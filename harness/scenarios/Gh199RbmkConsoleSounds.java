import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub issue 199: the RBMK console GUI played no click sounds. 1.7's {@code GUIRBMKConsole} plays
 * {@code gui.button.press} on each control with its own pitch; each click below must start one UI click at that pitch.
 */
public final class Gh199RbmkConsoleSounds implements Scenario {

    private static final int X = 1400, Y = 100, Z = 1400, L = Y + 1;
    private static final Pattern PITCH = Pattern.compile("pitch=([0-9.]+)");

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(180_000L);
        if (c.screenClass() != null) { c.key("escape"); c.waitScreen("none", 5000); }
        Fixtures.platform(c, X, Y, Z, 16);
        c.cmd("gamemode creative");
        c.cmd("fill " + (X - 10) + " " + L + " " + (Z - 10) + " " + (X + 10) + " " + (L + 6) + " " + (Z + 10) + " minecraft:air");

        int colX = X + 6, colZ = Z;
        placeFrom(c, "hbm:rbmk_fuel_rod", colX, colZ, colX + 0.5, colZ - 2.5);
        placeFrom(c, "hbm:rbmk_console", X - 4, Z, X - 3.5, Z - 2.5);
        int[] core = null;
        for (int y = L; y <= L + 3 && core == null; y++) {
            for (int dx = -3; dx <= 3 && core == null; dx++) for (int dz = -3; dz <= 3; dz++) {
                JsonObject params = new JsonObject();
                params.addProperty("x", X - 4 + dx);
                params.addProperty("y", y);
                params.addProperty("z", Z + dz);
                params.addProperty("server", true);
                JsonObject s = c.query("block", params);
                if ("hbm:rbmk_console".equals(s.get("block").getAsString()) && !s.get("block_entity").isJsonNull()) {
                    core = new int[] {X - 4 + dx, y, Z + dz};
                    break;
                }
            }
        }
        Expect.that(core != null, "the console was placed");
        c.cmd("data merge block " + core[0] + " " + core[1] + " " + core[2] + " {tX:" + colX + ",tY:" + L + ",tZ:" + colZ + "}");
        c.teleport(core[0] + 0.5, L + 2, core[2] - 2.5);
        c.lookAt(core[0] + 0.5, core[1] + 0.5, core[2] + 0.5);
        c.waitTicks(5);
        c.use(2);
        c.waitScreen("RBMKConsole", 5000);
        c.waitTicks(40);
        JsonObject screen = c.screen();
        int left = screen.get("left").getAsInt(), top = screen.get("top").getAsInt();

        String previous = c.setOption("sound_capture", "true");
        List<String> failures = new ArrayList<>();
        try {
            // the linked fuel column sits in the middle of the 15 by 15 grid
            click(c, left + 86 + 75, top + 11 + 75, 0, "select column", 1.0F, failures);
            click(c, left + 86 + 75, top + 11 + 75, 0, "deselect column", 0.75F, failures);
            click(c, left + 77, top + 75, 0, "deselect all", 0.5F, failures);
            click(c, left + 66, top + 75, 0, "select all control rods", 1.5F, failures);
            click(c, left + 55, top + 75, 0, "purple group", 1.2F, failures);
            click(c, left + 11, top + 75, 1, "assign red group", 0.8F, failures);
            click(c, left + 76, top + 88, 0, "compressor", 1.0F, failures);
            click(c, left + 15, top + 17, 0, "screen 1 type", 0.5F, failures);
            click(c, left + 33, top + 17, 0, "assign to screen 1", 0.75F, failures);
            c.screenshot("gh199-console");
        } finally {
            c.setOption("sound_capture", previous);
            c.key("escape");
        }
        Expect.that(failures.isEmpty(), "console clicks: " + failures);
        System.out.println("[Gh199] PASS");
    }

    private static void click(Client c, int x, int y, int button, String what, float pitch, List<String> failures) {
        c.markLog();
        c.click(x, y, button);
        c.waitTicks(4);
        List<String> clicks = c.logSinceMark("\\[harness-sound\\] id=minecraft:ui\\.button\\.click");
        String seen = clicks.isEmpty() ? "none" : pitches(clicks);
        System.out.println("[Gh199] " + c.node() + " " + what + ": " + seen);
        if (clicks.size() != 1 || Math.abs(Float.parseFloat(seen) - pitch) > 0.01F) {
            failures.add(what + " played " + seen + ", 1.7 plays one click at pitch " + String.format(Locale.ROOT, "%.2f", pitch));
        }
    }

    private static String pitches(List<String> lines) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            Matcher m = PITCH.matcher(line);
            out.add(m.find() ? m.group(1) : "?");
        }
        return String.join(",", out);
    }

    private static void placeFrom(Client c, String item, int x, int z, double px, double pz) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(px, L, pz);
        c.waitTicks(2);
        c.lookAt(x + 0.5, Y + 0.98, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
    }
}
