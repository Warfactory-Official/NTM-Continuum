import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * In-game acceptance for the inertial confinement fusion reactor, by the original assembly flow:
 *
 * <ol>
 *   <li>every ICF item in the inventory, and the fuel pellet's tooltip;</li>
 *   <li>the {@code struct_icf_core} seed placed by hand facing north and its ghost preview; a vessel wall welded
 *       with the blowtorch and a casing riveted with the boltgun by real use; the rest of the 17-long vessel filled
 *       in, and the seed turning into the reactor;</li>
 *   <li>a laser array (two cells, two flash tubes, four capacitors, four turbochargers, a port, casing) built
 *       behind a controller placed by hand, assembled by a real right-click; a creative generator on the port;</li>
 *   <li>the pellet maker placed by hand, a deuterium/tritium pellet and a muon-catalysed one pressed through its
 *       menu;</li>
 *   <li>the pellet clicked into the reactor's menu, sodium supplied, and the running reactor read back: heat,
 *       pellet depletion, hot sodium and stellar flux out; the running menu and the fired laser photographed.</li>
 * </ol>
 *
 * Also places an ICF directly from its item (the dummyable route) and checks its core.
 */
public final class FinalIcf implements Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;           // ground level
    private static final int SX = 0, SZ = 0;      // the seed / reactor core
    private static final int CX = -12, CY = G + 3, CZ = 0; // the laser controller, beam east into the vessel's end
    private static final int PX = 0, PZ = -9;     // pellet maker
    private static final int DX = 0, DZ = 16;     // an ICF placed from its item

    static final String[] IDS = {"hbm:struct_icf_core", "hbm:icf", "hbm:machine_icf_press", "hbm:icf_controller",
            "hbm:icf_component", "hbm:icf_component_vessel", "hbm:icf_component_vessel_welded",
            "hbm:icf_component_structure", "hbm:icf_component_structure_bolted", "hbm:icf_laser_component_casing",
            "hbm:icf_laser_component_port", "hbm:icf_laser_component_cell", "hbm:icf_laser_component_emitter",
            "hbm:icf_laser_component_capacitor", "hbm:icf_laser_component_turbo", "hbm:icf_pellet",
            "hbm:icf_pellet_empty", "hbm:icf_pellet_depleted"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 30);

        inventory(c);

        // --- the seed, by hand, facing north (the placer stands north and looks south) ------------------------
        Fixtures.giveOnly(c, "hbm:struct_icf_core", 1);
        c.teleport(SX + 0.5, G, SZ - 2.5);
        c.waitTicks(3);
        c.lookAt(SX + 0.5, Y + 0.98, SZ + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
        JsonObject seed = Expect.block(c, SX, G, SZ, "hbm:struct_icf_core");
        Expect.that(seed.toString().contains("facing=north"), "the seed faces its placer: " + seed);
        worldShot(c, "10-seed-ghost", SX - 9.5, G + 7, SZ - 12.5, SX + 0.5, G + 2, SZ + 0.5);

        // --- tool conversions by real use ---------------------------------------------------------------------
        weld(c);
        rivet(c);

        // --- the vessel (1.7 TileEntityICFStruct): a partial build shows the ghost of the rest ------------------
        c.cmd("gamemode creative");
        fill(c, -8, G, -1, 8, G, 1, "hbm:icf_component replace minecraft:air");
        fill(c, -8, G + 1, -1, 8, G + 1, 1, "hbm:icf_component_structure_bolted");
        fill(c, -2, G + 1, -1, 2, G + 1, 1, "hbm:icf_component_vessel_welded");
        c.waitTicks(5);
        worldShot(c, "11-seed-partial", SX - 9.5, G + 7, SZ - 12.5, SX + 0.5, G + 2, SZ + 0.5);
        for (int dy = 2; dy <= 4; dy++) {
            fill(c, -8, G + dy, -2, 8, G + dy, 2, "hbm:icf_component_structure_bolted");
            fill(c, -2, G + dy, -2, 2, G + dy, 2, "hbm:icf_component_vessel_welded");
        }
        fill(c, -8, G + 3, 0, 8, G + 3, 0, "hbm:icf_component_vessel_welded");
        fill(c, -8, G + 5, -1, 8, G + 5, 1, "hbm:icf_component_structure_bolted");
        fill(c, -2, G + 5, -1, 2, G + 5, 1, "hbm:icf_component_vessel_welded");
        c.waitTicks(45);
        JsonObject core = Expect.block(c, SX, G, SZ, "hbm:icf");
        Expect.that(core.toString().contains("facing=north") && core.toString().contains("cell=false"),
                "the seed became the ICF core facing north: " + core);
        Expect.block(c, 8, G + 5, 0, "hbm:icf");
        Expect.block(c, -8, G + 3, 2, "hbm:icf");
        worldShot(c, "12-icf-formed", SX - 12.5, G + 6, SZ - 10.5, SX + 0.5, G + 2, SZ + 0.5);

        // --- the laser array behind the controller --------------------------------------------------------------
        buildArray(c);
        // stand on a pad east of the controller, below the beam line, and place it on the casing below by hand
        fill(c, CX + 1, CY - 1, -1, CX + 3, CY - 1, 1, "minecraft:stone");
        Fixtures.giveOnly(c, "hbm:icf_controller", 1);
        c.teleport(CX + 2.5, CY, CZ + 0.5);
        c.waitTicks(3);
        c.lookAt(CX + 0.5, CY - 0.02, CZ + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
        JsonObject controller = Expect.block(c, CX, CY, CZ, "hbm:icf_controller");
        Expect.that(controller.toString().contains("facing=east"), "the controller faces the reactor: " + controller);
        worldShot(c, "20-array-built", CX + 4.5, CY + 4, CZ - 6.5, CX - 2, CY, CZ);

        c.teleport(CX + 2.5, CY, CZ + 0.5);
        c.waitTicks(3);
        Fixtures.interact(c, CX, CY, CZ);
        c.waitTicks(5);
        String ctrl = nbt(c, CX, CY, CZ);
        Expect.that(ctrl.contains("assembled:1b"), "the array assembled; nbt " + ctrl);
        Expect.that(longOf(ctrl, "cellCount") == 2 && longOf(ctrl, "emitterCount") == 2
                && longOf(ctrl, "capacitorCount") == 4 && longOf(ctrl, "turbochargerCount") == 4,
                "part counts 2/2/4/4; nbt " + ctrl);
        Expect.block(c, CX - 2, CY + 1, CZ, "hbm:icf_block");
        Expect.that(c.block(CX - 2, CY + 1, CZ).toString().contains("port=true"), "the port became a port cell");
        Expect.block(c, CX - 1, CY, CZ, "hbm:icf_block");
        worldShot(c, "21-array-assembled", CX + 4.5, CY + 4, CZ - 6.5, CX - 2, CY, CZ);
        // move off the beam line before power arrives
        c.teleport(CX + 2.5, CY, CZ - 3.5);

        // --- the pellet maker ------------------------------------------------------------------------------------
        JsonObject press = Fixtures.placeOnTop(c, "hbm:machine_icf_press", PX, Y, PZ);
        Expect.equal("hbm:machine_icf_press", press.get("block").getAsString(), "pellet maker placed by hand");
        c.cmd("data merge block " + PX + " " + G + " " + PZ + " {t0:2000,t1:2000}");
        c.waitTicks(2);
        c.cmd("clear");
        c.cmd("give @s hbm:icf_pellet_empty 2");
        c.cmd("give @s hbm:particle_muon 1");
        c.waitTicks(3);
        openPress(c);
        c.screenshot("30-gui-press-fueled");
        clickFromInventory(c, "hbm:icf_pellet_empty", 0);
        c.waitTicks(6);
        c.screenshot("31-gui-press-pressed");
        Expect.slotHolds(c, 1, "hbm:icf_pellet", 1);
        takeToInventory(c, 1);
        // the second pellet muon-catalysed
        clickFromInventory(c, "hbm:particle_muon", 2);
        c.waitTicks(3);
        c.cmd("data merge block " + PX + " " + G + " " + PZ + " {t0:2000,t1:2000}");
        clickFromInventory(c, "hbm:icf_pellet_empty", 0);
        c.waitTicks(6);
        c.screenshot("32-gui-press-muon");
        Expect.slotHolds(c, 1, "hbm:icf_pellet", 1);
        Expect.slotHolds(c, 3, "hbm:particle_empty", 1);
        takeToInventory(c, 1);
        close(c);
        String pressNbt = nbt(c, PX, G, PZ);
        Expect.that(pressNbt.contains("muon:15b"), "one muon charge used; nbt " + pressNbt);

        // pellet tooltips, hovered in the (survival) inventory
        c.cmd("gamemode survival");
        c.waitTicks(2);
        c.key("key.inventory");
        c.waitScreen("", 5000);
        c.waitTicks(4);
        hoverFirst(c, "hbm:icf_pellet", 0);
        c.screenshot("33-pellet-dt-tooltip");
        hoverFirst(c, "hbm:icf_pellet", 1);
        c.screenshot("34-pellet-muon-tooltip");
        close(c);
        c.cmd("gamemode creative");

        // --- the reactor's menu: the plain D/T pellet goes in by clicks, sodium in, the laser on ---------------
        openIcf(c);
        c.screenshot("40-gui-icf-empty");
        JsonObject plain = pelletSlot(c, false);
        c.clickSlot(plain.get("index").getAsInt());
        c.waitTicks(2);
        c.clickSlot(0);
        c.waitTicks(4);
        close(c);
        c.cmd("data merge block " + SX + " " + G + " " + SZ + " {t0:512000}");
        c.waitTicks(2);
        // power: a creative generator on the laser port
        Fixtures.setBlock(c, CX - 2, CY + 2, CZ, "hbm:creative_generator");
        c.waitTicks(8);

        String icf = nbt(c, SX, G, SZ);
        Expect.that(longOf(icf, "heat") > 0, "the reactor heats up; nbt " + icf);
        Expect.that(icf.contains("icf_depletion"), "the pellet in the chamber is depleting; nbt " + icf);
        Expect.that(longOf(icf, "t1") > 0, "hot sodium comes out; nbt " + icf);
        Expect.that(longOf(icf, "t0") < 512_000, "sodium is used; nbt " + icf);
        Expect.that(longOf(icf, "t2") > 0, "stellar flux comes out; nbt " + icf);
        String ctrlRunning = nbt(c, CX, CY, CZ);
        Expect.that(ctrlRunning.contains("assembled:1b"), "the array is still assembled; nbt " + ctrlRunning);

        openIcf(c);
        c.waitTicks(4);
        c.screenshot("41-gui-icf-running");
        JsonObject screen = c.screen();
        int left = screen.get("left").getAsInt(), top = screen.get("top").getAsInt();
        c.mouseMove(left + 16, top + 50);
        c.waitTicks(3);
        c.screenshot("42-gui-icf-laser-tooltip");
        c.mouseMove(left + 196, top + 98);
        c.waitTicks(3);
        c.screenshot("43-gui-icf-heat-tooltip");
        c.mouseMove(left + 196, top + 50);
        c.waitTicks(3);
        c.screenshot("44-gui-icf-hot-tank-tooltip");
        close(c);

        worldShot(c, "50-icf-running", CX + 2.5, CY + 3, CZ - 7.5, SX - 4, G + 3, SZ);
        worldShot(c, "51-icf-running-wide", SX + 6.5, G + 9, SZ - 14.5, SX - 3, G + 2, SZ);

        // --- the dummyable route: the ICF placed from its own item ----------------------------------------------
        Fixtures.giveOnly(c, "hbm:icf", 1);
        c.teleport(DX + 0.5, G, DZ - 3.5); // within the 5-block creative reach of the top face
        c.waitTicks(3);
        c.lookAt(DX + 0.5, Y + 0.98, DZ + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
        int[] placed = findCore(c, DX, G, DZ);
        Expect.that(placed != null, "an ICF placed from its item formed a core");
        System.out.println("[FinalIcf] item-placed core at " + placed[0] + "," + placed[1] + "," + placed[2] + ": "
                + c.block(placed[0], placed[1], placed[2]).get("state").getAsString());
        worldShot(c, "60-icf-from-item", DX + 0.5, G + 8, DZ - 14.5, DX, G + 2, DZ + 1);

        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture|Missing model");
    }

    private static void inventory(Client c) {
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.waitTicks(2);
        for (String id : IDS) c.cmd("give @s " + id + " 1");
        c.waitTicks(4);
        JsonObject inv = c.inventory();
        for (String id : IDS) Expect.that(inv.toString().contains("\"" + id + "\""), "inventory holds " + id);
        c.key("key.inventory");
        c.waitScreen("", 5000);
        c.waitTicks(10);
        c.screenshot("01-inventory");
        close(c);
        c.cmd("clear");
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }

    /** A vessel wall, welded by the blowtorch with one cast bismuth-bronze plate in the inventory. */
    private static void weld(Client c) {
        int x = 12, z = -9;
        Fixtures.setBlock(c, x, G, z, "hbm:icf_component_vessel");
        c.cmd("clear");
        c.cmd("gamemode survival");
        c.cmd("give @s hbm:blowtorch 1");
        c.cmd("give @s hbm:plate_cast_bismuth_bronze 1");
        c.waitTicks(3);
        c.hotbar(1);
        c.teleport(x + 0.5, G, z - 2.5);
        c.waitTicks(2);
        c.lookAt(x + 0.5, G + 0.5, z);
        c.waitTicks(1);
        c.screenshot("15-weld-before");
        c.use(2);
        c.waitTicks(4);
        Expect.block(c, x, G, z, "hbm:icf_component_vessel_welded");
        Expect.that(!c.inventory().toString().contains("plate_cast_bismuth_bronze"), "the plate was used up");
        c.screenshot("16-weld-after");
    }

    /** A casing, riveted by the boltgun with a cast steel plate and four dura bolts. */
    private static void rivet(Client c) {
        int x = 15, z = -9;
        Fixtures.setBlock(c, x, G, z, "hbm:icf_component_structure");
        c.cmd("clear");
        c.cmd("give @s hbm:boltgun 1");
        c.cmd("give @s hbm:plate_cast_steel 1");
        c.cmd("give @s hbm:bolt_dura_steel 4");
        c.waitTicks(3);
        c.hotbar(1);
        c.teleport(x + 0.5, G, z - 2.5);
        c.waitTicks(2);
        c.lookAt(x + 0.5, G + 0.5, z);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
        Expect.block(c, x, G, z, "hbm:icf_component_structure_bolted");
        Expect.that(!c.inventory().toString().contains("bolt_dura_steel"), "the bolts were used up");
        c.screenshot("17-rivet-after");
        c.cmd("clear");
    }

    /**
     * The array behind the controller (which faces east at {@code CX}): casing boxing in, around the line
     * x-1/x-2, two cells, flash tubes either side of the first, capacitors on the tubes, turbochargers on those,
     * and a port above the second cell.
     */
    private static void buildArray(Client c) {
        fill(c, CX - 4, CY - 1, CZ - 3, CX, CY + 1, CZ + 3, "hbm:icf_laser_component_casing");
        Fixtures.setBlock(c, CX, CY, CZ, "minecraft:air");
        set(c, -1, 0, "hbm:icf_laser_component_cell");
        set(c, -2, 0, "hbm:icf_laser_component_cell");
        set(c, -1, 1, "hbm:icf_laser_component_emitter");
        set(c, -1, -1, "hbm:icf_laser_component_emitter");
        set(c, -2, 1, "hbm:icf_laser_component_capacitor");
        set(c, -2, -1, "hbm:icf_laser_component_capacitor");
        set(c, -1, 2, "hbm:icf_laser_component_capacitor");
        set(c, -1, -2, "hbm:icf_laser_component_capacitor");
        set(c, -2, 2, "hbm:icf_laser_component_turbo");
        set(c, -2, -2, "hbm:icf_laser_component_turbo");
        set(c, -3, 1, "hbm:icf_laser_component_turbo");
        set(c, -3, -1, "hbm:icf_laser_component_turbo");
        Fixtures.setBlock(c, CX - 2, CY + 1, CZ, "hbm:icf_laser_component_port");
    }

    private static void set(Client c, int dx, int dz, String block) {
        Fixtures.setBlock(c, CX + dx, CY, CZ + dz, block);
    }

    private static void fill(Client c, int x0, int y0, int z0, int x1, int y1, int z1, String block) {
        c.cmd("fill " + x0 + " " + y0 + " " + z0 + " " + x1 + " " + y1 + " " + z1 + " " + block);
        c.waitTicks(1);
    }

    private static void openPress(Client c) {
        c.teleport(PX + 0.5, G, PZ - 2.5);
        c.waitTicks(2);
        Fixtures.interact(c, PX, G, PZ);
        c.waitScreen("GUIICFPress", 5000);
        Expect.screenStaysOpen(c, "GUIICFPress", 5);
    }

    /** A real right-click on the vessel's north bulge. */
    private static void openIcf(Client c) {
        c.teleport(SX + 0.5, G, SZ - 5.5);
        c.waitTicks(2);
        Fixtures.interact(c, SX, G + 3, SZ - 2);
        c.waitScreen("GUIICF", 5000);
        Expect.screenStaysOpen(c, "GUIICF", 5);
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(2);
    }

    private static void clickFromInventory(Client c, String id, int machineSlot) {
        JsonObject from = findSlot(c, id);
        Expect.that(from != null, "a player slot holds " + id);
        c.clickSlot(from.get("index").getAsInt());
        c.waitTicks(2);
        c.clickSlot(machineSlot, 1); // right-click: put one
        c.waitTicks(2);
        c.clickSlot(from.get("index").getAsInt()); // the rest back
        c.waitTicks(2);
    }

    private static void takeToInventory(Client c, int machineSlot) {
        c.clickSlot(machineSlot);
        c.waitTicks(2);
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.get("empty").getAsBoolean()) {
                c.clickSlot(s.get("index").getAsInt());
                c.waitTicks(2);
                return;
            }
        }
        throw new IllegalStateException("no empty player slot");
    }

    private static JsonObject findSlot(Client c, String id) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && !s.get("empty").getAsBoolean()
                    && s.get("id").getAsString().equals(id)) return s;
        }
        return null;
    }

    /** The plain (not catalysed) or the muon pellet among the player's slots, told apart by their tooltip data. */
    private static JsonObject pelletSlot(Client c, boolean muon) {
        JsonObject first = null, second = null;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && !s.get("empty").getAsBoolean()
                    && s.get("id").getAsString().equals("hbm:icf_pellet")) {
                if (first == null) first = s; else second = s;
            }
        }
        Expect.that(first != null && second != null, "both pressed pellets are in the inventory");
        // the plain pellet was taken out first, into the first empty slot, so it sits in the lower index
        return muon ? second : first;
    }

    private static void hoverFirst(Client c, String id, int nth) {
        int seen = 0;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(id)) {
                if (seen++ == nth) {
                    c.mouseMove(s.get("center_x").getAsInt(), s.get("center_y").getAsInt());
                    c.waitTicks(4);
                    return;
                }
            }
        }
        throw new IllegalStateException("no slot " + nth + " holding " + id);
    }

    private static int[] findCore(Client c, int x, int y, int z) {
        for (int dx = -9; dx <= 9; dx++) {
            for (int dz = -9; dz <= 9; dz++) {
                if (Math.abs(dx) > 2 && Math.abs(dz) > 2) continue; // the core is on one of the two axes
                JsonObject be = c.blockEntity(x + dx, y, z + dz);
                if (be.has("block_entity") && !be.get("block_entity").isJsonNull()
                        && be.get("block_entity").getAsString().endsWith("TileEntityICF")) {
                    return new int[]{x + dx, y, z + dz};
                }
            }
        }
        return null;
    }

    private static String nbt(Client c, int x, int y, int z) {
        return c.blockEntity(x, y, z).get("nbt").getAsString().replaceAll("\\s+", "");
    }

    private static long longOf(String nbt, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?<![A-Za-z0-9_])" + key + ":(-?[0-9]+)").matcher(nbt);
        Expect.that(m.find(), key + " present in " + nbt);
        return Long.parseLong(m.group(1));
    }

    private static void worldShot(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        // the player falls while it waits, so it is put back and re-aimed right before the frame
        c.teleport(x, y, z);
        c.lookAt(tx, ty, tz);
        c.waitTicks(8);
        c.teleport(x, y, z);
        c.lookAt(tx, ty, tz);
        c.waitTicks(1);
        c.screenshot(name);
    }
}
