import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * A complete dark fusion core, placed by hand and run: the chamber in the middle, the emitter firing
 * east into it, the receiver catching the beam on the far side, the injector feeding deuterium and
 * tritium from the north and the stabiliser holding the field from the south. The exotic core, both
 * catalysts and the stabiliser lens go in through the real menus. Fuel, HE and cryogel are primed
 * with {@code /data merge}, because the tank farm and the power plant are not what is under test.
 *
 * <p>At 100 watts the emitter puts ten thousand joules a tick into the chamber, which is one point of
 * heat; the stabiliser holds the field at fifty, so the reaction runs and nothing detonates. The
 * receiver turns the amplified beam into HE, which is what the readout at the end checks.
 */
public class DfcFullRun implements Scenario {

    private static final int Y = 100;
    private static final int CY = Y + 1;
    private static final int CX = 0, CZ = 0;
    private static final int EMITTER_X = CX - 3, RECEIVER_X = CX + 3, INJECTOR_Z = CZ - 3, STABILIZER_Z = CZ + 3;

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 12);

        // --- build; each aimed part faces the player who placed it, so stand on the chamber's side ---
        JsonObject core = Fixtures.placeOnTop(c, "hbm:dfc_core", CX, Y, CZ);
        Expect.equal("hbm:dfc_core", core.get("block").getAsString(), "chamber placed by hand");
        placeFacing(c, "hbm:dfc_emitter", EMITTER_X, CZ, 3, 0, "east");
        placeFacing(c, "hbm:dfc_receiver", RECEIVER_X, CZ, -3, 0, "west");
        placeFacing(c, "hbm:dfc_injector", CX, INJECTOR_Z, 0, 3, "south");
        placeFacing(c, "hbm:dfc_stabilizer", CX, STABILIZER_Z, 0, -3, "north");
        // A battery on the receiver's free side: the receiver only holds HE for the tick it is made in,
        // so what it pushed into the battery is the evidence that the beam came out as power.
        JsonObject battery = Fixtures.placeOnTop(c, "hbm:machine_battery", RECEIVER_X, Y, CZ + 1);
        Expect.equal("hbm:machine_battery", battery.get("block").getAsString(), "battery placed by hand");

        c.teleport(CX - 6.5, Y + 3, CZ - 6.5);
        c.lookAt(CX, CY, CZ);
        c.waitTicks(2);
        c.screenshot("10-dfc-built");

        // --- the exotic core and both catalysts, through the chamber's menu ---
        insert(c, CX, CZ, "GUICore", "hbm:ams_catalyst_iron", 0);
        insert(c, CX, CZ, "GUICore", "hbm:ams_core_sing", 1);
        insert(c, CX, CZ, "GUICore", "hbm:ams_catalyst_iron", 2);
        open(c, CX, CZ, "GUICore");
        c.screenshot("20-core-loaded");
        close(c);

        // --- the lens, through the stabiliser's menu ---
        insert(c, CX, STABILIZER_Z, "GUICoreStabilizer", "hbm:ams_lens", 0);

        // --- prime: fuel in the injector, HE and cryogel where they are spent ---
        merge(c, CX, INJECTOR_Z, "{fuel1_type:8,fuel1:50000,fuel2_type:9,fuel2:50000}");
        merge(c, CX, STABILIZER_Z, "{power:2500000000L,watts:50}");
        merge(c, RECEIVER_X, CZ, "{tank:64000}");
        c.waitTicks(10);

        String injector = nbt(c, CX, INJECTOR_Z);
        Expect.that(injector.contains("fuel1_type:8") && injector.contains("fuel2_type:9"),
                "injector tanks retyped to deuterium and tritium; nbt " + injector);
        String chamber = nbt(c, CX, CZ);
        Expect.that(chamber.contains("fuel1:50000") && chamber.contains("fuel2:50000"),
                "the injector filled the chamber with both fuels; nbt " + chamber);
        Expect.that(fieldOf(chamber) >= 49, "the stabiliser holds the field; nbt " + chamber);

        // --- fire: 100 watts, on ---
        merge(c, EMITTER_X, CZ, "{power:1000000000L,watts:100,isOn:1b,tank:64000}");
        c.waitTicks(15);
        open(c, EMITTER_X, CZ, "GUICoreEmitter");
        c.screenshot("30-emitter-running");
        close(c);
        open(c, CX, CZ, "GUICore");
        c.screenshot("40-core-running");
        close(c);

        String receiver = nbt(c, RECEIVER_X, CZ);
        String batteryAfter = nbt(c, RECEIVER_X, CZ + 1);
        Expect.that(longOf(batteryAfter, "power") > 0,
                "the receiver turned the beam into HE and charged the battery; battery nbt " + batteryAfter);
        String injectorAfter = nbt(c, CX, INJECTOR_Z);
        Expect.that(longOf(injectorAfter, "fuel1") < 50_000, "the reaction burned deuterium; injector nbt " + injectorAfter);
        Expect.that(longOf(injectorAfter, "fuel2") < 50_000, "the reaction burned tritium; injector nbt " + injectorAfter);
        Expect.block(c, CX, CY, CZ, "hbm:dfc_core");
        String chamberAfter = nbt(c, CX, CZ);
        Expect.that(fieldOf(chamberAfter) >= 49, "the field held through the run; nbt " + chamberAfter);
        Expect.that(longOf(receiver, "tank") < 64_000, "the receiver spent cryogel; nbt " + receiver);

        c.teleport(CX + 6.5, Y + 3, CZ + 6.5);
        c.lookAt(CX, CY, CZ);
        c.waitTicks(2);
        c.screenshot("50-dfc-running");

        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture");
    }

    /**
     * Places a single aimed component on the platform at {@code (x, Y+1, z)} with a real right-click on
     * the top face, standing {@code (dx, dz)} away, so it faces the player (and therefore the chamber).
     */
    private static void placeFacing(Client c, String item, int x, int z, int dx, int dz, String facing) {
        Fixtures.giveOnly(c, item, 1);
        // Stay outside the already-placed core; its ray shape can otherwise intercept the click.
        c.teleport(x + dx * 2.0 / 3.0 + 0.5, Y + 1, z + dz * 2.0 / 3.0 + 0.5);
        c.waitTicks(2);
        c.lookAt(x + 0.5, Y + 0.98, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(3);
        JsonObject placed = c.block(x, CY, z);
        Expect.equal(item, placed.get("block").getAsString(), item + " placed by hand at " + x + "," + CY + "," + z);
        String state = placed.toString();
        Expect.that(state.contains("facing=" + facing) || state.contains("\"facing\":\"" + facing + "\""),
                item + " faces " + facing + "; state " + state);
    }

    private static void merge(Client c, int x, int z, String nbt) {
        c.cmd("data merge block " + x + " " + CY + " " + z + " " + nbt);
        c.waitTicks(1);
    }

    /** The block entity's SNBT with its pretty-printing whitespace removed, so {@code key:value} matches. */
    private static String nbt(Client c, int x, int z) {
        return c.blockEntity(x, CY, z).get("nbt").getAsString().replaceAll("\\s+", "");
    }

    private static long longOf(String nbt, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?<![A-Za-z0-9_])" + key + ":(-?[0-9]+)").matcher(nbt);
        Expect.that(m.find(), key + " present in " + nbt);
        return Long.parseLong(m.group(1));
    }

    private static long fieldOf(String nbt) {
        return longOf(nbt, "field");
    }

    private static void open(Client c, int x, int z, String screen) {
        c.teleport(x + 0.5, Y + 1, z - 2.5);
        c.waitTicks(2);
        Fixtures.interact(c, x, CY, z);
        c.waitScreen(screen, 5_000L);
        Expect.screenStaysOpen(c, screen, 5);
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(2);
    }

    private static void insert(Client c, int x, int z, String screen, String item, int slot) {
        Fixtures.giveOnly(c, item, 1);
        open(c, x, z, screen);
        int source = hotbarSlotWith(c, item);
        c.clickSlot(source);
        c.waitTicks(2);
        c.clickSlot(slot);
        c.waitTicks(5);
        Expect.slotHolds(c, slot, item, 1);
        Expect.slotEmpty(c, source);
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
