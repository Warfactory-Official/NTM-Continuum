import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/**
 * The 1.7 X5846 network blocks in a real world: the flow control pump between two barrels (water moves, its screen sets
 * the throughput), the network separator (its screen sets the limit), the three pipe anchors linked by the pipe wrench,
 * the deprecated pump and diode opening no screen, and the multi identifier's bookmarks.
 */
public final class X5846NetworkBlocks implements Scenario {

    private static final int Y = 100, L = 101;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 24);
        c.cmd("fill -24 " + L + " -24 24 " + (L + 8) + " 24 minecraft:air");
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("time set 6000");

        pump(c);
        separator(c);
        anchors(c);
        deprecated(c);
        bookmarks(c);
        icons(c);

        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(pump|separator|anchor|diode|bookmark|GUIScreenFluid)");
    }

    /** Placed by hand facing south: 1.7 sends to the facing turned about DOWN, east. */
    private void pump(Client c) {
        int x = 0, z = -10;
        place(c, "hbm:flow_control_pump", x, z);
        String state = c.block(x, L, z).get("state").getAsString();
        Expect.that(state.contains("flow_control_pump") && state.contains("facing=south"), "the pump stands facing south: " + state);
        c.cmd("data merge block " + x + " " + L + " " + z + " {t_type:\"minecraft:water\"}");
        c.cmd("setblock " + (x - 2) + " " + L + " " + z + " hbm:barrel_steel{tank:16000,tank_type:\"minecraft:water\",mode:2s}");
        c.cmd("setblock " + (x + 2) + " " + L + " " + z + " hbm:barrel_steel{tank:0,tank_type:\"minecraft:water\",mode:0s}");
        c.waitTicks(40);
        int moved = tank(c, x + 2, L, z);
        Expect.that(moved > 0 && moved % 100 == 0, "the pump moved water west to east in 100 mB steps: " + moved);

        c.cmd("setblock " + x + " " + (L + 1) + " " + z + " hbm:block_steel");
        c.teleport(x + 0.5, L + 1, z + 4.5);
        c.lookAt(x + 0.5, L + 0.5, z + 0.5);
        c.waitTicks(25);
        c.screenshot("00-flow-control-pump-barrels-top-rail");

        c.hotbar(9);
        Fixtures.interact(c, x, L, z);
        c.waitScreen("GUIPump", 5_000L);
        Expect.screenStaysOpen(c, "GUIPump", 5);
        JsonObject box = editBox(c);
        c.click(box.get("center_x").getAsInt(), box.get("center_y").getAsInt());
        for (int i = 0; i < 3; i++) c.key("backspace");
        c.type("250");
        c.waitTicks(2);
        c.screenshot("01-flow-control-pump-screen");
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(5);
        Expect.that(nbt(c, x, L, z).contains("buffer:250"), "the screen set the throughput to 250: " + nbt(c, x, L, z));
    }

    private void separator(Client c) {
        int x = 0, z = 0;
        place(c, "hbm:network_separator", x, z);
        String state = c.block(x, L, z).get("state").getAsString();
        Expect.that(state.contains("network_separator"), "the separator stands: " + state);
        c.cmd("setblock " + (x - 2) + " " + L + " " + z + " hbm:red_cable");
        c.cmd("setblock " + (x + 2) + " " + L + " " + z + " hbm:red_cable");
        c.teleport(x + 0.5, L + 1, z + 5.5);
        c.lookAt(x + 0.5, L + 1, z + 0.5);
        c.waitTicks(25);
        c.screenshot("02-network-separator-cables");

        c.hotbar(9);
        Fixtures.interact(c, x, L, z);
        c.waitScreen("GUIDiode", 5_000L);
        Expect.screenStaysOpen(c, "GUIDiode", 5);
        JsonObject box = editBox(c);
        c.click(box.get("center_x").getAsInt(), box.get("center_y").getAsInt());
        for (int i = 0; i < 4; i++) c.key("backspace");
        c.type("2500");
        c.waitTicks(2);
        c.screenshot("03-network-separator-screen");
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(5);
        Expect.that(nbt(c, x, L, z).contains("configuredLimit:2500L"), "the screen set the limit to 2500: " + nbt(c, x, L, z));
    }

    /** Each pair is linked with the wrench and keeps one pipe in both lists; across kinds the wrench refuses. */
    private void anchors(Client c) {
        String[] kinds = {"hbm:pipe_anchor", "hbm:pipe_anchor_exhaust", "hbm:pipe_anchor_pneumatic"};
        int z = 10;
        for (int i = 0; i < kinds.length; i++) {
            int x = -8 + i * 8;
            c.cmd("setblock " + x + " " + L + " " + z + " " + kinds[i] + "[facing=up]");
            c.cmd("setblock " + (x + 5) + " " + (L + 2) + " " + z + " hbm:block_steel");
            c.cmd("setblock " + (x + 5) + " " + (L + 3) + " " + z + " " + kinds[i] + "[facing=up]");
        }
        c.cmd("data merge block -8 " + L + " " + z + " {type:\"minecraft:water\"}");
        Fixtures.giveOnly(c, "hbm:wrench", 1);
        for (int i = 0; i < kinds.length; i++) {
            int x = -8 + i * 8;
            c.teleport(x + 2.5, L, z + 2.5);
            c.waitTicks(4);
            useOn(c, x, L, z);
            useOn(c, x + 5, L + 3, z);
            c.waitTicks(4);
            Expect.that(nbt(c, x, L, z).contains("conCount:1") && nbt(c, x + 5, L + 3, z).contains("conCount:1"),
                    kinds[i] + " pair linked: " + nbt(c, x, L, z));
        }
        c.teleport(-3.5, L, z + 2.5);
        c.waitTicks(4);
        useOn(c, -8, L, z);
        useOn(c, 0, L, z);
        c.waitTicks(4);
        Expect.that(nbt(c, -8, L, z).contains("conCount:1"), "a fluid anchor does not link to an exhaust anchor: " + nbt(c, -8, L, z));
        c.teleport(2.5, L + 4, z + 13.5);
        c.lookAt(2.5, L + 1.5, z + 0.5);
        c.waitTicks(25);
        c.screenshot("04-pipe-anchors-fluid-exhaust-pneumatic");
    }

    /** 1.7 X5846: the old pump and the diode are conductors with no screen left. */
    private void deprecated(Client c) {
        int z = 18;
        c.cmd("setblock -2 " + L + " " + z + " hbm:fluid_pump[facing=north]");
        c.cmd("setblock 2 " + L + " " + z + " hbm:cable_diode[facing=north]");
        c.hotbar(9);
        c.teleport(0.5, L, z + 3.5);
        c.waitTicks(4);
        Fixtures.interact(c, -2, L, z);
        c.waitTicks(10);
        Expect.that(c.screenClass() == null,
                "the deprecated pump opens nothing: " + c.screenClass());
        Fixtures.interact(c, 2, L, z);
        c.waitTicks(10);
        Expect.that(c.screenClass() == null,
                "the deprecated diode opens nothing: " + c.screenClass());
    }

    /** A middle click bookmarks a fluid; the next empty search shows it first and the item keeps it. */
    private void bookmarks(Client c) {
        Fixtures.giveOnly(c, "hbm:fluid_identifier_multi", 1);
        c.teleport(0.5, L, 22.5);
        c.waitTicks(4);
        c.look(0, -60);
        c.keyDown("key.sneak");
        c.waitTicks(2);
        c.use(2);
        c.keyUp("key.sneak");
        c.waitScreen("GUIScreenFluid", 5_000L);
        c.type("diesel");
        c.waitTicks(3);
        JsonObject search = editBox(c);
        int left = search.get("x").getAsInt() - 46, top = search.get("y").getAsInt() - 11;
        c.click(left + 7 + 9, top + 29 + 9, 2);
        c.waitTicks(3);
        for (int i = 0; i < 6; i++) c.key("backspace");
        c.waitTicks(3);
        c.screenshot("05-fluid-identifier-bookmark");
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(5);
        c.cmd("gamerule sendCommandFeedback true");
        c.cmd("data get entity @s SelectedItem");
        c.waitTicks(10);
        String held = String.join(" ", c.log(400, "bookmarks"));
        Expect.that(held.contains("diesel"), "the identifier keeps the diesel bookmark: " + held);
    }

    private void icons(Client c) {
        c.cmd("clear");
        for (String id : new String[]{"flow_control_pump", "network_separator", "pipe_anchor", "pipe_anchor_exhaust",
                "pipe_anchor_pneumatic", "fluid_pump", "cable_diode", "barrel_corroded"}) {
            c.cmd("give @s hbm:" + id + " 1");
        }
        c.waitTicks(5);
        c.key("e");
        c.waitScreen("Inventory", 5_000L);
        c.waitTicks(10);
        c.screenshot("06-item-icons");
        c.key("escape");
        c.waitScreen("none", 5_000L);
    }

    /** Right-clicks the middle of an anchor with the item in hotbar slot 1. */
    private static void useOn(Client c, int x, int y, int z) {
        c.hotbar(1);
        c.lookAt(x + 0.5, y + 0.4, z + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(4);
    }

    private static void place(Client c, String item, int x, int z) {
        Fixtures.giveOnly(c, item, 1);
        c.cmd("attribute @s minecraft:player.block_interaction_range base set 6");
        c.teleport(x + 0.5, L, z + 3.5);
        c.waitTicks(4);
        c.lookAt(x + 0.5, Y + 0.98, z + 0.5);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(6);
    }

    private static JsonObject editBox(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("widgets")) {
            JsonObject w = e.getAsJsonObject();
            if (w.has("value")) return w;
        }
        throw new AssertionError("no text field on " + c.screenClass());
    }

    private static int tank(Client c, int x, int y, int z) {
        String n = nbt(c, x, y, z);
        int i = n.indexOf("tank:");
        if (i < 0) return 0;
        int j = i + 5;
        while (j < n.length() && Character.isDigit(n.charAt(j))) j++;
        return Integer.parseInt(n.substring(i + 5, j));
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
    }
}
