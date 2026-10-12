import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Playtest 2026-10-12: the 1.7 Soyuz launch setup and sequence, built and flown by hand.
 * <ol>
 *   <li>The launch complex is placed from its block item (it assembles itself). Two stone ledges at apron height
 *       carry the supply, every machine on them placed by a real right-click: on the east ledge a kerosene and an
 *       oxygen line, each a steel barrel (identifier + infinite barrel clicked into its slots, mode clicked to
 *       output) and two ducts typed with a fluid identifier, ending at the apron's rim ports (1.7 {@code getConPos}:
 *       seven out from the core, core level and one below); on the west ledge a battery socket holding a creative
 *       battery (mode clicked to output) and red cable to the rim.</li>
 *   <li>Positive signals: both 128,000 mB tanks fill through the ducts, power reaches 1,000,000 HE through the
 *       cable.</li>
 *   <li>SATELLITE mode: rocket + precision laser satellite clicked in, launch button clicked; the 30 s countdown
 *       runs, the rocket lifts off and rises, both tanks are drained by 128,000 and power by 750,000, and the
 *       satellite is listed by {@code /ntmsatellites list}.</li>
 *   <li>CARGO mode: tanks/power refill, mode switch clicked, a designator aimed at a block 20/20 away, rocket
 *       (skin 2) and cargo clicked in, launched; fuel drawn is 1.7's {@code 5000 + dist}; the capsule spawns at
 *       y=600 over the target, falls and turns into a {@code soyuz_capsule} block holding the cargo and the rocket.</li>
 * </ol>
 */
public final class Pt1012Soyuz implements Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;       // the ground block the complex grows from
    private static final int CY = G + 4;      // the launcher core (SoyuzLauncher.HEIGHT)
    private static final int LEDGE = CY - 2;  // ledges' top course; things on them sit at CY - 1
    private static final int TX = 20, TZ = 20; // cargo target
    private static final int FREQ = 24601;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 30);
        // an earlier run's complex reaches above the platform's cleared courses
        c.cmd("fill -15 " + G + " -15 15 " + (Y + 30) + " 15 minecraft:air");
        // and drops what that complex still held (the cargo run leaves its designator in slot 1, as in 1.7)
        c.cmd("kill @e[type=item]");
        c.waitTicks(2);
        c.cmd("clear");
        c.cmd("gamerule sendCommandFeedback true");
        c.cmd("ntmsatellites descend " + FREQ);
        c.markLog();

        // --- the complex ------------------------------------------------------------------------------------------
        Fixtures.giveOnly(c, "hbm:soyuz_launcher", 1);
        c.teleport(0.5, G, -3.5);
        c.waitTicks(3);
        c.lookAt(0.5, Y + 0.98, 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(10);
        Expect.that(isBe(c, 0, CY, 0, "soyuz_launcher"), "the launch complex formed its core at 0 " + CY + " 0: "
                + c.blockEntity(0, CY, 0));
        // the ledges: west for power, east for the propellant (the service towers stand north and south)
        for (int[] probe : new int[][]{{7, -2}, {7, 2}, {-7, -3}, {-7, 3}}) {
            String b = c.block(probe[0], CY - 1, probe[1]).toString();
            Expect.that(b.contains("minecraft:air"), "rim port cell " + probe[0] + "," + probe[1] + " is free: " + b);
        }
        c.cmd("fill 7 " + LEDGE + " -6 13 " + LEDGE + " 6 minecraft:stone");
        c.cmd("fill -13 " + LEDGE + " -6 -7 " + LEDGE + " 6 minecraft:stone");
        c.waitTicks(3);

        // --- propellant lines (east): duct, duct, barrel -------------------------------------------------------------
        line(c, -2, "hbm:kerosene");
        line(c, 2, "hbm:oxygen");
        fillBarrel(c, -2, "hbm:kerosene");
        fillBarrel(c, 2, "hbm:oxygen");

        // --- power (west): cable, cable, battery socket with a creative battery ----------------------------------------
        int pz = -3;
        place(c, "hbm:red_cable", -7, pz);
        place(c, "hbm:red_cable", -8, pz);
        place(c, "hbm:machine_battery_socket", -11, pz);
        int[] socket = findBe(c, -11, CY - 1, pz, "battery_socket", 2);
        Expect.that(socket != null, "the battery socket was placed near -11 " + pz);
        // close any gap between the cable and the socket's footprint
        for (int x = -9; x >= -12; x--) {
            String b = c.block(x, CY - 1, pz).toString();
            if (b.contains("battery_socket")) break;
            if (b.contains("minecraft:air")) place(c, "hbm:red_cable", x, pz);
        }
        Fixtures.giveOnly(c, "hbm:battery_creative", 1);
        openAt(c, -9.5, pz + 2.5, socket, "");
        clickFromInventory(c, "hbm:battery_creative", 0);
        JsonObject sg = c.screen();
        int sl = sg.get("left").getAsInt(), st = sg.get("top").getAsInt();
        c.click(sl + 106 + 9, st + 16 + 9); // redLow: input -> buffer
        c.waitTicks(3);
        c.click(sl + 106 + 9, st + 16 + 9); // buffer -> output
        c.waitTicks(5);
        c.screenshot("10-gui-socket-output");
        close(c);
        String socketNbt = nbt(c, socket[0], socket[1], socket[2]);
        Expect.that(socketNbt.contains("battery_creative") && socketNbt.contains("redLow:2s"),
                "the socket holds the creative battery and outputs unpowered: " + socketNbt);

        // --- supply arrives -------------------------------------------------------------------------------------
        c.waitTicks(100);
        String pad = nbt(c, 0, CY, 0);
        System.out.println("[Pt1012Soyuz] supplied launcher: " + pad);
        long fuel = tank(pad, "fuel"), oxy = tank(pad, "oxidizer"), power = longOf(pad, "power");
        Expect.that(fuel == 128_000, "kerosene filled through the duct to 128,000 mB: " + fuel);
        Expect.that(oxy == 128_000, "oxygen filled through the duct to 128,000 mB: " + oxy);
        Expect.that(power == 1_000_000L, "power filled through the cable to 1,000,000 HE: " + power);
        worldShot(c, "11-supply-east", 20.5, CY + 6, 0.5, 8.5, CY - 1, 0.5);
        worldShot(c, "12-supply-west", -20.5, CY + 6, -3.5, -8.5, CY - 1, -3.5);

        // --- SATELLITE launch ----------------------------------------------------------------------------------------
        // cut the lines (barrels to "none", one cable broken) so the drain at liftoff can be read exactly
        isolate(c, true);
        c.cmd("clear");
        c.cmd("give @s hbm:missile_soyuz_0 1");
        c.cmd("give @s " + satellite(c) + " 1");
        c.waitTicks(3);
        openLauncher(c);
        clickFromInventory(c, "hbm:missile_soyuz_0", 0);
        clickFromInventory(c, "hbm:satellite_precision_laser", 2);
        c.waitTicks(10);
        c.screenshot("20-gui-satellite-loaded");
        JsonObject gui = c.screen();
        int gl = gui.get("left").getAsInt(), gt = gui.get("top").getAsInt();
        c.click(gl + 97, gt + 106); // launch (88,97)
        long clicked = gameTime(c);
        c.waitTicks(40);
        c.screenshot("21-gui-satellite-counting");
        close(c);
        worldShot(c, "22-rocket-on-pad", 22.5, CY + 10, -22.5, 0.5, CY + 14, 0.5);
        Expect.that(nbt(c, 0, CY, 0).contains("missile_soyuz_0"), "the rocket still sits on the pad during the countdown");
        JsonObject rocket = awaitRocket(c);
        long countdown = gameTime(c) - clicked;
        System.out.printf("[Pt1012Soyuz] satellite liftoff %d game ticks after the launch click (1.7 maxCount 600)%n", countdown);
        Expect.that(countdown >= 595 && countdown <= 640, "the countdown ran 600 ticks: " + countdown);
        String after = nbt(c, 0, CY, 0);
        System.out.println("[Pt1012Soyuz] after liftoff: " + after);
        Expect.that(!after.contains("missile_soyuz") && !after.contains("satellite_precision_laser"),
                "liftoff took the rocket and the satellite: " + after);
        // 1.7 liftOff: both tanks -128,000 (oxygen too, although canLaunch never asks for it), power -750,000
        Expect.that(tank(after, "fuel") == 0 && tank(after, "oxidizer") == 0 && longOf(after, "power") == 250_000L,
                "liftoff drew 128,000 kerosene, 128,000 oxygen and 750,000 HE: " + after);
        observeRise(c, "23-satellite-liftoff");
        waitGone(c, "hbm:entity_soyuz");
        c.markLog();
        c.cmd("ntmsatellites list");
        c.waitTicks(10);
        String listed = String.join("\n", c.logSinceMark(" - Satellite"));
        Expect.that(listed.contains(FREQ + " - SatellitePrecisionLaser"), "the satellite reached orbit: " + listed);

        // --- CARGO launch ----------------------------------------------------------------------------------------------
        isolate(c, false);
        c.waitTicks(100);
        String refilled = nbt(c, 0, CY, 0);
        Expect.that(tank(refilled, "fuel") == 128_000 && tank(refilled, "oxidizer") == 128_000
                && longOf(refilled, "power") == 1_000_000L, "the lines refilled the pad: " + refilled);
        isolate(c, true);
        // the designator, aimed at the target block on the platform
        Fixtures.giveOnly(c, "hbm:designator", 1);
        c.teleport(TX + 0.5, G, TZ - 2.5);
        c.waitTicks(3);
        c.lookAt(TX + 0.5, Y + 0.98, TZ + 0.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(3);
        c.cmd("give @s hbm:missile_soyuz_2 1");
        c.cmd("give @s minecraft:iron_ingot 64");
        c.cmd("give @s minecraft:diamond 7");
        c.waitTicks(3);
        openLauncher(c);
        gui = c.screen();
        gl = gui.get("left").getAsInt();
        gt = gui.get("top").getAsInt();
        c.click(gl + 79 + 9, gt + 52 + 9); // CARGO
        c.waitTicks(5);
        Expect.that(nbt(c, 0, CY, 0).contains("mode:1b"), "the mode switch set CARGO: " + nbt(c, 0, CY, 0));
        clickFromInventory(c, "hbm:missile_soyuz_2", 0);
        clickFromInventory(c, "hbm:designator", 1);
        clickFromInventory(c, "minecraft:iron_ingot", 9);
        clickFromInventory(c, "minecraft:diamond", 14);
        c.waitTicks(10);
        c.screenshot("30-gui-cargo-loaded");
        c.mouseMove(gl + 88, gt + 61);
        c.waitTicks(3);
        c.screenshot("31-gui-cargo-tooltip");
        c.click(gl + 97, gt + 106);
        clicked = gameTime(c);
        c.waitTicks(40);
        c.screenshot("32-gui-cargo-counting");
        close(c);
        awaitRocket(c);
        countdown = gameTime(c) - clicked;
        System.out.printf("[Pt1012Soyuz] cargo liftoff %d game ticks after the launch click%n", countdown);
        Expect.that(countdown >= 595 && countdown <= 640, "the countdown ran 600 ticks: " + countdown);
        String cargoAfter = nbt(c, 0, CY, 0);
        System.out.println("[Pt1012Soyuz] after cargo liftoff: " + cargoAfter);
        int dist = (int) Math.sqrt(TX * TX + TZ * TZ);
        long drawn = 128_000 - tank(cargoAfter, "fuel");
        System.out.println("[Pt1012Soyuz] cargo fuel drawn " + drawn + " (1.7: 5000 + " + dist + " = " + (5000 + dist) + ")");
        Expect.that(drawn == 5000 + dist && 128_000 - tank(cargoAfter, "oxidizer") == 5000 + dist
                && longOf(cargoAfter, "power") == 250_000L, "cargo liftoff drew 5000 + dist of each and 750,000 HE: " + cargoAfter);
        Expect.that(!cargoAfter.contains("iron_ingot") && !cargoAfter.contains("minecraft:diamond")
                && !cargoAfter.contains("missile_soyuz"), "liftoff took the rocket and the cargo: " + cargoAfter);
        Expect.that(cargoAfter.contains("hbm:designator"), "the designator stays in its slot (1.7)");
        observeRise(c, "33-cargo-liftoff");
        waitGone(c, "hbm:entity_soyuz");

        // the capsule: spawned at y=600 over the target, falling at 0.2 blocks per tick
        c.cmd("gamemode spectator");
        c.teleport(TX + 12.5, Y + 20, TZ + 12.5);
        c.waitTicks(20);
        JsonObject capsule = null;
        double lastY = 9999;
        int seen = 0;
        boolean shotMid = false;
        for (int i = 0; i < 400; i++) {
            c.waitTicks(10);
            JsonObject e = entity(c, "hbm:entity_soyuz_capsule", 700);
            if (e != null) {
                capsule = e;
                double y = e.get("y").getAsDouble();
                if (seen++ % 20 == 0) System.out.printf("[Pt1012Soyuz] capsule at %.2f %.2f %.2f%n",
                        e.get("x").getAsDouble(), y, e.get("z").getAsDouble());
                Expect.that(y <= lastY + 0.01, "the capsule only falls: " + lastY + " -> " + y);
                lastY = y;
                if (!shotMid && y < Y + 30) {
                    c.lookAt(TX + 0.5, y, TZ + 0.5);
                    c.waitTicks(2);
                    c.screenshot("40-capsule-falling");
                    shotMid = true;
                }
            } else if (capsule != null) {
                break;
            }
            if (c.block(TX, G, TZ).toString().contains("soyuz_capsule")) break;
        }
        Expect.that(capsule != null, "the capsule entity appeared over the target");
        Expect.that(Math.abs(capsule.get("x").getAsDouble() - (TX + 0.5)) < 0.01
                && Math.abs(capsule.get("z").getAsDouble() - (TZ + 0.5)) < 0.01, "the capsule falls on the target column: " + capsule);
        for (int i = 0; i < 40 && !c.block(TX, G, TZ).toString().contains("soyuz_capsule"); i++) c.waitTicks(10);
        Expect.block(c, TX, G, TZ, "hbm:soyuz_capsule");
        String landed = nbt(c, TX, G, TZ);
        System.out.println("[Pt1012Soyuz] landed capsule: " + landed);
        Expect.that(landed.contains("minecraft:iron_ingot") && landed.contains("minecraft:diamond")
                && landed.contains("missile_soyuz_2"), "the capsule holds the cargo and the skin-2 rocket: " + landed);
        c.cmd("gamemode creative");
        c.waitTicks(2);
        worldShot(c, "41-capsule-landed", TX + 4.5, G + 2, TZ - 3.5, TX + 0.5, G + 0.5, TZ + 0.5);
        c.cmd("clear");
        c.teleport(TX + 2.5, G, TZ + 0.5);
        c.waitTicks(3);
        Fixtures.interact(c, TX, G, TZ);
        c.waitTicks(10);
        c.screenshot("42-gui-capsule");
        close(c);

        // --- after a reload: the battery socket broken by hand and placed again still powers the pad ----------------
        // (TileEntityBatteryBase kept its graph cells only when it minted them; after a reload it had none to drop,
        // so a socket broken then left its footprint in the saved power graph and a new one there carried nothing)
        isolate(c, false);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture|Missing model|Unable to load model");
        String world = c.currentWorld();
        c.leaveWorld(60_000L);
        c.joinWorld(world, 120_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(40);
        c.markLog(); // the title screen between leave and join logs a Realms connection error on dev clients
        c.cmd("gamemode creative");
        c.cmd("clear");
        c.teleport(-11.5, CY - 1, 0.5);
        c.waitTicks(5);
        c.hotbar(9);
        c.lookAt(socket[0] + 0.5, CY - 0.7, socket[2] + 0.5);
        c.waitTicks(1);
        c.attack(2);
        c.waitTicks(10);
        Expect.block(c, socket[0], CY - 1, socket[2], "minecraft:air");
        place(c, "hbm:machine_battery_socket", -11, pz);
        Expect.that(isBe(c, socket[0], CY - 1, socket[2], "battery_socket"), "the socket stands where it stood");
        Fixtures.giveOnly(c, "hbm:battery_creative", 1);
        openAt(c, -9.5, pz + 2.5, socket, "");
        clickFromInventory(c, "hbm:battery_creative", 0);
        sg = c.screen();
        c.click(sg.get("left").getAsInt() + 106 + 9, sg.get("top").getAsInt() + 16 + 9);
        c.waitTicks(3);
        c.click(sg.get("left").getAsInt() + 106 + 9, sg.get("top").getAsInt() + 16 + 9);
        c.waitTicks(3);
        close(c);
        c.cmd("data merge block 0 " + CY + " 0 {power:0L}"); // empty the pad to see it refill
        c.waitTicks(60);
        long repowered = longOf(nbt(c, 0, CY, 0), "power");
        System.out.println("[Pt1012Soyuz] pad power from the re-placed socket after a reload: " + repowered);
        Expect.that(repowered == 1_000_000L, "the re-placed socket powers the pad after a reload: " + repowered);

        c.cmd("ntmsatellites descend " + FREQ);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture|Missing model|Unable to load model");
    }

    // --- building ----------------------------------------------------------------------------------------------------

    /**
     * Cuts ({@code cut}) or restores the supply by hand: each barrel's mode button (output -> none, or none -> input
     * -> buffer -> output) and the cable at x=-8 broken or placed again.
     */
    private static void isolate(Client c, boolean cut) {
        for (int z : new int[]{-2, 2}) {
            c.cmd("clear");
            openAt(c, 9.5, z + 2.5, new int[]{9, CY - 1, z}, "");
            JsonObject g = c.screen();
            int l = g.get("left").getAsInt(), t = g.get("top").getAsInt();
            for (int i = 0; i < (cut ? 1 : 3); i++) {
                c.click(l + 151 + 9, t + 35 + 9);
                c.waitTicks(3);
            }
            close(c);
            String b = nbt(c, 9, CY - 1, z);
            Expect.that(b.contains(cut ? "mode:3s" : "mode:2s"), "barrel " + z + (cut ? " switched off: " : " back to output: ") + b);
        }
        if (cut) {
            c.cmd("clear");
            c.teleport(-7.5, CY - 1, -0.5);
            c.waitTicks(3);
            c.hotbar(9);
            c.lookAt(-7.5, CY - 0.5, -2.5);
            c.waitTicks(1);
            c.attack(2);
            c.waitTicks(5);
            Expect.block(c, -8, CY - 1, -3, "minecraft:air");
        } else {
            place(c, "hbm:red_cable", -8, -3);
        }
    }

    /** The server's game time, from the harness's server-side entities query. */
    private static long gameTime(Client c) {
        JsonObject p = new JsonObject();
        p.addProperty("radius", 1);
        p.addProperty("server", true);
        JsonObject r = c.query("entities", p);
        Expect.that(r.has("game_time"), "server game time: " + r);
        return r.get("game_time").getAsLong();
    }

    /** Two ducts from the rim port at x=7 outward, each typed with an identifier. */
    private static void line(Client c, int z, String fluid) {
        place(c, "hbm:fluid_duct_neo", 7, z);
        place(c, "hbm:fluid_duct_neo", 8, z);
        Fixtures.giveOnly(c, identifier(c, fluid), 1);
        for (int x = 7; x <= 8; x++) {
            c.teleport(x + 0.5, CY - 1, z + 2.5);
            c.waitTicks(3);
            c.lookAt(x + 0.5, CY - 0.5, z + 0.5);
            c.waitTicks(1);
            c.use(1);
            c.waitTicks(3);
            String duct = nbt(c, x, CY - 1, z);
            Expect.that(duct.contains("\"" + fluid + "\""), "duct " + x + "," + z + " typed " + fluid + ": " + duct);
        }
    }

    /** A steel barrel behind the ducts: identifier and infinite barrel clicked in, mode clicked to output. */
    private static void fillBarrel(Client c, int z, String fluid) {
        int x = 9;
        place(c, "hbm:barrel_steel", x, z);
        c.cmd("clear");
        c.cmd("give @s " + identifier(c, fluid) + " 1");
        c.cmd("give @s hbm:fluid_barrel_infinite 1");
        c.waitTicks(3);
        openAt(c, x + 0.5, z + 2.5, new int[]{x, CY - 1, z}, "");
        clickFromInventory(c, "hbm:fluid_identifier", 0);
        clickFromInventory(c, "hbm:fluid_barrel_infinite", 2);
        JsonObject g = c.screen();
        int l = g.get("left").getAsInt(), t = g.get("top").getAsInt();
        c.click(l + 151 + 9, t + 35 + 9); // input -> buffer
        c.waitTicks(3);
        c.click(l + 151 + 9, t + 35 + 9); // buffer -> output
        c.waitTicks(10);
        c.screenshot("05-gui-barrel-" + fluid.substring(4));
        close(c);
        String b = nbt(c, x, CY - 1, z);
        Expect.that(b.contains("\"" + fluid + "\"") && b.contains("mode:2s"), "barrel " + z + " is an output " + fluid + " barrel: " + b);
    }

    /** Places {@code item} on the ledge at {@code (x, CY-1, z)} by hand, standing 2.5 blocks south of it. */
    private static void place(Client c, String item, int x, int z) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(x + 0.5, CY - 1, z + 2.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, LEDGE + 0.98, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
        String b = c.block(x, CY - 1, z).toString();
        Expect.that(!b.contains("minecraft:air"), item + " placed at " + x + " " + (CY - 1) + " " + z + ": " + b);
    }

    private static void openLauncher(Client c) {
        // from the west ledge: the rim's west face is 6 out from the core
        c.teleport(-9.5, CY - 1, 3.5);
        c.waitTicks(3);
        c.hotbar(9);
        c.lookAt(-5.95, CY - 0.5, 3.5);
        c.waitTicks(1);
        c.use(2);
        c.waitScreen("Soyuz", 5000);
        Expect.screenStaysOpen(c, "Soyuz", 10);
        c.waitTicks(5);
    }

    private static void openAt(Client c, double sx, double sz, int[] target, String title) {
        c.teleport(sx, CY - 1, sz);
        c.waitTicks(3);
        c.hotbar(9);
        c.waitTicks(1);
        c.lookAt(target[0] + 0.5, target[1] + 0.5, target[2] + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitScreen(title, 5000);
        c.waitTicks(5);
        Expect.that(c.screen().has("slots"), "a container screen opened at " + target[0] + " " + target[1] + " " + target[2]);
    }

    // --- flight ------------------------------------------------------------------------------------------------------

    /** Waits out the countdown (600 server ticks) until the rocket leaves the pad; returns the rocket if seen. */
    private static JsonObject awaitRocket(Client c) {
        c.teleport(20.5, CY + 6, -20.5);
        c.waitTicks(400);
        for (int i = 0; i < 300; i++) {
            JsonObject r = entity(c, "hbm:entity_soyuz", 900);
            if (r != null) return r;
            if (!nbt(c, 0, CY, 0).contains("missile_soyuz")) return null;
            c.waitTicks(5);
        }
        Expect.that(false, "the rocket never left the pad: " + nbt(c, 0, CY, 0));
        return null;
    }

    private static void observeRise(Client c, String shot) {
        double prev = -1;
        boolean shotTaken = false;
        for (int i = 0; i < 12; i++) {
            JsonObject r = entity(c, "hbm:entity_soyuz", 900);
            if (r == null) break;
            double y = r.get("y").getAsDouble();
            System.out.printf("[Pt1012Soyuz] rocket y=%.2f%n", y);
            if (prev >= 0) Expect.that(y > prev, "the rocket rises: " + prev + " -> " + y);
            prev = y;
            if (!shotTaken && y > CY + 15) {
                worldShot(c, shot, 40.5, CY + 30, -40.5, 0.5, y + 10, 0.5);
                shotTaken = true;
            }
            c.waitTicks(10);
        }
        Expect.that(prev > CY, "the rocket was seen in flight");
    }

    private static void waitGone(Client c, String type) {
        for (int i = 0; i < 60 && entity(c, type, 900) != null; i++) c.waitTicks(20);
        Expect.that(entity(c, type, 900) == null, type + " separated its payload above y=600");
    }

    // --- helpers -----------------------------------------------------------------------------------------------------

    private static boolean legacy(Client c) {
        return c.node().startsWith("1.20.");
    }

    private static String identifier(Client c, String fluid) {
        return legacy(c) ? "hbm:fluid_identifier{\"hbm:fluid\":\"" + fluid + "\"}"
                : "hbm:fluid_identifier[hbm:fluid=\"" + fluid + "\"]";
    }

    private static String satellite(Client c) {
        String id = "hbm:satellite_precision_laser";
        return legacy(c) ? id + "{\"hbm:persistent_data\":{freq:" + FREQ + "}}" : id + "[hbm:persistent_data={freq:" + FREQ + "}]";
    }

    private static boolean isBe(Client c, int x, int y, int z, String id) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("block_entity") && !be.get("block_entity").isJsonNull() && be.toString().contains(id);
    }

    private static int[] findBe(Client c, int x, int y, int z, String id, int r) {
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (isBe(c, x + dx, y, z + dz, id)) return new int[]{x + dx, y, z + dz};
        }
        return null;
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return (be.has("nbt") ? be.get("nbt").getAsString() : be.toString()).replaceAll("\\s+", "").replace("\\\"", "\"");
    }

    private static long longOf(String nbt, String key) {
        Matcher m = Pattern.compile("(?:^|[{,])\"?" + Pattern.quote(key) + "\"?:(-?\\d+)[LlSsBb]?(?=[,}])").matcher(nbt);
        Expect.that(m.find(), "no " + key + " in " + nbt);
        return Long.parseLong(m.group(1));
    }

    /** The fill of the tank saved under {@code name} (FluidTankNTM writes {@code <name>} as the fill). */
    private static long tank(String nbt, String name) {
        return longOf(nbt, name);
    }

    private static void clickFromInventory(Client c, String id, int machineSlot) {
        JsonObject slot = findSlot(c, id);
        Expect.that(slot != null, "inventory slot holding " + id);
        c.clickSlot(slot.get("index").getAsInt());
        c.waitTicks(1);
        c.clickSlot(machineSlot);
        c.waitTicks(2);
        JsonObject carried = c.screen().getAsJsonObject("carried");
        if (carried != null && carried.has("id") && !carried.get("id").getAsString().contains("air")) {
            c.clickSlot(slot.get("index").getAsInt());
            c.waitTicks(1);
        }
        Expect.that(findSlot(c, id) == null, id + " left the player inventory for machine slot " + machineSlot);
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

    private static void close(Client c) {
        c.key("escape");
        c.waitTicks(5);
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
