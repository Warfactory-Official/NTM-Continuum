import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * In-game acceptance for the satellite system (1.7 saveddata.satellites and friends): every satellite item and
 * block in the inventory; the ID manager placed by hand and used through its GUI to copy and randomise
 * frequencies; the Soyuz launch complex placed, loaded by clicks (rocket + precision laser satellite), fuelled
 * and launched with its button, the satellite reaching orbit; resonator and miner put up with /ntmsatellites
 * orbit and /ntmsatellites list read back; the laser designator firing the precision laser (crater + beam); the
 * satellite designator's coordinate screen teleporting through the resonator; the radar's sat_relay link slot
 * teleporting from a map click; the cargo landing pad receiving a miner rocket and its cargo; the ground station
 * tuned by a chip and reporting its connection; and, when the world is older than five minutes, the death ray.
 */
public final class FinalSatellites implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    static final String[] IDS = {"hbm:sat_mapper", "hbm:sat_scanner", "hbm:sat_radar", "hbm:sat_laser",
            "hbm:sat_foeq", "hbm:sat_resonator", "hbm:sat_miner", "hbm:sat_lunar_miner", "hbm:sat_gerald",
            "hbm:sat_chip", "hbm:sat_coord", "hbm:sat_designator", "hbm:sat_relay", "hbm:machine_satlinker",
            "hbm:sat_dock", "hbm:machine_satlink", "hbm:soyuz_launcher", "hbm:missile_soyuz_0",
            "hbm:missile_soyuz_lander"};

    // 1.7 EnumSatType ordinals
    static final int SPY = 0, SCANNER = 1, RADAR = 2, MINER_ASTRO = 3, MINER_LUNAR = 4, PRECISION_LASER = 5,
            DEATH_RAY = 6, XENIUM_RESONATOR = 7, RELAY = 8, DETECTOR = 9, RAY_SCAN = 10;

    // layout on the platform
    private static final int LX = X, LZ = Z - 6;            // ID manager
    private static final int DX = X - 14, DZ = Z - 14;      // cargo landing pad
    private static final int GX = X + 14, GZ = Z - 14;      // ground station
    private static final int RX = X - 14, RZ = Z + 12;      // radar
    private static final int SX = X + 18, SZ = Z + 16;      // soyuz launch complex (ground block)
    private static final int TX = X - 4, TZ = Z + 24;       // precision laser target

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 30);
        c.cmd("gamerule sendCommandFeedback true");
        c.cmd("ntmsatellites list"); // whatever an earlier run left in orbit is cleared below
        clearOrbit(c);

        inventory(c);

        // --- the ID manager: randomise, copy -------------------------------------------------------------------
        place(c, "hbm:machine_satlinker", LX, Y, LZ);
        Expect.block(c, LX, Y + 1, LZ, "hbm:machine_satlinker");
        worldShot(c, "10-satlinker-block", LX + 2.5, Y + 2.2, LZ + 2.5, LX + 0.5, Y + 1.5, LZ + 0.5);

        int laserFreq = tune(c, PRECISION_LASER, "hbm:sat_designator", "11-gui-satlinker-laser");
        int resonatorFreq = tune(c, XENIUM_RESONATOR, "hbm:sat_coord", "12-gui-satlinker-resonator");
        int minerFreq = tune(c, MINER_ASTRO, "hbm:sat_chip", "13-gui-satlinker-miner");
        Expect.that(laserFreq != resonatorFreq && resonatorFreq != minerFreq,
                "three random frequencies: " + laserFreq + " " + resonatorFreq + " " + minerFreq);
        // the relay chip for the radar takes the resonator's frequency too
        copyFreq(c, XENIUM_RESONATOR, resonatorFreq, "hbm:sat_relay");

        // --- the Soyuz: placed, loaded by clicks, fuelled, launched -------------------------------------------
        c.cmd("clear");
        c.cmd("give @s hbm:soyuz_launcher 1");
        c.waitTicks(2);
        c.hotbar(1);
        // (0, -4) from the ground block is clear of the corner legs, the towers and (above head) the apron
        c.teleport(SX + 0.5, Y + 1, SZ - 3.5);
        c.waitTicks(3);
        c.lookAt(SX + 0.5, Y + 0.98, SZ + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(10);
        int[] launcher = findCore(c, SX, Y + 1 + 4, SZ, "soyuz_launcher", 2);
        Expect.that(launcher != null, "the Soyuz launch complex formed a core above " + SX + " " + SZ);
        worldShot(c, "20-soyuz-complex", SX + 16.5, Y + 12, SZ - 12.5, SX + 0.5, Y + 4, SZ + 0.5);

        c.cmd("clear");
        c.cmd("give @s " + satellite(c, PRECISION_LASER, laserFreq) + " 1");
        c.cmd("give @s hbm:missile_soyuz_0 1");
        c.waitTicks(3);
        openLauncher(c, launcher);
        clickFromInventory(c, "hbm:missile_soyuz_0", 0);
        clickFromInventory(c, satId(PRECISION_LASER), 2);
        c.waitTicks(3);
        close(c);
        c.cmd("data merge block " + launcher[0] + " " + launcher[1] + " " + launcher[2]
                + " {power:1000000L,fuel:128000,oxidizer:128000}");
        c.waitTicks(10);
        openLauncher(c, launcher);
        c.waitTicks(10);
        c.screenshot("21-gui-soyuz-loaded");
        JsonObject gui = c.screen();
        c.mouseMove(gui.get("left").getAsInt() + 106, gui.get("top").getAsInt() + 61);
        c.waitTicks(3);
        c.screenshot("22-gui-soyuz-satellite-mode-tooltip");
        c.click(gui.get("left").getAsInt() + 97, gui.get("top").getAsInt() + 106); // launch button (88,97)+9
        c.waitTicks(20);
        c.screenshot("23-gui-soyuz-counting");
        String counting = c.blockEntity(launcher[0], launcher[1], launcher[2]).toString();
        close(c);
        worldShot(c, "24-soyuz-on-pad", SX + 14.5, Y + 8, SZ - 14.5, SX + 0.5, Y + 10, SZ + 0.5);
        // thirty seconds of countdown on the SERVER's clock, which may lag the client's under load
        c.waitTicks(500);
        // poll the pad: liftoff empties the rocket slot (the server's thirty seconds may run slow under load)
        JsonObject rocket = null;
        boolean lifted = false;
        String pad = counting;
        for (int i = 0; i < 200 && rocket == null; i++) {
            c.waitTicks(10);
            rocket = entity(c, "hbm:entity_soyuz", 900);
            pad = c.blockEntity(launcher[0], launcher[1], launcher[2]).toString();
            lifted = !pad.contains("missile_soyuz");
            if (lifted && rocket == null) rocket = entity(c, "hbm:entity_soyuz", 900);
            if (lifted) break;
        }
        Expect.that(lifted, "the Soyuz left the pad: " + pad);
        worldShot(c, "25-soyuz-liftoff", SX + 30.5, Y + 20, SZ - 30.5, SX + 0.5, Y + 40, SZ + 0.5);
        for (int i = 0; i < 40 && entity(c, "hbm:entity_soyuz", 900) != null; i++) c.waitTicks(20);
        Expect.that(entity(c, "hbm:entity_soyuz", 900) == null, "the Soyuz separated its payload above y=600");

        // --- /ntmsatellites orbit + list ----------------------------------------------------------------------
        c.cmd("clear");
        c.cmd("give @s " + satellite(c, XENIUM_RESONATOR, resonatorFreq) + " 1");
        c.waitTicks(2);
        c.hotbar(1);
        c.waitTicks(4);
        c.cmd("ntmsatellites orbit");
        c.cmd("clear");
        c.cmd("give @s " + satellite(c, MINER_ASTRO, minerFreq) + " 1");
        c.waitTicks(2);
        c.hotbar(1);
        c.waitTicks(4);
        c.cmd("ntmsatellites orbit");
        c.waitTicks(4);
        c.markLog();
        c.cmd("ntmsatellites list");
        c.waitTicks(10);
        List<String> list = c.logSinceMark(" - Satellite");
        String listed = String.join("\n", list);
        Expect.that(listed.contains(laserFreq + " - SatellitePrecisionLaser"), "the Soyuz put the laser in orbit: " + listed);
        Expect.that(listed.contains(resonatorFreq + " - SatelliteResonator"), "orbit put the resonator up: " + listed);
        Expect.that(listed.contains(minerFreq + " - SatelliteMiner"), "orbit put the miner up: " + listed);
        c.key("key.chat");
        c.waitTicks(10);
        c.screenshot("30-chat-ntmsatellites-list");
        c.key("escape");
        c.waitTicks(5);

        // --- laser designator: the precision laser --------------------------------------------------------------
        c.cmd("clear");
        c.cmd("give @s " + chip(c, "hbm:sat_designator", laserFreq) + " 1");
        c.waitTicks(2);
        c.hotbar(1);
        c.teleport(TX + 0.5, Y + 1, TZ - 10.5);
        c.waitTicks(3);
        c.lookAt(TX + 0.5, Y + 1.0, TZ + 0.5);
        c.waitTicks(2);
        c.use(1);
        c.waitTicks(1);
        c.screenshot("40-orbital-laser-beam");
        c.waitTicks(10);
        JsonObject crater = c.block(TX, Y, TZ);
        Expect.that(crater.toString().contains("minecraft:air"), "the orbital laser blew a crater into the platform: " + crater);
        worldShot(c, "41-orbital-laser-crater", TX + 6.5, Y + 6, TZ - 6.5, TX + 0.5, Y, TZ + 0.5);

        // --- satellite designator screen: the resonator teleports ---------------------------------------------
        c.cmd("clear");
        c.cmd("give @s " + chip(c, "hbm:sat_coord", resonatorFreq) + " 1");
        c.waitTicks(2);
        c.hotbar(1);
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(10); // the held-item tick sets "connected"
        c.use(1);
        c.waitScreen("GUIScreenSatCoord", 5000);
        c.waitTicks(5);
        int[] co = coordOrigin(c);
        int cl = co[0], ct = co[1];
        c.click(cl + 70, ct + 24);
        c.type(String.valueOf(X + 20));
        c.click(cl + 70, ct + 59);
        c.type(String.valueOf(Y + 1));
        c.click(cl + 70, ct + 95);
        c.type(String.valueOf(Z - 20));
        c.waitTicks(3);
        c.screenshot("50-gui-sat-coord-connected");
        c.click(cl + 142, ct + 61);
        c.waitTicks(10);
        JsonObject moved = c.player();
        Expect.that(Math.abs(moved.get("x").getAsDouble() - (X + 20.5)) < 0.6
                        && Math.abs(moved.get("z").getAsDouble() - (Z - 19.5)) < 0.6,
                "the resonator teleported the player to the typed coordinates: " + moved);

        // --- the radar's sat_relay link slot ------------------------------------------------------------------
        c.cmd("clear");
        place(c, "hbm:machine_radar", RX, Y, RZ);
        Expect.block(c, RX, Y + 1, RZ, "hbm:machine_radar");
        c.cmd("clear");
        c.cmd("give @s " + chip(c, "hbm:sat_relay", resonatorFreq) + " 1");
        c.waitTicks(2);
        openAt(c, RX + 2.5, RZ + 0.5, RX + 0.5, RZ + 0.5, "GUIMachineRadarNT");
        clickRadarToggle(c, 158);
        c.waitScreen("GUIMachineRadarNTSlots", 5000);
        c.waitTicks(3);
        clickFromInventory(c, "hbm:sat_relay", 0);
        c.waitTicks(3);
        c.screenshot("60-gui-radar-sat-relay");
        close(c);
        openAt(c, RX + 2.5, RZ + 0.5, RX + 0.5, RZ + 0.5, "GUIMachineRadarNT");
        JsonObject status = c.status();
        int w = status.get("gui_width").getAsInt(), h = status.get("gui_height").getAsInt();
        int left = (w - 216) / 2, top = (h - 234) / 2;
        int mx = left + 108 + 10, mz = top + 117 - 10;
        c.mouseMove(mx, mz);
        c.waitTicks(3);
        c.type("1");
        c.waitTicks(10);
        JsonObject relayed = c.player();
        double ex = (int) ((mx - left - 108) * 2001 / 192D + RX), ez = (int) ((mz - top - 117) * 2001 / 192D + RZ);
        Expect.that(Math.abs(relayed.get("x").getAsDouble() - (ex + 0.5)) < 1.5
                        && Math.abs(relayed.get("z").getAsDouble() - (ez + 0.5)) < 1.5,
                "the relay handed the map click to the resonator (" + ex + ", " + ez + "): " + relayed);
        c.key("escape");
        c.waitTicks(5);
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(5);

        // --- cargo landing pad: a miner rocket lands and unloads ----------------------------------------------
        c.cmd("clear");
        place(c, "hbm:sat_dock", DX, Y, DZ);
        int[] dock = findCore(c, DX, Y + 1, DZ, "sat_dock", 1);
        Expect.that(dock != null, "the cargo pad formed a core");
        c.cmd("give @s " + chip(c, "hbm:sat_chip", minerFreq) + " 1");
        c.waitTicks(2);
        openAt(c, dock[0] + 3.5, dock[2] + 0.5, dock[0] + 0.5, dock[2] + 0.5, "Cargo Landing Pad");
        // 1.7 fills a miner after 15 minutes of ticks; the hook leaves one tick to go (GameTest rc_space_miner_timing covers the 18,000)
        JsonObject prime = new JsonObject();
        prime.addProperty("miner", minerFreq);
        Expect.that(c.query("hbm_gapsc", prime).get("miner_primed").getAsBoolean(), "the miner is up and primed");
        clickFromInventory(c, "hbm:sat_chip", 15);
        c.waitTicks(5);
        c.screenshot("70-gui-sat-dock-chip");
        JsonObject dockGui = c.screen();
        c.mouseMove(dockGui.get("left").getAsInt() - 1, dockGui.get("top").getAsInt() + 44);
        c.waitTicks(3);
        c.screenshot("71-gui-sat-dock-info");
        close(c);
        JsonObject lander = null;
        for (int i = 0; i < 20 && lander == null; i++) {
            c.waitTicks(5);
            lander = entity(c, "hbm:entity_miner_lander", 400);
        }
        Expect.that(lander != null, "the pad called down a miner rocket");
        c.waitTicks(160);
        worldShot(c, "72-miner-rocket-descending", dock[0] + 10.5, Y + 12, dock[2] + 10.5, dock[0] + 0.5, Y + 30, dock[2] + 0.5);
        boolean cargo = false;
        for (int i = 0; i < 30 && !cargo; i++) {
            c.waitTicks(10);
            String nbt = c.blockEntity(dock[0], dock[1], dock[2]).toString();
            cargo = nbt.contains("hbm:") && !nbt.replaceAll("\\s", "").matches(".*Items:\\[\\].*") && nbt.toLowerCase(java.util.Locale.ROOT).contains("count"); // 1.20.1 spells it "Count"
        }
        worldShot(c, "73-miner-rocket-landed", dock[0] + 5.5, Y + 4, dock[2] + 5.5, dock[0] + 0.5, Y + 2, dock[2] + 0.5);
        String dockNbt = c.blockEntity(dock[0], dock[1], dock[2]).toString();
        Expect.that(cargo, "the landed rocket unloaded cargo into the pad: " + dockNbt);
        openAt(c, dock[0] + 3.5, dock[2] + 0.5, dock[0] + 0.5, dock[2] + 0.5, "Cargo Landing Pad");
        c.waitTicks(5);
        c.screenshot("74-gui-sat-dock-cargo");
        close(c);

        // --- ground station: tuned by a chip, connected ------------------------------------------------------
        c.cmd("clear");
        place(c, "hbm:machine_satlink", GX, Y, GZ);
        int[] link = findCore(c, GX, Y + 1, GZ, "machine_satlink", 1);
        Expect.that(link != null, "the ground station formed a core");
        c.cmd("clear"); // creative placement kept the station item in slot 1
        c.cmd("give @s " + chip(c, "hbm:sat_chip", minerFreq) + " 1");
        c.waitTicks(2);
        c.hotbar(1);
        c.teleport(link[0] + 3.5, Y + 1, link[2] + 0.5);
        c.waitTicks(3);
        c.lookAt(link[0] + 0.5, Y + 1.5, link[2] + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(40);
        String linkNbt = c.blockEntity(link[0], link[1], link[2]).toString().replaceAll("\\s", "");
        Expect.that(linkNbt.contains("freq:" + minerFreq), "the chip retuned the ground station: " + linkNbt);
        c.hotbar(9);
        c.waitTicks(80);
        c.screenshot("80-satlink-overlay-connected");
        worldShot(c, "81-satlink-dish", link[0] + 6.5, Y + 5, link[2] + 6.5, link[0] + 0.5, Y + 4, link[2] + 0.5);

        // --- the death ray, where the world is old enough for it to have charged -----------------------------
        c.markLog();
        c.cmd("time query gametime");
        c.waitTicks(5);
        List<String> time = c.logSinceMark("The time is");
        long gametime = -1;
        if (!time.isEmpty()) {
            Matcher m = Pattern.compile("The time is (\\d+)").matcher(time.get(time.size() - 1));
            if (m.find()) gametime = Long.parseLong(m.group(1));
        }
        if (gametime > 6100) {
            c.cmd("ntmsatellites descend 424242"); // a death ray left by an earlier run would still be charging
            c.cmd("clear");
            c.cmd("give @s " + satellite(c, DEATH_RAY, 424242) + " 1");
            c.waitTicks(2);
            c.hotbar(1);
            c.waitTicks(4);
            c.cmd("ntmsatellites orbit");
            c.cmd("clear");
            c.cmd("give @s " + chip(c, "hbm:sat_coord", 424242) + " 1");
            c.waitTicks(10);
            c.hotbar(1);
            c.waitTicks(4);
            c.use(1);
            c.waitScreen("GUIScreenSatCoord", 5000);
            int[] ro = coordOrigin(c);
            int rl = ro[0], rt = ro[1];
            c.click(rl + 70, rt + 24);
            c.type("200");
            c.click(rl + 70, rt + 95);
            c.type("200");
            c.click(rl + 142, rt + 61);
            c.waitTicks(5);
            c.cmd("gamemode spectator");
            c.teleport(180.5, Y + 30, 180.5);
            c.waitTicks(20);
            JsonObject blast = entity(c, "hbm:entity_laser_blast", 300);
            Expect.that(blast != null, "the death ray dropped its blast at 200 / 200");
            worldShot(c, "90-death-ray-beam", 170.5, Y + 30, 170.5, 200.5, blast.get("y").getAsDouble(), 200.5);
            c.waitTicks(80);
            worldShot(c, "91-death-ray-after", 150.5, Y + 40, 150.5, 200.5, Y - 20, 200.5);
            c.cmd("ntmsatellites descend 424242");
        } else {
            System.out.println("[FinalSatellites] world game time " + gametime + " < 6100: the death ray is still charging (1.7 CHARGE_TIME), skipped");
        }

        clearOrbit(c, laserFreq, resonatorFreq, minerFreq);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture|Missing model|Unable to load model");
    }

    // --- helpers -------------------------------------------------------------------------------------------

    /** GUIScreenSatCoord is a plain screen (176x126, centred): no container left/top in the query. */
    private static int[] coordOrigin(Client c) {
        JsonObject status = c.status();
        int w = status.get("gui_width").getAsInt(), h = status.get("gui_height").getAsInt();
        return new int[]{(w - 176) / 2, (h - 126) / 2};
    }

    private static boolean legacy(Client c) {
        return c.node().startsWith("1.20.");
    }

    private static final String[] SAT_NAMES = {"spy", "scanner", "radar", "miner_astro", "miner_lunar",
            "precision_laser", "death_ray", "xenium_resonator", "relay", "detector", "ray_scan"};

    /** The item id of 1.7 satellite metadata {@code variant}. */
    private static String satId(int variant) {
        return "hbm:satellite_" + SAT_NAMES[variant];
    }

    /** A satellite variant, optionally with a frequency already on it. */
    private static String satellite(Client c, int variant, int freq) {
        // one item per satellite since History §19es-itemvariant (ntm-next ids)
        String id = "hbm:satellite_" + new String[] {"spy", "scanner", "radar", "miner_astro", "miner_lunar", "precision_laser", "death_ray", "xenium_resonator", "relay", "detector", "ray_scan"}[variant];
        if (legacy(c)) return id + (freq != 0 ? "{\"hbm:persistent_data\":{freq:" + freq + "}}" : "");
        return id + (freq != 0 ? "[hbm:persistent_data={freq:" + freq + "}]" : "");
    }

    private static String chip(Client c, String id, int freq) {
        if (legacy(c)) return id + "{\"hbm:persistent_data\":{freq:" + freq + "}}";
        return id + "[hbm:persistent_data={freq:" + freq + "}]";
    }

    private static void inventory(Client c) {
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.waitTicks(2);
        for (String id : IDS) c.cmd("give @s " + id + " 1");
        for (int v = 0; v <= 10; v++) c.cmd("give @s " + satellite(c, v, 0) + " 1");
        c.waitTicks(4);
        JsonObject inv = c.inventory();
        for (String id : IDS) Expect.that(inv.toString().contains("\"" + id + "\""), "inventory holds " + id);
        for (int v = 0; v <= 10; v++) Expect.that(inv.toString().contains("\"" + satId(v) + "\""), "inventory holds " + satId(v));
        c.key("key.inventory");
        c.waitScreen("", 5000);
        c.waitTicks(10);
        c.screenshot("01-inventory");
        // hover the gerald, the precision laser satellite and the laser designator for their tooltips
        hoverTooltip(c, "hbm:sat_gerald", "02-tooltip-gerald");
        hoverTooltip(c, "hbm:sat_designator", "03-tooltip-designator");
        close(c);
        c.cmd("clear");
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }

    private static void hoverTooltip(Client c, String id, String name) {
        JsonObject slot = findSlot(c, id);
        if (slot == null) return;
        c.mouseMove(slot.get("x").getAsInt() + 8, slot.get("y").getAsInt() + 8);
        c.waitTicks(3);
        c.screenshot(name);
    }

    /**
     * Puts the satellite variant in the ID manager's randomiser (slot 2), reads the random frequency back, then
     * moves it into the copy source (slot 0) and hands {@code chipId} the same frequency in slot 1. Returns it.
     */
    private static int tune(Client c, int variant, String chipId, String shot) {
        c.cmd("clear");
        c.cmd("give @s " + satellite(c, variant, 0) + " 1");
        c.cmd("give @s " + chipId + " 1");
        c.waitTicks(3);
        openAt(c, LX + 2.5, LZ + 0.5, LX + 0.5, LZ + 0.5, "SatLink Device");
        clickFromInventory(c, satId(variant), 2);
        c.waitTicks(5);
        int freq = freqIn(c.blockEntity(LX, Y + 1, LZ).toString(), 2);
        Expect.that(freq >= 0 && freq < 10_000_000, "the randomiser gave a frequency below 10,000,000: " + freq);
        // take it out of the randomiser (it re-rolls every tick) and into the copy source
        c.clickSlot(2);
        c.waitTicks(1);
        c.clickSlot(0);
        c.waitTicks(2);
        int held = freqIn(c.blockEntity(LX, Y + 1, LZ).toString(), 0);
        clickFromInventory(c, chipId, 1);
        c.waitTicks(5);
        String nbt = c.blockEntity(LX, Y + 1, LZ).toString();
        Expect.that(freqIn(nbt, 1) == held, "slot 1 took slot 0's frequency " + held + ": " + nbt);
        c.screenshot(shot);
        JsonObject gui = c.screen();
        c.mouseMove(gui.get("left").getAsInt() + 20, gui.get("top").getAsInt() + 36);
        c.waitTicks(3);
        c.screenshot(shot + "-info");
        c.clickSlot(0);
        c.waitTicks(1);
        c.clickSlot(emptyPlayerSlot(c));
        c.waitTicks(1);
        c.clickSlot(1);
        c.waitTicks(1);
        c.clickSlot(emptyPlayerSlot(c));
        c.waitTicks(2);
        close(c);
        return held;
    }

    /** Hands a second chip the frequency with the ID manager's copy slot. */
    private static void copyFreq(Client c, int variant, int freq, String chipId) {
        c.cmd("clear");
        c.cmd("give @s " + satellite(c, variant, freq) + " 1");
        c.cmd("give @s " + chipId + " 1");
        c.waitTicks(3);
        openAt(c, LX + 2.5, LZ + 0.5, LX + 0.5, LZ + 0.5, "SatLink Device");
        clickFromInventory(c, satId(variant), 0);
        clickFromInventory(c, chipId, 1);
        c.waitTicks(5);
        Expect.that(freqIn(c.blockEntity(LX, Y + 1, LZ).toString(), 1) == freq, "the copy slot took " + freq);
        c.clickSlot(0);
        c.waitTicks(1);
        c.clickSlot(emptyPlayerSlot(c));
        c.waitTicks(1);
        c.clickSlot(1);
        c.waitTicks(1);
        c.clickSlot(emptyPlayerSlot(c));
        c.waitTicks(2);
        close(c);
    }

    /** The frequency on the stack in machine slot {@code slot} of a block entity's SNBT; -1 where none. */
    private static int freqIn(String snbt, int slot) {
        String flat = snbt.replaceAll("\\s", "").replace("\\\"", "\"");
        Matcher m = Pattern.compile("\\{[^{}]*?[Ss]lot\"?:" + slot + "b?[,}].*?freq\"?:(-?\\d+)").matcher(flat);
        if (m.find()) return Integer.parseInt(m.group(1));
        return -1;
    }

    private static int emptyPlayerSlot(Client c) {
        JsonObject screen = c.screen();
        for (JsonElement e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && (!s.has("id") || s.get("id").getAsString().contains("air"))) {
                return s.get("index").getAsInt();
            }
        }
        throw new IllegalStateException("no empty player slot");
    }

    private static void clearOrbit(Client c, int... freqs) {
        for (int f : freqs) c.cmd("ntmsatellites descend " + f);
        c.waitTicks(2);
    }

    private static void openLauncher(Client c, int[] core) {
        // the apron is the core's course and the one under it; its north rim is 6 out from the core
        c.teleport(core[0] + 0.5, Y + 1, core[2] - 9.5);
        c.waitTicks(3);
        c.hotbar(9);
        c.lookAt(core[0] + 0.5, core[1] - 0.5, core[2] - 5.95);
        c.waitTicks(1);
        c.use(2);
        c.waitScreen("Soyuz", 5000);
        Expect.screenStaysOpen(c, "Soyuz", 10);
        c.waitTicks(5);
    }

    private static void place(Client c, String item, int x, int y, int z) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(x + 4.5, y + 1, z + 0.5);
        c.waitTicks(3);
        c.lookAt(x + 0.9, y + 0.98, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
    }

    /** The core of a multiblock near {@code (x, y, z)}: the block entity whose SNBT names {@code id}. */
    private static int[] findCore(Client c, int x, int y, int z, String id, int r) {
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    JsonObject be = c.blockEntity(x + dx, y + dy, z + dz);
                    if (be.has("block_entity") && !be.get("block_entity").isJsonNull() && be.toString().contains(id)) {
                        return new int[]{x + dx, y + dy, z + dz};
                    }
                }
            }
        }
        return null;
    }

    /** The radar map's left-edge toggles: 8x8 at {@code (guiLeft - 10, guiTop + top)}. */
    private static void clickRadarToggle(Client c, int top) {
        JsonObject status = c.status();
        int w = status.get("gui_width").getAsInt(), h = status.get("gui_height").getAsInt();
        int left = (w - 216) / 2, topPos = (h - 234) / 2;
        c.click(left - 10 + 4, topPos + top + 4);
        c.waitTicks(4);
    }

    private static void openAt(Client c, double sx, double sz, double lx, double lz, String title) {
        c.teleport(sx, Y + 1, sz);
        c.waitTicks(3);
        c.hotbar(9);
        c.waitTicks(1);
        c.lookAt(lx, Y + 1.5, lz);
        c.waitTicks(1);
        c.use(2);
        c.waitScreen(title, 5000);
        Expect.screenStaysOpen(c, title, 10);
        c.waitTicks(5);
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitTicks(5);
    }

    private static void clickFromInventory(Client c, String id, int machineSlot) {
        JsonObject slot = findSlot(c, id);
        Expect.that(slot != null, "inventory slot holding " + id);
        c.clickSlot(slot.get("index").getAsInt());
        c.waitTicks(1);
        c.clickSlot(machineSlot);
        c.waitTicks(1);
        JsonObject carried = c.screen().getAsJsonObject("carried");
        if (carried != null && carried.has("id") && !carried.get("id").getAsString().contains("air")) {
            c.clickSlot(slot.get("index").getAsInt());
            c.waitTicks(1);
        }
    }

    private static JsonObject findSlot(Client c, String id) {
        JsonObject screen = c.screen();
        if (!screen.has("slots")) return null;
        for (JsonElement e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && id.equals(s.get("id").getAsString())) return s;
        }
        return null;
    }

    private static JsonObject entity(Client c, String type, double radius) {
        for (JsonElement e : c.entities(radius)) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals(type)) return o;
        }
        return null;
    }

    private static void worldShot(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        c.hotbar(9);
        c.waitTicks(1);
        c.cmd("gamemode spectator");
        c.waitTicks(2);
        c.teleport(x, y, z);
        c.waitTicks(8);
        c.lookAt(tx, ty, tz);
        c.waitTicks(8);
        c.screenshot(name);
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }
}
