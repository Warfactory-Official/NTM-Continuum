import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;

import java.util.ArrayList;
import java.util.List;

/**
 * The machines whose registrations moved out of the feature-local top-level packages into the shared classes
 * (History section 19fg-package-layout) still open their screens from a real right-click: each one is placed by hand on a
 * stone floor, the player looks down at one of its cells from just above the structure and right-clicks empty-handed,
 * and the screen that opens must be the machine's own. Covers the assembly factory, chemical factory, large arc
 * furnace, strand caster, ash pit, DiFurnace, and the Redstone-over-Radio sender torch, controller torch and TELEX.
 * The controller torch only attaches to an ROR value provider (1.7 RadioTorchController), so it is sneak-placed on top of an
 * assembly factory (a provider; a plain click would open the factory instead).
 */
public final class PackageLayoutMenus implements dev.golem.harness.api.Scenario {

    private static final int X = 2600;
    private static final int Y = 100;
    private static final int Z = 2600;
    private static final int SPACING = 14;

    /** item id, expected screen class simple name[, support block placed first] */
    private static final String[][] MACHINES = {
            {"hbm:machine_assembly_factory", "GUIMachineAssemblyFactory"},
            {"hbm:machine_chemical_factory", "GUIMachineChemicalFactory"},
            {"hbm:machine_arc_furnace", "GUIMachineArcFurnaceLarge"},
            {"hbm:machine_strand_caster", "GUIMachineStrandCaster"},
            {"hbm:machine_ashpit", "GUIAshpit"},
            {"hbm:machine_difurnace_off", "GUIDiFurnace"},
            {"hbm:radio_torch_sender", "RadioTorchScreen"},
            {"hbm:radio_torch_controller", "RadioControllerScreen", "hbm:machine_assembly_factory"},
            {"hbm:radio_telex", "RadioTelexScreen"},
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        int width = MACHINES.length * SPACING;
        Fixtures.platform(c, X + width / 2, Y, Z, width / 2 + 10);
        // cleared one lane at a time: /fill refuses more than 32768 blocks
        for (int i = -1; i <= MACHINES.length; i++) {
            int lx = X + i * SPACING;
            c.cmd("fill " + (lx - 7) + " " + (Y + 1) + " " + (Z - 10) + " " + (lx + 6) + " " + (Y + 12) + " " + (Z + 10) + " minecraft:air");
        }
        c.waitTicks(10);

        List<String> failures = new ArrayList<>();
        for (int i = 0; i < MACHINES.length; i++) {
            String item = MACHINES[i][0], screen = MACHINES[i][1];
            int x = X + i * SPACING;
            try {
                open(c, item, screen, MACHINES[i].length > 2 ? MACHINES[i][2] : null, x, Z, i);
            } catch (Throwable t) {
                failures.add(item + ": " + t.getMessage());
                c.key("escape");
                c.waitTicks(5);
            }
        }
        Expect.that(failures.isEmpty(), "screens that did not open as expected: " + failures);
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(assembly_factory|chemical_factory|arc_furnace|strand_caster|ashpit|difurnace|radio)");
    }

    private static void open(Client c, String item, String screen, String support, int x, int z, int index) {
        int base = Y;
        if (support != null) {
            Fixtures.placeOnTop(c, support, x, Y, z);
            c.waitTicks(10);
            base = Y + 1;
            while (base < Y + 12 && !"minecraft:air".equals(id(c, x, base + 1, z))) base++;
            // stand on the support, sneak-click the top face of its (x, z) column
            Fixtures.giveOnly(c, item, 1);
            c.teleport(x + 0.5, base + 1, z + 1.5);
            c.waitTicks(3);
            c.lookAt(x + 0.5, base + 0.98, z + 0.5);
            c.waitTicks(1);
            c.keyDown("key.sneak");
            c.waitTicks(5);
            c.use(2);
            c.waitTicks(5);
            c.keyUp("key.sneak");
            c.waitTicks(3);
        } else {
            Fixtures.placeOnTop(c, item, x, base, z);
        }
        c.waitTicks(10);
        int[] cell = findCell(c, item, x, base + 1, z);
        if (cell == null) c.screenshot(String.format("%02d-%s-not-placed", index, item.substring(4)));
        Expect.that(cell != null, item + " was not placed near " + x + "," + z + " (column: "
                + id(c, x, Y + 1, z) + " / " + id(c, x, Y + 2, z) + ")");
        int top = cell[1];
        while (top < Y + 12 && !"minecraft:air".equals(id(c, cell[0], top + 1, cell[2]))) top++;
        // stand just above the structure, over the cell, and click its top face
        c.teleport(cell[0] + 0.5, top + 1.05, cell[2] + 0.5);
        c.waitTicks(3);
        c.hotbar(9);
        c.waitTicks(1);
        double aimY = item.contains("radio_torch") ? cell[1] + 0.4 : top + 0.95;
        if (item.contains("radio_torch")) {
            // a torch has no collision: stand beside it and aim at its stem
            c.teleport(cell[0] + 0.5, cell[1], cell[2] + 2.5);
            c.waitTicks(3);
        }
        c.lookAt(cell[0] + 0.5, aimY, cell[2] + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(10);
        String cls = c.screenClass();
        Expect.that(cls != null && cls.endsWith(screen), item + " opened " + cls + ", expected " + screen);
        c.screenshot(String.format("%02d-%s", index, item.substring(4)));
        c.key("escape");
        c.waitTicks(5);
    }

    /** The placed block's own column first, then the nearest cell of it in a 13x13 square, at level {@code y}. */
    private static int[] findCell(Client c, String item, int x, int y, int z) {
        String block = item;
        if (block.equals(id(c, x, y, z))) return new int[]{x, y, z};
        for (int r = 1; r <= 6; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    if (block.equals(id(c, x + dx, y, z + dz))) return new int[]{x + dx, y, z + dz};
                }
            }
        }
        return null;
    }

    private static String id(Client c, int x, int y, int z) {
        JsonObject b = c.block(x, y, z);
        return b.get("block").getAsString();
    }
}
