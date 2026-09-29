import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * A complete Albion particle accelerator, placed by hand and run to a recipe result.
 *
 * <p>Source -> RFC -> quadrupole -> RFC -> quadrupole -> RFC -> quadrupole -> dipole (turns the beam
 * north) -> detector. Three cavities give the 300 momentum the hydrogen + copper recipe needs, each
 * quadrupole undoes one cavity's defocus, and the dipole's default exit is north. Every multiblock is
 * placed with a real right-click on a replaceable structure void at the clicked cell, from the side
 * that gives the facing the beam needs (the beam leaves a machine at its facing rotated a quarter
 * turn in ForgeDirection's DOWN rotation: facing south sends it east, facing east sends it north).
 *
 * <p>Power and cryogenic coolant are primed with {@code /data merge}: the cable and perfluoromethyl
 * supply chain is not what is under test here, the beam walk and the machine menus are. Coils and
 * particles go in through the real menus.
 */
public class AlbionFullRun implements Scenario {

    private static final int Y = 100;        // platform top
    private static final int BEAM_Y = 103;   // every core sits here; the detector reaches two below it
    private static final int SX = -24;       // source core x
    private static final int ZC = 8;         // beam row z

    // Cores along the east-bound beam (source injects at core + 5; RFC entry core - 4, exit core + 5;
    // quadrupole and dipole entry core - 1, exit core + 2; detector entry core - 4).
    private static final int RFC1 = SX + 9, QUAD1 = SX + 15, RFC2 = SX + 21, QUAD2 = SX + 27,
            RFC3 = SX + 33, QUAD3 = SX + 39, DIPOLE = SX + 42;
    private static final int DET_X = DIPOLE, DET_Z = ZC - 6;

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 28); // 57x8x57 air fill stays under the 32768-block fill limit

        // --- build ---
        placeMultiblock(c, "hbm:pa_source", SX, BEAM_Y, ZC, "south", 1, 3);
        placeMultiblock(c, "hbm:pa_rfc", RFC1, BEAM_Y, ZC, "south", 1, 3);
        placeMultiblock(c, "hbm:pa_quadrupole", QUAD1, BEAM_Y, ZC, "south", 1, 3);
        placeMultiblock(c, "hbm:pa_rfc", RFC2, BEAM_Y, ZC, "south", 1, 3);
        placeMultiblock(c, "hbm:pa_quadrupole", QUAD2, BEAM_Y, ZC, "south", 1, 3);
        placeMultiblock(c, "hbm:pa_rfc", RFC3, BEAM_Y, ZC, "south", 1, 3);
        placeMultiblock(c, "hbm:pa_quadrupole", QUAD3, BEAM_Y, ZC, "south", 1, 3);
        placeMultiblock(c, "hbm:pa_dipole", DIPOLE, BEAM_Y, ZC, "south", 1, 3);
        placeMultiblock(c, "hbm:pa_detector", DET_X, BEAM_Y, DET_Z, "east", 2, 4);

        c.teleport(RFC2 + 0.5, Y + 26, ZC - 18.5);
        c.lookAt(RFC2, BEAM_Y, ZC);
        c.waitTicks(3);
        c.screenshot("10-accelerator-built");

        // --- prime power and coolant (cold perfluoromethyl in tank t0) ---
        prime(c, SX, ZC, 10_000_000L);
        for (int x : new int[]{RFC1, RFC2, RFC3}) prime(c, x, ZC, 1_000_000L);
        for (int x : new int[]{QUAD1, QUAD2, QUAD3}) prime(c, x, ZC, 2_500_000L);
        prime(c, DIPOLE, ZC, 2_500_000L);
        prime(c, DET_X, DET_Z, 1_000_000L);
        c.waitTicks(5);

        // --- coils through the real menus ---
        for (int x : new int[]{QUAD1, QUAD2, QUAD3}) {
            insert(c, x, BEAM_Y, ZC, "GUIPAQuadrupole", "hbm:pa_coil_gold", 1);
        }
        // The dipole is approached from the east: its north side is inside the detector's footprint.
        insert(c, DIPOLE, BEAM_Y, ZC, DIPOLE + 4.5, ZC + 0.5, "GUIPADipole", "hbm:pa_coil_gold", 1);
        open(c, DIPOLE, BEAM_Y, ZC, DIPOLE + 4.5, ZC + 0.5, "GUIPADipole");
        c.screenshot("20-dipole-menu");
        close(c);

        // --- detector: an empty capsule in its first container slot is what the antimatter lands in ---
        insert(c, DET_X + 2, BEAM_Y, DET_Z, DET_X + 4.5, DET_Z + 0.5, "GUIPADetector", "hbm:particle_empty", 1);
        open(c, DET_X + 2, BEAM_Y, DET_Z, DET_X + 4.5, DET_Z + 0.5, "GUIPADetector");
        Expect.slotEmpty(c, 3);
        c.screenshot("30-detector-before");
        close(c);

        // --- load the source: hydrogen and copper; the run starts on the tick both are in ---
        // Real clicks on the two input slots (the JEI recipe click area must be the arrow only, not the
        // slot block — a 36 x 45 area there took every slot click and opened the recipe browser).
        insert(c, SX, BEAM_Y, ZC, "GUIPASource", "hbm:particle_hydrogen", 1);
        // The copper is consumed on the very next tick, so only its departure from the hotbar is checked.
        insert(c, SX, BEAM_Y, ZC, SX + 0.5, ZC - 3.5, "GUIPASource", "hbm:particle_copper", 2, false);
        c.waitTicks(40);

        // --- result ---
        open(c, SX, BEAM_Y, ZC, "GUIPASource");
        c.screenshot("40-source-after-run");
        Expect.slotEmpty(c, 1);
        Expect.slotEmpty(c, 2);
        Expect.slotHolds(c, 3, "hbm:particle_empty", 1);
        Expect.slotHolds(c, 4, "hbm:particle_empty", 1);
        close(c);

        open(c, DET_X + 2, BEAM_Y, DET_Z, DET_X + 4.5, DET_Z + 0.5, "GUIPADetector");
        c.screenshot("50-detector-after-run");
        Expect.slotEmpty(c, 1);
        Expect.slotHolds(c, 3, "hbm:particle_amat", 1);
        close(c);

        String nbt = c.blockEntity(SX, BEAM_Y, ZC).get("nbt").getAsString();
        Expect.that(nbt.replace(" ", "").contains("hasParticle:0b"), "the source has finished its run; nbt " + nbt);

        c.teleport(DIPOLE - 9.5, Y + 12, ZC + 9.5);
        c.lookAt(DET_X, BEAM_Y, DET_Z + 2);
        c.waitTicks(3);
        c.screenshot("60-dipole-and-detector");

        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture");
    }

    /**
     * Places a multiblock by hand so that its core lands on {@code (cx, cy, cz)} facing {@code facing}.
     * The accelerator parts have no horizontal offset and a height offset (the core sits
     * {@code heightOffset} above the clicked cell), so the clicked cell is the core's column at
     * {@code cy - heightOffset}. A structure void there gives the right-click something replaceable to
     * hit inside the footprint, and the player stands {@code stand} blocks away on the facing side, so
     * the placement yaw resolves to that facing.
     */
    private static void placeMultiblock(Client c, String item, int cx, int cy, int cz, String facing,
                                        int heightOffset, int stand) {
        int fx = facing.equals("east") ? 1 : facing.equals("west") ? -1 : 0;
        int fz = facing.equals("south") ? 1 : facing.equals("north") ? -1 : 0;
        int ky = cy - heightOffset;
        Fixtures.setBlock(c, cx, ky, cz, "minecraft:structure_void");
        Fixtures.giveOnly(c, item, 1);
        c.teleport(cx + fx * stand + 0.5, Y + 1, cz + fz * stand + 0.5);
        c.waitTicks(2);
        c.lookAt(cx + 0.5, ky + 0.5, cz + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(3);
        JsonObject core = c.block(cx, cy, cz);
        Expect.equal(item, core.get("block").getAsString(),
                item + " core at " + cx + "," + cy + "," + cz + " placed by hand facing " + facing);
        Expect.equal("false", core.getAsJsonObject("properties").get("cell").getAsString(),
                item + " at " + cx + "," + cy + "," + cz + " is the core cell, not a dummy");
        Expect.equal(facing, core.getAsJsonObject("properties").get("facing").getAsString(),
                item + " facing");
    }

    private static void prime(Client c, int x, int z, long power) {
        c.cmd("data merge block " + x + " " + BEAM_Y + " " + z
                + " {power:" + power + "L,temperature:100.0f,t0:4000}");
        c.waitTicks(1);
    }

    /**
     * Opens a beam-row machine's menu: stand three blocks north of its core and right-click it (the
     * ray lands on the footprint's north face, which forwards to the core).
     */
    private static void open(Client c, int x, int y, int z, String screen) {
        open(c, x, y, z, x + 0.5, z - 3.5, screen);
    }

    private static void open(Client c, int aimX, int aimY, int aimZ, double standX, double standZ, String screen) {
        c.teleport(standX, Y + 1, standZ);
        c.waitTicks(2);
        Fixtures.interact(c, aimX, aimY, aimZ);
        c.waitScreen(screen, 5_000L);
        Expect.screenStaysOpen(c, screen, 5);
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(2);
    }

    /** Gives one item, opens the machine's menu and moves the item into {@code slot} with real clicks. */
    private static void insert(Client c, int x, int y, int z, String screen, String item, int slot) {
        insert(c, x, y, z, x + 0.5, z - 3.5, screen, item, slot);
    }

    private static void insert(Client c, int x, int y, int z, double standX, double standZ, String screen,
                               String item, int slot) {
        insert(c, x, y, z, standX, standZ, screen, item, slot, true);
    }

    private static void insert(Client c, int x, int y, int z, double standX, double standZ, String screen,
                               String item, int slot, boolean expectInSlot) {
        Fixtures.giveOnly(c, item, 1);
        open(c, x, y, z, standX, standZ, screen);
        int source = hotbarSlotWith(c, item);
        c.clickSlot(source);
        c.waitTicks(2);
        c.clickSlot(slot);
        c.waitTicks(5);
        if (expectInSlot) Expect.slotHolds(c, slot, item, 1);
        Expect.slotEmpty(c, source);
        JsonObject carried = c.screen().getAsJsonObject("carried");
        Expect.that(carried == null || carried.get("empty").getAsBoolean(), item + " left the cursor");
        close(c);
    }

    private static int hotbarSlotWith(Client c, String itemId) {
        JsonObject screen = c.screen();
        for (var e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && !s.get("empty").getAsBoolean()
                    && s.get("id").getAsString().equals(itemId)) return s.get("index").getAsInt();
        }
        throw new IllegalStateException("no player slot holding " + itemId);
    }
}
