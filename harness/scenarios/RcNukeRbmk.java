import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * RBMK block with every column type in one 5x3 grid: placed by hand, fuelled, run for 200 ticks, every menu
 * screenshotted, four views of the reactor, a save and rejoin while it burns, then a forced meltdown.
 */
public final class RcNukeRbmk implements Scenario {

    private static final int X = 200;
    private static final int Y = 100;
    private static final int Z = 200;
    private static final String[][] GRID = {
            {"hbm:rbmk_fuel_rod_moderated", "hbm:rbmk_fuel_rod", "hbm:rbmk_control_mod", "hbm:rbmk_fuel_rod", "hbm:rbmk_fuel_rod_moderated"},
            {"hbm:rbmk_boiler", "hbm:rbmk_reflector", "hbm:rbmk_absorber", "hbm:rbmk_moderator", "hbm:rbmk_cooler"},
            {"hbm:rbmk_heater", "hbm:rbmk_outgasser", "hbm:rbmk_storage", "hbm:rbmk_blank", "hbm:rbmk_control_auto"}};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X + 2, Y, Z + 1, 14);
        c.cmd("gamemode creative");
        c.waitTicks(5);

        for (int row = 0; row < GRID.length; row++) {
            for (int col = 0; col < GRID[row].length; col++) {
                JsonObject placed = Fixtures.placeOnTop(c, GRID[row][col], X + col, Y, Z + row);
                Expect.equal(GRID[row][col], placed.get("block").getAsString(), "placed " + GRID[row][col]);
                Expect.block(c, X + col, Y + 4, Z + row, GRID[row][col]);
            }
        }
        c.screenshot("00-before-fuel");
        view(c, "01-cold-south", X + 2.5, Y + 6, Z + 9, X + 2.5, Y + 3, Z + 1);

        // fuel: two driver sources next to uranium
        String[] fuels = {"hbm:rbmk_fuel_ra226be", "hbm:rbmk_fuel_meu", "", "hbm:rbmk_fuel_heu235", "hbm:rbmk_fuel_ueu"};
        for (int col = 0; col < 5; col++) {
            if (fuels[col].isEmpty()) continue;
            c.cmd("item replace block " + (X + col) + " " + (Y + 1) + " " + Z + " container.0 with " + fuels[col]);
        }
        c.cmd("item replace block " + (X + 1) + " " + (Y + 1) + " " + (Z + 2) + " container.0 with hbm:powder_lithium 16");
        c.cmd("data merge block " + (X + 2) + " " + (Y + 1) + " " + Z + " {targetLevel:0.6d}");
        c.cmd("data merge block " + (X) + " " + (Y + 1) + " " + (Z + 1) + " {}");
        c.waitTicks(200);
        for (int col = 0; col < 5; col++) {
            String nbt = c.blockEntity(X + col, Y + 1, Z).get("nbt").getAsString().replaceAll("\\s", "");
            System.out.println("rod " + col + " " + nbt);
        }
        String ctrl = c.blockEntity(X + 2, Y + 1, Z).get("nbt").getAsString().replaceAll("\\s", "");
        Expect.that(ctrl.contains("level:0.5") || ctrl.contains("level:0.55") || ctrl.contains("level:0.6"),
                "the control rods travelled 0.00277 per tick toward 0.6: " + ctrl);

        view(c, "10-run-south", X + 2.5, Y + 6, Z + 9, X + 2.5, Y + 3, Z + 1);
        view(c, "11-run-north", X + 2.5, Y + 6, Z - 7, X + 2.5, Y + 3, Z + 1);
        view(c, "12-run-east", X + 11, Y + 6, Z + 1, X + 2.5, Y + 3, Z + 1);
        view(c, "13-run-west", X - 6, Y + 6, Z + 1, X + 2.5, Y + 3, Z + 1);
        view(c, "14-top-down", X + 2.5, Y + 12, Z + 1.6, X + 2.5, Y + 4, Z + 1);
        view(c, "15-close-rods", X + 1.5, Y + 4, Z + 4.5, X + 1.5, Y + 3, Z);

        // menus: aimed straight down at each column top so no other column is in the way
        int[][] menus = {{0, 0}, {2, 0}, {0, 1}, {1, 2}, {2, 2}, {4, 2}, {4, 1}, {3, 1}};
        String[] names = {"rod", "control", "boiler", "outgasser", "storage", "control-auto", "cooler", "moderator"};
        String[] expect = {"ScreenRBMKRod", "ScreenRBMKControl", "ScreenRBMKBoiler", "ScreenRBMKOutgasser",
                "ScreenRBMKStorage", "ScreenRBMKControlAuto", null, null};
        c.cmd("gamemode creative");
        c.hotbar(9);
        for (int i = 0; i < menus.length; i++) {
            int mx = X + menus[i][0], mz = Z + menus[i][1];
            c.teleport(mx + 0.5, Y + 8, mz + 0.5);
            c.waitTicks(4);
            c.look(0, 90);
            c.waitTicks(2);
            c.use(2);
            c.waitTicks(6);
            String screen = c.screenClass();
            System.out.println("menu " + names[i] + " -> " + screen);
            if (expect[i] != null) {
                Expect.that(screen != null && screen.contains(expect[i]), names[i] + " opened " + screen + ", expected " + expect[i]);
                c.screenshot("20-gui-" + names[i]);
                c.key("escape");
                c.waitTicks(5);
            } else {
                Expect.that(screen == null || screen.equals("null") || !screen.contains("RBMK"), "the cooler and moderator have no menu in 1.7, got " + screen);
            }
        }

        // save and rejoin while burning
        String before = c.blockEntity(X + 1, Y + 1, Z).get("nbt").getAsString().replaceAll("\\s", "");
        c.leaveWorld(60_000L);
        c.joinWorld(c.currentWorld() == null ? Client.flatWorld : c.currentWorld(), 120_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(40);
        String after = c.blockEntity(X + 1, Y + 1, Z).get("nbt").getAsString().replaceAll("\\s", "");
        System.out.println("before " + before);
        System.out.println("after  " + after);
        Expect.that(after.contains("rbmk_fuel_meu"), "the rod survived the rejoin: " + after);
        view(c, "30-rejoined", X + 2.5, Y + 6, Z + 9, X + 2.5, Y + 3, Z + 1);

        // forced meltdown
        c.cmd("data merge block " + (X + 1) + " " + (Y + 1) + " " + Z + " {heat:2500d}");
        c.waitTicks(10);
        view(c, "40-meltdown", X + 2.5, Y + 9, Z + 12, X + 2.5, Y + 3, Z + 1);
        c.waitTicks(60);
        view(c, "41-meltdown-after", X + 2.5, Y + 9, Z + 12, X + 2.5, Y + 3, Z + 1);
        for (String error : c.logSinceMark("(ERROR|FATAL|Exception in thread|ReportedException)")) {
            Expect.that(error.contains("Realms"), "unexpected error: " + error);
        }
    }

    private static void view(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        c.hotbar(8);
        c.waitTicks(2);
        c.cmd("gamemode spectator");
        c.waitTicks(2);
        c.teleport(x, y, z);
        c.waitTicks(12);
        c.lookAt(tx, ty, tz);
        c.waitTicks(10);
        c.screenshot(name);
    }
}
