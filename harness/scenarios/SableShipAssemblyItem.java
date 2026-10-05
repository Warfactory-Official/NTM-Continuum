import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * The assembly machine's floating product item on a Sable sub-level. The renderer shows the selected recipe's output
 * only within 35 blocks of the camera, and measured that from the block's position, which on a sub-level is in Sable's
 * plot, far from where the machine is seen: the item never showed on a ship. Screenshots before and after assembly
 * must both show the recipe output (asbestos cloth) over the machine.
 *
 * <p>1.21.1 with Sable: {@code ${env:ORG_GRADLE_PROJECT_sable.run}="true"} and {@code up --rebuild}.
 */
public class SableShipAssemblyItem implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    @Override
    public void run(Client c) throws Exception {
        c.key("escape");
        c.waitTicks(2);
        c.cmd("sable paused false");
        c.cmd("sable remove @e");
        c.waitTicks(5);
        // marked after the cleanup: removing an earlier run's ship makes Sable's client log an ERROR for movement
        // packets still in flight for it
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 10);

        c.cmd("fill " + (X - 3) + " " + (Y + 1) + " " + (Z + 2) + " " + (X + 3) + " " + (Y + 1) + " " + (Z + 8) + " minecraft:iron_block");
        c.waitTicks(2);
        JsonObject placed = Fixtures.placeOnTop(c, "hbm:machine_assembly_machine", X, Y + 1, Z + 5);
        Expect.that(placed.get("block").getAsString().startsWith("hbm:machine_assembly_machine"),
                "assembly machine placed by hand on the deck: " + placed);
        c.waitTicks(5);

        int[] core = findCore(c);
        Expect.that(core != null, "found the assembly machine's core");
        c.cmd("data merge block " + core[0] + " " + core[1] + " " + core[2] + " {recipe0:\"ass.firecloth\"}");
        c.cmd("clear");
        c.waitTicks(10);
        Expect.that(c.blockEntity(core[0], core[1], core[2]).toString().contains("ass.firecloth"),
                "the recipe was stored: " + c.blockEntity(core[0], core[1], core[2]));

        // the plate lies flat on the machine: watch from above, on a pillar (a creative player teleported into the
        // air drops back down)
        c.cmd("fill " + X + " " + (Y + 1) + " " + (Z - 1) + " " + X + " " + (Y + 6) + " " + (Z - 1) + " minecraft:stone");
        c.teleport(X + 0.5, Y + 7, Z - 0.5);
        c.waitTicks(3);
        c.lookAt(core[0] + 0.5, core[1] + 1.1, core[2] + 0.5);
        c.waitTicks(10);
        c.screenshot("10-item-in-world");

        c.cmd("sable assemble area " + (X - 3) + " " + (Y + 1) + " " + (Z + 2) + " " + (X + 3) + " " + (Y + 6) + " " + (Z + 8));
        c.waitTicks(30);
        Expect.equal("minecraft:air", c.block(core[0], core[1], core[2]).get("block").getAsString(),
                "the machine left the world grid once Sable carried it");
        c.screenshot("20-item-on-ship");

        c.cmd("fill " + X + " " + (Y + 1) + " " + (Z - 1) + " " + X + " " + (Y + 6) + " " + (Z - 1) + " minecraft:air");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    /** The block entity holding the assembler module's recipe, near where the machine was placed. */
    private static int[] findCore(Client c) {
        for (int dy = 1; dy <= 3; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int x = X + dx, y = Y + dy, z = Z + 5 + dz;
                    JsonObject be = c.blockEntity(x, y, z);
                    if (be != null && be.toString().contains("recipe0")) return new int[] {x, y, z};
                }
            }
        }
        return null;
    }
}
