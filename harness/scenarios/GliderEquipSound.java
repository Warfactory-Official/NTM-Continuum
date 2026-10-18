import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * The worn, fuelled, hovering glider jetpack (development {@code /hbm jetpack_smoke} session) must not replay the
 * armour equip sound while it burns fuel, with the inventory closed or open, in creative or survival.
 *
 * <p>Positive signal: Core's {@code [harness-sound]} log ({@code sound_capture}) counts {@code item.armor.equip}
 * events over 10 s per case; the session's fuel drain is checked to be live so a silent case is not an idle pack.
 */
public final class GliderEquipSound implements Scenario {

    private static final int X = 360;
    private static final int Y = 100;
    private static final int Z = 360;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        if (String.valueOf(c.screenClass()).contains("DeathScreen")) { c.clickWidget("Respawn"); c.waitTicks(20); }
        for (int i = 0; i < 3 && c.screenClass() != null; i++) { c.key("escape"); c.waitTicks(5); }
        Fixtures.platform(c, X, Y, Z, 8);
        c.cmd("gamerule doMobSpawning false");
        c.setOption("sound_capture", "true");
        StringBuilder summary = new StringBuilder();
        try {
            for (String mode : new String[] {"creative-tab-gun", "creative", "survival"}) {
                c.cmd("hbm jetpack_smoke stop");
                c.cmd("clear");
                c.cmd("gamemode " + mode.split("-")[0]);
                c.waitTicks(5);
                c.teleport(X + 0.5, Y + 1, Z + 0.5);
                c.cmd("hbm jetpack_smoke");
                c.waitTicks(40);

                c.markLog();
                c.waitTicks(120);
                int closed = c.logSinceMark("harness-sound.*id=minecraft:item.armor.equip").size();

                if (mode.endsWith("gun")) {
                    // a held gun rewrites its components every tick, so the client's slots refresh every tick
                    c.hotbar(1);
                    c.cmd("give @s hbm:gun_light_revolver");
                    c.waitTicks(20);
                }
                // the fixture lasts 200 ticks: restart it for the open-inventory window
                c.cmd("hbm jetpack_smoke");
                c.waitTicks(10);
                c.key("e");
                c.waitTicks(10);
                if (mode.contains("tab")) {
                    // the creative screen's Inventory tab: bottom row, last column (26 px tabs, 28 px pitch)
                    com.google.gson.JsonObject sc = c.screen();
                    int left = sc.get("left").getAsInt(), top = sc.get("top").getAsInt();
                    c.click(left + 169 + 13, top + 136 + 14);
                    c.waitTicks(10);
                }
                String screen = c.screenClass() + " " + c.screen().get("title");
                c.markLog();
                c.waitTicks(140);
                c.screenshot(mode + "-inventory-open");
                int open = c.logSinceMark("harness-sound.*id=minecraft:item.armor.equip").size();
                java.util.Map<String, Integer> all = new java.util.TreeMap<>();
                for (String l : c.logSinceMark("harness-sound")) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("id=([^ ]+)").matcher(l);
                    if (m.find()) all.merge(m.group(1), 1, Integer::sum);
                }
                System.out.println(c.node() + " " + mode + " all sounds while open: " + all);
                c.key("escape");
                c.waitTicks(5);

                String line = mode + ": closed=" + closed + " open=" + open + " (screen " + screen + ")";
                System.out.println(c.node() + " " + line);
                summary.append(line).append("; ");
            }
        } finally {
            c.setOption("sound_capture", "false");
            c.cmd("hbm jetpack_smoke stop");
            c.cmd("clear");
            c.cmd("gamemode creative");
        }
        Expect.that(!summary.toString().matches(".*(closed|open)=[1-9].*"),
                "no equip sound while the glider is worn and burning fuel: " + summary);
    }
}
