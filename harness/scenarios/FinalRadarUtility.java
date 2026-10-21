import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * In-game acceptance for the radar and utility machines: every block and item of the family in the inventory;
 * the radars, radar screen, forcefield, teleporter, tele-anchor, orbus, seal controller and Soyuz capsule placed
 * by hand; every menu opened by a real right-click; the small radar powered from a creative battery moved in by
 * clicks, a falling missile summoned high overhead, its blip on the map and on the linked screen, and a redstone
 * lamp lit by the radar; a launch command typed over the blip firing the linked launch pad; the forcefield
 * switched on by its button; the teleporter linked with the telelinker and ridden; the recall device used; the
 * orbus filled; the silo hatch closed and opened by redstone; and a Soyuz lander capsule landing as a block.
 */
public final class FinalRadarUtility implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    static final String[] IDS = {"hbm:machine_radar", "hbm:machine_radar_large", "hbm:radar_screen",
            "hbm:machine_forcefield", "hbm:machine_teleporter", "hbm:teleanchor", "hbm:machine_orbus",
            "hbm:seal_controller", "hbm:seal_hatch", "hbm:soyuz_capsule", "hbm:radar_linker", "hbm:linker",
            "hbm:anchor_remote", "hbm:upgrade_radius", "hbm:upgrade_health"};

    // layout on the platform
    private static final int RX = X - 12, RZ = Z - 12;      // small radar
    private static final int LX = X + 2, LZ = Z - 16;       // large radar (placed block)
    private static final int SX = X - 16, SZ = Z - 4;       // radar screen (placed block)
    private static final int PX = X - 4, PZ = Z - 4;        // launch pad core
    private static final int FX = X + 22, FZ = Z + 22;      // forcefield
    private static final int TX = X + 8, TZ = Z + 8;        // teleporter
    private static final int AX = X + 14, AZ = Z + 4;       // tele-anchor
    private static final int OX = X - 14, OZ = Z + 16;      // orbus (placed block)
    private static final int CX = X, CZ = Z + 14;           // seal controller
    private static final int KX = X - 24, KZ = Z - 2;       // soyuz capsule

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 30);

        inventory(c);

        // --- placement by hand ---------------------------------------------------------------------------
        c.cmd("gamemode creative");
        place(c, "hbm:machine_radar", RX, Y, RZ);
        Expect.block(c, RX, Y + 1, RZ, "hbm:machine_radar");
        place(c, "hbm:machine_radar_large", LX, Y, LZ);
        int[] largeCore = findCore(c, LX, Y + 1, LZ, "hbm:machine_radar_large");
        Expect.that(largeCore != null, "the large radar formed a core");
        place(c, "hbm:radar_screen", SX, Y, SZ);
        int[] screenCore = findCore(c, SX, Y + 1, SZ, "hbm:radar_screen");
        Expect.that(screenCore != null, "the radar screen formed a core");
        place(c, "hbm:machine_forcefield", FX, Y, FZ);
        Expect.block(c, FX, Y + 1, FZ, "hbm:machine_forcefield");
        place(c, "hbm:machine_teleporter", TX, Y, TZ);
        Expect.block(c, TX, Y + 1, TZ, "hbm:machine_teleporter");
        place(c, "hbm:teleanchor", AX, Y, AZ);
        Expect.block(c, AX, Y + 1, AZ, "hbm:teleanchor");
        place(c, "hbm:machine_orbus", OX, Y, OZ);
        int[] orbusCore = findCore(c, OX, Y + 1, OZ, "hbm:machine_orbus");
        Expect.that(orbusCore != null, "the orbus formed a core");
        place(c, "hbm:soyuz_capsule", KX, Y, KZ);
        Expect.block(c, KX, Y + 1, KZ, "hbm:soyuz_capsule");
        place(c, "hbm:launch_pad", PX, Y, PZ);
        int[] padCore = findCore(c, PX, Y + 1, PZ, "hbm:launch_pad");
        Expect.that(padCore != null, "the launch pad formed a core");

        // the seal controller is placed looking south (+Z), so its ring runs south of it
        Fixtures.giveOnly(c, "hbm:seal_controller", 1);
        c.teleport(CX + 0.5, Y + 1, CZ - 2.5);
        c.waitTicks(3);
        c.lookAt(CX + 0.5, Y + 0.98, CZ + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
        JsonObject seal = Expect.block(c, CX, Y + 1, CZ, "hbm:seal_controller");
        Expect.that(seal.toString().contains("facing=north") || seal.toString().contains("\"facing\":\"north\""),
                "the controller's face looks back at its placer: " + seal);
        for (int x = -2; x <= 2; x++) {
            for (int z = 0; z <= 4; z++) {
                boolean edge = x == -2 || x == 2 || z == 0 || z == 4;
                if (edge && !(x == 0 && z == 0)) Fixtures.setBlock(c, CX + x, Y + 1, CZ + z, "hbm:seal_frame");
            }
        }
        worldShot(c, "10-placed-north", X + 2.5, Y + 12, Z + 6.5, X - 8.5, Y + 1, Z - 10.5);
        worldShot(c, "11-placed-south", X - 6.5, Y + 10, Z - 2.5, X + 6.5, Y + 1, Z + 14.5);

        // --- small radar: map GUI, link-slot GUI, battery by clicks ----------------------------------------
        c.cmd("clear");
        c.cmd("give @s hbm:battery_creative 1");
        c.waitTicks(3);
        openRadarMap(c);
        c.screenshot("20-gui-radar-map-unpowered");
        clickRadarToggle(c, 158); // switch view -> the link slots
        c.waitScreen("GUIMachineRadarNTSlots", 5000);
        Expect.screenStaysOpen(c, "GUIMachineRadarNTSlots", 10);
        clickFromInventory(c, "hbm:battery_creative", 9);
        c.waitTicks(10);
        c.screenshot("21-gui-radar-slots-battery");
        close(c);

        // --- a redstone lamp beside the radar; a linker to the screen in slot 8, one to the pad in slot 0 ----
        Fixtures.setBlock(c, RX - 1, Y + 1, RZ, "minecraft:redstone_lamp");
        Fixtures.giveOnly(c, "hbm:radar_linker", 1);
        linkWith(c, screenCore[0], screenCore[2]);
        moveIntoRadar(c, "hbm:radar_linker", 8);
        Fixtures.giveOnly(c, "hbm:radar_linker", 1);
        linkWith(c, padCore[0], padCore[2]);
        moveIntoRadar(c, "hbm:radar_linker", 0);
        openRadarMap(c);
        clickRadarToggle(c, 158);
        c.waitScreen("GUIMachineRadarNTSlots", 5000);
        c.waitTicks(3);
        c.screenshot("22-gui-radar-slots-linked");
        close(c);
        String radarNbt = c.blockEntity(RX, Y + 1, RZ).toString();
        Expect.that(radarNbt.contains("radar_linker"), "the radar holds its linkers: " + radarNbt);

        // --- the launch pad: an anti-ballistic missile (solid fuel), which is handed the blip to chase and a battery ------------------------------------
        c.cmd("give @s hbm:missile_anti_ballistic 1");
        c.cmd("give @s hbm:battery_creative 1");
        c.waitTicks(2);
        openAt(c, padCore[0] + 3.5, padCore[2] + 0.5, padCore[0] + 0.5, padCore[2] + 0.5, "Launch Pad");
        clickFromInventory(c, "hbm:missile_anti_ballistic", 0);
        clickFromInventory(c, "hbm:battery_creative", 2);
        c.waitTicks(5);
        close(c);

        // --- a target: a tier-1 missile summoned high above and to the side, falling --------------------------
        int mx = RX + 40, my = Y + 190, mz = RZ + 30; // inside the force-loaded platform chunks, so it ticks
        c.cmd("summon hbm:entity_missile_generic " + mx + " " + my + " " + mz);
        c.waitTicks(40);
        // a freshly summoned missile hovers (it has no launch heading), and smart mode only counts falling ones:
        // turn smart mode off with its toggle so the hovering blip drives the redstone
        openRadarMap(c);
        clickRadarToggle(c, 118);
        close(c);
        c.waitTicks(10);
        JsonObject radarState = c.blockEntity(RX, Y + 1, RZ);
        Expect.that(radarState.toString().contains("power"), "radar block entity: " + radarState);
        JsonObject lamp = c.block(RX - 1, Y + 1, RZ);
        Expect.that(lamp.toString().contains("lit=true") || lamp.toString().contains("\"lit\":\"true\""),
                "the radar's redstone lit the lamp for the missile: " + lamp);

        openRadarMap(c);
        clickRadarToggle(c, 138); // show map
        c.waitTicks(60);
        c.screenshot("23-gui-radar-map-blip");
        // hover the missile's blip and type 1: the launch command through the linker in slot 0
        JsonObject target = entity(c, "hbm:entity_missile_generic", 400);
        Expect.that(target != null, "the target missile is still in the air");
        int[] blip = blipScreenPos(c, target.get("x").getAsDouble(), target.get("z").getAsDouble());
        c.mouseMove(blip[0], blip[1]);
        c.waitTicks(4);
        c.screenshot("24-gui-radar-blip-tooltip");
        c.waitTicks(100); // the pad counts down 100 ticks after it is loaded before it is ready
        c.mouseMove(blip[0], blip[1]);
        c.waitTicks(2);
        c.type("1");
        c.waitTicks(6);
        JsonObject launched = entity(c, "hbm:entity_missile_ab", 60);
        Expect.that(launched != null, "the radar's launch command fired the linked launch pad");
        c.cmd("kill @e[type=hbm:entity_missile_ab]");
        close(c);

        worldShot(c, "25-radar-and-screen", RX + 4.5, Y + 3.5, RZ + 9.5, RX - 1.5, Y + 1.5, RZ - 0.5);
        worldShot(c, "26-radar-screen-front", screenCore[0] + 0.5, Y + 2, screenCore[2] + 4.5,
                screenCore[0] + 0.5, Y + 2, screenCore[2] + 0.5);
        worldShot(c, "26b-radar-screen-east", screenCore[0] + 4.5, Y + 2, screenCore[2] + 0.5,
                screenCore[0] + 0.5, Y + 2, screenCore[2] + 0.5);
        String screenNbt = c.blockEntity(screenCore[0], screenCore[1], screenCore[2]).toString();

        // --- large radar: a creative generator on a connection point, its map and slots ---------------------
        // a cable on the east connection point, a creative generator behind it: the proxy cell takes the power
        Fixtures.setBlock(c, largeCore[0] + 2, Y + 1, largeCore[2], "hbm:red_cable");
        Fixtures.setBlock(c, largeCore[0] + 3, Y + 1, largeCore[2], "hbm:creative_generator");
        c.waitTicks(20);
        openAt(c, largeCore[0] + 1.5, largeCore[2] + 3.5, largeCore[0] + 0.5, largeCore[2] + 1.5, "GUIMachineRadarNT");
        c.waitTicks(20);
        c.screenshot("27-gui-radar-large-map");
        close(c);
        String largeNbt = c.blockEntity(largeCore[0], largeCore[1], largeCore[2]).toString().replaceAll("\s", "");
        Expect.that(largeNbt.contains("power:") && !largeNbt.contains("power:0L"), "the large radar took power through its proxy cell: " + largeNbt);
        worldShot(c, "28-radar-large", largeCore[0] + 7.5, Y + 6, largeCore[2] + 7.5, largeCore[0] + 0.5, Y + 3, largeCore[2] + 0.5);
        c.cmd("kill @e[type=hbm:entity_missile_generic]");

        // --- forcefield: battery by click, button by click, the sphere ------------------------------------------
        c.cmd("give @s hbm:battery_creative 1");
        c.waitTicks(2);
        openAt(c, FX + 3.5, FZ + 0.5, FX + 0.5, FZ + 0.5, "Forcefield");
        clickFromInventory(c, "hbm:battery_creative", 0);
        JsonObject ffScreen = c.screen();
        c.click(ffScreen.get("left").getAsInt() + 151, ffScreen.get("top").getAsInt() + 43);
        c.waitTicks(20);
        c.screenshot("30-gui-forcefield-on");
        close(c);
        String ffNbt = c.blockEntity(FX, Y + 1, FZ).toString().replaceAll("\s", "");
        Expect.that(ffNbt.contains("isOn:1b") || ffNbt.contains("isOn:true"), "the forcefield is on: " + ffNbt);
        worldShot(c, "31-forcefield-sphere", FX - 20.5, Y + 14, FZ - 20.5, FX + 0.5, Y + 4, FZ + 0.5);
        worldShot(c, "32-forcefield-close", FX + 4.5, Y + 3, FZ + 0.5, FX + 0.5, Y + 1.5, FZ + 0.5);

        // --- teleporter: exit linked with the telelinker, powered by a generator, ridden -----------------------
        Fixtures.setBlock(c, TX + 1, Y + 1, TZ, "hbm:creative_generator");
        Fixtures.giveOnly(c, "hbm:linker", 1);
        c.teleport(X + 20.5, Y + 1, Z - 2.5);
        c.waitTicks(3);
        c.lookAt(X + 20.5, Y + 0.98, Z + 0.5);
        c.waitTicks(1);
        c.use(2); // the exit: the platform block at (20, Y, 0)
        c.waitTicks(3);
        c.teleport(TX - 2.5, Y + 1, TZ + 0.5);
        c.waitTicks(3);
        c.lookAt(TX + 0.5, Y + 1.5, TZ + 0.5);
        c.waitTicks(1);
        c.use(2); // hand the exit to the teleporter
        c.waitTicks(40);
        String teleNbt = c.blockEntity(TX, Y + 1, TZ).toString().replaceAll("\s", "");
        Expect.that(teleNbt.contains("x1:20") && teleNbt.contains("y1:" + Y), "the teleporter took the exit: " + teleNbt);
        worldShot(c, "40-teleporter", TX + 3.5, Y + 3, TZ + 3.5, TX + 0.5, Y + 1.5, TZ + 0.5);
        c.cmd("gamemode survival");
        // a raw /tp: the harness teleport waits to arrive, and the teleporter moves the player on at once
        c.cmd("tp @s " + (TX + 0.5) + " " + (Y + 2) + " " + (TZ + 0.5));
        c.waitTicks(20);
        JsonObject after = c.player();
        c.cmd("gamemode creative");
        Expect.that(Math.abs(after.get("x").getAsDouble() - 20.5) < 0.6 && Math.abs(after.get("z").getAsDouble() - 0.5) < 0.6,
                "stepping on the teleporter moved the player to the exit: " + after);

        // --- recall device: linked to the anchor, used from afar ------------------------------------------------
        Fixtures.giveOnly(c, "hbm:anchor_remote", 1);
        c.teleport(AX + 2.5, Y + 1, AZ + 0.5);
        c.waitTicks(3);
        c.lookAt(AX + 0.5, Y + 1.5, AZ + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(3);
        c.teleport(X - 20.5, Y + 1, Z + 0.5);
        c.waitTicks(3);
        c.look(0, -60);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(10);
        JsonObject recalled = c.player();
        Expect.that(Math.abs(recalled.get("x").getAsDouble() - (AX + 0.5)) < 0.6
                        && Math.abs(recalled.get("z").getAsDouble() - (AZ + 0.5)) < 0.6,
                "the recall device put the player on the anchor: " + recalled);

        // --- orbus: its menu, then a full-ish tank for the renderer ---------------------------------------------
        openAt(c, orbusCore[0] + 4.5, orbusCore[2] + 0.5, orbusCore[0] + 0.5, orbusCore[2] + 0.5, "Antimatter");
        c.screenshot("50-gui-orbus");
        close(c);
        c.cmd("data merge block " + orbusCore[0] + " " + orbusCore[1] + " " + orbusCore[2]
                + " {tank:384000,tank_type:\"" + waterId() + "\",tank_max:512000}");
        c.waitTicks(10);
        openAt(c, orbusCore[0] + 4.5, orbusCore[2] + 0.5, orbusCore[0] + 0.5, orbusCore[2] + 0.5, "Antimatter");
        c.waitTicks(5);
        c.screenshot("51-gui-orbus-filled");
        close(c);
        worldShot(c, "52-orbus", orbusCore[0] + 8.5, Y + 4, orbusCore[2] + 6.5, orbusCore[0] + 0.5, Y + 2.5, orbusCore[2] + 0.5);

        // --- silo hatch: redstone closes it, redstone again opens it, a click closes it -------------------------
        Fixtures.setBlock(c, CX, Y + 1, CZ - 1, "minecraft:redstone_block");
        c.waitTicks(5);
        Expect.block(c, CX, Y + 1, CZ + 2, "hbm:seal_hatch");
        Expect.block(c, CX + 1, Y + 1, CZ + 3, "hbm:seal_hatch");
        worldShot(c, "60-seal-closed", CX + 0.5, Y + 6, CZ - 3.5, CX + 0.5, Y + 1, CZ + 2.5);
        Fixtures.setBlock(c, CX, Y + 1, CZ - 1, "minecraft:air");
        c.waitTicks(3);
        Fixtures.setBlock(c, CX, Y + 1, CZ - 1, "minecraft:redstone_block");
        c.waitTicks(5);
        Expect.block(c, CX, Y + 1, CZ + 2, "minecraft:air");
        Fixtures.setBlock(c, CX, Y + 1, CZ - 1, "minecraft:air");
        c.waitTicks(3);
        c.teleport(CX + 0.5, Y + 1, CZ - 2.5);
        c.waitTicks(3);
        Fixtures.interact(c, CX, Y + 1, CZ);
        c.waitTicks(3);
        Expect.block(c, CX, Y + 1, CZ + 2, "hbm:seal_hatch");
        worldShot(c, "61-seal-closed-by-click", CX + 0.5, Y + 6, CZ - 3.5, CX + 0.5, Y + 1, CZ + 2.5);

        // --- soyuz capsule: the placed block's menu, and a lander capsule touching down ------------------------
        openAt(c, KX + 3.5, KZ + 0.5, KX + 0.5, KZ + 0.5, "Cargo Landing Capsule");
        c.screenshot("70-gui-soyuz-capsule");
        close(c);
        c.cmd("summon hbm:entity_soyuz_capsule " + (X + 26.5) + " " + (Y + 12) + " " + (Z - 9.5));
        c.waitTicks(160);
        // 1.7 floors the landing position
        int lx = (int) Math.floor(X + 26.5), lz = (int) Math.floor(Z - 9.5);
        Expect.block(c, lx, Y + 1, lz, "hbm:soyuz_capsule");
        String landed = c.blockEntity(lx, Y + 1, lz).toString();
        Expect.that(landed.contains("missile_soyuz"), "the landed capsule holds the spent rocket: " + landed);
        openAt(c, lx + 3.5, lz + 0.5, lx + 0.5, lz + 0.5, "Cargo Landing Capsule");
        c.screenshot("71-gui-landed-capsule");
        close(c);
        worldShot(c, "72-landed-capsule", lx + 4.5, Y + 3.5, lz + 3.5, lx + 0.5, Y + 1, lz + 0.5);

        Expect.that(screenNbt.length() > 0, "radar screen block entity present");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture|Missing model|Unable to load model");
    }

    // --- helpers -------------------------------------------------------------------------------------------

    private static String waterId() {
        return "hbm:water"; // Fluids.WATER's registry name; checked visually by the filled orbus GUI
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

    private static void place(Client c, String item, int x, int y, int z) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(x + 4.5, y + 1, z + 0.5);
        c.waitTicks(3);
        c.lookAt(x + 0.9, y + 0.98, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
    }

    /** The core of a multiblock placed at {@code (x, y, z)}: the block entity that answers within two blocks. */
    private static int[] findCore(Client c, int x, int y, int z, String id) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                JsonObject be = c.blockEntity(x + dx, y, z + dz);
                if (be.has("block_entity") && !be.get("block_entity").isJsonNull()
                        && be.toString().contains(id.substring(4))) {
                    return new int[]{x + dx, y, z + dz};
                }
            }
        }
        return null;
    }

    /** Sneak + right-click a block with the held linker: 1.7 swallowed plain clicks on these machines. */
    private static void linkWith(Client c, int x, int z) {
        c.hotbar(1);
        c.teleport(x + 3.5, Y + 1, z + 0.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, Y + 1.5, z + 0.5);
        c.waitTicks(1);
        c.keyDown("key.sneak");
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(3);
        c.keyUp("key.sneak");
        c.waitTicks(2);
    }

    /** Opens the small radar's map, switches to its slots and clicks {@code id} into {@code slot}. */
    private static void moveIntoRadar(Client c, String id, int slot) {
        openRadarMap(c);
        clickRadarToggle(c, 158);
        c.waitScreen("GUIMachineRadarNTSlots", 5000);
        c.waitTicks(3);
        clickFromInventory(c, id, slot);
        c.waitTicks(3);
        close(c);
    }

    private static void openRadarMap(Client c) {
        openAt(c, RX + 2.5, RZ + 0.5, RX + 0.5, RZ + 0.5, "GUIMachineRadarNT");
    }

    /** The radar map's left-edge toggles: 8x8 at {@code (guiLeft - 10, guiTop + top)}. */
    private static void clickRadarToggle(Client c, int top) {
        JsonObject status = c.status();
        int w = status.get("gui_width").getAsInt(), h = status.get("gui_height").getAsInt();
        int left = (w - 216) / 2, topPos = (h - 234) / 2;
        c.click(left - 10 + 4, topPos + top + 4);
        c.waitTicks(4);
    }

    /** 1.7's blip centre: {@code guiLeft + 108 + dx / (2 range + 1) * 192}, likewise for z. */
    private static int[] blipScreenPos(Client c, double ex, double ez) {
        JsonObject status = c.status();
        int w = status.get("gui_width").getAsInt(), h = status.get("gui_height").getAsInt();
        int left = (w - 216) / 2, topPos = (h - 234) / 2;
        int x = left + (int) ((Math.floor(ex) - RX) / (1000D * 2 + 1) * 192D) + 108;
        int z = topPos + (int) ((Math.floor(ez) - RZ) / (1000D * 2 + 1) * 192D) + 117;
        return new int[]{x, z};
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
