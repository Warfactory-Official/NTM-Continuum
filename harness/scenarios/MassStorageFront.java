import dev.golem.harness.api.*;

/**
 * 1.7 {@code RenderMassStorage}: the stored item as an 8-pixel GUI icon in the upper middle of the front face,
 * the green abbreviated count under it, and a red-to-green fill bar along the bottom; front texture on the
 * facing side only.
 *
 * <ul>
 *   <li>00-north-iron: stone, 5000 / 10,000: "5000", bar half width, olive (0.5 red, 0.5 green).</li>
 *   <li>01-south-mass: diamond, 123,456 / 1,000,000: "123K", bar 12% wide, mostly red.</li>
 *   <li>02-west-desh: oak log (3D GUI icon), 25,000 / 100,000: "25.0K", bar a quarter wide.</li>
 *   <li>03-east-wood: apple, full: count = capacity, bar full width, pure green.</li>
 *   <li>04-side-view: the north-facing unit seen from the west: side texture only, no panel.</li>
 * </ul>
 */
public final class MassStorageFront implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 12);
        c.cmd("fill -12 " + (Y + 1) + " -12 12 " + (Y + 6) + " 12 minecraft:air");

        boolean legacy = c.node().startsWith("1.20.1");
        storage(c, legacy, "hbm:mass_storage_iron", "north", 0, Y + 2, 0, "minecraft:stone", 5000);
        storage(c, legacy, "hbm:mass_storage", "south", 6, Y + 2, 0, "minecraft:diamond", 123456);
        storage(c, legacy, "hbm:mass_storage_desh", "west", -6, Y + 2, 6, "minecraft:oak_log", 25000);
        storage(c, legacy, "hbm:mass_storage_wood", "east", 6, Y + 2, 6, "minecraft:apple", 1_000_000);
        c.waitTicks(10);

        // the server really holds what the panels should show
        stock(c, 0, Y + 2, 0, 5000);
        stock(c, 6, Y + 2, 0, 123456);
        stock(c, -6, Y + 2, 6, 25000);

        c.setOption("hide_gui", "true");

        // each unit floats one block up so a player standing on the floor has the panel at eye height
        shot(c, "00-north-iron", 0.5, Y + 1.0, -1.6, 0.5, Y + 2.5, 0.0);
        shot(c, "01-south-mass", 6.5, Y + 1.0, 2.6, 6.5, Y + 2.5, 1.0);
        shot(c, "02-west-desh", -7.6, Y + 1.0, 6.5, -6.0, Y + 2.5, 6.5);
        shot(c, "03-east-wood", 8.6, Y + 1.0, 6.5, 7.0, Y + 2.5, 6.5);
        shot(c, "04-side-view", -1.6, Y + 1.0, 0.5, 0.0, Y + 2.5, 0.5);

        c.setOption("hide_gui", "false");
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(mass|RenderMassStorage)");
    }

    private static void storage(Client c, boolean legacy, String block, String facing, int x, int y, int z,
                                String item, int stock) {
        c.cmd("setblock " + x + " " + y + " " + z + " " + block + "[facing=" + facing + "]");
        String count = legacy ? "Count:1b" : "count:1";
        // 1.20.1's carried-state emulation re-reads CarriedData on load, so a bare "stack" would be overwritten
        String carried = legacy ? "CarriedData:{stack:" + stock + "}," : "";
        c.cmd("data merge block " + x + " " + y + " " + z + " {" + carried + "stack:" + stock
                + ",inventory:{Items:[{Slot:1b,id:\"" + item + "\"," + count + "}]}}");
    }

    private static void stock(Client c, int x, int y, int z, int expected) {
        String nbt = c.blockEntity(x, y, z).toString();
        Expect.that(nbt.matches("(?s).*\\bstack\\W{1,4}" + expected + "\\b.*"),
                "server block entity at " + x + "," + y + "," + z + " must hold stack " + expected + ": " + nbt);
    }

    private static void shot(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        c.teleport(x, y, z);
        c.lookAt(tx, ty, tz);
        c.waitTicks(8);
        c.teleport(x, y, z);
        c.lookAt(tx, ty, tz);
        c.waitTicks(2);
        c.screenshot(name);
    }
}
