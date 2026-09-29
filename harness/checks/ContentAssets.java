import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * World and item-asset acceptance fixture for the old HBM rail/machine family.
 *
 * <p>This deliberately uses server-side {@code setblock} for rail cores.  A hand placement of a
 * folded rail builds its full track and needs a different amount of empty space for each of the
 * eleven old shapes; the core is the renderer's authoritative anchor and keeps this visual check
 * bounded and repeatable on every loader.  The cargo lift and cooling towers use their real hand
 * placement paths so their folded cells and block entities are exercised too.
 *
 * <p>Expected report artefacts: a three-row rail contact shot, a funnel and one-course cargo lift
 * shot, separate small/large tower shots (the tower heights differ substantially), and an open
 * player-inventory shot containing every checked block item.  The screenshots are intentionally
 * separate from gameplay assertions: a green block/BE query cannot prove that an OBJ or BER was
 * actually submitted to the frame.
 */
public final class ContentAssets implements Scenario {
    // Keep this fixture away from the normal origin-based harness scenes and from any stale world
    // left by a previous visual run.  All placements below are relative to this anchor.
    private static final int X = 256;
    private static final int Y = 100;
    private static final int Z = 256;

    private static final String[] RAILS = {
            "hbm:rail_large_straight",
            "hbm:rail_large_straight_short",
            "hbm:rail_large_curve",
            "hbm:rail_large_curve_7",
            "hbm:rail_large_curve_9",
            "hbm:rail_large_ramp",
            "hbm:rail_large_buffer",
            "hbm:rail_large_switch",
            "hbm:rail_large_switch_flipped",
            "hbm:rail_narrow_straight",
            "hbm:rail_narrow_curve"
    };

    private static final int[][] RAIL_POSITIONS = {
            {-28, -15}, {-18, -15}, {-8, -15}, {2, -15}, {12, -15}, {22, -15},
            {-28, 0}, {-16, 0}, {-4, 0}, {8, 0}, {18, 0}
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        // Radius 30 keeps the helper's 61x8x61 air fill under vanilla's 32,768-block limit.
        Fixtures.platform(c, X, Y, Z, 30);
        // The camera/interaction positions and tower footprints extend beyond the cleared air square.
        // A single floor layer is small enough to cover them without exceeding the fill limit.
        c.cmd("fill " + (X - 48) + " " + Y + " " + (Z - 48) + " "
                + (X + 48) + " " + Y + " " + (Z + 48) + " minecraft:stone");
        // Towers are the only tall structures in this fixture.  Extend the cleared air column before
        // placing rails so this cannot erase a previously placed visual anchor.
        // Keep each fill below vanilla's 32,768-block /fill limit.  Only the two tower footprints
        // need the extra vertical clearance; the platform helper already clears the rail rows.
        c.cmd("fill " + (X - 40) + " " + (Y + 9) + " " + (Z + 16) + " "
                + (X - 28) + " " + (Y + 24) + " " + (Z + 34) + " minecraft:air");
        c.cmd("fill " + (X + 8) + " " + (Y + 9) + " " + (Z + 14) + " "
                + (X + 28) + " " + (Y + 24) + " " + (Z + 36) + " minecraft:air");
        c.waitTicks(2);

        // Bulk fills performed while distant chunks load can leave stale client skylight on older
        // nodes. A fresh join receives the completed fixture's chunk/light snapshot. Keep this
        // before any HBM placement so real machine interactions remain in the same live session.
        c.leaveWorld(60_000L);
        c.joinWorld("harness", 120_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(30);

        placeRailCores(c);
        placeFunnel(c);
        placeCargoElevator(c);
        placeTowers(c);
        inventoryIcons(c);

        for (String error : c.logSinceMark("(ERROR|FATAL|Exception in thread|ReportedException|Failed to load texture)")) {
            // Fabric's offline development token cannot authenticate to Realms. The fixture's
            // title/rejoin step can trigger this external service check; retain every other error.
            boolean offlineRealms = error.contains("[RealmsAvailability]")
                    && error.contains("Realms authentication error")
                    && error.contains("Failed to parse into SignedJWT: FabricMC");
            Expect.that(offlineRealms, "unexpected client error during content checks: " + error);
        }
    }

    private static void placeRailCores(Client c) {
        for (int i = 0; i < RAILS.length; i++) {
            int x = X + RAIL_POSITIONS[i][0];
            int z = Z + RAIL_POSITIONS[i][1];
            String id = RAILS[i];
            // The state is explicit so all eleven BERs receive the same world-facing presentation.
            Fixtures.setBlock(c, x, Y + 1, z, id + "[facing=east]");
            Expect.block(c, x, Y + 1, z, id);
            JsonObject be = c.blockEntity(x, Y + 1, z);
            assertBlockEntity(be, "rail BE exists for " + id + " at " + x + "," + (Y + 1) + "," + z);
        }

        // The two switch BEs have real persisted state.  Toggle one through the client click path
        // and verify the server-side NBT changes; the visual report then shows both switch routes.
        int switchX = X + RAIL_POSITIONS[7][0];
        int switchZ = Z + RAIL_POSITIONS[7][1];
        String before = c.blockEntity(switchX, Y + 1, switchZ).get("nbt").getAsString();
        c.teleport(switchX + 2.5, Y + 1, switchZ - 2.5);
        c.waitTicks(12);
        Fixtures.interact(c, switchX, Y + 1, switchZ);
        String after = c.blockEntity(switchX, Y + 1, switchZ).get("nbt").getAsString();
        Expect.that(!before.equals(after) && after.contains("isSwitched"),
                "rail switch state persisted after the real interaction path");

        // One overview plus closer rows makes the OBJ/BER contact sheet useful at ordinary harness
        // FOV, while keeping every model separated by at least one empty block.
        worldShot(c, "10-rails-contact", X + 0.5, Y + 18, Z - 42.5,
                X, Y + 1.0, Z - 7.0);
        worldShot(c, "11-rails-large-row", X - 3.5, Y + 9, Z - 36.5,
                X - 3.0, Y + 1.0, Z - 15.0);
        worldShot(c, "12-rails-switch-narrow-row", X - 3.5, Y + 9, Z - 27.5,
                X - 3.0, Y + 1.0, Z);
    }

    private static void placeFunnel(Client c) {
        int x = X + 29;
        int z = Z + 14;
        c.teleport(x + 2.5, Y + 1, z - 2.5);
        c.waitTicks(15);
        Fixtures.setBlock(c, x, Y + 1, z, "hbm:machine_funnel");
        c.waitTicks(10);
        Expect.block(c, x, Y + 1, z, "hbm:machine_funnel");
        JsonObject be = c.blockEntity(x, Y + 1, z);
        assertBlockEntity(be, "funnel block entity exists at " + x + "," + (Y + 1) + "," + z);

        c.teleport(x + 2.5, Y + 1, z - 2.5);
        c.waitTicks(12);
        Fixtures.interact(c, x, Y + 1, z);
        c.waitScreen("Funnel", 5_000L);
        Expect.screenOpen(c, "Funnel");
        c.screenshot("20-funnel-menu");
        c.key("escape");
        c.waitTicks(3);

        worldShot(c, "21-funnel-world", x + 7.5, Y + 5, z - 7.5,
                x + 0.5, Y + 1.0, z + 0.5);
    }

    private static void placeCargoElevator(Client c) {
        int x = X + 29;
        int z = Z + 25;
        c.teleport(x + 2.5, Y + 1, z - 2.5);
        c.waitTicks(15);
        Fixtures.setBlock(c, x, Y + 1, z, "hbm:cargo_elevator[facing=east]");
        c.waitTicks(10);
        Expect.block(c, x, Y + 1, z, "hbm:cargo_elevator");
        JsonObject be = c.blockEntity(x, Y + 1, z);
        assertBlockEntity(be, "cargo elevator block entity exists at " + x + "," + (Y + 1) + "," + z);

        // The old BlockCargoElevator adds one 3x3 course when another elevator is used on it.  This
        // gives the renderer a visible guide/piston/platform stack and tests the actual multiblock path.
        Fixtures.giveOnly(c, "hbm:cargo_elevator", 1);
        // Aim down through the actual core cell. A command-placed core has no peripheral cells
        // for a side-on ray to hit even though the platform mesh extends beyond the core.
        c.teleport(x + 0.5, Y + 4, z + 0.5);
        c.waitTicks(12);
        c.lookAt(x + 0.5, Y + 1.95, z + 0.5);
        c.waitTicks(12);
        c.use(2);
        c.waitTicks(5);
        String courseNbt = c.blockEntity(x, Y + 1, z).get("nbt").getAsString();
        Expect.matches("\\bheight:\\s*1\\s*[,}]", courseNbt,
                "cargo elevator gained one source-faithful course: " + courseNbt);

        Fixtures.interact(c, x, Y + 1, z);
        c.waitTicks(14);
        worldShot(c, "30-cargo-elevator-running", x + 8.5, Y + 6, z - 8.5,
                x + 0.5, Y + 2.0, z + 0.5);
    }

    private static void placeTowers(Client c) {
        // The old cores are offset from the clicked east edge by 2 and 4 blocks respectively; the
        // placement helper's exact westward aim therefore gives stable core positions on all nodes.
        int smallX = X - 30;
        int smallZ = Z + 25;
        JsonObject small = Fixtures.placeOnTop(c, "hbm:machine_tower_small", smallX, Y, smallZ);
        Expect.equal("hbm:machine_tower_small", small.get("block").getAsString(), "small cooling tower placed");
        Expect.block(c, smallX - 2, Y + 1, smallZ, "hbm:machine_tower_small");
        JsonObject smallBe = c.blockEntity(smallX - 2, Y + 1, smallZ);
        assertBlockEntity(smallBe, "small cooling tower core BE exists");

        int largeX = X + 20;
        int largeZ = Z + 25;
        JsonObject large = Fixtures.placeOnTop(c, "hbm:machine_tower_large", largeX, Y, largeZ);
        Expect.equal("hbm:machine_tower_large", large.get("block").getAsString(), "large cooling tower placed");
        Expect.block(c, largeX - 4, Y + 1, largeZ, "hbm:machine_tower_large");
        JsonObject largeBe = c.blockEntity(largeX - 4, Y + 1, largeZ);
        assertBlockEntity(largeBe, "large cooling tower core BE exists");

        worldShot(c, "40-cooling-tower-small", smallX + 0.5, Y + 10, smallZ - 24.5,
                smallX - 2.0, Y + 10.0, smallZ);
        worldShot(c, "41-cooling-tower-large", largeX + 0.5, Y + 8, largeZ - 24.5,
                largeX - 4.0, Y + 7.0, largeZ);
    }

    private static void inventoryIcons(Client c) {
        // Creative inventory hides the player's slots behind the catalog.  Switch to survival for
        // the real player inventory screen so the icons being checked are the stacks actually given.
        // Return to the platform before leaving creative: the preceding visual shots use a floating
        // spectator camera, and survival would otherwise let the player fall while the give commands
        // are being sent.
        c.cmd("gamemode creative");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(12);
        c.cmd("gamemode survival");
        c.waitTicks(2);
        c.cmd("clear");
        String[] icons = {
                "hbm:rail_large_straight", "hbm:rail_large_straight_short", "hbm:rail_large_curve",
                "hbm:rail_large_curve_7", "hbm:rail_large_curve_9", "hbm:rail_large_ramp",
                "hbm:rail_large_buffer", "hbm:rail_large_switch", "hbm:rail_large_switch_flipped",
                "hbm:rail_narrow_straight", "hbm:rail_narrow_curve", "hbm:machine_funnel",
                "hbm:cargo_elevator", "hbm:machine_tower_small", "hbm:machine_tower_large",
                "hbm:machine_rotary_furnace", "hbm:machine_microwave", "hbm:machine_radiolysis",
                "hbm:machine_reactor", "hbm:machine_reactor_small", "hbm:machine_excavator",
                "hbm:machine_ore_slopper", "hbm:machine_annihilator", "hbm:machine_hephaestus",
                "hbm:machine_industrial_generator", "hbm:decon", "hbm:machine_compressor_compact"
        };
        for (String id : icons) c.cmd("give @s " + id + " 1");
        c.waitTicks(4);
        for (String id : icons) Expect.inventoryHolds(c, id, 1);

        c.key("e");
        c.waitScreen("Inventory", 5_000L);
        Expect.screenOpen(c, "Inventory");
        c.screenshot("50-inventory-icons");
        c.key("escape");
        c.waitTicks(3);
    }

    private static void assertBlockEntity(JsonObject view, String message) {
        Expect.that(view.has("nbt") && !view.get("nbt").isJsonNull(), message);
    }

    /** Allow the minimized client and integrated server to settle before changing the camera. */
    private static void worldShot(Client c, String name, double x, double y, double z,
                                  double targetX, double targetY, double targetZ) {
        // The world captures are asset checks, so the held hand must not occlude the model.
        // Creative players do not fly by default in this harness. Spectator keeps the high camera
        // positions exact while the minimized client catches up with the teleport.
        c.hotbar(9);
        c.waitTicks(2);
        c.cmd("gamemode spectator");
        c.waitTicks(2);
        c.teleport(x, y, z);
        c.waitTicks(12);
        JsonObject player = c.player();
        double dx = player.get("x").getAsDouble() - x;
        double dy = player.get("y").getAsDouble() - y;
        double dz = player.get("z").getAsDouble() - z;
        Expect.that(Math.abs(dx) < 0.75 && Math.abs(dy) < 0.75 && Math.abs(dz) < 0.75,
                "camera teleport settled for " + name + " at " + player);
        c.lookAt(targetX, targetY, targetZ);
        c.waitTicks(12);
        c.screenshot(name);
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }
}
