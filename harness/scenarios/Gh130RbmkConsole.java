import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 130. On a crafting table the RBMK console and crane console craft with {@code X528_forceReasimBoilers} at
 * its default of true, and without {@code enable528Mode}. Set {@code GH130_528=1} for a client whose {@code hbm.cfg} has
 * {@code enable528Mode=true}: as in 1.7's {@code if(!GeneralConfig.enable528)}, then they do not.
 */
public final class Gh130RbmkConsole implements Scenario {

    private static final int Y = 100, G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        boolean mode528 = "1".equals(System.getenv("GH130_528"));
        c.useFlatWorld();
        c.waitInWorld(180_000L);
        Fixtures.platform(c, 0, Y, 0, 6);
        c.cmd("gamemode survival");
        Fixtures.setBlock(c, 0, G, 0, "minecraft:crafting_table");
        c.teleport(-1.5, G, 0.5);
        c.lookAt(0.5, G + 0.5, 0.5);
        Fixtures.waitForTarget(c, 0, G, 0);

        craft(c, "rbmk_console", new String[] {"B", "B", "B", "D", "G", "D", "D", "C", "D"}, mode528);
        craft(c, "rbmk_crane_console", new String[] {"B", "C", "D", "D", "D", "D", null, null, null}, mode528);
        c.cmd("clear @s");
        c.cmd("gamemode creative");
    }

    private static void craft(Client c, String result, String[] grid, boolean mode528) {
        c.cmd("clear @s");
        Fixtures.give(c, "hbm:ingot_boron", 8);
        Fixtures.give(c, "hbm:deco_rbmk", 8);
        Fixtures.give(c, "minecraft:glass_pane", 8);
        Fixtures.give(c, "hbm:circuit_analog", 8);
        c.waitTicks(10);
        c.use(2);
        c.waitScreen("Crafting", 10_000L);
        // the menu: 0 result, 1-9 grid, 10-36 inventory, 37-45 hotbar
        String order = "BDGC";
        for (int k = 0; k < 4; k++) {
            boolean any = false;
            for (int slot = 0; slot < 9; slot++) any |= grid[slot] != null && grid[slot].charAt(0) == order.charAt(k);
            if (!any) continue;
            c.clickSlot(37 + k);
            c.waitTicks(3);
            for (int slot = 0; slot < 9; slot++) {
                if (grid[slot] != null && grid[slot].charAt(0) == order.charAt(k)) {
                    c.clickSlot(1 + slot, 1);
                    c.waitTicks(2);
                }
            }
            c.clickSlot(37 + k);
            c.waitTicks(3);
        }
        c.waitTicks(10);
        String out = c.slot(0).toString();
        System.out.println("[Gh130] " + result + " with enable528Mode " + mode528 + ": " + out);
        c.screenshot("gh130-" + result + (mode528 ? "-528" : ""));
        boolean crafted = out.contains("hbm:" + result);
        Expect.that(crafted != mode528, result + (mode528 ? " has no recipe in 528 mode: " : " crafts: ") + out);
        c.key("escape");
        c.waitTicks(5);
    }
}
