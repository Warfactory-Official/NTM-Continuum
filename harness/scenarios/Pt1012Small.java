import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Playtest 2026-10-12 "small" fixes.
 *
 * <ul>
 *   <li>turrets: a second turret clicked onto the first turret's cells or side is refused (and refunded); a footprint
 *       overlap never leaves two cores; printed block maps.</li>
 *   <li>infinite tank: inf_water / the infinite barrel in a machine's fill slot stays there (never moved to the
 *       empty-container slot) while the tank fills.</li>
 *   <li>battery socket: creative battery (spinning pony + arcs), a plain item (floating, spinning), a battery pack,
 *       and the supports under a ceiling.</li>
 *   <li>RBMK console: lid, open, pressed AZ-5 button against the 1.7 texture.</li>
 *   <li>red ladder + red ladder top: modelled, not flat.</li>
 * </ul>
 */
public final class Pt1012Small implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        if (c.screenClass() != null) {   // a screen left open by an earlier run
            c.key("escape");
            c.waitTicks(5);
        }
        c.cmd("gamemode creative");
        c.cmd("time set 6000");
        c.cmd("gamerule doDaylightCycle false");
        Fixtures.platform(c, 0, Y, 0, 30);
        for (int zz = -30; zz <= 30; zz += 10) c.cmd("fill -30 " + (Y + 1) + " " + zz + " 30 " + (Y + 12) + " " + (zz + 9) + " minecraft:air");
        c.waitTicks(5);

        turrets(c);
        infiniteTank(c);
        batterySocket(c);
        rbmkConsole(c);
        ladders(c);
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(turret|socket|rbmk|ladder|CanisterBays|FluidContainer)");
    }

    // ------------------------------------------------------------------ turrets

    private static String blocksAt(Client c, int x0, int x1, int y, int z0, int z1) {
        StringBuilder sb = new StringBuilder();
        for (int z = z0; z <= z1; z++) {
            for (int x = x0; x <= x1; x++) {
                JsonObject b = c.block(x, y, z);
                String id = b.has("block") ? b.get("block").getAsString() : b.toString();
                id = id.replace("minecraft:", "").replace("hbm:", "");
                sb.append(String.format("%-14s", id.length() > 13 ? id.substring(0, 13) : id));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static int aimN;

    private static void aimClick(Client c, double px, double py, double pz, double tx, double ty, double tz) {
        c.teleport(px, py, pz);
        c.waitTicks(2);
        c.lookAt(tx, ty, tz);
        c.waitTicks(2);
        c.keyDown("key.sneak");
        c.waitTicks(2);
        c.screenshot("aim-" + (aimN++));
        c.use(2);
        c.waitTicks(4);
        c.keyUp("key.sneak");
        c.waitTicks(2);
    }

    private static int count(Client c, int x0, int x1, int y, int z0, int z1, String id) {
        int n = 0;
        for (int z = z0; z <= z1; z++)
            for (int x = x0; x <= x1; x++) if (c.block(x, y, z).get("block").getAsString().contains(id)) n++;
        return n;
    }

    private void turrets(Client c) {
        // A: placed from the floor
        Fixtures.giveOnly(c, "hbm:turret_chekhov", 1);
        c.teleport(-20 + 0.5, Y + 1, -20 - 2.5);
        c.waitTicks(2);
        c.look(0, 30);
        // aim at the floor block (-20, Y, -20)'s top
        c.lookAt(-20 + 0.5, Y + 0.98, -20 + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(5);
        System.out.println("[" + c.node() + "] [turret] A at y=" + (Y + 1) + "\n" + blocksAt(c, -22, -18, Y + 1, -22, -18));

        // control: a sneak-click on a stone block places a turret on top of it
        c.cmd("setblock -10 " + (Y + 1) + " -10 minecraft:stone");
        c.waitTicks(2);
        sneakPlace(c, -10, Y + 1, -10, 1.0);
        System.out.println("[" + c.node() + "] [turret] control on stone y=101\n" + blocksAt(c, -12, -8, Y + 1, -12, -8)
                + "y=102\n" + blocksAt(c, -12, -8, Y + 2, -12, -8));

        // B: sneak-click the top of A's core cell (a half block): the turret would land on top of A
        sneakPlace(c, -20, Y + 1, -20, 0.5);
        System.out.println("[" + c.node() + "] [turret] after B y=101\n" + blocksAt(c, -22, -18, Y + 1, -22, -18));
        System.out.println("[" + c.node() + "] [turret] after B y=102\n" + blocksAt(c, -22, -18, Y + 2, -22, -18));
        System.out.println("[" + c.node() + "] [turret] after B y=103\n" + blocksAt(c, -22, -18, Y + 3, -22, -18));
        Expect.equal(1, count(c, -22, -18, Y + 1, -22, -18, "turret_chekhov"), "turret A stands");
        Expect.equal(0, count(c, -22, -18, Y + 2, -22, -18, "turret_chekhov") + count(c, -22, -18, Y + 2, -22, -18, "turret_pad"),
                "a turret is refused on top of another turret's pad");
        // C: sneak-click the top of A's far pad cell
        sneakPlace(c, -19, Y + 1, -19, 0.5);
        Expect.equal(0, count(c, -22, -18, Y + 2, -22, -18, "turret_chekhov"), "second refusal (far pad cell)");
        System.out.println("[" + c.node() + "] [turret] after C y=102\n" + blocksAt(c, -22, -18, Y + 2, -22, -18));
    }

    /** Sneak-right-click the top face of the block at (x, y, z) (its top is {@code top} blocks up) holding a Chekhov. */
    private static void sneakPlace(Client c, int x, int y, int z, double top) {
        Fixtures.giveOnly(c, "hbm:turret_chekhov", 1);
        c.teleport(x + 2.5, y + 1, z + 0.5);
        c.waitTicks(2);
        c.lookAt(x + 0.5, y + top - 0.02, z + 0.5);
        c.waitTicks(1);
        c.keyDown("key.sneak");
        c.waitTicks(2);
        c.screenshot("sneak-" + x + "-" + z);
        c.use(2);
        c.waitTicks(5);
        c.keyUp("key.sneak");
        c.waitTicks(2);
    }

    // ------------------------------------------------------------------ infinite tank

    private static void inv(Client c, String block, int x, int y, int z, int slot, String item) {
        boolean legacy = c.node().startsWith("1.20.1");
        c.cmd("data merge block " + x + " " + y + " " + z + " {inventory:{Items:[{Slot:" + slot + "b,id:\"" + item + "\","
                + (legacy ? "Count:1b" : "count:1") + "}]}}");
    }

    /** Place a machine on the floor at (x, Y, z), find its block entity and print its inventory + tank NBT. */
    private JsonObject machine(Client c, String id, int x, int z) {
        Fixtures.placeOnTop(c, "hbm:" + id, x, Y, z);
        c.waitTicks(10);
        return findBE(c, x - 3, x + 3, Y + 1, z - 3, z + 3);
    }

    private static String nbt(JsonObject be) {
        return be == null || !be.has("nbt") ? "(none)" : be.get("nbt").getAsString().replaceAll("\\s+", " ");
    }

    private void infiniteTank(Client c) {
        // 1: the universal fluid tank, fill slot 2 / empty-container slot 3, a typed infinite item (inf_water, 50 mB/t)
        JsonObject tank = machine(c, "machine_fluidtank", 15, -20);
        int tx = tank.get("pos") == null ? 14 : 14;
        inv(c, "", 14, Y + 1, -20, 2, "hbm:inf_water");
        c.waitTicks(40);
        String after = nbt(c.blockEntity(14, Y + 1, -20));
        System.out.println("[" + c.node() + "] [inf] fluidtank after 40 ticks: " + after);
        Expect.that(after.contains("hbm:water") || after.contains("minecraft:water"), "the fluid tank filled with water: " + after);
        Expect.that(after.matches("(?s).*Slot: 2b.*hbm:inf_water.*") || after.matches("(?s).*hbm:inf_water.*Slot: 2b.*"),
                "inf_water stays in the fill slot 2: " + after);
        Expect.that(!after.matches("(?s).*Slot: 3b.*"), "nothing was moved to the empty-container slot 3: " + after);

        // 2: the combustion engine: its own bay (slot 0 in, 1 out); the infinite barrel feeds the diesel tank in place
        machine(c, "machine_combustion_engine", 15, -10);
        int[] ep = findPos(c, 12, 18, Y + 1, -13, -7);
        inv(c, "", ep[0], ep[1], ep[2], 0, "hbm:fluid_barrel_infinite");
        c.waitTicks(40);
        String eng = nbt(c.blockEntity(ep[0], ep[1], ep[2]));
        System.out.println("[" + c.node() + "] [inf] engine after 40 ticks: " + eng);
        Expect.that(eng.matches("(?s).*Slot: 0b.*hbm:fluid_barrel_infinite.*") || eng.matches("(?s).*hbm:fluid_barrel_infinite.*Slot: 0b.*"),
                "the infinite barrel stays in the engine's fill slot 0: " + eng);
        Expect.that(!eng.matches("(?s).*Slot: 1b.*"), "nothing in the engine's empty-container slot 1: " + eng);
        Expect.that(!eng.contains("tank: 0,"), "the engine's tank filled: " + eng);
    }

    private JsonObject findBE(Client c, int x0, int x1, int y, int z0, int z1) {
        for (int z = z0; z <= z1; z++)
            for (int x = x0; x <= x1; x++) {
                JsonObject be = c.blockEntity(x, y, z);
                if (be != null && be.has("block_entity") && !be.get("block_entity").isJsonNull()) {
                    System.out.println("[" + c.node() + "] [be] " + x + "," + y + "," + z + " " + be.toString().substring(0, Math.min(400, be.toString().length())));
                    return be;
                }
            }
        return null;
    }

    private int[] findPos(Client c, int x0, int x1, int y, int z0, int z1) {
        for (int z = z0; z <= z1; z++)
            for (int x = x0; x <= x1; x++) {
                JsonObject be = c.blockEntity(x, y, z);
                if (be != null && be.has("block_entity") && !be.get("block_entity").isJsonNull()) return new int[]{x, y, z};
            }
        return null;
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

    private void batterySocket(Client c) {
        boolean legacy = c.node().startsWith("1.20.1");
        String[] items = {"hbm:battery_creative", "minecraft:diamond", "hbm:battery_lead", "none"};
        int[][] spots = {{-25, 5}, {-20, 5}, {-15, 5}, {-10, 5}};
        for (int i = 0; i < items.length; i++) {
            Fixtures.placeOnTop(c, "hbm:machine_battery_socket", spots[i][0], Y, spots[i][1]);
            c.waitTicks(8);
            int[] pos = findPos(c, spots[i][0] - 3, spots[i][0] + 3, Y + 1, spots[i][1] - 3, spots[i][1] + 3);
            Expect.that(pos != null, "socket " + i + " placed");
            System.out.println("[" + c.node() + "] [socket] " + i + " core " + pos[0] + "," + pos[1] + "," + pos[2]);
            if (!items[i].equals("none")) inv(c, "", pos[0], pos[1], pos[2], 0, items[i]);
            if (i == 3) c.cmd("fill " + (pos[0] - 1) + " " + (Y + 3) + " " + (pos[2] - 1) + " " + (pos[0] + 1) + " " + (Y + 3) + " " + (pos[2] + 1) + " minecraft:stone");
            spots[i] = new int[]{pos[0], pos[2]};
        }
        c.waitTicks(40);
        for (int i = 0; i < items.length; i++) {
            shot(c, "socket-" + i + "-" + items[i].replace(':', '_'), spots[i][0] + 0.5, Y + 2.2, spots[i][1] + 4.5,
                    spots[i][0] + 0.5, Y + 1.8, spots[i][1] + 0.5);
        }
    }

    private void rbmkConsole(Client c) {
        Fixtures.placeOnTop(c, "hbm:rbmk_console", 20, Y, 5);
        c.waitTicks(10);
        int[] cp = findPos(c, 15, 25, Y + 1, 0, 10);
        Expect.that(cp != null, "console placed");
        System.out.println("[" + c.node() + "] [rbmk] core " + cp[0] + "," + cp[1] + "," + cp[2]);
        String screen = null;
        double[][] stands = {{0.5, 5.5}, {5.5, 0.5}, {-4.5, 0.5}, {0.5, -4.5}};
        for (int attempt = 0; attempt < stands.length && screen == null; attempt++) {
            c.teleport(cp[0] + stands[attempt][0], Y + 1, cp[2] + stands[attempt][1]);
            c.waitTicks(3);
            c.hotbar(9);
            c.waitTicks(1);
            c.lookAt(cp[0] + 0.5, Y + 1.5, cp[2] + 0.5);
            c.waitTicks(2);
            c.use(2);
            c.waitTicks(15);
            screen = c.screenClass();
        }
        System.out.println("[" + c.node() + "] [rbmk] screen " + screen);
        Expect.that(screen != null && screen.contains("RBMKConsole"), "RBMK console screen opens: " + screen);
        JsonObject st = c.status();
        int gw = st.get("gui_width").getAsInt(), gh = st.get("gui_height").getAsInt();
        int left = (gw - 244) / 2, top = (gh - 172) / 2;
        c.mouseMove(2, 2);
        c.waitTicks(3);
        c.screenshot("rbmk-0-lid", new int[]{left - 4, top - 4, 252, 180});
        c.click(left + 44, top + 152);   // lid
        c.waitTicks(5);
        c.mouseMove(2, 2);
        c.waitTicks(3);
        c.screenshot("rbmk-1-open", new int[]{left - 4, top - 4, 252, 180});
        c.click(left + 44, top + 152);   // press
        c.waitTicks(4);
        c.mouseMove(2, 2);
        c.screenshot("rbmk-2-pressed", new int[]{left - 4, top - 4, 252, 180});
        c.key("escape");
        c.waitTicks(5);
    }

    private void ladders(Client c) {
        // a four-high stone wall; red ladders up its south face at x=6, the top cap above the wall, an iron ladder for scale
        c.cmd("fill 5 " + (Y + 1) + " 20 12 " + (Y + 4) + " 20 minecraft:stone");
        c.cmd("fill 5 " + (Y + 1) + " 21 12 " + (Y + 6) + " 21 minecraft:air");
        c.waitTicks(3);
        for (int i = 1; i <= 4; i++) c.cmd("setblock 6 " + (Y + i) + " 21 hbm:ladder_red[facing=south]");
        c.cmd("setblock 6 " + (Y + 5) + " 21 hbm:ladder_red_top[facing=south]");
        c.cmd("setblock 8 " + (Y + 5) + " 21 hbm:ladder_red_top[facing=south]");
        for (int i = 1; i <= 4; i++) c.cmd("setblock 10 " + (Y + i) + " 21 hbm:ladder_iron[facing=south]");
        c.waitTicks(5);
        shot(c, "ladders-front", 7.5, Y + 4.0, 28.0, 7.5, Y + 3.5, 21.0);
        shot(c, "ladders-top-close", 7.0, Y + 6.2, 24.0, 7.0, Y + 5.3, 21.0);
        shot(c, "ladders-top-angle", 4.0, Y + 6.3, 24.5, 7.0, Y + 5.2, 21.0);
        shot(c, "ladders-low-angle", 3.0, Y + 2.3, 24.0, 6.5, Y + 2.0, 21.0);
        // held items (first person) and the hotbar icon
        Fixtures.giveOnly(c, "hbm:ladder_red", 1);
        c.teleport(7.5, Y + 2.3, 25.0);
        c.waitTicks(10);
        c.screenshot("item-ladder_red-held");
        Fixtures.giveOnly(c, "hbm:ladder_red_top", 1);
        c.waitTicks(10);
        c.screenshot("item-ladder_red_top-held");
    }
}
