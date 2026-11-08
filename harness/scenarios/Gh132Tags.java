import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * GitHub issue 132: HBM's category and convention tags are in the game. Each tag is asked through vanilla commands
 * ({@code /clear @s <tag> 0} counts the held members, {@code /execute if block} tests a placed block); with EMI in the
 * run its tag search ({@code #hbm:ammo}) is filmed in the inventory. The armour slot tags exist from 1.21 only.
 */
public final class Gh132Tags implements Scenario {

    private static final String[][] ITEMS = {
            {"hbm:guns", "hbm:gun_light_revolver"},
            {"hbm:ammo", "hbm:ammo_dgk"},
            {"hbm:food", "hbm:apple_lead"},
            {"c:foods", "hbm:apple_lead"},
            {"hbm:armor", "hbm:ajr_helmet"},
            {"c:armors", "hbm:ajr_helmet"},
            {"hbm:bombs", "hbm:bomb_multi"},
            {"hbm:missiles", "hbm:designator"},
            {"hbm:machines", "hbm:anvil_desh"},
            {"hbm:hazardous/radioactive", "hbm:ingot_uranium"},
            {"hbm:hazardous/hot", "hbm:ingot_pu238"},
            {"hbm:hazardous", "hbm:ingot_pu238"},
            {"hbm:blast_resistant/nuclear", "hbm:ducrete"},
            {"hbm:blast_resistant", "hbm:concrete"},
            {"c:tools/ranged_weapon", "hbm:gun_light_revolver"},
            {"c:ores", "hbm:ore_aluminium"},
    };

    private static final int Y = 100, G = 101;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 6);
        c.cmd("gamemode creative");
        String convention = c.node().startsWith("1.20.1-forge") ? "forge:" : "c:";
        for (String[] row : ITEMS) {
            String tag = row[0].startsWith("c:") ? convention + row[0].substring(2) : row[0];
            Expect.that(counts(c, tag, row[1]), tag + " holds " + row[1]);
        }
        Expect.that(!counts(c, "hbm:blast_resistant/nuclear", "hbm:concrete"), "concrete is below the nuclear tier");
        boolean armorSlots = !c.node().startsWith("1.20.1");
        Expect.that(counts(c, "minecraft:head_armor", "hbm:ajr_helmet") == armorSlots, "head_armor only from 1.21");

        Fixtures.setBlock(c, 0, G, 2, "hbm:brick_ducrete");
        Expect.that(blockIs(c, "hbm:blast_resistant/nuclear"), "ducrete bricks are a nuclear-tier block");
        Expect.that(!blockIs(c, "hbm:blast_resistant/obsidian"), "ducrete bricks are below the obsidian tier");
        Fixtures.setBlock(c, 0, G, 2, "hbm:cmb_brick");
        Expect.that(blockIs(c, "hbm:blast_resistant/obsidian"), "CMB bricks are an obsidian-tier block");

        if (has(c, "emi")) {
            c.cmd("clear");
            for (int i = 0; i < 3 && !String.valueOf(c.screenClass()).contains("Inventory"); i++) {
                c.key("e");
                c.waitTicks(20);
            }
            Expect.that(String.valueOf(c.screenClass()).contains("Inventory"), "the inventory is open: " + c.screenClass());
            for (String tag : new String[] {"#hbm:ammo", "#hbm:blast_resistant/nuclear"}) {
                JsonObject search = new JsonObject();
                search.addProperty("op", "search");
                search.addProperty("text", tag);
                c.query("emi", search);
                c.waitTicks(40);
                c.screenshot("01-emi-search-" + tag.substring(5).replace('/', '-'));
            }
            c.key("escape");
            c.waitTicks(5);
        }
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[ERROR\\]).*(hbm|golem|tag)");
        System.out.println("[Gh132] PASS");
    }

    /** Whether {@code id} in the hand is a member of {@code tag}: {@code /clear} with a zero count only counts. */
    private static boolean counts(Client c, String tag, String id) {
        c.cmd("clear");
        c.cmd("give @s " + id);
        c.waitTicks(3);
        c.markLog();
        c.cmd("clear @s #" + tag + " 0");
        c.waitTicks(5);
        List<String> lines = c.logSinceMark("matching item");
        boolean found = lines.stream().anyMatch(line -> line.contains("Found 1 matching item"));
        System.out.println("[Gh132] " + tag + " " + id + ": " + found + " " + lines);
        return found;
    }

    private static boolean has(Client c, String query) {
        try {
            c.query(query, new JsonObject());
            return true;
        } catch (RuntimeException e) {
            // "unknown query kind": that viewer is not in this run
            return false;
        }
    }

    private static boolean blockIs(Client c, String tag) {
        c.markLog();
        c.cmd("execute if block 0 " + G + " 2 #" + tag);
        c.waitTicks(5);
        return !c.logSinceMark("Test passed").isEmpty();
    }
}
