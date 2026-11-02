import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.HarnessException;
import dev.golem.harness.api.Scenario;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub issue 102 with worldgen mods in the client's mods folder: broadcasters and AP/AT mines counted in 15 x 15 chunks
 * of a fresh seeded world. Set the hbm.cfg keys first; {@code GH102_LABEL} names the world and the printed line.
 */
public final class Gh102ModdedSpawns implements Scenario {

    private static final Pattern FILLED = Pattern.compile("(?:Successfully filled|filled) (\\d+) block");
    private static final int CX = 8000, CZ = 8000, RADIUS = 7;

    @Override
    public void run(Client c) throws Exception {
        String label = System.getenv().getOrDefault("GH102_LABEL", "run");
        answerPrompts(c);
        c.leaveWorld(120_000L);
        try {
            c.createWorld("gh102-" + label + "-" + System.currentTimeMillis(), "seeded", 102L, 30_000L);
        } catch (HarnessException e) {
            // the experimental-settings warning holds the creation until it is answered
        }
        answerPrompts(c);
        c.setOption("render_distance", "8");
        c.cmd("gamemode spectator");
        c.teleport(CX + 0.5, 200, CZ + 0.5);
        int x0 = (CX >> 4) - RADIUS << 4, z0 = (CZ >> 4) - RADIUS << 4;
        int x1 = ((CX >> 4) + RADIUS << 4) + 15, z1 = ((CZ >> 4) + RADIUS << 4) + 15;
        c.cmd("forceload add " + x0 + " " + z0 + " " + x1 + " " + z1);
        // Generating 225 chunks with worldgen mods stalls the server tick, so wait by wall clock until it answers.
        for (int attempt = 0; ; attempt++) {
            Thread.sleep(30_000L);
            try {
                c.waitServerTicks(100);
                break;
            } catch (HarnessException e) {
                if (attempt == 20) throw e;
            }
        }
        int broadcasters = count(c, "hbm:broadcaster_pc");
        int apMines = count(c, "hbm:mine_ap");
        int atMines = count(c, "hbm:mine_he");
        c.cmd("forceload remove all");
        int chunks = (2 * RADIUS + 1) * (2 * RADIUS + 1);
        System.out.println("[Gh102] " + label + " broadcasters=" + broadcasters + " ap_mines=" + apMines + " at_mines="
                + atMines + " in " + chunks + " chunks");
        Expect.that(true, "counted");
    }

    /**
     * Worldgen mods open two vanilla prompts: the backup prompt on an existing world and the experimental-settings
     * warning on a new one. Loads without a backup, accepts the warning and returns once in a world.
     */
    private static void answerPrompts(Client c) throws InterruptedException {
        for (int attempt = 0; attempt < 300; attempt++) {
            JsonObject screen = c.screen();
            if (!screen.get("open").getAsBoolean() && c.inWorld()) return;
            String name = screen.has("class") ? screen.get("class").getAsString() : "";
            if (name.contains("BackupConfirm") || name.contains("ConfirmScreen")) {
                for (JsonElement e : screen.getAsJsonArray("widgets")) {
                    String text = e.getAsJsonObject().has("message") ? e.getAsJsonObject().get("message").getAsString() : "";
                    String lower = text.toLowerCase(Locale.ROOT);
                    if (lower.contains("know what") || lower.equals("yes")) {
                        c.clickWidget(text);
                        break;
                    }
                }
            }
            Thread.sleep(2_000L);
        }
        throw new HarnessException("not in a world after the prompts");
    }

    private static int count(Client c, String block) {
        int total = 0;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int x = (CX >> 4) + dx << 4, z = (CZ >> 4) + dz << 4;
                for (int y = -64; y < 320; y += 128) {
                    c.markLog();
                    c.cmd("fill " + x + " " + y + " " + z + " " + (x + 15) + " " + (y + 127) + " " + (z + 15)
                            + " minecraft:air replace " + block);
                    c.waitTicks(1);
                    List<String> lines = c.logSinceMark("filled");
                    for (String line : lines) {
                        Matcher m = FILLED.matcher(line);
                        if (m.find()) total += Integer.parseInt(m.group(1));
                    }
                }
            }
        }
        return total;
    }
}
