import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * GitHub issue 146: ducts and pipe anchors lost 1.7's placement sound ({@code ModSoundTypes.pipe}). Places each kind by
 * hand and breaks it with {@code setblock ... destroy}, and reads the client's sound log: placing must play
 * {@code hbm:block.pipe_placed} at volume 0.925 and pitch (0.85..1.05) x 0.8, breaking it at (0.70..0.90) x 0.8.
 */
public final class Gh146DuctSound implements Scenario {

    private static final int X = 1460;
    private static final int Y = 100;
    private static final int Z = 1460;

    private static final String[] ITEMS = {
            "hbm:fluid_duct_neo", "hbm:fluid_duct_neo_silver", "hbm:fluid_duct_box_copper_2", "hbm:fluid_duct_exhaust_1",
            "hbm:pipe_anchor", "hbm:pneumatic_tube", "hbm:pneumatic_storage_mono"
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, X, Y, Z, 12);
        c.cmd("time set 6000");
        String capture = c.setOption("sound_capture", "true");
        List<String> wrong = new ArrayList<>();
        try {
            for (int i = 0; i < ITEMS.length; i++) {
                int x = X - 6 + i * 2;
                c.markLog();
                Fixtures.placeOnTop(c, ITEMS[i], x, Y, Z);
                c.waitTicks(5);
                List<String> placed = c.logSinceMark("harness-sound.*pipe_placed");
                if (i == 0) c.screenshot("01-placed-duct");
                c.markLog();
                c.cmd("setblock " + x + " " + (Y + 1) + " " + Z + " minecraft:air destroy");
                c.waitTicks(5);
                c.cmd("kill @e[type=item]");
                List<String> broken = c.logSinceMark("harness-sound.*pipe_placed");
                System.out.println("[Gh146] " + ITEMS[i] + " place " + placed + " break " + broken);
                if (placed.size() != 1 || !within(placed.get(0), 0.85F * 0.8F, 1.05F * 0.8F))
                    wrong.add(ITEMS[i] + " place " + placed);
                if (broken.size() != 1 || !within(broken.get(0), 0.70F * 0.8F, 0.90F * 0.8F))
                    wrong.add(ITEMS[i] + " break " + broken);
            }
        } finally {
            if (capture != null) c.setOption("sound_capture", capture);
        }
        Expect.that(wrong.isEmpty(), "1.7 pipe sound on every duct kind: " + wrong);
    }

    /** True when the logged sound has volume 0.925 and a pitch in [low, high). */
    private static boolean within(String line, float low, float high) {
        float volume = Float.parseFloat(field(line, "volume"));
        float pitch = Float.parseFloat(field(line, "pitch"));
        return Math.abs(volume - 0.925F) < 0.002F && pitch >= low - 0.001F && pitch < high + 0.001F;
    }

    private static String field(String line, String name) {
        int at = line.indexOf(name + "=") + name.length() + 1;
        int end = line.indexOf(' ', at);
        return line.substring(at, end < 0 ? line.length() : end).trim().toLowerCase(Locale.ROOT);
    }
}
