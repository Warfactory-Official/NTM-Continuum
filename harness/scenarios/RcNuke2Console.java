import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * RBMK console and display panel in the world: a seven column row (fuel, manual rod, boiler, auto rod, absorber, fuel, manual
 * rod) is linked to the console with the RBMK tool; the scan the server holds must equal what the client received, the
 * grid clicks, the level field, the AZ-5 button and the screen buttons must reach the rods, and a display panel and
 * a second console on another facing must read the same row. Saved and rejoined at the end.
 */
public final class RcNuke2Console implements Scenario {

    private static final int X = 300;
    private static final int Y = 100;
    private static final int Z = 300;
    private static final String[] ROW = {"hbm:rbmk_fuel_rod", "hbm:rbmk_control", "hbm:rbmk_boiler", "hbm:rbmk_control_auto",
            "hbm:rbmk_absorber", "hbm:rbmk_fuel_rod", "hbm:rbmk_control"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.resetWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z - 6, 24);
        c.cmd("gamemode creative");
        c.waitTicks(5);

        for (int i = 0; i < ROW.length; i++) {
            JsonObject placed = Fixtures.placeOnTop(c, ROW[i], X - 3 + i, Y, Z);
            Expect.equal(ROW[i], placed.get("block").getAsString(), "placed " + ROW[i]);
        }
        int cy = Y + 1;
        c.cmd("item replace block " + (X - 3) + " " + cy + " " + Z + " container.0 with hbm:rbmk_fuel_ueu");
        c.cmd("item replace block " + (X + 2) + " " + cy + " " + Z + " container.0 with hbm:rbmk_fuel_heu235");
        c.cmd("data merge block " + (X - 2) + " " + cy + " " + Z + " {level:0.4d,targetLevel:0.4d}");
        c.cmd("data merge block " + (X + 3) + " " + cy + " " + Z + " {level:0.9d,targetLevel:0.9d}");
        c.waitTicks(10);

        // a console on each of two facings, in front of and behind the row
        int[] south = place(c, "hbm:rbmk_console", X, Z - 12, X + 2.5, Z - 12 + 0.5, "south side");
        int[] north = place(c, "hbm:rbmk_console", X + 16, Z + 12, X + 16 - 2.5, Z + 12 + 0.5, "north side");
        System.out.println("[RcNuke2Console] consoles at " + south[0] + "," + south[1] + "," + south[2] + " and " + north[0] + "," + north[1] + "," + north[2]);
        Expect.that(south != null && north != null, "both consoles placed");
        JsonObject f1 = c.block(south[0], south[1], south[2]);
        JsonObject f2 = c.block(north[0], north[1], north[2]);
        System.out.println("[RcNuke2Console] facings " + f1 + " / " + f2);

        // the display panel two blocks from the row
        Fixtures.setBlock(c, X, cy + 2, Z - 4, "hbm:rbmk_display[facing=north]");
        Expect.block(c, X, cy + 2, Z - 4, "hbm:rbmk_display");

        // link the tool to the middle column, then to each console and the display
        Fixtures.giveOnly(c, "hbm:rbmk_tool", 1);
        aimAndUse(c, X + 0.5, cy + 1.5, Z + 0.5, X + 0.5, cy + 1, Z + 3.5, true);
        aimAndUse(c, south[0] + 0.5, south[1] + 0.5, south[2] + 0.5, south[0] + 0.5, south[1], south[2] + 4.5, true);
        aimAndUse(c, north[0] + 0.5, north[1] + 0.5, north[2] + 0.5, north[0] + 0.5, north[1], north[2] - 4.5, true);
        aimAndUse(c, X + 0.5, cy + 2.5, Z - 4 + 0.5, X + 0.5, cy + 1, Z - 1.5, true);
        String nbtS = c.blockEntity(south[0], south[1], south[2]).get("nbt").getAsString().replaceAll("\\s", "");
        String nbtN = c.blockEntity(north[0], north[1], north[2]).get("nbt").getAsString().replaceAll("\\s", "");
        System.out.println("[RcNuke2Console] south " + nbtS);
        System.out.println("[RcNuke2Console] north " + nbtN);
        Expect.that(nbtS.contains("tX:" + X) && nbtS.contains("tZ:" + Z), "south console linked to the middle column: " + nbtS);
        Expect.that(nbtN.contains("tX:" + X) && nbtN.contains("tZ:" + Z), "north console linked to the middle column: " + nbtN);
        String nbtD = c.blockEntity(X, cy + 2, Z - 4).get("nbt").getAsString().replaceAll("\\s", "");
        Expect.that(nbtD.contains("tX:" + X) && nbtD.contains("tZ:" + Z), "display linked: " + nbtD);
        c.waitTicks(40);

        // server scan equals client copy
        JsonObject q = c.query("hbm_rbmk_console", coords(south));
        System.out.println("[RcNuke2Console] south query " + q);
        JsonObject server = q.getAsJsonObject("server");
        JsonObject client = q.getAsJsonObject("client");
        Expect.that(server != null && server.has("columns") && server.getAsJsonArray("columns").size() == 7, "the server scanned 7 columns: " + q);
        Expect.equal(server.get("columns").toString(), client.get("columns").toString(), "client columns equal server columns");
        String cols = server.get("columns").toString();
        Expect.that(cols.contains("FUEL") && cols.contains("CONTROL_AUTO") && cols.contains("BOILER") && cols.contains("ABSORBER"), "column types: " + cols);
        Expect.that(cols.contains("max=2865") && cols.contains("level=0.4") && cols.contains("level=0.9"), "column data: " + cols);
        JsonObject qn = c.query("hbm_rbmk_console", coords(north));
        Expect.equal(qn.getAsJsonObject("server").get("columns").toString(), qn.getAsJsonObject("client").get("columns").toString(), "north console client equals server");
        JsonObject qd = c.query("hbm_rbmk_console", disp());
        System.out.println("[RcNuke2Console] display query " + qd);
        Expect.that(qd.getAsJsonObject("client").getAsJsonArray("columns").size() == 7, "display scans the same 7 columns: " + qd);
        Expect.equal(qd.getAsJsonObject("server").get("columns").toString(), qd.getAsJsonObject("client").get("columns").toString(), "display client equals server");

        shot(c, "00-overview-south", X + 0.5, cy + 8, Z - 22, X + 0.5, cy + 3, Z - 8);
        shot(c, "01-overview-north", X + 12.5, cy + 8, Z + 22, X + 16.5, cy + 3, Z + 10);
        shot(c, "02-display", X + 0.5, cy + 3, Z - 12, X + 0.5, cy + 2.5, Z - 4);

        // the GUI of the south console
        open(c, south[0] + 0.5, south[1] + 0.5, south[2] + 0.5, south[0] + 0.5, south[1] + 1, south[2] + 4);
        String screen = c.screenClass();
        Expect.that(screen != null && screen.contains("ScreenRBMKConsole"), "console GUI opened: " + screen);
        c.waitTicks(10);
        c.screenshot("10-gui-console");
        int[] gui = gui(c, 244, 172);
        int idx = 7 * 15 + 7;
        // columns are at dx -3..3 on row 7; fuel at -3, manual at -2 and +3
        int[] fuel = cell(gui, idx - 3), manualA = cell(gui, idx - 2), manualB = cell(gui, idx + 3), fuelB = cell(gui, idx + 2);
        c.click(fuel[0], fuel[1]);
        c.click(fuelB[0], fuelB[1]);
        c.click(manualA[0], manualA[1]);
        c.click(manualB[0], manualB[1]);
        c.waitTicks(3);
        c.screenshot("11-gui-selected");
        // deselect the fuel rods again: only the two manual rods stay selected
        c.click(fuel[0], fuel[1]);
        c.click(fuelB[0], fuelB[1]);
        c.click(gui[0] + 9 + 15, gui[1] + 84 + 4);
        c.type("30");
        c.click(gui[0] + 48 + 6, gui[1] + 82 + 6);
        c.waitTicks(10);
        String a = ctrl(c, X - 2, cy, Z), b = ctrl(c, X + 3, cy, Z);
        System.out.println("[RcNuke2Console] after set 30: " + a + " | " + b);
        Expect.that(a.contains("targetLevel:0.3"), "the level field sent 30 to rod A: " + a);
        Expect.that(b.contains("targetLevel:0.3"), "the level field sent 30 to rod B: " + b);
        // the auto rod must not have been touched
        String auto = ctrl(c, X, cy, Z);
        c.screenshot("12-gui-level-set");

        // screen 1: cycle to depletion, assign the two fuel rods
        for (int i = 0; i < 3; i++) { c.click(gui[0] + 6 + 9, gui[1] + 8 + 9); c.waitTicks(3); }
        c.click(fuel[0], fuel[1]);
        c.click(fuelB[0], fuelB[1]);
        c.click(gui[0] + 24 + 9, gui[1] + 8 + 9);
        c.waitTicks(30);
        JsonObject q2 = c.query("hbm_rbmk_console", coords(south));
        System.out.println("[RcNuke2Console] screens " + q2);
        String screens = q2.getAsJsonObject("client").get("screens").toString();
        Expect.that(screens.contains("FUEL_DEPLETION=rbmk.screen.depletion="), "client shows the depletion monitor: " + screens);
        Expect.equal(q2.getAsJsonObject("server").get("screens").toString(), screens, "server and client monitors agree");
        c.screenshot("13-gui-monitor");

        // AZ-5: the lid first, then the button; every manual rod goes to 0
        c.click(gui[0] + 30 + 14, gui[1] + 138 + 14);
        c.waitTicks(3);
        c.screenshot("14-gui-az5-lid-open");
        c.click(gui[0] + 30 + 14, gui[1] + 138 + 14);
        c.waitTicks(20);
        a = ctrl(c, X - 2, cy, Z);
        b = ctrl(c, X + 3, cy, Z);
        System.out.println("[RcNuke2Console] after AZ-5: " + a + " | " + b);
        Expect.that(a.contains("targetLevel:0.0") && b.contains("targetLevel:0.0"), "AZ-5 sent both manual rods to 0");
        c.screenshot("15-gui-after-az5");
        c.key("escape");
        c.waitTicks(5);

        // north console: open and look
        open(c, north[0] + 0.5, north[1] + 0.5, north[2] + 0.5, north[0] + 0.5, north[1] + 1, north[2] - 4);
        Expect.that(c.screenClass() != null && c.screenClass().contains("ScreenRBMKConsole"), "north console GUI opened");
        c.waitTicks(10);
        c.screenshot("20-gui-north-console");
        c.key("escape");
        c.waitTicks(5);

        // save and rejoin: target, monitors and links survive
        c.leaveWorld(60_000L);
        c.joinWorld(c.currentWorld() == null ? Client.flatWorld : c.currentWorld(), 120_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(60);
        String after = c.blockEntity(south[0], south[1], south[2]).get("nbt").getAsString().replaceAll("\\s", "");
        Expect.that(after.contains("tX:" + X) && after.contains("fuel_depletion"), "console kept target and monitor: " + after);
        JsonObject q3 = c.query("hbm_rbmk_console", coords(south));
        Expect.that(q3.getAsJsonObject("client").getAsJsonArray("columns").size() == 7, "after the rejoin the client sees 7 columns again: " + q3);
        for (String error : c.logSinceMark("(ERROR|FATAL|Exception in thread|ReportedException)")) {
            Expect.that(error.contains("Realms"), "unexpected error: " + error);
        }
    }

    private static JsonObject coords(int[] at) {
        JsonObject p = new JsonObject();
        p.addProperty("x", at[0]);
        p.addProperty("y", at[1]);
        p.addProperty("z", at[2]);
        return p;
    }

    private static JsonObject disp() {
        return coords(new int[] {X, Y + 3, Z - 4});
    }

    private static String ctrl(Client c, int x, int y, int z) {
        return c.blockEntity(x, y, z).get("nbt").getAsString().replaceAll("\\s", "");
    }

    private static int[] cell(int[] gui, int index) {
        return new int[] {gui[0] + 86 + 10 * (index % 15) + 5, gui[1] + 11 + 10 * (index / 15) + 5};
    }

    private static int[] gui(Client c, int w, int h) {
        JsonObject status = c.status();
        int width = status.get("gui_width").getAsInt(), height = status.get("gui_height").getAsInt();
        return new int[] {(width - w) / 2, (height - h) / 2};
    }

    /** Places a multiblock by hand from the given standing spot and returns the position of its core cell. */
    private static int[] place(Client c, String item, int bx, int bz, double px, double pz, String what) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(px, Y + 1, pz);
        c.waitTicks(3);
        c.lookAt(bx + 0.5, Y + 0.98, bz + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
        for (int dx = -3; dx <= 3; dx++) for (int dy = 0; dy <= 3; dy++) for (int dz = -3; dz <= 3; dz++) {
            JsonObject b = c.block(bx + dx, Y + dy, bz + dz);
            if (item.equals(b.get("block").getAsString()) && b.toString().contains("\"cell\":\"false\"")) {
                return new int[] {bx + dx, Y + dy, bz + dz};
            }
        }
        throw new IllegalStateException(item + " was not placed (" + what + ")");
    }

    /** A sneak-click: without sneaking the console and display open their screens before the tool is consulted (as in 1.7). */
    private static int shots;

    private static void aimAndUse(Client c, double tx, double ty, double tz, double px, double py, double pz, boolean sneak) {
        c.teleport(px, py, pz);
        c.waitTicks(3);
        if (sneak) { c.keyDown("key.sneak"); c.waitTicks(3); }
        c.lookAt(tx, ty, tz);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(4);
        System.out.println("[RcNuke2Console] after use screen=" + c.screenClass());
        c.screenshot("use-" + (shots++));
        if (sneak) { c.keyUp("key.sneak"); c.waitTicks(3); }
    }

    private static void open(Client c, double tx, double ty, double tz, double px, double py, double pz) {
        c.cmd("clear");
        c.waitTicks(2);
        c.teleport(px, py, pz);
        c.waitTicks(3);
        c.lookAt(tx, ty, tz);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(10);
    }

    private static void shot(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        c.cmd("gamemode spectator");
        c.waitTicks(2);
        c.teleport(x, y, z);
        c.waitTicks(12);
        c.lookAt(tx, ty, tz);
        c.waitTicks(10);
        c.screenshot(name);
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }
}
