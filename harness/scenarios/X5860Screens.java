import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/**
 * The 1.7 X5860 screens in a real client: the fluid burner's burn rate slider and overdrive slot, the calculator's
 * expression and QMAW search modes, and a deposit and shift-click withdrawal through the pneumatic access terminal.
 */
public final class X5860Screens implements Scenario {

    private static final int Y = 100, L = 101;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 24);
        c.cmd("gamerule sendCommandFeedback false");

        burner(c);
        calculator(c);
        pneumatic(c);

        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)exception.*(oilburner|calculator|qmaw|pneumo)");
    }

    /** Placed by hand from the south, so the core is one block north of the clicked cell. */
    private void burner(Client c) {
        int x = 0, z = -8;
        place(c, "hbm:heater_oilburner", x, z);
        int core = z - 1;
        Expect.that(c.block(x, L, core).get("state").getAsString().contains("heater_oilburner"), "the burner stands");

        c.cmd("give @s hbm:upgrade_overdrive_1 1");
        c.waitTicks(2);
        open(c, x, z, "GUIOilburner");
        JsonObject screen = c.screen();
        int left = screen.get("left").getAsInt(), top = screen.get("top").getAsInt();
        c.screenshot("00-burner-open");

        // 1.7 slider: x [70,106), rate = (mouseX - 70) * maxBurnRate / 32, applied each frame while held
        holdClick(c, left + 100, top + 27);
        c.waitTicks(6);
        Expect.that(nbt(c, x, L, core).contains("setting:9"), "the slider set 9 mB/t of 10: " + nbt(c, x, L, core));

        c.clickSlot(slotWith(c, "hbm:upgrade_overdrive_1"));
        c.waitTicks(1);
        c.clickSlot(3);
        c.waitTicks(10);
        Expect.slotHolds(c, 3, "hbm:upgrade_overdrive_1", 1);
        holdClick(c, left + 100, top + 27);
        c.waitTicks(6);
        Expect.that(nbt(c, x, L, core).contains("setting:18"), "overdrive I doubles the slider's range: " + nbt(c, x, L, core));
        c.mouseMove(left + 72, top + 27);
        c.waitTicks(3);
        c.screenshot("01-burner-overdrive");
        close(c);
    }

    private void calculator(Client c) {
        c.hotbar(9);
        c.key("n");
        c.waitScreen("GUIScreenCalculator", 5_000L);
        // the mode is static, as in 1.7: only calculator mode turns 1+1 into 2
        c.type("1+1");
        c.key("enter");
        c.waitTicks(2);
        if (!input(c).startsWith("2")) c.key("tab");
        clear(c);
        c.type("2+3*4");
        c.key("enter");
        c.waitTicks(3);
        Expect.that(input(c).startsWith("14"), "2+3*4 evaluates to 14: " + input(c));
        c.screenshot("02-calculator");

        c.key("tab");
        clear(c);
        c.type("steel");
        c.waitTicks(3);
        // as in 1.7, Enter opens only a selected entry
        c.key("down");
        c.waitTicks(2);
        c.screenshot("03-calculator-qmaw-search");
        c.key("enter");
        c.waitScreen("GuiQMAW", 5_000L);
        c.waitTicks(5);
        c.screenshot("04-qmaw-page");
        close(c);
        c.key("n");
        c.waitScreen("GUIScreenCalculator", 5_000L);
        c.key("tab");
        close(c);
    }

    private static String input(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("widgets")) {
            JsonObject w = e.getAsJsonObject();
            if (w.has("value")) return w.get("value").getAsString();
        }
        return "";
    }

    private static void clear(Client c) {
        c.key("end");
        for (int i = 0; i < 24; i++) c.key("backspace");
        c.waitTicks(1);
    }

    /** Terminal, two tubes, a clutter crate: sixteen cobblestone go in through the grid and come back with a shift-click. */
    private void pneumatic(Client c) {
        int x = 0, z = 8;
        Fixtures.setBlock(c, x, L, z, "hbm:pneumatic_storage_access");
        for (int i = 1; i <= 2; i++) Fixtures.setBlock(c, x + i, L, z, "hbm:pneumatic_tube[west=true,east=true]");
        Fixtures.setBlock(c, x + 3, L, z, "hbm:pneumatic_storage_clutter");
        air(c, x + 3, z);
        c.waitTicks(20);

        Fixtures.giveOnly(c, "minecraft:cobblestone", 16);
        air(c, x + 3, z);
        open(c, x, z, "GUIPneumoStorageAccess");
        c.clickSlot(slotWith(c, "minecraft:cobblestone"));
        c.waitTicks(2);
        c.clickSlot(0);
        c.waitTicks(10);
        Expect.that(nbt(c, x + 3, L, z).contains("cobblestone"), "the crate holds the deposit: " + nbt(c, x + 3, L, z));
        // a grid slot shows one of its type; the amount is drawn over it
        Expect.slotHolds(c, 0, "minecraft:cobblestone", 1);
        c.mouseMove(4, 4);
        c.waitTicks(2);
        c.screenshot("05-terminal-deposit");

        air(c, x + 3, z);
        // before 26.1 a screen reads shift from the physical keyboard, which the harness cannot hold: the click withdraws to the cursor
        boolean shiftReaches = c.node().startsWith("26.");
        shiftClickSlot(c, 0);
        c.waitTicks(10);
        Expect.that(!nbt(c, x + 3, L, z).contains("cobblestone"), "the withdrawal emptied the crate: " + nbt(c, x + 3, L, z));
        if (shiftReaches) {
            Expect.inventoryHolds(c, "minecraft:cobblestone", 16);
        } else {
            JsonObject carried = c.inventory().getAsJsonObject("carried");
            Expect.that(carried.get("id").getAsString().equals("minecraft:cobblestone") && carried.get("count").getAsInt() == 16,
                    "the click withdrew all 16 to the cursor: " + carried);
        }
        c.mouseMove(4, 4);
        c.waitTicks(2);
        c.screenshot("06-terminal-withdraw");
        close(c);
    }

    private static void holdClick(Client c, int x, int y) {
        JsonObject click = new JsonObject();
        click.addProperty("x", x);
        click.addProperty("y", y);
        click.addProperty("button", 0);
        click.addProperty("hold_ticks", 10);
        c.send("click", click);
        c.waitTicks(12);
    }

    /** The scenario click sends no modifiers, so shift goes into the click itself (GLFW_MOD_SHIFT) as well as the key state. */
    private static void shiftClickSlot(Client c, int index) {
        JsonObject slot = c.slot(index);
        JsonObject click = new JsonObject();
        click.addProperty("x", slot.get("center_x").getAsDouble());
        click.addProperty("y", slot.get("center_y").getAsDouble());
        click.addProperty("button", 0);
        click.addProperty("hold_ticks", 1);
        click.addProperty("mods", 1);
        c.keyDown("lshift");
        c.waitTicks(1);
        c.send("click", click);
        c.waitTicks(1);
        c.keyUp("lshift");
    }

    private static void air(Client c, int x, int z) {
        c.cmd("data merge block " + x + " " + L + " " + z + " {tank:4000}");
        c.waitTicks(1);
    }

    private static void place(Client c, String item, int x, int z) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(x + 0.5, L, z + 3.5);
        c.waitTicks(4);
        c.lookAt(x + 0.5, Y + 0.98, z + 0.5);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(6);
    }

    private static void open(Client c, int x, int z, String screen) {
        c.teleport(x + 0.5, L, z + 3.5);
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

    private static int slotWith(Client c, String itemId) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && s.get("id").getAsString().equals(itemId)) {
                return s.get("index").getAsInt();
            }
        }
        throw new IllegalStateException("no player slot holding " + itemId);
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
    }
}
