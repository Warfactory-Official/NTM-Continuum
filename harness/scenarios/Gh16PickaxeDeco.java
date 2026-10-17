import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 16: metal deco blocks were not mineable/pickaxe. In 1.7 they are Material.iron: a pickaxe is effective,
 * and without one they drop nothing. Checks the tool tag with {@code execute if block} and the drop with
 * {@code loot ... mine}. The tool requirement and break speed are GameTests ({@code mining_pickaxe_effective},
 * {@code mining_break_speed}): held mining needs a grabbed mouse, which harness clients never take, and
 * {@code loot ... mine} ignores the tool requirement.
 */
public final class Gh16PickaxeDeco implements Scenario {

    private static final int Y = 100, BX = 0, BY = 101, BZ = 2;
    private static final String[][] CASES = {
            {"hbm:deco_aluminium", "#minecraft:mineable/pickaxe", "minecraft:iron_pickaxe"},
            {"hbm:deco_steel", "#minecraft:mineable/pickaxe", "minecraft:iron_pickaxe"},
            {"hbm:gravel_obsidian", "#minecraft:mineable/pickaxe", "minecraft:iron_pickaxe"},
            {"hbm:crate_supply", "#minecraft:mineable/axe", null},
    };

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 6);
        c.cmd("gamemode creative");
        c.teleport(0.5, BY, 0.5);

        for (String[] k : CASES) {
            Fixtures.setBlock(c, BX, BY, BZ, k[0]);
            Expect.block(c, BX, BY, BZ, k[0]);

            c.markLog();
            c.cmd("execute if block " + BX + " " + BY + " " + BZ + " " + k[1]);
            c.waitTicks(3);
            Expect.that(!c.logSinceMark("Test passed").isEmpty(), k[0] + " is in " + k[1]);

            if (k[2] == null) continue; // a supply crate drops its contents, not itself
            c.cmd("clear");
            c.cmd("loot give @s mine " + BX + " " + BY + " " + BZ + " " + k[2]);
            c.waitTicks(3);
            Expect.that(count(c, k[0]) >= 1, k[0] + " drops itself to " + k[2]);
        }

        Fixtures.setBlock(c, BX, BY, BZ, CASES[0][0]);
        c.cmd("clear");
        c.cmd("give @s minecraft:iron_pickaxe 1");
        c.hotbar(1);
        c.lookAt(BX, BY, BZ);
        c.waitTicks(20);
        c.screenshot("deco-aluminium-jade");
    }

    private static int count(Client c, String id) {
        int total = 0;
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("id").getAsString().equals(id)) total += s.get("count").getAsInt();
        }
        return total;
    }
}
