import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Pre-release client wave (History §19es-client): the two §19eg-decisions id changes seen on a client.
 * <ul>
 *   <li>{@code hbm:hev_battery} is now 1.7's item (ItemFusionCore, flat sprite {@code items/hev_battery.png}, stack 4);
 *       the block's item is {@code hbm:hev_battery_block}. Both in the hotbar and in the inventory (screenshots), the
 *       item's display name and max stack, and the block item placing the block.</li>
 *   <li>{@code fluid_duct_neo} after the mk2 merge: a run of ducts with a corner and a tee, typed water, drawn by
 *       {@code NeoDuctRenderer} (screenshot), each typed by right-clicking it with a water identifier, each a {@code fluid_duct_neo} block entity carrying {@code type}.</li>
 * </ul>
 */
public final class ReleaseIdDecisions implements Scenario {

    private static final int X = 0, Y = 100, Z = 0, L = 101;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        for (int i = 0; i < 3 && c.screenClass() != null; i++) { c.key("escape"); c.waitTicks(5); }
        c.useFlatWorld();
        Fixtures.platform(c, X, Y, Z, 8);
        c.markLog();

        c.cmd("clear");
        c.cmd("give @s hbm:hev_battery 8");
        c.cmd("give @s hbm:hev_battery_block 1");
        c.waitTicks(10);
        JsonObject inv = c.inventory();
        String s = inv.toString();
        System.out.println(c.node() + " inventory: " + s);
        Expect.that(s.contains("\"id\":\"hbm:hev_battery\"") && s.contains("\"count\":4"),
                "hev_battery is the 1.7 item with a stack limit of 4 (8 given -> two stacks of 4): " + s);
        Expect.that(s.contains("hbm:hev_battery_block"), "the block item is hbm:hev_battery_block: " + s);
        c.hotbar(1);
        c.waitTicks(10);
        c.screenshot("10-hotbar-hev-battery");
        c.key("e");
        c.waitScreen("", 5_000L);
        c.waitTicks(10);
        c.screenshot("11-inventory-hev-battery");
        c.key("escape");
        c.waitTicks(5);

        // the block item places the block
        Fixtures.placeOnTop(c, "hbm:hev_battery_block", X + 3, Y, Z + 3);
        Expect.block(c, X + 3, L, Z + 3, "hbm:hev_battery");

        // ducts: a straight run, a corner and a tee, typed water
        int dz = Z + 4;
        for (int x = X - 3; x <= X + 1; x++) Fixtures.setBlock(c, x, L, dz, "hbm:fluid_duct_neo");
        Fixtures.setBlock(c, X + 1, L, dz + 1, "hbm:fluid_duct_neo");
        Fixtures.setBlock(c, X + 1, L, dz + 2, "hbm:fluid_duct_neo");
        Fixtures.setBlock(c, X - 1, L + 1, dz, "hbm:fluid_duct_neo");
        Fixtures.setBlock(c, X - 1, L + 2, dz, "hbm:fluid_duct_neo");
        c.waitTicks(5);
        // typed as a player does: right-click with a water identifier, which recomputes the arms.
        // (/setblock NBT would leave the graph cell and the arms of an untyped duct.)
        boolean legacy = c.node().startsWith("1.20.");
        c.cmd("clear");
        c.cmd("give @s " + (legacy ? "hbm:fluid_identifier{\"hbm:fluid\":\"hbm:water\"}" : "hbm:fluid_identifier[hbm:fluid=\"hbm:water\"]") + " 1");
        c.hotbar(1);
        c.teleport(X - 0.5, L, Z + 1.5);
        c.waitTicks(5);
        int[][] ducts = {{X - 3, L, dz}, {X - 2, L, dz}, {X - 1, L, dz}, {X, L, dz}, {X + 1, L, dz}, {X + 1, L, dz + 1},
                {X + 1, L, dz + 2}, {X - 1, L + 1, dz}, {X - 1, L + 2, dz}};
        for (int[] d : ducts) {
            c.lookAt(d[0] + 0.5, d[1] + 0.5, d[2] + 0.5);
            c.waitTicks(2);
            c.use(2);
            c.waitTicks(3);
        }
        c.waitTicks(20);
        JsonObject be = c.blockEntity(X - 1, L, dz);
        System.out.println(c.node() + " duct: " + be);
        Expect.that(be.toString().contains("hbm:water"), "the duct block entity carries its fluid type by name: " + be);
        c.teleport(X - 0.5, L + 1, Z - 0.5);
        c.lookAt(X - 1.0, L + 0.5, dz + 0.5);
        c.cmd("clear");
        c.hotbar(9);
        c.waitTicks(30);
        c.screenshot("20-fluid-duct-neo");
        c.teleport(X + 3.5, L + 2.5, dz + 3.5);
        c.lookAt(X - 1.0, L + 0.5, dz + 0.5);
        c.waitTicks(20);
        c.screenshot("21-fluid-duct-neo-other-side");

        Expect.logCleanSinceMark(c, "(?i)(exception|unregistered hbm:|is not registered|missing model).*");
    }
}
