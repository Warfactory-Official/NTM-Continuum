import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub issue 102: {@code 4.13_broadcasterSpawn} and {@code 4.14_landmineSpawn} must change how many corrupted
 * broadcasters and AP mines generate. Creates a fresh seeded world named after {@code GH102_LABEL}, lets 9 x 9 chunks
 * generate around a fixed spot and counts both blocks there (each fill replaces them with air and reports the count).
 * Run once per {@code hbm.cfg} setting and compare the printed counts.
 */
public final class Gh102DungeonSpawns implements Scenario {

    private static final Pattern FILLED = Pattern.compile("(?:Successfully filled|filled) (\\d+) block");
    private static final int CX = 8000, CZ = 8000;

    @Override
    public void run(Client c) throws Exception {
        String label = System.getenv().getOrDefault("GH102_LABEL", "run");
        c.waitInWorld(120_000L);
        c.leaveWorld(120_000L);
        c.createWorld("gh102-" + label + "-" + System.currentTimeMillis(), "seeded", 102L, 300_000L);
        c.waitInWorld(300_000L);
        c.setOption("render_distance", "8");
        c.cmd("gamemode spectator");
        c.teleport(CX + 0.5, 180, CZ + 0.5);
        c.waitServerTicks(600);
        int broadcasters = count(c, "hbm:broadcaster_pc");
        int mines = count(c, "hbm:mine_ap");
        System.out.println("[Gh102] " + label + " broadcasters=" + broadcasters + " mines=" + mines + " in 81 chunks");
        Expect.that(true, "counted");
    }

    private static int count(Client c, String block) {
        int total = 0;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                int x = (CX >> 4 << 4) + dx * 16, z = (CZ >> 4 << 4) + dz * 16;
                c.markLog();
                c.cmd("fill " + x + " 40 " + z + " " + (x + 15) + " 167 " + (z + 15) + " minecraft:air replace " + block);
                c.waitTicks(2);
                List<String> lines = c.logSinceMark("filled");
                for (String line : lines) {
                    Matcher m = FILLED.matcher(line);
                    if (m.find()) total += Integer.parseInt(m.group(1));
                }
            }
        }
        return total;
    }
}
