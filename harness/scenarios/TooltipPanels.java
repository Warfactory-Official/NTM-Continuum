import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/**
 * 1.7's {@code drawStackText} panels in a real client: the turret's ammo list over a filled ammo grid (no slot item may
 * show through the panel) and the hydrotreater's catalytic converter hint.
 */
public final class TooltipPanels implements Scenario {

    private static final int Y = 100, L = 101;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 24);

        turret(c);
        hydrotreater(c);

        Expect.logCleanSinceMark(c, "(?i)exception.*(turret|hydrotreater|GuiUtil|stackText)");
    }

    private void turret(Client c) {
        int x = 0, z = -8;
        open(c, "hbm:turret_chekhov", x, z, "GUITurretBase");
        int[] core = blockEntity(c, x, z, "turret");
        Expect.that(core != null, "the turret has a block entity near the clicked cell");
        for (int slot = 2; slot <= 9; slot++) {
            c.cmd("item replace block " + core[0] + " " + core[1] + " " + core[2] + " container." + slot + " with minecraft:cobblestone 64");
        }
        c.waitTicks(5);
        Expect.slotHolds(c, 5, "minecraft:cobblestone", 64);
        JsonObject first = c.slot(1);
        c.mouseMove(first.get("center_x").getAsDouble(), first.get("center_y").getAsDouble());
        // the hover is read on rendered frames, which a fresh client draws slowly
        c.waitTicks(20);
        c.screenshot("00-turret-ammo-panel");
        close(c);
    }

    private void hydrotreater(Client c) {
        int x = 0, z = 8;
        open(c, "hbm:machine_hydrotreater", x, z, "GUIMachineHydrotreater");
        JsonObject catalyst = c.slot(10);
        c.mouseMove(catalyst.get("center_x").getAsDouble(), catalyst.get("center_y").getAsDouble());
        c.waitTicks(20);
        c.screenshot("01-hydrotreater-converter-panel");
        close(c);
    }

    private static void open(Client c, String block, int x, int z, String screen) {
        Fixtures.placeOnTop(c, block, x, Y, z);
        c.waitTicks(5);
        c.teleport(x + 0.5, L, z - 3.5);
        c.waitTicks(3);
        Fixtures.interact(c, x, L, z);
        c.waitScreen(screen, 5_000L);
        c.waitTicks(3);
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(2);
    }

    /** The first block within two blocks of (x, L, z) whose block entity type names {@code kind}. */
    private static int[] blockEntity(Client c, int x, int z, String kind) {
        for (int dy = 0; dy <= 2; dy++)
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++) {
                    JsonElement be = c.block(x + dx, L + dy, z + dz).get("block_entity");
                    if (be != null && !be.isJsonNull() && be.toString().contains(kind)) return new int[] {x + dx, L + dy, z + dz};
                }
        return null;
    }
}
