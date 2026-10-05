import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An HBM factory that builds a machine, powered by its own coal, and a tour of every harness feature.
 *
 * <pre>
 *   ORE DEPOT (structure template) -- iron, coal blocks, coal, copper
 *        |  extractor + ejector upgrade
 *   ROUTER A  (filters and modes set in its menu with real clicks)
 *     |-- coal + coal blocks --> ROUTER B
 *     |      |-- coal (1/2) -------> ROUTER C --> 3 wood burners --> cable bus --> battery bank --> floor grid
 *     |      '-- coal + blocks ---> ROUTER D
 *     |             |-- coal --------> shredder --> powder coal --.
 *     |             '-- coal blocks -> (wand) --> ROUTER F --> blast furnace fuel (both units)
 *     |-- iron (wand route) ------------------------------------+--> ROUTER E --> blast furnace 1 + 2 (clone)
 *     |                                                                     '--> steel --> press --> steel plates --.
 *     '-- copper (wand route) --> press --> copper plates ----------------------------------------------------------+
 *                                                               motors (parts crate) ---------------------------+
 *                                                                                                                v
 *                                                                ASSEMBLER "ass.shredder" --> shipping crate: a new shredder
 * </pre>
 *
 * <p>Nothing runs on creative batteries. The wood burners burn coal that came through the sorting line, and every
 * powered machine (shredder, both presses, assembler) draws from their cable network. Placement uses every
 * layer: a structure template, {@code /fill} with block states and block-entity data, {@code /clone} of a whole
 * blast-furnace unit, the mod's own conveyor wand for three long routes, {@code /setblock}, and real right-clicks
 * for the multiblocks and single machines.
 *
 * <p>HARNESS_PACE scales the viewer pauses (0 = test speed, 1 = recording).
 */
public class FactoryShowcase implements Scenario {

    private static final int Y = 100, L = 101;

    // depot, sorting
    private static final int DEPOT_OX = -29, DEPOT_OZ = -2;           // template origin (floor level)
    private static final int CRATE_X = -27, CRATE_Z = 0, X0 = -26;     // stocked crate, depot extractor
    private static final int RA_X = -21, RA_Z = 0;                     // router A
    private static final int RB_Z = -5, RC_Z = -10;                    // routers B and C (x = RA_X)
    private static final int RD_X = -15, RD_Z = -5;                    // router D
    private static final int[] BURNERS = {-24, -21, -18};              // burner cores, z = -13
    private static final int BURNER_Z = -13, BUS_Z = -15;
    private static final int SHRED_X = -12, SHRED_Z = -5;
    // blast furnaces: router E on the upper deck, unit 1 east of it, unit 2 cloned three blocks south
    private static final int RE_X = -4, RE_Z = -2, DECK = L + 2;
    private static final int DF_X = -2, DF1_Z = -2, DF2_Z = 1;
    private static final int RF_X = -4, RF_Z = -4, RF_Y = L + 1;       // router F (fuel)
    // presses, assembler, shipping
    private static final int STEEL_X = 2, STEEL_Z = 2, COPPER_X = 2, COPPER_Z = 8;
    private static final int ASM_X = 8, ASM_Z = 5;                      // assembler core
    private static final int PARTS_Z = 0, SHIP_X = 13;

    // oil field, refinery and petrochemicals (all multiblocks placed from the south, so FACING = south)
    private static final int PJ_X = -28, PJ1_Z = 20, PJ2_Z = 26;       // pumpjack cores
    private static final int FA_X = -16, FB_X = -10, HEAT_Z = 22;      // fireboxes (boilers on top)
    private static final int REF_X = -2, REF_Z = 22;                   // refinery
    private static final int TL_X = 6, TL_Z = 21;                      // fraction tower: light oil
    private static final int RP_X = 16, RP_Z = 24;                     // reformer: petroleum
    private static final int CHEM_Z = 30, TN_X = -5, RN_X = 2, TR_X = 8, TNT_X = 12, PVC_X = 16;
    private static final int PEROX_X = 46, PEROX_Z = 14;               // peroxide plant
    private static final int PAD_X = 40, PAD_Z = 10;                   // launch pad
    private static final int MA_X = 31, MA_Z = 4, TA_X = 26;           // missile assembler, Ti/Al press
    private static final int HOUSE_X = 128, HOUSE_Z = 8;               // the target
    private static final int SOUTH_BUS = 13, GRID_B = 36;
    // floor cable switches: the grid tie, then reformer P, reformer N and the chemical plants on their risers
    private static final int TIE_X = 21, TIE_Z = 25, S_RP_Z = 30, S_RN_Z = 34, S_CHEM_Z = 34;
    // the RTG power station on grid A: two RTG furnaces at z = RTG_Z, 15 polonium pellets each
    private static final int[] RTGS = {24, 26};
    private static final int RTG_Z = 24;
    // fluid registry names - a fluid is saved by name, never by its numeric id (History section 19ej-ids)
    private static final String WATER = "hbm:water", OIL = "hbm:oil", HOTOIL = "hbm:hotoil", HEAVY = "hbm:heavyoil",
            HEATING = "hbm:heatingoil", NAPHTHA = "hbm:naphtha", LIGHT = "hbm:lightoil", KERO = "hbm:kerosene",
            PETRO = "hbm:petroleum", PEROX = "hbm:peroxide", H2 = "hbm:hydrogen", AROM = "hbm:aromatics",
            UNSAT = "hbm:unsaturateds", CHLOR = "hbm:chlorine", REFORMATE = "hbm:reformate", XYLENE = "hbm:xylene",
            REFGAS = "hbm:reformgas", DIESEL = "hbm:diesel";

    /** Server tick rate during the long waits; 20 is normal. */
    private static final int FAST_TICKS = 60;

    private static final double PACE = Double.parseDouble(System.getenv().getOrDefault("HARNESS_PACE", "1"));

    /** The client, for the placement helpers. */
    private static Client C;

    /** 1.20.1 spells item NBT {@code Count:<n>b} and item data as NBT, and has no reach attribute. */
    private static boolean LEGACY;

    @Override
    public void run(Client c) throws Exception {
        C = c;
        LEGACY = c.node().startsWith("1.20.1");
        try {
            tour(c);
        } finally {
            // a failed tour must not leave the world without command feedback (it broke a later /locate check)
            c.cmd("gamerule sendCommandFeedback true");
            c.cmd("gamerule send_command_feedback true");
            reach(c, 4.5);
        }
    }

    private void tour(Client c) throws Exception {
        if ("launch".equals(System.getenv("HARNESS_STAGE"))) {   // development aid: re-run the launch only
            houseBlocksBefore = houseBlocks(c);
            clearInventory(c);
            Fixtures.give(c, "hbm:missile_stealth", 1);
            fire(c);
            return;
        }
        c.markLog();
        c.setOption("hide_gui", "false");
        c.setOption("camera", "first_person");
        c.setOption("fov", "80");
        c.setOption("render_distance", "12");

        // =========================================================================================
        // 1. SITE: platform, zones and the underfloor grid, all mass-placed with /fill
        // =========================================================================================
        Fixtures.platform(c, -8, Y, -4, 24);
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("gamerule send_command_feedback false");   // 26.1 spelling
        // The oil field and the missile site extend the ground south and east: forceload, stone, air.
        c.cmd("forceload add -32 -12 56 40");
        c.cmd("forceload add 112 -8 144 24");
        fill(-32, Y, 21, 16, Y, 38, "minecraft:stone");
        fill(-32, L, 21, 16, L + 7, 38, "minecraft:air");
        fill(17, Y, -12, 54, Y, 38, "minecraft:stone");
        fill(17, L, -12, 54, L + 7, 38, "minecraft:air");
        fill(-31, Y, 17, -13, Y, 29, "minecraft:brown_concrete");          // oil field
        fill(-12, Y, 18, 20, Y, 35, "minecraft:cyan_terracotta");          // refinery and petrochemicals
        fill(21, Y, -2, 37, Y, 9, "minecraft:white_concrete");             // missile hall
        fill(37, Y, 6, 51, Y, 18, "minecraft:red_concrete");               // launch site
        fill(-27, Y, -17, -15, Y, -9, "minecraft:yellow_concrete");        // power plant
        fill(-17, Y, -8, -9, Y, -2, "minecraft:gray_concrete");             // coal processing
        fill(-7, Y, -6, 0, Y, 3, "minecraft:black_concrete");               // blast furnaces
        fill(1, Y, 0, 14, Y, 10, "minecraft:light_gray_concrete");         // presses and assembly
        c.chat("Harness demo: an HBM factory that builds a shredder, powered by its own coal");
        Expect.logMatches(c, "Harness demo: an HBM factory", 200);

        startFlying(c);
        title(c, "HBM Factory", "a production line that builds a machine");
        overview(c, "00-site");

        title(c, "Mass placement", "structure template, /fill, /clone");
        // 1.20.1 cannot read the 1.21 template's item stacks (lowercase count, DataVersion 3955): its own copy
        Fixtures.template(c, LEGACY ? "ore_depot_1201" : "ore_depot", DEPOT_OX, Y, DEPOT_OZ);
        Expect.block(c, CRATE_X, L, CRATE_Z, "hbm:crate_steel");
        Expect.that(nbt(c, CRATE_X, L, CRATE_Z).contains("minecraft:coal_block"), "the template brought its stocked crate");
        Expect.block(c, DEPOT_OX, L + 2, DEPOT_OZ, "hbm:steel_beam");
        c.cmd("data modify block " + CRATE_X + " " + L + " " + CRATE_Z + " inventory.Items[{Slot:0b}]."
                + (LEGACY ? "Count set value 12b" : "count set value 12"));
        Expect.that(countIn(nbt(c, CRATE_X, L, CRATE_Z), "minecraft:iron_ingot") == 12, "the depot holds 12 iron; "
                + nbt(c, CRATE_X, L, CRATE_Z));

        // Power: cable bus behind the burners (one /fill) and a battery bank in buffer mode (one /fill with
        // block-entity data). The grid under the floor follows once the machines stand on it.
        fill(-25, L, BUS_Z, -9, L, BUS_Z, "hbm:red_cable");
        fill(-17, L, BUS_Z - 1, -12, L, BUS_Z - 1, "hbm:machine_battery{redLow:1s,redHigh:1s}");
        c.waitTicks(3);
        for (int x = -17; x <= -12; x++) Expect.block(c, x, L, BUS_Z - 1, "hbm:machine_battery");

        // =========================================================================================
        // 2. POWER PLANT: three wood burners placed by hand, fed from router C
        // =========================================================================================
        title(c, "Power plant", "three wood burners, placed by hand");
        for (int x : BURNERS) {
            JsonObject b = placeFrom(c, "hbm:machine_wood_burner", x, BURNER_Z, 0, 3);
            Expect.equal("hbm:machine_wood_burner", b.get("block").getAsString(), "burner core at x=" + x);
            Expect.that(b.toString().contains("south"), "burner at x=" + x + " faces the player; " + b);
        }
        router(RA_X, L, RC_Z, modes(0, 0, 3, 0, 3, 3), "");                 // W / N / E wildcard
        belt(RA_X, L, RC_Z - 1, "north");
        crane("inserter", RA_X, L, RC_Z - 2, "south", "north");
        beltRow(RA_X - 1, RA_X - 2, RC_Z, "west");
        belt(BURNERS[0], L, RC_Z, "north");
        belt(BURNERS[0], L, RC_Z - 1, "north");
        crane("inserter", BURNERS[0], L, RC_Z - 2, "south", "north");
        beltRow(RA_X + 1, RA_X + 2, RC_Z, "east");
        belt(BURNERS[2], L, RC_Z, "north");
        belt(BURNERS[2], L, RC_Z - 1, "north");
        crane("inserter", BURNERS[2], L, RC_Z - 2, "south", "north");

        // =========================================================================================
        // 3. SORTING: depot extractor, router A, router B, router D
        // =========================================================================================
        title(c, "Sorting", "routers split one depot into four streams");
        crane("extractor", X0, L, CRATE_Z, "east", "west");
        // A redstone block holds the depot shut until every machine is configured: redstone stops a crane.
        // Router A drops what it cannot route, and a dropped item that lands on a belt rides it; an open depot
        // before commissioning would send raw coal into the blast furnaces' carbon slots.
        c.cmd("setblock " + X0 + " " + (L + 1) + " " + CRATE_Z + " minecraft:redstone_block");
        fill(X0 + 1, L, 0, RA_X - 1, L, 0, "hbm:conveyor[facing=east]");
        c.cmd("setblock " + RA_X + " " + L + " " + RA_Z + " hbm:crane_router");   // configured in its menu below
        fill(RA_X, L, RA_Z - 1, RA_X, L, RB_Z + 1, "hbm:conveyor[facing=north]");      // coal line
        // B: north blacklists coal blocks, east blacklists nothing -> coal splits, coal blocks go east
        router(RA_X, L, RB_Z, modes(0, 0, 2, 0, 0, 2), slot(10, "minecraft:coal_block"));
        fill(RA_X, L, RB_Z - 1, RA_X, L, RC_Z + 1, "hbm:conveyor[facing=north]");      // to the power plant
        fill(RA_X + 1, L, RB_Z, RD_X - 1, L, RB_Z, "hbm:conveyor[facing=east]");       // to router D
        // D: north whitelists coal blocks (fuel), east whitelists coal (shredder)
        router(RD_X, L, RD_Z, modes(0, 0, 1, 0, 0, 1), slot(10, "minecraft:coal_block") + "," + slot(25, "minecraft:coal"));
        belt(RD_X + 1, L, RD_Z, "east");
        crane("inserter", RD_X + 2, L, RD_Z, "west", "east");
        JsonObject shred = placeFrom(c, "hbm:machine_shredder", SHRED_X, SHRED_Z, 1, 2);
        Expect.equal("hbm:machine_shredder", shred.get("block").getAsString(), "shredder placed by hand");
        crane("extractor", SHRED_X, L, SHRED_Z + 1, "south", "north");
        fill(SHRED_X, L, SHRED_Z + 2, SHRED_X, L, -1, "hbm:conveyor[facing=south]");  // powder joins the iron line

        // =========================================================================================
        // 4. BLAST FURNACES: build unit 1, clone it, then router E and router F
        // =========================================================================================
        title(c, "Blast furnaces", "one unit by hand, the second by /clone");
        crane("extractor", DF_X, L, DF1_Z, "east", "up");
        belt(DF_X + 1, L, DF1_Z, "south");
        sneakPlaceOnTop(c, "hbm:machine_difurnace_off", DF_X, L, DF1_Z, DF_X + 2.5, DF1_Z + 0.5);
        Expect.block(c, DF_X, L + 1, DF1_Z, "hbm:machine_difurnace_off");
        crane("inserter", DF_X, DECK, DF1_Z, "west", "down");
        belt(DF_X - 1, DECK, DF1_Z, "east");
        crane("inserter", DF_X, RF_Y, DF1_Z - 1, "north", "south");
        belt(DF_X, RF_Y, DF1_Z - 2, "south");
        belt(DF_X - 1, RF_Y, DF1_Z - 2, "east");
        c.waitTicks(2);
        overviewOf(c, "10-unit-one", DF_X + 0.5, L + 1, DF1_Z + 0.5, 7);
        // One command copies the whole 3 x 3 x 3 unit, machines and cranes included, three blocks south.
        c.cmd("clone " + (DF_X - 1) + " " + L + " " + (DF1_Z - 2) + " " + (DF_X + 1) + " " + DECK + " " + DF1_Z
                + " " + (DF_X - 1) + " " + L + " " + (DF2_Z - 2));
        c.waitTicks(3);
        Expect.block(c, DF_X, L + 1, DF2_Z, "hbm:machine_difurnace_off");
        Expect.block(c, DF_X, DECK, DF2_Z, "hbm:crane_inserter");
        Expect.block(c, DF_X, L, DF2_Z, "hbm:crane_extractor");
        fill(DF_X + 1, L, DF1_Z + 1, DF_X + 1, L, DF2_Z - 1, "hbm:conveyor[facing=south]");  // join both outputs
        belt(DF_X + 1, L, DF2_Z + 1, "east");                  // (0, 2) is the splitter, placed with the presses
        // Router E: iron and powder in from the west, out east (unit 1) and south (unit 2)
        router(RE_X, DECK, RE_Z, modes(0, 0, 0, 3, 0, 3), "");
        belt(RE_X, DECK, RE_Z + 1, "south");
        belt(RE_X, DECK, RE_Z + 2, "south");
        belt(RE_X, DECK, RE_Z + 3, "east");
        // Router F: coal blocks in from the north, out east (unit 1 fuel) and south (unit 2 fuel)
        router(RF_X, RF_Y, RF_Z, modes(0, 0, 0, 3, 0, 3), "");
        belt(RF_X, RF_Y, RF_Z + 1, "south");
        belt(RF_X, RF_Y, RF_Z + 2, "south");
        belt(RF_X, RF_Y, RF_Z + 3, "east");
        c.cmd("setblock " + RE_X + " " + L + " " + RE_Z + " hbm:steel_scaffold");

        // =========================================================================================
        // 5. PRESSES AND ASSEMBLY
        // =========================================================================================
        title(c, "Assembly hall", "two presses and an assembler, by hand");
        JsonObject sp = placeFrom(c, "hbm:machine_epress", STEEL_X, STEEL_Z, -2, 0);
        Expect.equal("hbm:machine_epress", sp.get("block").getAsString(), "steel press core placed by hand");
        JsonObject cp = placeFrom(c, "hbm:machine_epress", COPPER_X, COPPER_Z, -2, 0);
        Expect.equal("hbm:machine_epress", cp.get("block").getAsString(), "copper press core placed by hand");
        // Assembler: offset 1, so aiming at x=7 from the west puts the core at x=8.
        placeFrom(c, "hbm:machine_assembly_machine", ASM_X - 1, ASM_Z, -2, 0);
        Expect.block(c, ASM_X, L, ASM_Z, "hbm:machine_assembly_machine");
        Expect.that(!c.block(ASM_X + 1, L + 2, ASM_Z + 1).get("block").getAsString().equals("minecraft:air"), "assembler footprint filled");
        // steel: press -> assembler (west face, north cell)
        crane("inserter", STEEL_X - 1, L, STEEL_Z, "west", "east");
        // A conveyor splitter deals the steel 2 : 1, left lane to the press, right lane to the bolt stock.
        JsonObject split = placeFrom(c, "hbm:crane_splitter", STEEL_X - 2, STEEL_Z, -2, 0);
        Expect.equal("hbm:crane_splitter", split.get("block").getAsString(), "splitter placed by hand");
        Expect.that(c.block(STEEL_X - 2, L, STEEL_Z + 1).get("block").getAsString().equals("hbm:crane_splitter"),
                "the splitter's right lane lies south of its core");
        c.cmd("data merge block " + (STEEL_X - 2) + " " + L + " " + STEEL_Z + " {left:2b,right:1b}");
        crane("inserter", STEEL_X - 1, L, STEEL_Z + 1, "west", "east");
        c.cmd("setblock " + STEEL_X + " " + L + " " + (STEEL_Z + 1) + " hbm:crate_steel");
        crane("extractor", STEEL_X + 1, L, STEEL_Z, "east", "west");
        beltRow(STEEL_X + 2, STEEL_X + 3, STEEL_Z, "east");
        belt(ASM_X - 2, L, STEEL_Z, "south");
        belt(ASM_X - 2, L, STEEL_Z + 1, "south");
        crane("inserter", ASM_X - 2, L, ASM_Z - 1, "north", "east");
        // copper: press -> assembler (west face, south cell)
        crane("inserter", COPPER_X - 1, L, COPPER_Z, "west", "east");
        crane("extractor", COPPER_X + 1, L, COPPER_Z, "east", "west");
        beltRow(COPPER_X + 2, COPPER_X + 3, COPPER_Z, "east");
        belt(ASM_X - 2, L, COPPER_Z, "north");
        belt(ASM_X - 2, L, COPPER_Z - 1, "north");
        crane("inserter", ASM_X - 2, L, ASM_Z + 1, "south", "east");
        // motors: parts crate -> assembler (north face)
        c.cmd("setblock " + ASM_X + " " + L + " " + PARTS_Z + " " + items("hbm:crate_steel{inventory:{Items:[{Slot:0b,id:\"hbm:motor\",count:2}]}}"));
        crane("extractor", ASM_X, L, PARTS_Z + 1, "south", "north");
        belt(ASM_X, L, PARTS_Z + 2, "south");
        crane("inserter", ASM_X, L, PARTS_Z + 3, "north", "south");
        // shipping
        crane("extractor", ASM_X + 2, L, ASM_Z, "east", "west");
        belt(ASM_X + 3, L, ASM_Z, "east");
        crane("inserter", ASM_X + 4, L, ASM_Z, "west", "east");
        c.cmd("setblock " + SHIP_X + " " + L + " " + ASM_Z + " hbm:crate_steel");
        fill(RA_X, L, RA_Z + 1, RA_X, L, 5, "hbm:conveyor[facing=south]");   // copper line, first leg
        // The underfloor grid, laid after the machines were placed on solid floor: a spine from the bus and one
        // branch under each powered machine (a machine takes power through its bottom face).
        fill(-9, Y, BUS_Z, -9, Y, 8, "hbm:red_cable");
        fill(SHRED_X, Y, SHRED_Z, -10, Y, SHRED_Z, "hbm:red_cable");
        fill(-8, Y, STEEL_Z, STEEL_X, Y, STEEL_Z, "hbm:red_cable");
        fill(-8, Y, COPPER_Z, COPPER_X, Y, COPPER_Z, "hbm:red_cable");
        fill(-8, Y, ASM_Z, ASM_X + 2, Y, ASM_Z, "hbm:red_cable");
        fill(ASM_X + 2, Y, ASM_Z + 1, ASM_X + 2, Y, ASM_Z + 1, "hbm:red_cable");
        c.cmd("setblock " + (ASM_X + 2) + " " + L + " " + (ASM_Z + 1) + " hbm:red_cable");   // side port (x+2, z+1)
        c.waitTicks(3);
        Expect.block(c, -9, Y, 0, "hbm:red_cable");

        // =========================================================================================
        // 6. THE CONVEYOR WAND: three long routes, two clicks each
        // =========================================================================================
        title(c, "Conveyor wand", "three routes, two clicks each");
        giveOnly(c, "hbm:conveyor_wand_regular", 1);
        // iron: from router A's east side up onto the deck, into router E
        wandStart(c, RA_X + 1, 0, RA_X + 1.5, 2.5);
        wandEnd(c, RE_X - 0.02, DECK + 0.5, RE_Z + 0.5, RE_X - 3.5, RE_Z + 0.5);
        Expect.that(c.log(20, "Conveyor built!").size() > 0, "the wand reports the iron route built");
        Expect.block(c, RE_X - 1, DECK, RE_Z, "hbm:conveyor_lift");
        // coal blocks: from router D's north side, up into router F
        wandStart(c, RD_X, RD_Z - 1, RD_X + 0.5, RD_Z - 3.5);
        wandEnd(c, RF_X + 0.5, RF_Y + 0.5, RF_Z - 0.02, RF_X + 0.5, RF_Z - 4.5);
        Expect.block(c, RF_X, RF_Y, RF_Z - 1, "hbm:conveyor_lift");
        // copper: from the end of the first leg to the copper press inserter
        wandStart(c, RA_X, 6, RA_X - 1.5, 6.5);
        wandEnd(c, COPPER_X - 1 - 0.02, L + 0.5, COPPER_Z + 0.5, COPPER_X - 4.5, COPPER_Z + 1.5);
        Expect.that(c.block(COPPER_X - 2, L, COPPER_Z).get("block").getAsString().startsWith("hbm:conveyor"),
                "the copper route ends at the press inserter");
        overview(c, "20-built");

        // =========================================================================================
        // 6b. OIL FIELD AND REFINERY: every multiblock by hand, from the south
        // =========================================================================================
        title(c, "Oil field", "two pumpjacks over an oil pocket");
        fill(PJ_X - 1, 95, PJ1_Z - 1, PJ_X + 1, 97, PJ1_Z + 1, "hbm:ore_oil");     // the pockets under the heads
        fill(PJ_X - 1, 95, PJ2_Z - 1, PJ_X + 1, 97, PJ2_Z + 1, "hbm:ore_oil");
        for (int z : new int[]{PJ2_Z, PJ1_Z}) {
            JsonObject pj = placeFrom(c, "hbm:machine_pumpjack", PJ_X, z, 0, 2);
            Expect.equal("hbm:machine_pumpjack", pj.get("block").getAsString(), "pumpjack core at z=" + z);
        }
        title(c, "Refinery", "fireboxes, heat boilers, a refinery, a fraction tower");
        for (int x : new int[]{FA_X, FB_X}) {
            placeSouth(c, "hbm:heater_firebox", x, HEAT_Z);
            Expect.block(c, x, L, HEAT_Z, "hbm:heater_firebox");
            // The boiler goes on the south cell of the firebox with a sneak-click (a plain click opens the firebox).
            giveOnly(c, "hbm:heat_boiler", 1);
            standAt(c, x + 0.5, L, HEAT_Z + 4.5);
            sneakUse(c, x + 0.5, L + 0.98, HEAT_Z + 1.5);
            Expect.block(c, x, L + 1, HEAT_Z, "hbm:heat_boiler");
        }
        placeSouth(c, "hbm:machine_refinery", REF_X, REF_Z);
        Expect.block(c, REF_X, L, REF_Z, "hbm:machine_refinery");
        placeSouth(c, "hbm:machine_fraction_tower", TL_X, TL_Z);
        Expect.block(c, TL_X, L, TL_Z, "hbm:machine_fraction_tower");

        title(c, "Petrochemicals", "reformers, towers and chemical plants");
        placeSouth(c, "hbm:machine_catalytic_reformer", RP_X, RP_Z);
        Expect.block(c, RP_X, L, RP_Z, "hbm:machine_catalytic_reformer");
        placeSouth(c, "hbm:machine_fraction_tower", TN_X, CHEM_Z);
        placeSouth(c, "hbm:machine_catalytic_reformer", RN_X, CHEM_Z);
        placeSouth(c, "hbm:machine_fraction_tower", TR_X, CHEM_Z);
        placeSouth(c, "hbm:machine_chemical_plant", TNT_X, CHEM_Z);
        placeSouth(c, "hbm:machine_chemical_plant", PVC_X, CHEM_Z);
        for (int x : new int[]{TN_X, TR_X}) Expect.block(c, x, L, CHEM_Z, "hbm:machine_fraction_tower");
        Expect.block(c, RN_X, L, CHEM_Z, "hbm:machine_catalytic_reformer");
        for (int x : new int[]{TNT_X, PVC_X}) Expect.block(c, x, L, CHEM_Z, "hbm:machine_chemical_plant");

        title(c, "Missile hall", "a stealth missile assembler and a plate press");
        placeSouth(c, "hbm:machine_assembly_machine", MA_X, MA_Z);
        Expect.block(c, MA_X, L, MA_Z, "hbm:machine_assembly_machine");
        JsonObject ta = placeFrom(c, "hbm:machine_epress", TA_X, MA_Z, -2, 0);
        Expect.equal("hbm:machine_epress", ta.get("block").getAsString(), "titanium/aluminium press placed by hand");
        // The base load: polonium RTG furnaces, 5 HE/t per heat unit, 15 x 50 heat = 3,750 HE/t each. Polonium
        // halves in 3.3 million ticks, so the output is constant for the whole demo, whatever the oil chain does.
        title(c, "Power station", "two RTG furnaces, 7,500 HE/t");
        for (int x : RTGS) {
            JsonObject r = placeFrom(c, "hbm:machine_rtg_grey", x, RTG_Z, 0, 2);
            Expect.equal("hbm:machine_rtg_grey", r.get("block").getAsString(), "RTG furnace placed by hand at x=" + x);
        }
        fill(RTGS[0] - 1, L, RTG_Z - 1, RTGS[1], L, RTG_Z - 1, "hbm:red_cable");     // an RTG outputs on all six faces
        riser(RTGS[0] - 1, RTG_Z - 1);
        title(c, "Launch site", "a launch pad and a peroxide plant");
        placeSouth(c, "hbm:launch_pad", PAD_X, PAD_Z);
        Expect.block(c, PAD_X, L, PAD_Z, "hbm:launch_pad");
        placeSouth(c, "hbm:machine_chemical_plant", PEROX_X, PEROX_Z);
        Expect.block(c, PEROX_X, L, PEROX_Z, "hbm:machine_chemical_plant");
        overviewOf(c, "21-oil-machines", -2.5, L, 18.5, 30);

        // ---- single blocks: generators, storage, sinks, cranes -------------------------------------
        fill(TL_X + 3, L, TL_Z - 1, TL_X + 5, L, TL_Z - 1, "hbm:machine_diesel");    // 3 on the light-oil diesel
        fill(-8, L, 34, -3, L, 34, "hbm:machine_diesel");                           // 6 on the naphtha diesel
        c.cmd("setblock " + TL_X + " " + L + " " + (TL_Z - 2) + " hbm:barrel_steel{mode:0s,tank_type:\"" + KERO + "\"}");
        sink(REF_X - 2, L, REF_Z - 1, HEAVY);                                        // heavy oil
        sink(RP_X - 1, L, RP_Z - 2, H2);
        sink(RP_X + 1, L, RP_Z - 2, REFGAS);
        sink(RN_X - 1, L, CHEM_Z - 2, PETRO);
        sink(RN_X + 1, L, CHEM_Z - 2, H2);
        sink(TN_X, L, CHEM_Z - 2, HEATING);
        sink(TR_X, L, CHEM_Z - 2, XYLENE);
        c.cmd("setblock 20 " + L + " " + CHEM_Z + " hbm:barrel_steel{mode:2s,tank:16000,tank_type:\"" + CHLOR + "\"}");
        c.cmd("setblock " + (PEROX_X + 4) + " " + L + " " + PEROX_Z + " hbm:barrel_steel{mode:2s,tank_type:\"" + WATER + "\""
                + items(",inventory:{Items:[{Slot:2b,id:\"hbm:inf_water_mk2\",count:1}]}}"));
        crane("extractor", TNT_X, L, CHEM_Z + 2, "south", "north");
        crane("extractor", PVC_X, L, CHEM_Z + 2, "south", "north");
        // missile hall: Ti/Al stock -> press -> assembler; dye and circuits -> assembler; output -> hangar
        c.cmd("setblock " + (TA_X - 4) + " " + L + " " + MA_Z + " hbm:crate_steel{inventory:{Items:["
                + items("{Slot:0b,id:\"hbm:ingot_titanium\",count:20},{Slot:1b,id:\"hbm:ingot_aluminium\",count:20}]}}"));
        crane("extractor", TA_X - 3, L, MA_Z, "east", "west");
        belt(TA_X - 2, L, MA_Z, "east");
        crane("inserter", TA_X - 1, L, MA_Z, "west", "east");
        crane("extractor", TA_X + 1, L, MA_Z, "east", "west");
        belt(TA_X + 2, L, MA_Z, "east");
        crane("inserter", TA_X + 3, L, MA_Z, "west", "east");
        c.cmd("setblock " + MA_X + " " + L + " " + (MA_Z - 5) + " hbm:crate_steel{inventory:{Items:["
                + items("{Slot:0b,id:\"minecraft:black_dye\",count:16},{Slot:1b,id:\"hbm:circuit_advanced\",count:4}]}}"));
        crane("extractor", MA_X, L, MA_Z - 4, "south", "north");
        belt(MA_X, L, MA_Z - 3, "south");
        crane("inserter", MA_X, L, MA_Z - 2, "north", "south");
        crane("inserter", MA_X, L, MA_Z + 2, "south", "north");
        crane("extractor", MA_X + 2, L, MA_Z, "east", "west");
        belt(MA_X + 3, L, MA_Z, "east");
        crane("inserter", MA_X + 4, L, MA_Z, "west", "east");
        c.cmd("setblock " + (MA_X + 5) + " " + L + " " + MA_Z + " hbm:crate_steel");        // missile hangar
        c.cmd("setblock " + (TA_X + 1) + " " + L + " " + (MA_Z + 4) + " minecraft:crafting_table");
        fill(TNT_X, L, CHEM_Z + 3, MA_X - 1, L, CHEM_Z + 3, "hbm:conveyor[facing=east]"); // TNT + PVC belt

        // ---- the machines take their fluid types BEFORE the ducts: a duct looks for receivers only when typed
        title(c, "Fluid types", "identifiers on boilers and towers, menus for reformers");
        for (int x : new int[]{FA_X, FB_X}) {
            identify(c, OIL, x + 0.5, L + 2.5, HEAT_Z + 1.98, x + 0.5, L + 1, HEAT_Z + 4.5);   // boiler south face
            Expect.that(nbt(c, x, L + 1, HEAT_Z).contains("water_type:\"" + OIL + "\""), "boiler at x=" + x + " takes crude oil");
        }
        identify(c, LIGHT, TL_X + 0.5, L + 0.5, TL_Z + 0.5, TL_X + 0.5, L + 1, TL_Z + 3.5);
        identify(c, NAPHTHA, TN_X + 0.5, L + 0.5, CHEM_Z + 0.5, TN_X + 0.5, L + 1, CHEM_Z + 3.5);
        identify(c, REFORMATE, TR_X + 0.5, L + 0.5, CHEM_Z + 0.5, TR_X + 0.5, L + 1, CHEM_Z + 3.5);
        Expect.that(nbt(c, TL_X, L, TL_Z).contains("tank0_type:\"" + LIGHT + "\""), "light-oil tower typed; " + nbt(c, TL_X, L, TL_Z));
        Expect.that(nbt(c, TN_X, L, CHEM_Z).contains("tank0_type:\"" + NAPHTHA + "\""), "naphtha tower typed");
        Expect.that(nbt(c, TR_X, L, CHEM_Z).contains("tank0_type:\"" + REFORMATE + "\""), "reformate tower typed");
        // Reformers: the fluid identifier and the catalytic converter go in through the menu.
        reformer(c, RP_X, RP_Z, PETRO);
        reformer(c, RN_X, CHEM_Z, NAPHTHA);
        // Chemical plants: recipes through the selector, typed searches.
        selectRecipe(c, PEROX_X, PEROX_Z + 1, 0, 2.5, "GUIMachineChemicalPlant", "peroxide", "chem.peroxide", PEROX_X, PEROX_Z);
        selectRecipe(c, PVC_X, CHEM_Z + 1, 0, 2.5, "GUIMachineChemicalPlant", "pvc", "chem.pvc", PVC_X, CHEM_Z);
        selectRecipe(c, TNT_X, CHEM_Z + 1, 0, 2.5, "GUIMachineChemicalPlant", "tnt", "chem.tnt", TNT_X, CHEM_Z);

        // ---- pipes: /fill, then one identifier click per duct ------------------------------------------
        title(c, "Pipework", "every duct typed with a fluid identifier");
        pipe(c, OIL, new int[][]{{-24, L, 22, -24, L, 24}, {-23, L, 24, -12, L, 24}, {-18, L, 22, -18, L, 23}, {-18, L + 1, 22, -18, L + 1, 22},
                {-12, L, 22, -12, L, 23}, {-12, L + 1, 22, -12, L + 1, 22}});
        pipe(c, HOTOIL, new int[][]{{-14, L + 1, 22, -14, L + 1, 22}, {-14, L, 20, -14, L, 22}, {-13, L, 20, -3, L, 20},
                {-8, L, 21, -8, L, 22}, {-8, L + 1, 22, -8, L + 1, 22}});
        pipe(c, LIGHT, new int[][]{{0, L, 21, 4, L, 21}});
        pipe(c, PETRO, new int[][]{{0, L, 23, 0, L, 24}, {1, L, 24, 13, L, 24}});
        pipe(c, NAPHTHA, new int[][]{{-3, L, 24, -3, L, 30}, {-2, L, 30, -1, L, 30}});
        pipe(c, UNSAT, new int[][]{{15, L, 26, 15, L, 28}});
        pipe(c, REFORMATE, new int[][]{{5, L, 30, 6, L, 30}});
        pipe(c, AROM, new int[][]{{10, L, 30, 10, L, 30}});
        pipe(c, CHLOR, new int[][]{{18, L, 30, 19, L, 30}});
        pipe(c, DIESEL, new int[][]{{8, L, 21, 11, L, 21}});                                         // diesel 1
        pipe(c, DIESEL, new int[][]{{-5, L, 32, -5, L, 32}, {-8, L, 33, -2, L, 33}});               // diesel 2
        pipe(c, WATER, new int[][]{{PEROX_X + 2, L, PEROX_Z, PEROX_X + 3, L, PEROX_Z}});

        // ---- overflow valves: a buffer-mode barrel is a LOW-priority receiver, so with a bottomless barrel in its
        // unload slot it takes only what every machine on the line leaves. No stalled consumer can back the refinery
        // up into the heat boilers, which explode when their hot-oil output has nowhere to go.
        overflow(-6, L, 21, HOTOIL);                        // on the hot-oil row
        overflow(2, L, 22, LIGHT);                          // light oil
        overflow(-2, L, 25, NAPHTHA);                       // naphtha
        overflow(5, L, 25, PETRO);                          // petroleum
        overflow(12, L, 21, DIESEL);                        // diesel 1
        overflow(-1, L, 33, DIESEL);                        // diesel 2

        // ---- power: two grids under the floor ------------------------------------------------------------
        // Grid A (coal burners, battery bank, light-oil diesel): the old factory, pumpjack 1 and the refinery.
        // Grid B (naphtha diesel, its own batteries): pumpjack 2, both reformers and the PVC and TNT plants. The
        // reformers buffer a million HE each; on one grid with the pumpjacks they would starve the whole start.
        fill(-30, Y, SOUTH_BUS, 48, Y, SOUTH_BUS, "hbm:red_cable");
        fill(-9, Y, 8, -9, Y, SOUTH_BUS, "hbm:red_cable");                                            // joins the old spine
        riser(-26, PJ1_Z + 2);
        c.cmd("setblock -26 " + L + " " + (PJ1_Z + 2) + " hbm:red_cable");                            // pumpjack 1
        riser(REF_X + 1, REF_Z - 2);
        c.cmd("setblock " + (REF_X + 1) + " " + L + " " + (REF_Z - 2) + " hbm:red_cable");
        fill(TL_X + 3, Y, TL_Z - 1, TL_X + 5, Y, TL_Z - 1, "hbm:red_cable");
        riser(TL_X + 4, TL_Z - 1);                                                                    // diesel bank 1
        fill(-26, Y, GRID_B, 21, Y, GRID_B, "hbm:red_cable");                                        // grid B bus
        fill(-8, Y, 34, -3, Y, 34, "hbm:red_cable");                                                  // diesel bank 2
        fill(-5, Y, 35, -5, Y, 35, "hbm:red_cable");                                                  // bank 2 -> grid B bus
        fill(-11, L, GRID_B, -10, L, GRID_B, "hbm:machine_battery{redLow:1s,redHigh:1s}");            // grid B buffer
        fill(-26, Y, PJ2_Z + 2, -26, Y, GRID_B, "hbm:red_cable");
        c.cmd("setblock -26 " + L + " " + (PJ2_Z + 2) + " hbm:red_cable");                            // pumpjack 2
        // Each big consumer hangs off grid B through a cable switch set into the floor, all open at the start.
        switchedRiser(c, RP_X + 3, RP_Z, S_RP_Z);                                                    // reformer P
        c.cmd("setblock " + (RP_X + 3) + " " + L + " " + RP_Z + " hbm:red_cable");
        switchedRiser(c, RN_X - 1, CHEM_Z + 2, S_RN_Z);                                              // reformer N
        c.cmd("setblock " + (RN_X - 1) + " " + L + " " + (CHEM_Z + 2) + " hbm:red_cable");
        switchedRiser(c, 14, CHEM_Z, S_CHEM_Z);                                                      // TNT + PVC
        c.cmd("setblock 14 " + L + " " + CHEM_Z + " hbm:red_cable");
        // The tie between the grids: a switch in the floor between the south bus and the grid B bus.
        fill(TIE_X, Y, SOUTH_BUS, TIE_X, Y, TIE_Z - 1, "hbm:red_cable");
        c.cmd("setblock " + TIE_X + " " + Y + " " + TIE_Z + " hbm:cable_switch[on=false]");
        fill(TIE_X, Y, TIE_Z + 1, TIE_X, Y, GRID_B, "hbm:red_cable");
        c.waitTicks(3);

        // ---- the TNT + PVC belt goes north to the assembler: one more wand route ---------------------
        giveOnly(c, "hbm:conveyor_wand_regular", 1);
        wandStart(c, MA_X, CHEM_Z + 3, MA_X + 1.5, CHEM_Z + 5.5);
        wandEnd(c, MA_X + 0.5, L + 0.5, MA_Z + 2.98, MA_X + 0.5, MA_Z + 6.5);
        Expect.that(c.block(MA_X, L, MA_Z + 3).get("block").getAsString().startsWith("hbm:conveyor"),
                "the wand route ends at the assembler's south inserter");

        // ---- the target: a house on its own island, 90 blocks east --------------------------------------
        title(c, "The target", "a house, 90 blocks east");
        view(c, HOUSE_X - 9.5, L + 6, HOUSE_Z - 9.5, HOUSE_X + 3.0, L + 2.0, HOUSE_Z + 3.0);
        fill(HOUSE_X - 6, Y, HOUSE_Z - 6, HOUSE_X + 11, Y, HOUSE_Z + 11, "minecraft:grass_block");
        pause(c, 10);
        house(c, HOUSE_X, HOUSE_Z);
        houseBlocksBefore = houseBlocks(c);
        Expect.that(houseBlocksBefore >= 30, "the house stands (" + houseBlocksBefore + " of 32 sampled blocks)");
        standAt(c, HOUSE_X - 7.5, L + 5, HOUSE_Z - 7.5);
        c.lookAt(HOUSE_X + 3.0, L + 2.0, HOUSE_Z + 3.0);
        pause(c, 30);
        c.screenshot("22-target-house");
        overviewOf(c, "23-full-site", 8.5, L, 12.5, 44);

        // =========================================================================================
        // 7. COMMISSIONING through the machines' own menus
        // =========================================================================================
        title(c, "Commissioning", "configuring machines through their menus");
        // Router A: ghost filters by real clicks with the item on the cursor, modes by the side buttons.
        configureRouterA(c);
        // Depot extractor: an ejector upgrade (one item every 5 ticks instead of 20).
        insertByClicks(c, X0, L, CRATE_Z, 0, 2.5, "GUICraneExtractor", "hbm:upgrade_ejector_2", 1, 19);
        // Shredder blades: shift-click, the menu routes them to the blade slots.
        clearInventory(c);
        Fixtures.give(c, "hbm:blades_steel", 2);
        open(c, SHRED_X, L, SHRED_Z, 0, -2.5, "GUIMachineShredder");
        for (int i = 0; i < 2; i++) {
            c.slotClick(playerSlotWith(c, "hbm:blades_steel"), 0, "QUICK_MOVE");
            c.waitTicks(3);
            pause(c, 6);
        }
        Expect.slotHolds(c, 27, "hbm:blades_steel", 1);
        Expect.slotHolds(c, 28, "hbm:blades_steel", 1);
        close(c);
        // Press stamps.
        insertByClicks(c, STEEL_X, L, STEEL_Z, 0, -2.5, "GUIMachineEPress", "hbm:stamp_iron_plate", 1, 1);
        insertByClicks(c, COPPER_X, L, COPPER_Z, 0, 2.5, "GUIMachineEPress", "hbm:stamp_iron_plate", 1, 1);
        // Speed upgrades, Mk.III, into every machine that takes one (the presses in slot 4, assemblers in slot 2).
        insertByClicks(c, STEEL_X, L, STEEL_Z, 0, -2.5, "GUIMachineEPress", "hbm:upgrade_speed_3", 1, 4);
        insertByClicks(c, COPPER_X, L, COPPER_Z, 0, 2.5, "GUIMachineEPress", "hbm:upgrade_speed_3", 1, 4);
        insertByClicks(c, ASM_X - 1, L, ASM_Z, -2.5, 0, "GUIMachineAssemblyMachine", "hbm:upgrade_speed_3", 1, 2);
        // Assembler: the recipe selector, scrolled, searched by typing, picked by a click.
        selectRecipe(c, ASM_X - 1, ASM_Z, -2.5, 0, "GUIMachineAssemblyMachine", "shredder", "ass.shredder", ASM_X, ASM_Z);
        // Burners: the middle one through its menu's power button, the other two by /data merge.
        open(c, BURNERS[1], L + 1, BURNER_Z, 0, 3.5, "GUIMachineWoodBurner");   // upper cell: the ray passes over the inserter
        JsonObject bs = c.screen();
        c.click(bs.get("left").getAsInt() + 61, bs.get("top").getAsInt() + 24);
        c.waitTicks(4);
        pause(c, 15);
        c.screenshot("30-burner-menu");
        close(c);
        Expect.that(nbt(c, BURNERS[1], L, BURNER_Z).contains("isOn:1b"), "the burner's power button switched it on");
        for (int x : new int[]{BURNERS[0], BURNERS[2]}) c.cmd("data merge block " + x + " " + L + " " + BURNER_Z + " {isOn:1b}");

        commissionOil(c);

        // =========================================================================================
        // 8. RUN
        // =========================================================================================
        title(c, "Production start", "the depot opens");
        // Break the redstone block with a real left-click: the depot extractor starts.
        clearInventory(c);
        c.hotbar(9);
        standAt(c, X0 + 0.5, L + 1, CRATE_Z + 3.5);
        c.lookAt(X0, L + 1, CRATE_Z);
        pause(c, 15);
        c.attack(2);
        c.waitTicks(3);
        Expect.block(c, X0, L + 1, CRATE_Z, "minecraft:air");
        lightFireboxes(c);
        long started = System.currentTimeMillis();
        long startTick = c.status().get("game_time").getAsLong();
        int maxBelt = 0;
        // Watch each chain come alive; the camera follows the stage being waited for.
        maxBelt = Math.max(maxBelt, waitFor(c, "the burners making power", 1_200, () -> longOf(nbt(c, BURNERS[1], L, BURNER_Z), "power") > 0,
                -21.5, L + 4, -5.5, -21, L, -12));
        c.screenshot("40-power-plant");
        maxBelt = Math.max(maxBelt, waitFor(c, "the pumpjacks drawing power", 1_200, () -> longOf(nbt(c, PJ_X, L, PJ1_Z), "power") > 0
                || longOf(nbt(c, PJ_X, L, PJ1_Z), "t0") > 0, PJ_X + 3.5, L + 6, PJ1_Z - 8.5, PJ_X + 3, L, PJ1_Z + 3));
        maxBelt = Math.max(maxBelt, waitFor(c, "powder coal from the shredder", 1_200, () -> (nbt(c, DF_X, L + 1, DF1_Z) + nbt(c, DF_X, L + 1, DF2_Z)
                + nbt(c, SHRED_X, L, SHRED_Z)).contains("hbm:powder_coal"), SHRED_X + 0.5, L + 3, SHRED_Z + 5.5, SHRED_X, L, SHRED_Z));
        maxBelt = Math.max(maxBelt, waitFor(c, "both blast furnaces fuelled", 1_200, () -> longOf(nbt(c, DF_X, L + 1, DF1_Z), "powerTime") > 0
                && longOf(nbt(c, DF_X, L + 1, DF2_Z), "powerTime") > 0, DF_X + 4.5, L + 4, DF1_Z - 4.5, DF_X, L + 1, 0));
        c.screenshot("50-blast-furnaces");
        maxBelt = Math.max(maxBelt, watchOil(c));
        maxBelt = Math.max(maxBelt, waitFor(c, "steel from the blast furnaces", 3_000, () -> (nbt(c, DF_X, L + 1, DF1_Z) + nbt(c, DF_X, L + 1, DF2_Z)
                + nbt(c, STEEL_X, L, STEEL_Z)).contains("hbm:ingot_steel") || nbt(c, STEEL_X, L, STEEL_Z).contains("hbm:plate_steel"),
                DF_X + 4.5, L + 4, DF1_Z - 4.5, DF_X, L + 1, 0));
        maxBelt = Math.max(maxBelt, waitFor(c, "copper plates at the assembler", 3_000, () -> nbt(c, ASM_X, L, ASM_Z).contains("hbm:plate_copper"),
                COPPER_X + 0.5, L + 3, COPPER_Z + 5.5, COPPER_X + 2, L, COPPER_Z));

        // Tour while the steel builds up: walk the hall holding W, then a third-person shot without the HUD.
        c.chat("Harness demo: walking the assembly hall");
        standAt(c, -6.5, L + 1, 11.5);
        c.look(-90, 15);
        c.keyDown("key.forward");
        for (int i = 0; i < 3; i++) {
            c.waitTicks(10);
            maxBelt = Math.max(maxBelt, beltItems(c));
        }
        c.keyUp("key.forward");
        Expect.that(c.player().get("x").getAsDouble() > -4, "holding W moved the player east");
        c.setOption("camera", "third_person_back");
        c.setOption("hide_gui", "true");
        standAt(c, -1.5, L + 3, 12.5);
        c.lookAt(ASM_X + 0.5, L + 1.0, ASM_Z + 0.5);
        pause(c, 40);
        c.screenshot("60-third-person");
        c.setOption("camera", "first_person");
        c.setOption("hide_gui", "false");

        // The long wait: the camera tours the stations, one every 10 s, while the plates build up.
        double[][] tour = {
                {ASM_X - 4.5, L + 4, ASM_Z - 6.5, ASM_X, L, ASM_Z},                 // assembly hall
                {DF_X + 4.5, L + 4, DF1_Z - 4.5, DF_X, L + 1, 0},                   // blast furnaces
                {-21.5, L + 4, -5.5, -21, L, -12},                                  // power plant
                {STEEL_X + 0.5, L + 3, STEEL_Z - 4.5, STEEL_X + 2, L, STEEL_Z}};    // steel press
        maxBelt = Math.max(maxBelt, waitFor(c, "eight steel plates in the assembler", 6_000,
                () -> countIn(nbt(c, ASM_X, L, ASM_Z), "hbm:plate_steel") >= 8 || nbt(c, SHIP_X, L, ASM_Z).contains("hbm:machine_shredder"),
                tour));
        title(c, "Assembling", "8 steel plates, 4 copper plates, 2 motors");
        open(c, ASM_X - 1, L, ASM_Z, -2.5, 0, "GUIMachineAssemblyMachine");
        pause(c, 20);
        c.screenshot("70-assembler-menu");
        close(c);
        maxBelt = Math.max(maxBelt, waitFor(c, "a shredder in the shipping crate", 1_200,
                () -> nbt(c, SHIP_X, L, ASM_Z).contains("hbm:machine_shredder"), SHIP_X - 2.5, L + 3, ASM_Z - 4.5, SHIP_X, L, ASM_Z));
        long ticks = c.status().get("game_time").getAsLong() - startTick;
        long seconds = (System.currentTimeMillis() - started) / 1000;
        Expect.that(maxBelt > 0, "the entities query saw items riding the belts (max " + maxBelt + ")");

        // The power numbers behind it: nothing came from a creative battery.
        long bank = 0;
        for (int x = -17; x <= -12; x++) bank += longOf(nbt(c, x, L, BUS_Z - 1), "power");
        c.chat("Harness demo: shredder built in " + ticks + " ticks (" + seconds + " s), battery bank holds " + bank + " HE");
        Expect.logMatches(c, "shredder built in", 200);

        // =========================================================================================
        // 9. THE PRODUCT: take the new shredder out and place it by hand
        // =========================================================================================
        title(c, "Delivered", "a shredder, made by the factory");
        open(c, SHIP_X, L, ASM_Z, 0, 2.5, "GUICrate");
        Expect.slotHolds(c, 0, "hbm:machine_shredder", 1);
        JsonObject item = c.slot(0);
        c.mouseMove(item.get("center_x").getAsDouble(), item.get("center_y").getAsDouble());
        c.waitTicks(3);
        pause(c, 20);
        Expect.equal(0, c.screen().get("hovered_slot").getAsInt(), "the cursor hovers the shredder");
        c.screenshot("80-shipping-crate");
        c.slotClick(0, 0, "QUICK_MOVE");
        c.waitTicks(3);
        close(c);
        Expect.inventoryHolds(c, "hbm:machine_shredder", 1);
        c.hotbar(hotbarOf(c, "hbm:machine_shredder"));
        standAt(c, SHIP_X + 0.5, L, ASM_Z + 3.5);
        c.lookAt(SHIP_X + 0.5, Y + 0.98, ASM_Z + 1.5);
        c.waitTicks(2);
        pause(c, 10);
        c.use(2);
        c.waitTicks(3);
        Expect.block(c, SHIP_X, L, ASM_Z + 1, "hbm:machine_shredder");

        // =========================================================================================
        // 9b. THE STEALTH MISSILE: parts from the oil chain, bolts by hand, the assembler, the pad
        // =========================================================================================
        double[][] missileTour = {
                {MA_X - 5.5, L + 5, MA_Z + 8.5, MA_X, L, MA_Z},                     // missile hall
                {PVC_X - 4.5, L + 6, CHEM_Z + 9.5, TNT_X + 2, L + 1, CHEM_Z},       // chemical plants
                {REF_X - 6.5, L + 7, REF_Z - 9.5, REF_X, L + 3, REF_Z},             // refinery
                {PJ_X + 3.5, L + 6, PJ1_Z - 8.5, PJ_X + 3, L, PJ1_Z + 3}};          // oil field
        // Control room: the grid tie and the reformers go on until the chemical plants hold their feedstock, then
        // the reformers go off (they burn 20 kHE per operation on naphtha nobody needs) and the plants go on.
        title(c, "Control room", "floor switches route the power");
        connectMissileSite(c);
        flip(c, "grid tie", TIE_X, TIE_Z, true);
        flip(c, "reformer P power", RP_X + 3, S_RP_Z, true);
        flip(c, "reformer N power", RN_X - 1, S_RN_Z, true);
        double[][] petroTour = {
                {PVC_X - 4.5, L + 6, CHEM_Z + 9.5, RP_X, L + 1, RP_Z + 3},
                {RN_X - 6.5, L + 6, CHEM_Z + 8.5, TR_X, L + 1, CHEM_Z}};
        statusLines = true;
        waitFor(c, "feedstock for the PVC and TNT plants", 4_000, () ->
                longOf(nbt(c, PVC_X, L, CHEM_Z), "i0") >= 2_000 && longOf(nbt(c, TNT_X, L, CHEM_Z), "i0") >= 500, petroTour);
        statusLines = false;
        flip(c, "reformer P power", RP_X + 3, S_RP_Z, false);
        flip(c, "reformer N power", RN_X - 1, S_RN_Z, false);
        flip(c, "chemical plant power", 14, S_CHEM_Z, true);
        c.screenshot("44-control-room");

        title(c, "Missile parts", "PVC, TNT and Ti/Al plates on their way");
        waitFor(c, "titanium and aluminium plates at the missile assembler", 3_000, () -> {
            String a = nbt(c, MA_X, L, MA_Z);
            return countIn(a, "hbm:plate_titanium") >= 20 && countIn(a, "hbm:plate_aluminium") >= 20;
        }, missileTour);
        waitFor(c, "TNT at the missile assembler", 6_000, () -> countIn(nbt(c, MA_X, L, MA_Z), "hbm:ball_tnt") >= 4, missileTour);
        waitFor(c, "16 PVC bars at the missile assembler", 8_000, () -> countIn(nbt(c, MA_X, L, MA_Z), "hbm:ingot_pvc") >= 16, missileTour);
        craftBolts(c);
        waitFor(c, "a stealth missile in the hangar", 3_000, () -> nbt(c, MA_X + 5, L, MA_Z).contains("hbm:missile_stealth"),
                new double[][]{{MA_X - 5.5, L + 5, MA_Z + 8.5, MA_X, L, MA_Z}});
        launch(c);

        // =========================================================================================
        // 10. PERSISTENCE: leave the world, join again, the factory is still there
        // =========================================================================================
        title(c, "Save and reload", "leave the world and come back");
        pause(c, 20);
        c.leaveWorld(60_000L);
        Expect.that(!c.inWorld(), "the client left the world");
        c.joinWorld("harness", 180_000L);
        c.waitTicks(20);
        Expect.block(c, SHIP_X, L, ASM_Z + 1, "hbm:machine_shredder");
        Expect.block(c, DF_X, L + 1, DF2_Z, "hbm:machine_difurnace_off");
        long bankAfter = 0;
        for (int x = -17; x <= -12; x++) bankAfter += longOf(nbt(c, x, L, BUS_Z - 1), "power");
        Expect.that(bankAfter > 0, "the battery bank kept its charge across the reload (" + bankAfter + " HE)");
        startFlying(c);
        overview(c, "90-finale");
        standAt(c, -8.5, L + 44, -3.5);
        c.look(0, 90);
        c.setOption("hide_gui", "true");
        pause(c, 40);
        c.screenshot("95-top-down");
        c.setOption("hide_gui", "false");

        c.setOption("fov", "70");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture");
    }

    // ---- building blocks ---------------------------------------------------------------------

    private static void fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        C.cmd("fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2 + " " + z2 + " " + block);
    }

    private static void belt(int x, int y, int z, String facing) {
        C.cmd("setblock " + x + " " + y + " " + z + " hbm:conveyor[facing=" + facing + "]");
    }

    private static void beltRow(int x1, int x2, int z, String facing) {
        fill(Math.min(x1, x2), L, z, Math.max(x1, x2), L, z, "hbm:conveyor[facing=" + facing + "]");
    }

    private static void crane(String kind, int x, int y, int z, String input, String output) {
        C.cmd("setblock " + x + " " + y + " " + z + " hbm:crane_" + kind + "[input=" + input + ",output=" + output + "]");
    }

    private static String modes(int d, int u, int n, int s, int w, int e) {
        return "modes:[I;" + d + "," + u + "," + n + "," + s + "," + w + "," + e + "]";
    }

    private static String slot(int slot, String id) {
        return items("{Slot:" + slot + "b,id:\"" + id + "\",count:1}");
    }

    /** A router with its modes (D, U, N, S, W, E) and filters (slot = side * 5 + k). */
    private static void router(int x, int y, int z, String modes, String filters) {
        C.cmd("setblock " + x + " " + y + " " + z + " hbm:crane_router{" + modes + ",inventory:{Items:[" + filters + "]}}");
    }

    /** Right-clicks the floor's top face at (x, z) by hand, standing (dx, dz) away. Returns the block placed. */
    private static JsonObject placeFrom(Client c, String item, int x, int z, int dx, int dz) {
        giveOnly(c, item, 1);
        standAt(c, x + dx + 0.5, L, z + dz + 0.5);
        c.lookAt(x + 0.5, Y + 0.98, z + 0.5);
        c.waitTicks(2);
        pause(c, 6);
        c.use(2);
        c.waitTicks(4);
        return c.block(x, L, z);
    }

    /** Sneak-right-click on a crane's top face: sneaking skips the crane's menu, so the block is placed. */
    private static void sneakPlaceOnTop(Client c, String item, int x, int y, int z, double px, double pz) {
        giveOnly(c, item, 1);
        standAt(c, px, L, pz);
        sneakUse(c, x + 0.5, y + 0.98, z + 0.5);
    }

    private static void sneakUse(Client c, double tx, double ty, double tz) {
        c.keyDown("key.sneak");
        c.waitTicks(5);                  // lands the player if flying; the eye drops to sneak height
        Expect.that(c.player().get("sneaking").getAsBoolean(), "holding the sneak key makes the player sneak");
        c.lookAt(tx, ty, tz);
        c.waitTicks(2);
        pause(c, 6);
        c.use(2);
        c.waitTicks(3);
        c.keyUp("key.sneak");
        c.waitTicks(2);
    }

    /** First wand click: the floor's top face at (x, z); the route's first belt goes on that cell. */
    private static void wandStart(Client c, int x, int z, double px, double pz) {
        c.hotbar(1);
        standAt(c, px, L + 1, pz);
        c.lookAt(x + 0.5, Y + 0.98, z + 0.5);
        c.waitTicks(2);
        pause(c, 8);
        c.use(2);
        c.waitTicks(3);
    }

    /** Second wand click, sneaking, on a face point: sneaking skips the target's menu and builds the route. */
    private static void wandEnd(Client c, double tx, double ty, double tz, double px, double pz) {
        standAt(c, px, L, pz);
        sneakUse(c, tx, ty, tz);
        c.waitTicks(3);
        pause(c, 20);
    }

    private static void configureRouterA(Client c) {
        clearInventory(c);
        for (String id : new String[]{"minecraft:coal", "minecraft:coal_block", "minecraft:iron_ingot", "minecraft:copper_ingot"}) {
            Fixtures.give(c, id, 1);
        }
        open(c, RA_X, L, RA_Z, -2.5, 2.5, "GUICraneRouter");
        // side * 5 + k: north = 2, south = 3, east = 5
        ghost(c, "minecraft:coal", 10);
        ghost(c, "minecraft:coal_block", 11);
        ghost(c, "minecraft:iron_ingot", 25);
        ghost(c, "minecraft:copper_ingot", 15);
        // mode buttons: index j * 3 + k at (7 + j * 222, 16 + k * 26); one click = whitelist
        JsonObject s = c.screen();
        int left = s.get("left").getAsInt(), top = s.get("top").getAsInt();
        for (int index : new int[]{2, 3, 5}) {
            int j = index / 3, k = index % 3;
            c.click(left + 7 + j * 222 + 9, top + 16 + k * 26 + 9);
            c.waitTicks(3);
            pause(c, 6);
        }
        pause(c, 15);
        c.screenshot("25-router-menu");
        close(c);
        String n = nbt(c, RA_X, L, RA_Z);
        Expect.that(n.contains("modes:[I;0,0,1,1,0,1]"), "router A modes set by its buttons; " + n);
        Expect.that(n.contains("minecraft:coal_block") && n.contains("minecraft:copper_ingot"), "router A filters set by clicks; " + n);
    }

    /** Carries the item from the player's inventory, clicks the ghost slot, puts the item back. */
    private static void ghost(Client c, String item, int slot) {
        int source = playerSlotWith(c, item);
        c.clickSlot(source);
        c.waitTicks(2);
        c.clickSlot(slot);
        c.waitTicks(2);
        c.clickSlot(source);
        c.waitTicks(2);
        pause(c, 4);
        Expect.slotHolds(c, slot, item, 1);
    }

    /**
     * The recipe selector of an assembler or chemical plant: scroll the list once, click the search box, type
     * {@code term}, then click the matching icons until the machine holds {@code recipe}.
     */
    private static void selectRecipe(Client c, int x, int z, double dx, double dz, String screen, String term, String recipe,
                                     int coreX, int coreZ) {
        open(c, x, L + 1, z, dx, dz, screen);   // middle layer: clear of the cranes in front
        JsonObject s = c.screen();
        int left = s.get("left").getAsInt(), top = s.get("top").getAsInt();
        c.click(left + 16, top + 134);                         // the recipe button
        c.waitScreen("RecipeSelector", 5_000L);
        JsonObject search = editBox(c);
        int gl = search.get("x").getAsInt() - 28, gt = search.get("y").getAsInt() - 111;
        c.mouseMove(gl + 80, gt + 60);
        c.scroll(-1);                                          // page through the list once
        c.waitTicks(3);
        pause(c, 10);
        c.scroll(1);
        c.click(search.get("center_x").getAsDouble(), search.get("center_y").getAsDouble());
        c.waitTicks(2);
        c.type(term);
        c.waitTicks(3);
        Expect.equal(term, editBox(c).get("value").getAsString(), "the search box holds the typed text");
        pause(c, 15);
        c.screenshot("27-search-" + term);
        String want = "recipe0:\"" + recipe + "\"";
        for (int i = 0; i < 8; i++) {                          // the first match that is the wanted recipe
            c.click(gl + 16 + 18 * i, gt + 26);
            c.waitTicks(2);
            c.click(gl + 160, gt + 98);                        // close: sends the selection
            c.waitTicks(4);
            if (nbt(c, coreX, L, coreZ).contains(want)) break;
            if (c.screenClass() == null) open(c, x, L + 1, z, dx, dz, screen);   // middle layer: clear of the cranes in front
            if (!c.screenClass().contains("RecipeSelector")) {
                c.click(left + 16, top + 134);
                c.waitScreen("RecipeSelector", 5_000L);
            }
            JsonObject box = editBox(c);
            c.click(box.get("center_x").getAsDouble(), box.get("center_y").getAsDouble());
            c.type(term);
            c.waitTicks(2);
        }
        if (c.screenClass() != null) close(c);
        Expect.that(nbt(c, coreX, L, coreZ).contains(want), recipe + " picked in the selector; " + nbt(c, coreX, L, coreZ));
    }

    private static JsonObject editBox(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("widgets")) {
            JsonObject w = e.getAsJsonObject();
            if (w.get("class").getAsString().contains("EditBox")) return w;
        }
        throw new IllegalStateException("no text field on " + c.screenClass());
    }

    // ---- oil and missile helpers -----------------------------------------------------------------

    private static int houseBlocksBefore;

    /** A multiblock clicked on the floor one cell south of its core, the player two blocks further south. */
    private static void placeSouth(Client c, String item, int coreX, int coreZ) {
        placeFrom(c, item, coreX, coreZ + 1, 0, 2);
    }

    /** A plain right-click with a fluid identifier at a face point, from a stand point. */
    private static void identify(Client c, String fluid, double tx, double ty, double tz, double px, double py, double pz) {
        giveOnly(c, identifier(fluid), 1);
        standAt(c, px, py, pz);
        c.lookAt(tx, ty, tz);
        c.waitTicks(2);
        pause(c, 6);
        c.use(2);
        c.waitTicks(3);
    }

    /** Reformer through its menu: the fluid identifier into slot 9, the catalytic converter into slot 10. */
    private static void reformer(Client c, int x, int z, String fluid) {
        clearInventory(c);
        Fixtures.give(c, identifier(fluid), 1);
        Fixtures.give(c, "hbm:catalytic_converter", 1);
        open(c, x, L, z + 1, 0, 2.5, "GUIMachineCatalyticReformer");
        moveTo(c, "hbm:fluid_identifier", 9);
        moveTo(c, "hbm:catalytic_converter", 10);
        pause(c, 10);
        close(c);
        c.waitTicks(3);
        Expect.that(nbt(c, x, L, z).contains("input_type:\"" + fluid + "\""), "reformer at x=" + x + " reads its identifier; " + nbt(c, x, L, z));
    }

    /** Real clicks: pick the stack up from the player inventory, put it down in {@code slot}. */
    private static void moveTo(Client c, String item, int slot) {
        int source = playerSlotWith(c, item);
        c.clickSlot(source);
        c.waitTicks(2);
        c.clickSlot(slot);
        c.waitTicks(3);
        pause(c, 4);
        Expect.that(!c.slot(slot).get("empty").getAsBoolean(), item + " went into slot " + slot);
    }

    /** Fills each box with ducts, then types every duct with one identifier click. */
    private static void pipe(Client c, String fluid, int[][] boxes) {
        for (int[] b : boxes) fill(b[0], b[1], b[2], b[3], b[4], b[5], "hbm:fluid_duct_neo");
        c.waitTicks(2);
        typeDucts(c, fluid, boxes);
    }

    private static final int[][] CLICK_FROM = {{0, 1, 0}, {0, 1, 2}, {0, 1, -2}, {2, 1, 0}, {-2, 1, 0}, {0, 0, 2}, {0, 0, -2}, {2, 0, 0}, {-2, 0, 0}};

    /**
     * Types a pipe network with a fluid identifier: one sneak-click on the first duct spreads the type through the
     * connected run, then a plain click on any duct still untyped. The player hovers above or beside a duct at a
     * free spot and aims at its centre; every duct is checked at the end.
     */
    private static void typeDucts(Client c, String fluid, int[][] boxes) {
        giveOnly(c, identifier(fluid), 1);
        // One sneak-click on the first duct retypes the whole connected run.
        int[] b0 = boxes[0];
        for (int[] o : CLICK_FROM) {
            int sx = b0[0] + o[0], sy = b0[1] + o[1], sz = b0[2] + o[2];
            if (!air(c, sx, sy, sz) || !air(c, sx, sy + 1, sz)) continue;
            standAt(c, sx + 0.5, sy, sz + 0.5);
            sneakUse(c, b0[0] + 0.5, b0[1] + 0.5, b0[2] + 0.5);
            if (ductType(c, b0[0], b0[1], b0[2]).equals(fluid)) break;
        }
        // Plain clicks for anything the spread did not reach (a separate run, or a missed aim).
        for (int[] b : boxes) {
            for (int x = Math.min(b[0], b[3]); x <= Math.max(b[0], b[3]); x++)
                for (int y = Math.min(b[1], b[4]); y <= Math.max(b[1], b[4]); y++)
                    for (int z = Math.min(b[2], b[5]); z <= Math.max(b[2], b[5]); z++) {
                        if (!ductType(c, x, y, z).equals("hbm:none")) continue;
                        for (int[] o : CLICK_FROM) {
                            int sx = x + o[0], sy = y + o[1], sz = z + o[2];
                            if (!air(c, sx, sy, sz) || !air(c, sx, sy + 1, sz)) continue;
                            standAt(c, sx + 0.5, sy, sz + 0.5);
                            c.lookAt(x + 0.5, y + 0.5, z + 0.5);
                            c.waitTicks(1);
                            c.use(1);
                            c.waitTicks(2);
                            if (ductType(c, x, y, z).equals(fluid)) break;
                        }
                        Expect.equal(fluid, ductType(c, x, y, z), "duct at " + x + "," + y + "," + z + " typed");
                    }
        }
    }

    private static String ductType(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        Expect.equal("hbm:fluid_duct_neo", be.get("block").getAsString(), "a duct at " + x + "," + y + "," + z);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("type:\"([^\"]+)\"")
                .matcher(be.get("nbt").getAsString().replaceAll("\\s+", ""));
        return m.find() ? m.group(1) : "hbm:none";
    }

    private static boolean air(Client c, int x, int y, int z) {
        return c.block(x, y, z).get("block").getAsString().equals("minecraft:air");
    }

    /** An infinite drain: a barrel of one fluid with a bottomless barrel in its unload slot. */
    private static void sink(int x, int y, int z, String fluid) {
        C.cmd("setblock " + x + " " + y + " " + z + " hbm:barrel_steel{mode:0s,tank_type:\"" + fluid + "\""
                + items(",inventory:{Items:[{Slot:4b,id:\"hbm:fluid_barrel_infinite\",count:1}]}}"));
    }

    /** A low-priority overflow drain beside a pipe: buffer mode (LOW priority) plus a bottomless barrel. */
    private static void overflow(int x, int y, int z, String fluid) {
        C.cmd("setblock " + x + " " + y + " " + z + " hbm:barrel_steel{mode:1s,tank_type:\"" + fluid + "\""
                + items(",inventory:{Items:[{Slot:4b,id:\"hbm:fluid_barrel_infinite\",count:1}]}}"));
    }

    /** A floor cable from (x, z) to the grid B bus with an open cable switch at z = {@code switchZ}. */
    private static void switchedRiser(Client c, int x, int z, int switchZ) {
        fill(x, Y, z, x, Y, switchZ - 1, "hbm:red_cable");
        c.cmd("setblock " + x + " " + Y + " " + switchZ + " hbm:cable_switch[on=false]");
        fill(x, Y, switchZ + 1, x, Y, GRID_B, "hbm:red_cable");
    }

    /** Flips a floor switch with a real right-click from above, empty-handed, and checks its new state. */
    private static void flip(Client c, String what, int x, int z, boolean on) {
        clearInventory(c);
        c.hotbar(9);
        standAt(c, x + 0.5, L + 1, z + 2.5);
        c.lookAt(x + 0.5, Y + 0.95, z + 0.5);
        c.waitTicks(2);
        pause(c, 8);
        c.use(2);
        c.waitTicks(3);
        Expect.that(c.block(x, Y, z).get("state").getAsString().contains("on=" + on), what + " switched " + (on ? "on" : "off"));
        c.chat("Harness demo: " + what + (on ? " ON" : " OFF"));
    }

    /** A floor cable from the south bus to (x, z). */
    private static void riser(int x, int z) {
        fill(x, Y, Math.min(z, SOUTH_BUS), x, Y, Math.max(z, SOUTH_BUS), "hbm:red_cable");
    }

    private static void house(Client c, int x, int z) {
        fill(x, L, z, x + 5, L, z + 5, "minecraft:cobblestone");                      // floor first, then walls
        pause(c, 8);
        fill(x, L, z, x + 5, L + 3, z + 5, "minecraft:oak_planks");
        pause(c, 8);
        fill(x + 1, L, z + 1, x + 4, L + 3, z + 4, "minecraft:air");
        fill(x, L, z, x, L + 3, z, "minecraft:oak_log");
        fill(x + 5, L, z, x + 5, L + 3, z, "minecraft:oak_log");
        fill(x, L, z + 5, x, L + 3, z + 5, "minecraft:oak_log");
        fill(x + 5, L, z + 5, x + 5, L + 3, z + 5, "minecraft:oak_log");
        pause(c, 8);
        fill(x - 1, L + 4, z - 1, x + 6, L + 4, z + 6, "minecraft:dark_oak_planks");
        pause(c, 6);
        fill(x, L + 5, z, x + 5, L + 5, z + 5, "minecraft:dark_oak_planks");
        fill(x + 1, L + 6, z + 1, x + 4, L + 6, z + 4, "minecraft:dark_oak_planks");
        c.cmd("setblock " + (x + 2) + " " + L + " " + z + " minecraft:oak_door[half=lower,facing=north]");
        c.cmd("setblock " + (x + 2) + " " + (L + 1) + " " + z + " minecraft:oak_door[half=upper,facing=north]");
        fill(x, L + 1, z + 2, x, L + 2, z + 3, "minecraft:glass_pane");
        fill(x + 5, L + 1, z + 2, x + 5, L + 2, z + 3, "minecraft:glass_pane");
        fill(x + 2, L + 1, z + 5, x + 3, L + 2, z + 5, "minecraft:glass_pane");
        fill(x + 4, L + 4, z + 4, x + 4, L + 7, z + 4, "minecraft:bricks");               // chimney
        c.cmd("setblock " + (x + 1) + " " + L + " " + (z + 1) + " minecraft:crafting_table");
        c.cmd("setblock " + (x + 4) + " " + L + " " + (z + 1) + " minecraft:furnace");
        c.cmd("setblock " + (x + 1) + " " + L + " " + (z + 4) + " minecraft:red_bed[facing=north,part=head]");
        c.cmd("setblock " + (x + 1) + " " + L + " " + (z + 5 - 2) + " minecraft:red_bed[facing=north,part=foot]");
        c.waitTicks(3);
    }

    /** Sampled house blocks: walls, corners, roof and chimney. 32 block queries, about a second. */
    private static final int[][] HOUSE_SAMPLES = {
            {0, 0, 0}, {5, 0, 0}, {0, 0, 5}, {5, 0, 5}, {0, 3, 0}, {5, 3, 0}, {0, 3, 5}, {5, 3, 5},
            {1, 1, 0}, {3, 2, 0}, {4, 1, 0}, {1, 2, 5}, {4, 1, 5}, {0, 1, 1}, {0, 2, 4}, {5, 1, 1}, {5, 2, 4},
            {-1, 4, -1}, {6, 4, -1}, {-1, 4, 6}, {6, 4, 6}, {2, 4, 2}, {1, 5, 1}, {4, 5, 4}, {2, 5, 3}, {2, 6, 2},
            {3, 6, 3}, {4, 6, 4}, {4, 7, 4}, {1, 0, 1}, {4, 0, 1}, {3, 0, 3}};

    private static int houseBlocks(Client c) {
        int n = 0;
        for (int[] o : HOUSE_SAMPLES) if (!air(c, HOUSE_X + o[0], L + o[1], HOUSE_Z + o[2])) n++;
        return n;
    }

    /** Right-clicks a block empty-handed from an exact stand point (for machines with something above them). */
    private static void openAt(Client c, int x, int y, int z, double px, double py, double pz, String screen) {
        c.hotbar(9);
        standAt(c, px, py, pz);
        Fixtures.interact(c, x, y, z);
        c.waitScreen(screen, 5_000L);
        Expect.screenStaysOpen(c, screen, 5);
    }

    // ---- commissioning of the oil and missile site ------------------------------------------------

    private static void commissionOil(Client c) {
        title(c, "Commissioning", "the petrochemical plant and the missile hall");
        insertByClicks(c, PVC_X, L + 1, CHEM_Z + 1, 0, 2.5, "GUIMachineChemicalPlant", "hbm:powder_cadmium", 8, 4);
        insertByClicks(c, TNT_X, L + 1, CHEM_Z + 1, 0, 2.5, "GUIMachineChemicalPlant", "hbm:niter", 1, 4);
        open(c, PVC_X, L + 1, CHEM_Z + 1, 0, 2.5, "GUIMachineChemicalPlant");
        pause(c, 15);
        c.screenshot("31-pvc-plant-menu");
        close(c);
        insertByClicks(c, TA_X, L, MA_Z, 0, 2.5, "GUIMachineEPress", "hbm:stamp_iron_plate", 1, 1);
        insertByClicks(c, TA_X - 3, L, MA_Z, 0, 2.5, "GUICraneExtractor", "hbm:upgrade_ejector_2", 1, 19);
        insertByClicks(c, TA_X, L, MA_Z, 0, 2.5, "GUIMachineEPress", "hbm:upgrade_speed_3", 1, 4);
        insertByClicks(c, MA_X, L + 1, MA_Z + 1, 0, 2.5, "GUIMachineAssemblyMachine", "hbm:upgrade_speed_3", 1, 2);
        for (int[] chem : new int[][]{{PVC_X, CHEM_Z}, {TNT_X, CHEM_Z}, {PEROX_X, PEROX_Z}}) {
            insertByClicks(c, chem[0], L + 1, chem[1] + 1, 0, 2.5, "GUIMachineChemicalPlant", "hbm:upgrade_speed_3", 1, 2);
        }
        // The pumpjacks' upgrade slots (5..7) through NBT; their inventories are empty at this point.
        for (int z : new int[]{PJ1_Z, PJ2_Z}) {
            c.cmd("data merge block " + PJ_X + " " + L + " " + z + items(" {inventory:{Items:[{Slot:5b,id:\"hbm:upgrade_speed_3\",count:1}]}}"));
        }
        Expect.that(nbt(c, PJ_X, L, PJ1_Z).contains("hbm:upgrade_speed_3"), "pumpjack 1 carries a speed upgrade");
        selectRecipe(c, MA_X, MA_Z + 1, 0, 2.5, "GUIMachineAssemblyMachine", "stealth", "ass.stealthmissile", MA_X, MA_Z);
        loadRtgs(c);
    }

    /** Coal into both fireboxes through their menus: the heat that starts the whole oil chain. */
    private static void lightFireboxes(Client c) {
        for (int x : new int[]{FA_X, FB_X}) {
            clearInventory(c);
            Fixtures.give(c, "minecraft:coal", 4);
            // From the ground: a higher eye would see the boiler standing on the firebox. The crude-oil duct runs
            // along z = HEAT_Z + 2 right in front of the firebox's south face, so aim high on that face (y + 0.9):
            // the ray passes over the duct, whose pipe ends at y + 0.69. Aiming at the block corner hit the duct.
            c.hotbar(9);
            standAt(c, x + 0.5, L, HEAT_Z + 4.5);
            c.lookAt(x + 0.5, L + 0.9, HEAT_Z + 1.98);
            c.waitTicks(1);
            c.use(2);
            c.waitTicks(3);
            c.waitScreen("GUIFirebox", 5_000L);
            Expect.screenStaysOpen(c, "GUIFirebox", 5);
            moveTo(c, "minecraft:coal", 0);
            pause(c, 10);
            if (x == FA_X) c.screenshot("41-firebox-menu");
            close(c);
        }
    }

    /** Missile hall, peroxide plant and pad join grid A once the diesel generators run. */
    private static void connectMissileSite(Client c) {
        title(c, "Missile site power", "the hall and the pad join the grid");
        fill(TA_X, Y, MA_Z, MA_X + 2, Y, MA_Z, "hbm:red_cable");                                      // press + assembler
        fill(MA_X + 2, Y, MA_Z + 1, MA_X + 2, Y, MA_Z + 1, "hbm:red_cable");
        c.cmd("setblock " + (MA_X + 2) + " " + L + " " + (MA_Z + 1) + " hbm:red_cable");            // side port (x+2, z+1)
        riser(MA_X, MA_Z);
        riser(PEROX_X, PEROX_Z + 2);
        c.cmd("setblock " + PEROX_X + " " + L + " " + (PEROX_Z + 2) + " hbm:red_cable");
        riser(PAD_X - 1, PAD_Z - 2);
        c.cmd("setblock " + (PAD_X - 1) + " " + L + " " + (PAD_Z - 2) + " hbm:red_cable");
        c.waitTicks(3);
        Expect.block(c, MA_X, Y, MA_Z + 5, "hbm:red_cable");
    }

    /** The RTG furnaces: 15 polonium pellets each, shift-clicked in through the menu (pellets do not stack). */
    private static void loadRtgs(Client c) {
        for (int x : RTGS) {
            clearInventory(c);
            for (int i = 0; i < 15; i++) C.cmd("give @s hbm:pellet_rtg_polonium 1");
            c.waitTicks(2);
            open(c, x, L, RTG_Z, 0, 2.5, "GUIMachineRTG");
            for (int i = 0; i < 15; i++) {
                c.slotClick(playerSlotWith(c, "hbm:pellet_rtg_polonium"), 0, "QUICK_MOVE");
                c.waitTicks(1);
            }
            pause(c, 10);
            if (x == RTGS[0]) c.screenshot("32-rtg-menu");
            close(c);
            // 15 pellets x 50 = 750, capped at 1.7 heatMax 600 (decaying pellets; History 19ej-8)
            Expect.equal(600L, longOf(nbt(c, x, L, RTG_Z), "heat"), "RTG heat at x=" + x + " (15 pellets x 50, capped at heatMax 600)");
        }
    }

    /** The oil chain coming alive, stage by stage, with the camera on each stage, at the fast tick rate. */
    private static int watchOil(Client c) throws Exception {
        c.cmd("tick rate " + FAST_TICKS);
        statusLines = true;
        try {
            return watchOilStages(c);
        } finally {
            statusLines = false;
            c.cmd("tick rate 20");
        }
    }

    /** Posted every 100 ticks during the oil waits: the tank levels nobody can see inside the pipes. */
    private static boolean statusLines;

    private static void oilStatus(Client c) {
        String r = nbt(c, REF_X, L, REF_Z);
        c.chat("Harness demo: refinery hot oil " + longOf(r, "input") + " mB, sulfur " + longOf(r, "sulfur")
                + "; kerosene " + longOf(nbt(c, TL_X, L, TL_Z - 2), "tank") + " mB; bank "
                + longOf(nbt(c, -13, L, BUS_Z - 1), "power") + " HE");
    }

    private static int watchOilStages(Client c) throws Exception {
        int most = 0;
        // The crude leaves the pumpjacks as fast as it comes up, so look for it in the boilers.
        most = Math.max(most, waitFor(c, "crude oil reaching the boilers", 1_600,
                () -> longOf(nbt(c, FA_X, L + 1, HEAT_Z), "water") > 0 || longOf(nbt(c, FB_X, L + 1, HEAT_Z), "water") > 0,
                PJ_X + 3.5, L + 6, PJ1_Z - 8.5, PJ_X + 3, L, PJ1_Z + 3));
        c.screenshot("42-pumpjacks");
        most = Math.max(most, waitFor(c, "hot oil in the refinery", 1_600, () -> {
            String r = nbt(c, REF_X, L, REF_Z);
            return longOf(r, "input") > 0 || longOf(r, "light") > 0;
        }, REF_X - 6.5, L + 7, REF_Z - 9.5, REF_X, L + 3, REF_Z));
        c.screenshot("43-refinery");
        // Everything after the refinery runs in the background; the tour below shows it while the steel builds up.
        return most;
    }

    /** Bolts: the steel the splitter set aside, crafted by hand (2 ingots, one above the other: 16 bolts). */
    private static void craftBolts(Client c) throws Exception {
        title(c, "Steel bolts", "crafted by hand from the steel set aside");
        waitFor(c, "four steel ingots in the bolt stock", 6_000, () -> countIn(nbt(c, STEEL_X, L, STEEL_Z + 1), "hbm:ingot_steel") >= 4,
                new double[][]{{STEEL_X - 3.5, L + 3, STEEL_Z + 5.5, STEEL_X, L, STEEL_Z + 1}});
        clearInventory(c);
        open(c, STEEL_X, L, STEEL_Z + 1, 0, 2.5, "GUICrate");
        int s = slotOf(c, "hbm:ingot_steel", false);
        c.slotClick(s, 0, "QUICK_MOVE");
        c.waitTicks(3);
        close(c);
        Expect.inventoryHolds(c, "hbm:ingot_steel", 4);
        openAt(c, TA_X + 1, L, MA_Z + 4, TA_X + 1.5, L + 1, MA_Z + 6.5, "Crafting");
        // pick up the four ingots, drop two on the top middle cell and two on the centre cell (right-click = one)
        int src = playerSlotWith(c, "hbm:ingot_steel");
        c.clickSlot(src);
        c.waitTicks(2);
        for (int cell : new int[]{2, 2, 5, 5}) {
            c.clickSlot(cell, 1);
            c.waitTicks(2);
            pause(c, 3);
        }
        Expect.slotHolds(c, 0, "hbm:bolt_steel", 16);
        pause(c, 15);
        c.screenshot("75-crafting-bolts");
        c.slotClick(0, 0, "QUICK_MOVE");           // shift-click the result: both crafts at once
        c.waitTicks(3);
        close(c);
        Expect.inventoryHolds(c, "hbm:bolt_steel", 32);
        // into the missile assembler through its menu
        open(c, MA_X, L + 1, MA_Z + 1, 0, 2.5, "GUIMachineAssemblyMachine");
        // A real click into input slot 10: a shift-click puts the bolts into the battery slot 0 instead.
        moveTo(c, "hbm:bolt_steel", 10);
        pause(c, 10);
        close(c);
        Expect.that(countIn(nbt(c, MA_X, L, MA_Z), "hbm:bolt_steel") >= 32, "32 bolts in the missile assembler; " + nbt(c, MA_X, L, MA_Z));
    }

    /** A slot of the open screen holding the item, on the machine side ({@code player == false}) or the player side. */
    private static int slotOf(Client c, String item, boolean player) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() == player && !s.get("empty").getAsBoolean()
                    && s.get("id").getAsString().equals(item)) return s.get("index").getAsInt();
        }
        throw new IllegalStateException("no " + (player ? "player" : "machine") + " slot holding " + item);
    }

    private static void launch(Client c) throws Exception {
        title(c, "Stealth missile", "assembled from the oil chain and the steel line");
        open(c, MA_X + 5, L, MA_Z, 0, 2.5, "GUICrate");
        Expect.slotHolds(c, 0, "hbm:missile_stealth", 1);
        JsonObject m = c.slot(0);
        c.mouseMove(m.get("center_x").getAsDouble(), m.get("center_y").getAsDouble());
        c.waitTicks(3);
        pause(c, 20);
        c.screenshot("85-stealth-missile");
        c.slotClick(0, 0, "QUICK_MOVE");
        c.waitTicks(3);
        close(c);
        Expect.inventoryHolds(c, "hbm:missile_stealth", 1);
        fire(c);
    }

    /** From the designator on: target, load the pad, fuel it, fire, watch. The missile is in the inventory. */
    private static void fire(Client c) throws Exception {
        // Designate the house with a real right-click from the pad, 90 blocks away.
        title(c, "Target", "the long-range designator");
        Fixtures.give(c, "hbm:designator_range", 1);
        c.hotbar(hotbarOf(c, "hbm:designator_range"));
        standAt(c, PAD_X + 0.5, L + 6, PAD_Z + 3.5);        // high enough to see over the peroxide plant
        c.lookAt(HOUSE_X + 2.5, L + 2.0, HOUSE_Z + 0.5);
        pause(c, 15);
        // Up to three right-clicks: the first click after a menu closes can be swallowed by vanilla's use delay.
        boolean set = false;
        for (int i = 0; i < 3 && !set; i++) {
            c.waitTicks(10);
            c.use(2);
            c.waitTicks(4);
            set = !c.log(30, "Pos set to " + HOUSE_X).isEmpty();
        }
        Expect.that(set, "the designator reports the house as its target");

        // Load the pad through its menu: missile in slot 0, designator in slot 1.
        title(c, "Launch pad", "missile and designator through the menu");
        openAt(c, PAD_X, L, PAD_Z, PAD_X + 0.5, L + 2, PAD_Z + 3.5, "GUILaunchPad");
        moveTo(c, "hbm:missile_stealth", 0);
        moveTo(c, "hbm:designator_range", 1);
        pause(c, 15);
        c.screenshot("86-pad-loaded");
        close(c);
        String pad = nbt(c, PAD_X, L, PAD_Z);
        Expect.that(pad.contains("t0_type:\"" + KERO + "\"") && pad.contains("t1_type:\"" + PEROX + "\""), "the missile typed the pad tanks; " + pad);

        // Now the pad accepts kerosene and peroxide. The kerosene tank goes from RECEIVE to SEND in its menu, and
        // only then are the fuel lines laid: a pipe finds its sources and receivers when it is typed, so both
        // ends must already be in their final state.
        title(c, "Fuelling", "the kerosene tank to SEND, then the fuel lines");
        open(c, TL_X, L, TL_Z - 2, -2.5, 0, "GUIBarrel");
        JsonObject bs = c.screen();
        for (int i = 0; i < 2; i++) {                        // receive -> buffer -> send
            c.click(bs.get("left").getAsInt() + 160, bs.get("top").getAsInt() + 43);
            c.waitTicks(3);
            pause(c, 6);
        }
        close(c);
        Expect.that(nbt(c, TL_X, L, TL_Z - 2).contains("mode:2s"), "the kerosene tank sends; " + nbt(c, TL_X, L, TL_Z - 2));
        pipe(c, KERO, new int[][]{{6, L + 1, 19, 6, L + 2, 19}, {6, L + 2, 17, 6, L + 2, 18}, {7, L + 2, 17, 39, L + 2, 17},
                {39, L + 2, 12, 39, L + 2, 16}, {39, L, 12, 39, L + 1, 12}});   // down at z=12 into the pad port
        pipe(c, PEROX, new int[][]{{PEROX_X - 4, L, PEROX_Z - 1, PEROX_X - 2, L, PEROX_Z - 1},
                {PEROX_X - 4, L, PEROX_Z - 2, PEROX_X - 4, L, PEROX_Z - 3}});
        waitFor(c, "the pad fuelled and powered", 6_000, () -> {
            String p = nbt(c, PAD_X, L, PAD_Z);
            return longOf(p, "t0") >= 8_000 && longOf(p, "t1") >= 8_000 && longOf(p, "power") >= 75_000;
        }, new double[][]{{PAD_X + 6.5, L + 4, PAD_Z + 7.5, PAD_X, L, PAD_Z}});
        c.chat("Harness demo: pad " + nbt(c, PAD_X, L, PAD_Z).replaceAll(".*?(power:[0-9]+L).*?(t0:[0-9]+).*?(t1:[0-9]+).*", "$1 $2 $3"));
        c.waitTicks(110);                          // the pad counts 100 ticks of readiness before it will fire

        // Fire: a lever beside the pad, flipped with a real right-click.
        title(c, "Launch", "a lever beside the pad");
        c.cmd("setblock " + (PAD_X - 2) + " " + L + " " + PAD_Z + " minecraft:lever[face=floor,facing=east]");
        clearInventory(c);
        c.hotbar(9);
        standAt(c, PAD_X - 4.5, L, PAD_Z + 2.5);
        c.lookAt(PAD_X - 2 + 0.5, L + 0.1, PAD_Z + 0.5);
        pause(c, 10);
        c.use(2);
        c.waitTicks(2);
        // Watch from close to the house: the camera looks at the house while the missile comes down on it.
        view(c, HOUSE_X - 14.5, L + 7, HOUSE_Z - 12.5, HOUSE_X + 3.0, L + 2.0, HOUSE_Z + 3.0);
        c.setOption("hide_gui", "true");
        JsonObject missile = null;
        int seen = 0, shot = 0;
        double peak = 0;
        for (int t = 0; t < 600; t += 2) {
            JsonObject q = new JsonObject();
            q.addProperty("radius", 256);
            q.addProperty("type", "hbm:entity_missile_stealth");
            var list = c.query("entities", q).getAsJsonArray("entities");
            if (list.size() > 0) {
                missile = list.get(0).getAsJsonObject();
                seen++;
                peak = Math.max(peak, missile.get("y").getAsDouble());
                if (seen == 10 || seen == 40) c.screenshot("9" + (shot++) + "-missile-in-flight");
            } else if (seen > 0) {
                break;                             // it hit
            }
            c.waitTicks(2);
        }
        Expect.that(seen > 0, "the entities query saw the stealth missile in flight");
        c.lookAt(HOUSE_X + 3.0, L + 2.0, HOUSE_Z + 3.0);
        c.waitTicks(10);
        c.screenshot("93-impact");
        c.setOption("hide_gui", "false");
        c.waitTicks(40);
        int after = houseBlocks(c);
        c.chat("Harness demo: missile flew " + seen * 2 + " ticks, peak y " + Math.round(peak) + "; house blocks "
                + houseBlocksBefore + " -> " + after);
        Expect.that(after < houseBlocksBefore / 4, "the house is destroyed (" + houseBlocksBefore + " -> " + after + " sampled blocks)");
        standAt(c, HOUSE_X - 9.5, L + 8, HOUSE_Z - 9.5);
        c.lookAt(HOUSE_X + 3.0, L, HOUSE_Z + 3.0);
        pause(c, 40);
        c.screenshot("94-crater");
    }

    // ---- menus ------------------------------------------------------------------------------

    /** Hovers (dx, dz) from the block, one block up, and right-clicks it empty-handed. */
    private static void open(Client c, int x, int y, int z, double dx, double dz, String screen) {
        c.hotbar(9);
        standAt(c, x + 0.5 + dx, L + 1, z + 0.5 + dz);
        Fixtures.interact(c, x, y, z);
        c.waitScreen(screen, 5_000L);
        Expect.screenStaysOpen(c, screen, 5);
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(2);
    }

    private static void insertByClicks(Client c, int x, int y, int z, double dx, double dz, String screen, String item, int count, int slot) {
        clearInventory(c);
        Fixtures.give(c, item, count);
        open(c, x, y, z, dx, dz, screen);
        int source = playerSlotWith(c, item);
        c.clickSlot(source);
        c.waitTicks(3);
        pause(c, 6);
        c.clickSlot(slot);
        c.waitTicks(4);
        Expect.slotHolds(c, slot, item, count);
        close(c);
    }

    private static void giveOnly(Client c, String item, int count) {
        clearInventory(c);
        Fixtures.give(c, item, count);
        c.hotbar(1);
        c.waitTicks(1);
    }

    /** {@code /clear}, only when there is something to clear: a failing /clear prints a red chat line. */
    private static void clearInventory(Client c) {
        if (c.inventory().getAsJsonArray("slots").size() > 0) c.cmd("clear");
        c.waitTicks(1);
    }

    private static int playerSlotWith(Client c, String item) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && !s.get("empty").getAsBoolean()
                    && s.get("id").getAsString().equals(item)) return s.get("index").getAsInt();
        }
        throw new IllegalStateException("no player slot holding " + item);
    }

    private static int hotbarOf(Client c, String item) {
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("id").getAsString().equals(item) && s.get("slot").getAsInt() < 9) return s.get("slot").getAsInt() + 1;
        }
        return 1;
    }

    // ---- observation ------------------------------------------------------------------------

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
    }

    private static long longOf(String nbt, String key) {
        Matcher m = Pattern.compile("(?<![A-Za-z0-9_])" + key + ":(-?[0-9]+)").matcher(nbt);
        return m.find() ? Long.parseLong(m.group(1)) : 0;
    }

    /** Item stack SNBT in this node's spelling: 1.20.1 writes {@code Count:<n>b}. */
    private static String items(String snbt) {
        return LEGACY ? snbt.replaceAll("count:([0-9]+)", "Count:$1b") : snbt;
    }

    /** A fluid identifier set to a fluid: a component from 1.20.5 on, item NBT on 1.20.1. */
    private static String identifier(String fluid) {
        return LEGACY ? "hbm:fluid_identifier{\"hbm:fluid\":\"" + fluid + "\"}" : "hbm:fluid_identifier[hbm:fluid=\"" + fluid + "\"]";
    }

    /**
     * Block reach: 26.1 calls the attribute {@code minecraft:block_interaction_range}, 1.20.5..1.21.x
     * {@code minecraft:player.block_interaction_range}, and 1.20.1 has none (the camera shots keep their overlay there).
     */
    private static void reach(Client c, double blocks) {
        if (LEGACY) return;
        String attribute = c.node().startsWith("26.") ? "minecraft:block_interaction_range" : "minecraft:player.block_interaction_range";
        c.cmd("attribute @s " + attribute + " base set " + blocks);
    }

    private static int countIn(String nbt, String id) {
        int total = 0;
        Matcher m = Pattern.compile("\\{[^{}]*id:\"" + Pattern.quote(id) + "\"[^{}]*}").matcher(nbt);
        while (m.find()) {
            Matcher n = Pattern.compile("[cC]ount:([0-9]+)").matcher(m.group());
            total += n.find() ? Integer.parseInt(n.group(1)) : 1;
        }
        return total;
    }

    private static int beltItems(Client c) {
        JsonObject q = new JsonObject();
        q.addProperty("radius", 64);
        q.addProperty("type", "hbm:entity_c_item");
        return c.query("entities", q).getAsJsonArray("entities").size();
    }

    interface Check { boolean ok() throws Exception; }

    /**
     * Polls every 20 ticks until {@code check} holds, failing after {@code maxTicks}; the camera watches
     * {@code (lx, ly, lz)} from {@code (cx, cy, cz)}. Returns the most belt items seen meanwhile.
     */
    private static int waitFor(Client c, String what, int maxTicks, Check check,
                               double cx, double cy, double cz, double lx, double ly, double lz) throws Exception {
        view(c, cx, cy, cz, lx + 0.5, ly + 0.5, lz + 0.5);
        orbit(c, lx, ly, lz, 45, 240);                       // 20 ticks per second: 12 s out and back
        try {
            int most = 0;
            for (int t = 0; t <= maxTicks; t += 20) {
                most = Math.max(most, beltItems(c));
                if (statusLines && t > 0 && t % 100 == 0) oilStatus(c);
                if (check.ok()) {
                    c.chat("Harness demo: " + what + " after " + t + " ticks of waiting");
                    pause(c, 20);
                    return most;
                }
                c.waitTicks(20);
            }
            Expect.that(check.ok(), what + " within " + maxTicks + " ticks");
            return most;
        } finally {
            c.orbitStop();
        }
    }

    /**
     * {@link #waitFor} with the camera moving to the next {@code tour} point every 200 ticks. The server runs at
     * 60 ticks per second meanwhile ({@code /tick rate}), and always goes back to 20 afterwards.
     */
    private static int waitFor(Client c, String what, int maxTicks, Check check, double[][] tour) throws Exception {
        c.cmd("tick rate " + FAST_TICKS);
        try {
            return tourWait(c, what, maxTicks, check, tour);
        } finally {
            c.cmd("tick rate 20");
        }
    }

    private static int tourWait(Client c, String what, int maxTicks, Check check, double[][] tour) throws Exception {
        int most = 0;
        try {
            for (int t = 0; t <= maxTicks; t += 20) {
                if (t % 200 == 0) {
                    double[] p = tour[(t / 200) % tour.length];
                    view(c, p[0], p[1], p[2], p[3] + 0.5, p[4] + 0.5, p[5] + 0.5);
                    // 200 ticks per tour point and a 400-tick period: out to one side and back, then the cut.
                    orbit(c, p[3], p[4], p[5], 35, 400);
                }
                most = Math.max(most, beltItems(c));
                if (statusLines && t > 0 && t % 100 == 0) oilStatus(c);
                if (check.ok()) {
                    c.chat("Harness demo: " + what + " after " + t + " ticks of waiting");
                    pause(c, 20);
                    return most;
                }
                c.waitTicks(20);
            }
            Expect.that(check.ok(), what + " within " + maxTicks + " ticks");
            return most;
        } finally {
            c.orbitStop();
        }
    }

    /**
     * Sweeps the camera round the block at {@code (lx, ly, lz)} and keeps looking at its centre. The harness
     * moves the player on the client once per tick and the game interpolates every frame, so the motion is
     * smooth; it runs in the background until the next teleport or {@code orbitStop}.
     */
    private static void orbit(Client c, double lx, double ly, double lz, double amplitudeDeg, int periodTicks) {
        c.orbit(lx + 0.5, lz + 0.5, lx + 0.5, ly + 0.5, lz + 0.5, amplitudeDeg, periodTicks);
    }

    // ---- camera -----------------------------------------------------------------------------

    private static void startFlying(Client c) {
        standAt(c, -8.5, L, 14.5);
    }

    /** A camera position: stand, look, and a block reach of 0 so no machine overlay covers the shot. */
    private static void view(Client c, double x, double y, double z, double lx, double ly, double lz) {
        standAt(c, x, y, z);
        reach(c, 0);
        c.lookAt(lx, ly, lz);
    }

    /** Stands (hovers) at a point: flight goes off whenever the player lands, so double-jump it back on. */
    private static void standAt(Client c, double x, double y, double z) {
        reach(c, 4.5);   // back to reach for clicks
        if (!c.player().get("flying").getAsBoolean()) {
            c.key("space", 1);
            c.waitTicks(2);
            c.key("space", 1);
            c.waitTicks(6);
            Expect.that(c.player().get("flying").getAsBoolean(), "double-tapped jump switched creative flight on");
        }
        c.teleport(x, y, z);
        c.waitTicks(4);
    }

    private static void overview(Client c, String shot) {
        overviewOf(c, shot, -7.5, L, -3.5, 24);
    }

    private static void overviewOf(Client c, String shot, double x, double y, double z, double dist) {
        view(c, x, y + dist * 0.7, z + dist, x, y, z);
        pause(c, 15);
        c.waitTicks(2);
        c.screenshot(shot);
    }

    private static void title(Client c, String title, String subtitle) {
        c.cmd("title @s times 5 50 10");
        c.cmd("title @s subtitle {\"text\":\"" + subtitle + "\",\"color\":\"gray\"}");
        c.cmd("title @s title {\"text\":\"" + title + "\",\"color\":\"gold\"}");
        pause(c, 30);
    }

    private static void pause(Client c, int ticks) {
        int t = (int) Math.round(ticks * PACE);
        if (t > 0) c.waitTicks(t);
    }
}
