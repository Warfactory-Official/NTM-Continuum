import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * In-game acceptance for the RBMK console peripherals: the blank panel, display, keypad, lever,
 * numitron, graph and indicator panels, the autoloader and the steam connector.
 *
 * <p>Every configurable panel is opened by a real sneak + screwdriver right-click and screenshotted; the
 * first unit is switched on with real clicks on the old toggle rectangle and saved with the old save
 * button, so the in-world renderers have something to draw. A keypad button and a numitron share one
 * RoR channel, typed into their text fields, and a real right-click on the button must put its command
 * on the numitron: the end-to-end Redstone-over-Radio signal. The display is linked to a fuel column
 * with the linking device, and the autoloader is fed a rod and must lower it into the empty column.
 */
public final class FinalRbmkPeripherals implements Scenario {

    private static final int X = 96;
    private static final int Y = 100;
    private static final int Z = 96;
    /** Panels float in one row at this height, all facing south towards the camera. */
    private static final int PY = Y + 2;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 12);
        c.cmd("gamemode creative");
        c.waitTicks(5);

        // --- the reactor column the display, autoloader and connector work against ---
        int colX = X - 8, colZ = Z - 4;
        JsonObject rod = Fixtures.placeOnTop(c, "hbm:rbmk_fuel_rod", colX, Y, colZ);
        Expect.equal("hbm:rbmk_fuel_rod", rod.get("block").getAsString(), "fuel column core placed by hand");
        int top = Y + 4; // default column: core + three cells
        Expect.block(c, colX, top, colZ, "hbm:rbmk_fuel_rod");

        // --- panels ---
        String[] panels = {"hbm:rbmk_display_blank", "hbm:rbmk_display", "hbm:rbmk_key_pad", "hbm:rbmk_lever",
                "hbm:rbmk_numitron", "hbm:rbmk_graph", "hbm:rbmk_indicator"};
        int[] px = new int[panels.length];
        for (int i = 0; i < panels.length; i++) {
            px[i] = X - 6 + i * 2;
            Fixtures.setBlock(c, px[i], PY, Z, panels[i] + "[facing=south]");
            Expect.block(c, px[i], PY, Z, panels[i]);
        }
        for (int i = 1; i < panels.length; i++) {
            JsonObject be = c.blockEntity(px[i], PY, Z);
            Expect.that(be.has("block_entity") && !be.get("block_entity").isJsonNull(), "block entity exists for " + panels[i]);
        }
        Fixtures.setBlock(c, X + 8, Y + 1, Z, "hbm:rbmk_loader");
        Expect.block(c, X + 8, Y + 1, Z, "hbm:rbmk_loader");

        // keypad: open, activate key 1, give it channel + command, save
        String channel = "rbmkh";
        openWithScrewdriver(c, px[2], "Keypad");
        c.screenshot("10-gui-keypad");
        int[] gui = gui(c, 256, 204);
        c.click(gui[0] + 119, gui[1] + 62);          // active toggle, key 1
        typeInto(c, gui[0] + 40, gui[1] + 81, channel); // rtty field, key 1
        typeInto(c, gui[0] + 130, gui[1] + 81, "1234"); // command field, key 1
        c.screenshot("11-gui-keypad-configured");
        c.click(gui[0] + 218, gui[1] + 26);           // save
        closeScreen(c);

        // lever
        openWithScrewdriver(c, px[3], "Lever");
        c.screenshot("12-gui-lever");
        gui = gui(c, 256, 150);
        c.click(gui[0] + 119, gui[1] + 62);
        c.click(gui[0] + 119, gui[1] + 116);
        c.click(gui[0] + 218, gui[1] + 26);
        closeScreen(c);

        // numitron: listens on the keypad's channel
        openWithScrewdriver(c, px[4], "Numeric Display");
        c.screenshot("13-gui-numitron");
        gui = gui(c, 256, 150);
        c.click(gui[0] + 132, gui[1] + 62);
        typeInto(c, gui[0] + 40, gui[1] + 63, channel);
        c.click(gui[0] + 132, gui[1] + 116);
        c.screenshot("14-gui-numitron-configured");
        c.click(gui[0] + 218, gui[1] + 26);
        closeScreen(c);

        // graph
        openWithScrewdriver(c, px[5], "Graph");
        c.screenshot("15-gui-graph");
        gui = gui(c, 256, 150);
        c.click(gui[0] + 119, gui[1] + 62);
        typeInto(c, gui[0] + 40, gui[1] + 63, channel);
        c.click(gui[0] + 218, gui[1] + 26);
        closeScreen(c);

        // indicator: all six lamps on
        openWithScrewdriver(c, px[6], "Indicator Lights");
        c.screenshot("16-gui-indicator");
        gui = gui(c, 256, 258);
        for (int i = 0; i < 6; i++) c.click(gui[0] + 119, gui[1] + i * 36 + 44);
        c.click(gui[0] + 218, gui[1] + 17);
        closeScreen(c);

        // the pre-existing gauge through the same screwdriver path, as a control for the renderers' sync
        Fixtures.setBlock(c, X + 4, PY + 2, Z, "hbm:rbmk_gauge[facing=south]");
        openGauge(c, X + 4);
        c.screenshot("17-gui-gauge");
        gui = gui(c, 256, 204);
        c.click(gui[0] + 119, gui[1] + 62);
        c.click(gui[0] + 218, gui[1] + 26);
        closeScreen(c);

        String keyNbt = c.blockEntity(px[2], PY, Z).get("nbt").getAsString().replaceAll("\s", "");
        Expect.that(keyNbt.contains("rbmkh") && keyNbt.contains("1234"), "keypad saved channel and command: " + keyNbt);
        String numNbt = c.blockEntity(px[4], PY, Z).get("nbt").getAsString().replaceAll("\s", "");
        Expect.that(numNbt.contains("rbmkh"), "numitron saved its channel: " + numNbt);

        // --- RoR end to end: press key 1 by hand, the numitron must show 1234 ---
        c.hotbar(9);
        c.teleport(px[2] + 0.5, Y + 1, Z + 2.5);
        c.waitTicks(3);
        c.lookAt(px[2] + 0.25, PY + 0.75, Z + 0.74);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(6);
        String shown = c.blockEntity(px[4], PY, Z).get("nbt").getAsString().replaceAll("\s", "");
        Expect.that(shown.contains("value0:1234L"), "numitron received the keypad's RoR command: " + shown);

        // throw lever 1 by hand
        c.teleport(px[3] + 0.5, Y + 1, Z + 2.5);
        c.waitTicks(3);
        c.lookAt(px[3] + 0.25, PY + 0.5, Z + 0.74);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);

        // --- display: link to the fuel column with the linking device ---
        Fixtures.giveOnly(c, "hbm:rbmk_tool", 1);
        c.teleport(colX + 2.5, Y + 1, colZ + 0.5);
        c.waitTicks(3);
        c.lookAt(colX + 0.5, Y + 2.5, colZ + 0.5);
        c.waitTicks(1);
        // 1.7 as here: an unsneaking click on a column opens its GUI, so the linker is used sneaking
        c.keyDown("key.sneak");
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(3);
        c.keyUp("key.sneak");
        c.teleport(px[1] + 0.5, Y + 1, Z + 2.5);
        c.waitTicks(3);
        c.lookAt(px[1] + 0.5, PY + 0.5, Z + 0.74);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(15);
        String display = c.blockEntity(px[1], PY, Z).get("nbt").getAsString().replaceAll("\s", "");
        Expect.that(display.contains("tX:" + colX) && display.contains("tZ:" + colZ), "display linked to the column: " + display);

        // --- in-world renderers ---
        worldShot(c, "20-panels-row", X + 0.5, PY + 0.5, Z + 7.5, X + 0.5, PY + 0.5, Z + 0.75);
        worldShot(c, "21-panels-left", px[1] + 1.0, PY + 0.6, Z + 3.2, px[1] + 1.0, PY + 0.5, Z + 0.75);
        worldShot(c, "22-panels-middle", px[4] + 0.5, PY + 0.6, Z + 3.2, px[4] + 0.5, PY + 0.5, Z + 0.75);
        worldShot(c, "23-panels-right", px[6] - 0.5, PY + 0.6, Z + 3.2, px[6] - 0.5, PY + 0.5, Z + 0.75);
        worldShot(c, "24-gauge-control", X + 4.5, PY + 2.6, Z + 3.2, X + 4.5, PY + 2.5, Z + 0.75);
        c.cmd("gamemode creative");
        c.waitTicks(3);

        // --- autoloader on top of the fuel column ---
        // A plain right-click on the column would open its rod GUI, so the autoloader is placed sneaking.
        Fixtures.giveOnly(c, "hbm:rbmk_autoloader", 1);
        Fixtures.setBlock(c, colX + 2, top, colZ, "minecraft:stone"); // a step level with the column top
        c.teleport(colX + 2.5, top + 1, colZ + 0.5);
        c.waitTicks(3);
        c.lookAt(colX + 0.5, top + 0.98, colZ + 0.5);
        c.waitTicks(1);
        c.keyDown("key.sneak");
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(3);
        c.keyUp("key.sneak");
        c.waitTicks(3);
        JsonObject loader = c.block(colX, top + 1, colZ);
        Expect.equal("hbm:rbmk_autoloader", loader.get("block").getAsString(), "autoloader core placed on the column top");
        Expect.block(c, colX, top + 9, colZ, "hbm:rbmk_autoloader");
        c.teleport(colX + 2.5, top + 1, colZ + 0.5);
        c.waitTicks(3);
        Fixtures.interact(c, colX, top + 1, colZ);
        c.waitScreen("Autoloader", 5000);
        Expect.screenStaysOpen(c, "Autoloader", 10);
        c.screenshot("30-gui-autoloader");
        closeScreen(c);
        c.cmd("item replace block " + colX + " " + (top + 1) + " " + colZ + " container.0 with hbm:rbmk_fuel_ueu");
        c.waitTicks(80);
        worldShot(c, "31-autoloader-lowering", colX + 5.5, top + 3, colZ + 0.5, colX + 0.5, top + 4, colZ + 0.5);
        c.cmd("gamemode creative");
        c.waitTicks(260);
        String core = c.blockEntity(colX, Y + 1, colZ).get("nbt").getAsString().replaceAll("\s", "");
        Expect.that(core.contains("rbmk_fuel_ueu"), "the autoloader lowered the rod into the column: " + core);
        worldShot(c, "32-autoloader-and-loader", colX + 6.5, top + 2, colZ + 6.5, colX + 0.5, top + 2, colZ + 0.5);

        // --- inventory icons ---
        c.cmd("gamemode creative");
        c.teleport(X + 0.5, Y + 1, Z + 4.5);
        c.waitTicks(10);
        c.cmd("gamemode survival");
        c.waitTicks(2);
        c.cmd("clear");
        String[] icons = {"hbm:rbmk_display_blank", "hbm:rbmk_display", "hbm:rbmk_key_pad", "hbm:rbmk_lever",
                "hbm:rbmk_numitron", "hbm:rbmk_graph", "hbm:rbmk_indicator", "hbm:rbmk_autoloader", "hbm:rbmk_loader",
                "hbm:rbmk_gauge"};
        for (String icon : icons) c.cmd("give @s " + icon);
        c.waitTicks(5);
        for (String icon : icons) Expect.inventoryHolds(c, icon, 1);
        c.key("key.inventory");
        c.waitTicks(10);
        c.screenshot("40-inventory-icons");
        closeScreen(c);
        c.cmd("gamemode creative");

        for (String error : c.logSinceMark("(ERROR|FATAL|Exception in thread|ReportedException|Failed to load texture)")) {
            boolean offlineRealms = error.contains("RealmsAvailability") || error.contains("Realms");
            Expect.that(offlineRealms, "unexpected client error: " + error);
        }
    }

    /** Sneak + screwdriver right-click on the panel's front face, then wait for its screen. */
    private static void openWithScrewdriver(Client c, int x, String title) {
        Fixtures.giveOnly(c, "hbm:screwdriver", 1);
        c.teleport(x + 0.5, Y + 1, Z + 2.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, PY + 0.5, Z + 0.74);
        c.waitTicks(1);
        c.keyDown("key.sneak");
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(2);
        c.keyUp("key.sneak");
        c.waitScreen(title, 5000);
        c.waitTicks(3);
    }

    private static void openGauge(Client c, int x) {
        Fixtures.giveOnly(c, "hbm:screwdriver", 1);
        c.teleport(x + 0.5, Y + 1, Z + 3.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, PY + 2.5, Z + 0.74);
        c.waitTicks(1);
        c.keyDown("key.sneak");
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(2);
        c.keyUp("key.sneak");
        c.waitScreen("Gauge", 5000);
        c.waitTicks(3);
    }

    /** {guiLeft, guiTop} for a centred sheet of the given size. */
    private static int[] gui(Client c, int w, int h) {
        JsonObject status = c.status();
        int width = status.get("gui_width").getAsInt(), height = status.get("gui_height").getAsInt();
        return new int[] {(width - w) / 2, (height - h) / 2};
    }

    private static void typeInto(Client c, int x, int y, String text) {
        c.click(x, y);
        c.waitTicks(1);
        c.type(text);
        c.waitTicks(1);
    }

    private static void closeScreen(Client c) {
        c.key("escape");
        c.waitTicks(5);
    }

    private static void worldShot(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        c.hotbar(9);
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
