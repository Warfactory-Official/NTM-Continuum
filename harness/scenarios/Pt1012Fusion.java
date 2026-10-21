import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Playtest 2026-10-12: "verify that the klystron / air compressor work in a fusion setup". A 1.7 fusion reactor built by
 * hand and run, with the 1.7 rules ({@code TileEntityFusionKlystron}, {@code TileEntityMachineIntake},
 * {@code TileEntityFusionTorus}):
 * <ul>
 *   <li>the torus is placed from its block item (the whole 925-cell multiblock); a klystron twelve blocks east of the core
 *       faces away from it, so its emitter claim (four blocks behind it) meets the torus's east arm (seven out);</li>
 *   <li>three air intakes ("air compressors": 1.7's only source of compressed air, 1,000 mB a tick each for 100 HE a
 *       tick) feed the klystron's south air port over an air duct; a creative generator
 *       feeds the intakes over red cable;</li>
 *   <li>a deuterium tank and a cold-perfluoromethyl tank on the torus's top ring feed its top ports, each typed with a
 *       fluid identifier and filled by an infinite barrel through the tank menu;</li>
 *   <li>the torus gets a creative battery and the D-D cycle ({@code fus.dd}: 750 kKyU ignition, 20 mB D a tick, 1,000 mB
 *       helium-4 per 100 ticks) through the recipe selector; the klystron gets a creative battery and a 1 MKyU target
 *       typed into its field.</li>
 * </ul>
 * Negative controls first: with fuel, coolant and power the torus does not progress until the klystron fires, and the
 * klystron does not fire while its air tank is empty. Then the intakes are powered and the klystron's air tank, the torus
 * progress and the helium output are sampled.
 */
public class Pt1012Fusion implements Scenario {

    private static final int Y = 100, G = 101, TOP = 106;

    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 22);
        c.cmd("fill -22 " + G + " -22 22 " + (Y + 8) + " 22 minecraft:air");
        c.cmd("gamerule sendCommandFeedback false");
        c.waitTicks(5);
        boolean legacy120 = c.node().startsWith("1.20.");

        // --- the torus, placed from its item: the core lands seven blocks ahead of the clicked block ---
        place(c, "hbm:fusion_torus", 0.5, G, -9.5, 0, Y, -7);
        expectCore(c, 0, G, 0, "hbm:fusion_torus", null);

        // --- the klystron twelve blocks east, placed standing east of it so it faces east (away from the torus) ---
        place(c, "hbm:fusion_klystron", 18.5, G, 0.5, 15, Y, 0);
        expectCore(c, 12, G, 0, "hbm:fusion_klystron", "east");

        // --- three air intakes, 2x2 each, on the klystron's south side ---
        for (int z = 4; z <= 8; z += 2) {
            place(c, "hbm:machine_intake", 16.5, G, z + 0.5, 14, Y, z);
            expectCore(c, 14, G, z, "hbm:machine_intake", "east");
        }
        // air duct from the klystron's south port (12, G, 3) along the intakes' west faces
        for (int z = 3; z <= 9; z++) place(c, "hbm:fluid_duct_neo", 10.5, G, z + 0.5, 12, Y, z);
        // red cable along the intakes' east faces; the power source is placed at its end later
        for (int z = 4; z <= 9; z++) place(c, "hbm:red_cable", 17.5, G, z + 0.5, 15, Y, z);

        // --- the two fuel tanks on the torus's top ring ---
        place(c, "hbm:machine_fluidtank", -6.5, TOP, 0.5, -5, TOP - 1, 0);   // deuterium, core (-4, TOP, 0)
        expectCore(c, -4, TOP, 0, "hbm:machine_fluidtank", null);
        place(c, "hbm:machine_fluidtank", 0.5, TOP, -6.5, 0, TOP - 1, -5);   // coolant, core (0, TOP, -4)
        expectCore(c, 0, TOP, -4, "hbm:machine_fluidtank", null);
        place(c, "hbm:machine_fluidtank", 0.5, TOP, 7.5, 0, TOP - 1, 5);     // warm coolant out, core (0, TOP, 4)
        expectCore(c, 0, TOP, 4, "hbm:machine_fluidtank", null);
        // deuterium: tank port (-6, TOP, 1) -> torus top port (-6, TOP, 0)
        place(c, "hbm:fluid_duct_neo", -5.5, TOP, 1.5, -6, TOP - 1, 0);
        place(c, "hbm:fluid_duct_neo", -6.5, TOP, 3.5, -6, TOP - 1, 1);
        // coolant: tank port (1, TOP, -6) -> torus top port (2, TOP, -6)
        place(c, "hbm:fluid_duct_neo", 3.5, TOP, -6.5, 1, TOP - 1, -6);
        place(c, "hbm:fluid_duct_neo", 3.5, TOP, -6.5, 2, TOP - 1, -6);

        // warm coolant: torus top port (0, TOP, 6) -> tank port (1, TOP, 6)
        place(c, "hbm:fluid_duct_neo", 1.5, TOP, 7.5, 0, TOP - 1, 6);
        place(c, "hbm:fluid_duct_neo", 1.5, TOP, 7.5, 1, TOP - 1, 6);

        // --- type the ducts with fluid identifiers (sneak-click spreads along the run) ---
        typeDuct(c, legacy120, "hbm:air", 10.5, G, 6.5, 12, G, 6);
        typeDuct(c, legacy120, "hbm:deuterium", -6.5, TOP, 3.5, -6, TOP, 1);
        typeDuct(c, legacy120, "hbm:perfluoromethyl_cold", 3.5, TOP, -6.5, 2, TOP, -6);
        typeDuct(c, legacy120, "hbm:perfluoromethyl", 1.5, TOP, 7.5, 1, TOP, 6);
        for (int z = 3; z <= 9; z++) Expect.that(nbt(c, 12, G, z).contains("\"hbm:air\""), "air duct at z=" + z + " typed: " + nbt(c, 12, G, z));
        Expect.that(nbt(c, -6, TOP, 0).contains("\"hbm:deuterium\""), "deuterium duct typed: " + nbt(c, -6, TOP, 0));
        Expect.that(nbt(c, 2, TOP, -6).contains("\"hbm:perfluoromethyl_cold\""), "coolant duct typed: " + nbt(c, 2, TOP, -6));
        Expect.that(nbt(c, 0, TOP, 6).contains("\"hbm:perfluoromethyl\""), "warm coolant duct typed: " + nbt(c, 0, TOP, 6));

        c.teleport(22.5, G + 9, -16.5);
        c.lookAt(4.0, G + 1, 2.0);
        c.waitTicks(10);
        c.screenshot("10-built");

        // --- fuel tanks: identifier, infinite barrel, send mode, all through the tank menu ---
        fillTank(c, legacy120, "hbm:deuterium", -6.5, TOP, 0.5, -5, TOP, 0, "10-tank-deuterium");
        fillTank(c, legacy120, "hbm:perfluoromethyl_cold", 0.5, TOP, -6.5, 0, TOP, -5, "11-tank-coolant");
        // the warm-coolant tank stays in receive mode; an infinite barrel in its drain bay empties it (1.7 FluidLoaderInfinite)
        c.cmd("clear");
        openAt(c, 0.5, TOP, 7.5, 0, TOP, 5, "GUIMachineFluidTank");
        insertOpen(c, identifier(legacy120, "hbm:perfluoromethyl"), 0, 1);
        insertOpen(c, "hbm:fluid_barrel_infinite", 4);
        c.waitTicks(10);
        c.screenshot("12-tank-warm-coolant");
        closeAll(c);

        // --- torus: creative battery, D-D cycle through the recipe selector ---
        openAt(c, 0.5, G, -9.5, 0, G, -7, "GUIFusionTorus");
        insertOpen(c, "hbm:battery_creative", 0);
        JsonObject s = c.screen();
        int left = s.get("left").getAsInt(), top = s.get("top").getAsInt();
        c.click(left + 52, top + 89);                  // 1.7 checkClick(43, 80, 18, 18)
        c.waitScreen("RecipeSelector", 5_000L);
        JsonObject search = editBox(c);
        int gl = search.get("x").getAsInt() - 28, gt = search.get("y").getAsInt() - 111;
        c.mouseMove(gl + 16, gt + 26);
        c.waitTicks(4);
        c.screenshot("20-recipe-selector");
        c.click(gl + 16, gt + 26);                     // fus.dd, the first fuel cycle
        c.waitTicks(3);
        c.click(gl + 160, gt + 98);                    // confirm and close
        c.waitTicks(6);
        closeAll(c);
        String torus = nbt(c, 0, G, 0);
        Expect.that(torus.contains("recipe0:\"fus.dd\""), "the selector set fus.dd: " + torus);

        // --- negative control 1: fuel, coolant and power but no klystron -> the torus cools and fuels, never lights ---
        c.waitTicks(120);
        torus = nbt(c, 0, G, 0);
        System.out.println("[Pt1012Fusion] torus before klystron: " + summary(torus));
        Expect.that(intOf(torus, "ft0") > 0 && torus.contains("ft0_type:\"hbm:deuterium\""), "deuterium reached the torus: " + summary(torus));
        Expect.that(floatOf(torus, "temperature") <= 123.0f, "the coolant cooled the torus to 123 K: " + summary(torus));
        Expect.that(longOf(torus, "power") > 5_000_000L, "the creative battery powers the torus: " + summary(torus));
        Expect.that(doubleOf(torus, "progress0") == 0.0 && intOf(torus, "ft3") == 0, "no ignition without a klystron: " + summary(torus));

        // --- klystron: creative battery, 1 MKyU target typed into its field ---
        openAt(c, 18.5, G, 0.5, 15, G, 0, "GUIFusionKlystron");
        insertOpen(c, "hbm:battery_creative", 0);
        JsonObject field = editBox(c);
        c.click(field.get("x").getAsInt() + 90, field.get("center_y").getAsDouble());
        c.waitTicks(2);
        c.type("1000000");
        c.waitTicks(6);
        c.screenshot("30-klystron-set-no-air");
        closeAll(c);
        c.waitTicks(40);
        String kly = nbt(c, 12, G, 0);
        torus = nbt(c, 0, G, 0);
        System.out.println("[Pt1012Fusion] klystron without air: " + kly + " torus " + summary(torus));
        Expect.that(longOf(kly, "outputTarget") == 1_000_000L, "the typed target reached the klystron: " + kly);
        Expect.that(intOf(kly, "t") == 0, "no air yet (intakes unpowered): " + kly);
        // negative control 2: a klystron without air emits nothing, so the torus still does not light
        Expect.that(doubleOf(torus, "progress0") == 0.0, "no air, no ignition: " + summary(torus));

        // --- power the intakes: a creative generator at the cable's end ---
        place(c, "hbm:creative_generator", 17.5, G, 10.5, 15, Y, 10);
        Expect.block(c, 15, G, 10, "hbm:creative_generator");
        c.waitTicks(10);

        // --- run and sample ---
        long helium = 0, lastHelium = -1;
        double maxProgress = 0;
        int maxAir = 0;
        for (int i = 0; i < 10; i++) {
            c.waitTicks(40);
            kly = nbt(c, 12, G, 0);
            torus = nbt(c, 0, G, 0);
            String intake = nbt(c, 14, G, 6);
            int air = intOf(kly, "t");
            helium = intOf(torus, "ft3");
            maxAir = Math.max(maxAir, air);
            maxProgress = Math.max(maxProgress, doubleOf(torus, "progress0"));
            System.out.println("[Pt1012Fusion] t+" + (40 * (i + 1)) + " intake power " + longOf(intake, "power") + " air "
                    + intOf(intake, "compair") + " | klystron air " + air + "/150000 power " + longOf(kly, "power")
                    + " | torus " + summary(torus));
            // while the D-D cycle burns (before the 4,000 mB helium tank fills and stops it, as in 1.7)
            if (i == 1) {
                c.teleport(22.5, G + 9, -16.5);
                c.lookAt(4.0, G + 1, 2.0);
                c.waitTicks(4);
                c.screenshot("50-running");
                c.teleport(23.5, G + 5, 15.5);
                c.lookAt(13.0, G, 5.0);
                c.waitTicks(4);
                c.screenshot("51-intakes");
            }
            if (i == 2) {
                openAt(c, 0.5, G, -9.5, 0, G, -7, "GUIFusionTorus");
                c.waitTicks(10);
                c.screenshot("52-torus-burning");
                closeAll(c);
                openAt(c, 18.5, G, 0.5, 15, G, 0, "GUIFusionKlystron");
                c.waitTicks(10);
                c.screenshot("53-klystron-firing");
                closeAll(c);
            }
        }
        Expect.that(maxAir >= 75_000, "the intakes filled the klystron's air tank past half (full speed in 1.7): max " + maxAir);
        Expect.that(helium >= 1000 && torus.contains("ft3_type:\"hbm:helium4\""), "the D-D cycle produced helium-4: " + summary(torus));
        Expect.that(floatOf(torus, "temperature") <= 123.0f, "the torus stayed cold: " + summary(torus));

        openAt(c, 0.5, G, -9.5, 0, G, -7, "GUIFusionTorus");
        c.waitTicks(10);
        c.screenshot("61-torus-helium-full");
        closeAll(c);
        c.teleport(-16.5, G + 8, 14.5);
        c.lookAt(2.0, G + 2, -1.0);
        c.waitTicks(6);
        c.screenshot("62-running-west");

        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[ERROR\\]).*(hbm|golem|fusion|klystron)");
        System.out.println("[Pt1012Fusion] PASS: max klystron air " + maxAir + ", max progress " + maxProgress + ", helium " + helium);
    }

    // --- placement ---

    /** Stand at (px, py, pz), aim at the top face of block (bx, by, bz), right-click with one {@code item}. */
    private static void place(Client c, String item, double px, double py, double pz, int bx, int by, int bz) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(px, py, pz);
        c.waitTicks(3);
        // On top of a machine (the torus ring) a plain right-click opens its menu, as in 1.7: sneak to place.
        // Sneak before aiming: crouching lowers the eye, which would move the aim point.
        boolean sneak = by > Y;
        if (sneak) { c.keyDown("key.sneak"); c.waitTicks(3); }
        c.lookAt(bx + 0.5, by + 0.98, bz + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
        if (sneak) { c.keyUp("key.sneak"); c.waitTicks(2); }
        JsonObject b = c.block(bx, by + 1, bz);
        if (!item.equals(b.get("block").getAsString())) {
            for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 2; dy++) for (int dz = -1; dz <= 1; dz++) {
                String id = c.block(bx + dx, by + dy, bz + dz).get("block").getAsString();
                if (id.equals(item)) System.out.println("[Pt1012Fusion] " + item + " landed at " + (bx + dx) + "," + (by + dy) + "," + (bz + dz));
            }
        }
        Expect.equal(item, b.get("block").getAsString(), item + " placed by hand on top of " + bx + "," + by + "," + bz);
    }

    private static void expectCore(Client c, int x, int y, int z, String id, String facing) {
        JsonObject b = c.block(x, y, z);
        String st = b.toString();
        Expect.equal(id, b.get("block").getAsString(), id + " at " + x + "," + y + "," + z);
        Expect.that(st.contains("\"cell\":\"false\""), id + " core at " + x + "," + y + "," + z + ": " + st);
        if (facing != null) Expect.that(st.contains("\"facing\":\"" + facing + "\""), id + " faces " + facing + ": " + st);
    }

    private static String identifier(boolean legacy120, String fluid) {
        return legacy120 ? "hbm:fluid_identifier{\"hbm:fluid\":\"" + fluid + "\"}"
                : "hbm:fluid_identifier[hbm:fluid=\"" + fluid + "\"]";
    }

    /** Sneak-right-clicks one duct of a run with an identifier, which types the whole connected run. */
    private static void typeDuct(Client c, boolean legacy120, String fluid, double px, double py, double pz, int x, int y, int z) {
        Fixtures.giveOnly(c, identifier(legacy120, fluid), 1);
        c.teleport(px, py, pz);
        c.waitTicks(3);
        c.keyDown("key.sneak");
        c.waitTicks(3);
        c.lookAt(x + 0.5, y + 0.5, z + 0.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(3);
        c.keyUp("key.sneak");
        c.waitTicks(2);
    }

    // --- menus ---

    private static void openAt(Client c, double px, double py, double pz, int x, int y, int z, String screen) {
        c.teleport(px, py, pz);
        c.waitTicks(3);
        c.hotbar(9);
        c.waitTicks(1);
        c.lookAt(x + 0.5, y + 0.5, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitScreen(screen, 5_000L);
        Expect.screenStaysOpen(c, screen, 5);
    }

    /** Gives one item (into hotbar slot 1) and moves it into machine slot {@code slot} of the open menu with two clicks. */
    private static void insertOpen(Client c, String item, int slot) {
        insertOpen(c, item, slot, slot);
    }

    /** As above; {@code endSlot} is where the machine keeps it (a fluid tank moves a used identifier from 0 to 1). */
    private static void insertOpen(Client c, String item, int slot, int endSlot) {
        c.cmd("give @s " + item + " 1");
        c.waitTicks(4);
        int source = playerSlotWith(c, item);
        c.clickSlot(source);
        c.waitTicks(2);
        c.clickSlot(slot);
        c.waitTicks(5);
        String id = item.contains("[") ? item.substring(0, item.indexOf('[')) : item.contains("{") ? item.substring(0, item.indexOf('{')) : item;
        Expect.slotHolds(c, endSlot, id, 1);
    }

    private static void fillTank(Client c, boolean legacy120, String fluid, double px, double py, double pz,
                                 int x, int y, int z, String shot) {
        c.cmd("clear");
        openAt(c, px, py, pz, x, y, z, "GUIMachineFluidTank");
        insertOpen(c, identifier(legacy120, fluid), 0, 1);
        c.waitTicks(5);
        insertOpen(c, "hbm:fluid_barrel_infinite", 2);
        JsonObject s = c.screen();
        int left = s.get("left").getAsInt(), top = s.get("top").getAsInt();
        for (int i = 0; i < 2; i++) { c.click(left + 160, top + 44); c.waitTicks(3); }   // mode: receive -> buffer -> send
        c.waitTicks(10);
        c.screenshot(shot);
        closeAll(c);
    }

    private static void closeAll(Client c) {
        for (int i = 0; i < 4 && c.screenClass() != null && !"none".equals(c.screenClass()); i++) {
            c.key("escape");
            c.waitTicks(4);
        }
        c.waitTicks(2);
    }

    private static int playerSlotWith(Client c, String item) {
        String id = item.contains("[") ? item.substring(0, item.indexOf('[')) : item.contains("{") ? item.substring(0, item.indexOf('{')) : item;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && !s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(id)) {
                return s.get("index").getAsInt();
            }
        }
        throw new IllegalStateException("no player slot holding " + id);
    }

    private static JsonObject editBox(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("widgets")) {
            JsonObject w = e.getAsJsonObject();
            if (w.get("class").getAsString().contains("EditBox")) return w;
        }
        throw new IllegalStateException("no text field on " + c.screenClass());
    }

    // --- NBT ---

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : be.toString();
    }

    private static String num(String nbt, String key) {
        Matcher m = Pattern.compile("(?:^|[{,])" + Pattern.quote(key) + ":(-?[0-9.Ee+-]+)[LlSsBbFfDd]?(?=[,}])").matcher(nbt);
        Expect.that(m.find(), "no " + key + " in " + nbt);
        return m.group(1);
    }

    private static long longOf(String nbt, String key) { return Long.parseLong(num(nbt, key)); }
    private static int intOf(String nbt, String key) { return Integer.parseInt(num(nbt, key)); }
    private static double doubleOf(String nbt, String key) { return Double.parseDouble(num(nbt, key)); }
    private static float floatOf(String nbt, String key) { return Float.parseFloat(num(nbt, key)); }

    private static String summary(String torus) {
        return "power " + longOf(torus, "power") + ", T " + floatOf(torus, "temperature") + " K, cold " + intOf(torus, "t0")
                + ", hot " + intOf(torus, "t1") + ", D " + intOf(torus, "ft0") + ", He4 " + intOf(torus, "ft3")
                + ", progress " + doubleOf(torus, "progress0") + ", bonus " + doubleOf(torus, "bonus0");
    }
}
