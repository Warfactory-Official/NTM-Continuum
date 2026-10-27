import com.google.gson.JsonObject;
import dev.golem.harness.api.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A working factory DE's own server built and ran before saving ({@code DE_WORLD_FACTORY}): an RTG, a diesel generator fed
 * by ducts from a barrel and a Stirling engine on a coal firebox, all on one cable line into a battery socket, an electric
 * furnace fed by a hopper, a centrifuge, a shredder, a water barrel and pipes into a fluid tank, a test dummy in charged AJR
 * power armour, a hazmat armour stand, a gun in an item frame, and Sellafield at every shade. After the import every chain
 * must keep working in Continuum: each generator alone still charges the battery, fluids still move, machines take new
 * work and their screens open. The esoteric half: a 1.7 fusion reactor (torus, klystron, air intakes, fuel and coolant
 * barrels on ducts), satellites DE's own Soyuz rockets put in orbit, an assembler and a chemical plant on recipes, a cold
 * RBMK, a crane and conveyor line, a teleporter and a siren. 1.21.1 NeoForge only.
 */
public class DeFactoryWorld implements Scenario {
    public void run(Client c) throws Exception {
        if (!c.node().startsWith("1.21.1-neoforge")) return;
        c.leaveWorld(60000);
        Path saves = Path.of("versions/1.21.1-neoforge/runs/harness/saves");
        Path world = saves.resolve("de_factory");
        if (Files.exists(world)) delete(world);
        copy(Path.of(System.getenv("DE_WORLD_FACTORY")), world);
        int before = c.log(20000, "DE import done").size();
        c.joinWorld("de_factory", 240000);
        c.waitInWorld(240000);
        c.waitTicks(20);
        // the live phase finishes over a few server ticks after the join
        for (int i = 0; i < 120 && c.log(20000, "DE import done").size() == before; i++) c.waitTicks(10);
        List<String> done = c.log(20000, "DE import done");
        Expect.that(done.size() == before + 1, "this join imported the world once");
        String summary = done.get(done.size() - 1);
        System.out.println(summary);
        Expect.that(summary.contains("multiblock cells blocked 0") && summary.contains("multiblocks rebuilt 17"),
                "every multiblock rebuilt whole, the torus's 925 cells included: " + summary);
        Expect.that(summary.contains("satellites converted 3"), "the three satellites in orbit converted: " + summary);
        // What stays parked is DE-only or derived: the electric furnace's vanilla recipe XP, the Stirling engine's cached
        // heat link, and the connection states of the RTG and barrels.
        Expect.that(summary.contains("block entity fields parked 2") && summary.contains(", entity fields parked 0"),
                "nothing else parked: " + summary);

        // The dummy keeps its charged power armour, the stand its hazmat suit, the frame its gun. Checked first: the world
        // is irradiated, and DE's own first dummy died of it before the save.
        marker(c, "execute if entity @e[type=hbm:entity_test_dummy,nbt={ArmorItems:[{id:\"hbm:ajr_boots\"},{id:\"hbm:ajr_legs\"},{id:\"hbm:ajr_plate\",components:{\"hbm:battery_charge\":250000L}},{id:\"hbm:ajr_helmet\"}]}]", 0, "the dummy wears its charged AJR armour");
        marker(c, "execute if entity @e[type=armor_stand,nbt={ArmorItems:[{id:\"hbm:hazmat_boots\"},{id:\"hbm:hazmat_legs\"},{id:\"hbm:hazmat_plate\"},{id:\"hbm:hazmat_helmet\"}]}]", 2, "the stand wears its hazmat suit");
        marker(c, "execute if entity @e[type=item_frame,nbt={Item:{id:\"hbm:gun_light_revolver\"}}]", 4, "the frame holds its gun");

        // Everything running together: the battery charges, the hopper feeds the furnace, diesel leaves the barrel. DE ran
        // the world long enough to empty the hopper's chest and fill the furnace's output, so both are reset first.
        c.cmd("item replace block 14 -58 11 container.0 with minecraft:raw_iron 64");
        c.cmd("item replace block 14 -60 11 container.2 with minecraft:air");
        c.cmd("data merge block 26 -60 14 {tank:16000}");
        c.waitTicks(20);
        long charge0 = charge(c), ingots0 = count(c, 14, -60, 11, "minecraft:iron_ingot"), diesel0 = num(c, 26, -60, 14, "tank");
        c.waitTicks(200);
        Expect.that(charge(c) > charge0, "the cable line charges the battery: " + charge0 + " -> " + charge(c));
        Expect.that(count(c, 14, -60, 11, "minecraft:iron_ingot") > ingots0, "the electric furnace smelts what the hopper brings: " + ingots0 + " " + nbt(c, 14, -60, 11)
                + " hopper " + nbt(c, 14, -59, 11) + " chest " + nbt(c, 14, -58, 11));
        Expect.that(num(c, 26, -60, 14, "tank") < diesel0, "diesel flows from the barrel through the ducts to the generator");

        // Each generator alone, with the furnace stopped so the battery takes all of it. The diesel generator is taken
        // off the line by routing the cable around it (z 9) instead of through the cell it touches.
        c.cmd("setblock 14 -58 11 minecraft:air");
        c.cmd("setblock 14 -59 11 minecraft:air");
        for (int slot = 0; slot <= 2; slot++) c.cmd("item replace block 14 -60 11 container." + slot + " with minecraft:air");
        c.cmd("fill 25 -60 9 27 -60 9 hbm:red_cable");
        c.cmd("setblock 26 -60 10 minecraft:air");
        c.cmd("setblock 30 -60 10 minecraft:air");
        c.waitTicks(40);
        long rtg = gain(c, 100);
        Expect.that(rtg > 0, "the RTG alone charges the battery: " + rtg);
        c.cmd("setblock 2 -60 10 minecraft:air");
        c.cmd("setblock 26 -60 10 hbm:red_cable");
        c.waitTicks(40);
        long diesel = gain(c, 100);
        Expect.that(diesel > 0, "the diesel generator alone charges the battery: " + diesel);
        c.cmd("setblock 26 -60 10 minecraft:air");
        c.cmd("setblock 30 -60 10 hbm:red_cable");
        c.waitTicks(40);
        long stirling = gain(c, 200);
        Expect.that(stirling > 0, "the Stirling engine alone, heated by the coal firebox, charges the battery: " + stirling);
        System.out.println("gain per generator: rtg " + rtg + "/100t, diesel " + diesel + "/100t, stirling " + stirling + "/200t");
        c.cmd("setblock 2 -60 10 hbm:red_cable");
        c.cmd("setblock 26 -60 10 hbm:red_cable");

        // New work after the import: water through the pipes into the tank, ore into the centrifuge, stone into the shredder.
        long water0 = num(c, 44, -60, 4, "tank");
        c.cmd("data merge block 40 -60 4 {mode:2s}");
        for (int slot = 2; slot <= 5; slot++) c.cmd("item replace block 18 -60 11 container." + slot + " with minecraft:air");
        c.cmd("item replace block 18 -60 11 container.0 with minecraft:iron_ore 4");
        c.cmd("item replace block 22 -60 11 container.9 with minecraft:air");
        c.cmd("item replace block 22 -60 11 container.0 with minecraft:cobblestone 8");
        c.waitTicks(300);
        Expect.that(num(c, 44, -60, 4, "tank") > water0, "water moves from the barrel through the pipes into the tank");
        Expect.that(count(c, 18, -60, 11, "hbm:powder_iron") > 0, "the centrifuge processes new ore: " + c.blockEntity(18, -60, 11));
        Expect.that(count(c, 22, -60, 11, "minecraft:gravel") > 0, "the shredder shreds new stone: " + c.blockEntity(22, -60, 11));

        esoteric(c);
        c.teleport(8.5, -60, 16.5);
        c.waitTicks(40);

        // Sellafield keeps every shade, which is what its tint reads.
        for (int shade = 0; shade <= 15; shade++) {
            for (int[] row : new int[][]{{24}, {26}, {28}}) {
                JsonObject block = c.block(shade, -60, row[0]);
                Expect.that(block.getAsJsonObject("properties").get("shade").getAsString().equals(Integer.toString(shade)),
                        "shade " + shade + " at z " + row[0] + ": " + block);
            }
        }
        // Hot sellafield cools a level now and then on a random tick, as in 1.7.
        for (int level = 0; level <= 5; level++) {
            String block = c.block(level * 2, -60, 30).get("block").getAsString();
            Expect.that(block.startsWith("hbm:sellafield_") && Integer.parseInt(block.substring(15)) <= level,
                    "hot sellafield level " + level + ": " + block);
        }
        c.cmd("gamemode spectator");
        c.teleport(8.0, -47, 27.5);
        c.look(0, 90);
        c.waitTicks(40);
        c.screenshot("sellafield-shades");
        c.teleport(8.0, -54, 34.5);
        c.look(180, 45);
        c.waitTicks(20);
        c.screenshot("sellafield-angle");
        c.cmd("gamemode creative");

        c.teleport(8.5, -60, 8.5);
        c.waitTicks(10);
        Fixtures.interact(c, 8, -60, 12);
        c.waitTicks(10);
        Expect.that(c.screen().get("open").getAsBoolean(), "the battery socket opens: " + c.screen());
        c.screenshot("battery-socket");
        c.key("escape");
        c.teleport(28.5, -60, 11.5);
        c.waitTicks(10);
        Fixtures.interact(c, 26, -60, 11);
        c.waitTicks(10);
        Expect.that(c.screen().get("open").getAsBoolean(), "the diesel generator opens: " + c.screen());
        c.screenshot("diesel-generator");
        c.key("escape");
        c.teleport(20.5, -54, -2.5);
        c.lookAt(20, -60, 12);
        c.waitTicks(20);
        c.screenshot("factory");

        c.markLog();
        c.leaveWorld(60000);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]].*(hbm|deimport|Skipping|invalid item)");
        c.joinWorld("harness", 180000);
        c.waitInWorld(180000);
    }

    private static void esoteric(Client c) {
        // Satellites DE's Soyuz rockets put up: listed, then used through Continuum's own chips.
        c.markLog();
        c.cmd("ntmsatellites list");
        c.waitTicks(10);
        String listed = String.join(" | ", c.logSinceMark(" - Satellite"));
        Expect.that(listed.contains("4242 - SatelliteResonator") && listed.contains("5151 - SatellitePrecisionLaser")
                && listed.contains("6262 - SatelliteRadar"), "the launched satellites are in orbit: " + listed);
        c.cmd("clear");
        c.cmd("give @s hbm:sat_designator[hbm:persistent_data={freq:5151}]");
        c.hotbar(1);
        c.teleport(60.5, -60, 50.5);
        c.waitTicks(20);
        c.lookAt(60, -61, 60);
        c.waitTicks(2);
        c.use(1);
        c.waitTicks(20);
        Expect.that(c.block(60, -61, 60).toString().contains("minecraft:air"), "the precision laser burns a crater: " + c.block(60, -61, 60));
        c.screenshot("laser-crater");
        c.cmd("clear");
        c.cmd("give @s hbm:sat_coord[hbm:persistent_data={freq:4242}]");
        c.hotbar(1);
        c.waitTicks(10);
        c.use(1);
        c.waitScreen("GUIScreenSatCoord", 5000);
        c.waitTicks(5);
        JsonObject status = c.status();
        int cl = (status.get("gui_width").getAsInt() - 176) / 2, ct = (status.get("gui_height").getAsInt() - 126) / 2;
        c.click(cl + 70, ct + 24);
        c.type("40");
        c.click(cl + 70, ct + 59);
        c.type("-60");
        c.click(cl + 70, ct + 95);
        c.type("48");
        c.waitTicks(3);
        c.click(cl + 142, ct + 61);
        c.waitTicks(10);
        JsonObject moved = c.player();
        Expect.that(Math.abs(moved.get("x").getAsDouble() - 40.5) < 0.6 && Math.abs(moved.get("z").getAsDouble() - 48.5) < 0.6,
                "the resonator teleports to the typed coordinates: " + moved);
        c.cmd("clear");

        // Fusion: DE saved it stopped (cold coolant used up, warm coolant and helium tanks full); refilled and emptied,
        // the converted torus burns D-D again on the klystron's beam.
        c.teleport(100.5, -50, 20.5);
        c.cmd("data merge block 100 -55 -6 {tank:16000}");
        c.cmd("data merge block 102 -55 6 {tank:0}");
        c.cmd("data merge block 100 -60 0 {ft3:0}");
        c.waitTicks(300);
        String torus = nbt(c, 100, -60, 0);
        Expect.that(num(c, 100, -60, 0, "ft3") > 0 && torus.contains("hbm:helium4"), "the torus makes helium-4 again: " + torus);
        Expect.that(Pattern.compile("temperature: 123\\.0f").matcher(torus).find(), "the coolant holds it at 123 K: " + torus);
        Expect.that(num(c, 112, -60, 0, "t") >= 75000, "the intakes keep the klystron's air up: " + nbt(c, 112, -60, 0));
        Expect.that(num(c, 102, -55, 6, "tank") > 0, "warm coolant returns through the ducts: " + nbt(c, 102, -55, 6));
        c.lookAt(100, -58, 0);
        c.waitTicks(5);
        c.screenshot("fusion");

        // Recipe machines, the crane line, a cold RBMK started in Continuum, the teleporter and the siren.
        c.teleport(78.5, -60, 30.5);
        c.cmd("item replace block 70 -60 25 container.16 with minecraft:air");
        c.cmd("item replace block 70 -60 25 container.4 with hbm:nugget_lead 64");
        c.cmd("item replace block 78 -60 25 container.4 with minecraft:sugar 64");
        c.cmd("data merge block 78 -60 25 {o0:0}");
        c.cmd("item replace block 70 -60 36 container.0 with minecraft:gold_ingot 64");
        c.waitTicks(5);
        long buckshot = count(c, 70, -60, 25, "hbm:pellet_buckshot"), ethanol = num(c, 78, -60, 25, "o0"),
                moved0 = count(c, 76, -60, 36, "minecraft:gold_ingot");
        c.cmd("data merge block 87 -60 32 {hasRod:1b,inventory:{Items:[{Slot:0b,count:1,id:\"hbm:rbmk_fuel_po210be\"}]}}");
        c.cmd("data merge block 86 -60 33 {targetLevel:1.0d}");
        c.waitTicks(200);
        Expect.that(count(c, 70, -60, 25, "hbm:pellet_buckshot") > buckshot, "the assembler keeps making buckshot (ass.buckshot)");
        Expect.that(num(c, 78, -60, 25, "o0") > ethanol, "the chemical plant keeps making ethanol (chem.ethanol)");
        Expect.that(count(c, 76, -60, 36, "minecraft:gold_ingot") > moved0, "the cranes and conveyor keep moving gold");
        String fuel = nbt(c, 86, -60, 32);
        Expect.that(Pattern.compile("fluxQuantity: [1-9]").matcher(fuel).find(), "the neutron source starts the RBMK's fuel column: " + fuel);
        Expect.that(Pattern.compile("level: [1-9]|level: 0\\.[1-9]").matcher(nbt(c, 86, -60, 33)).find(),
                "the control rod withdraws: " + nbt(c, 86, -60, 33));
        c.cmd("data merge block 87 -60 32 {hasRod:0b,inventory:{Items:[]}}");
        c.cmd("data merge block 86 -60 32 {hasRod:0b,inventory:{Items:[]}}");
        c.cmd("data merge block 86 -60 33 {targetLevel:0.0d}");
        // a plain /tp: the harness's teleport waits for the player to stay where it was put, and the teleporter moves it on
        c.teleport(66.5, -60, 27.5);
        c.cmd("tp @s 66.5 -59 30.5");
        c.waitTicks(40);
        JsonObject tele = c.player();
        Expect.that(Math.abs(tele.get("z").getAsDouble() - 50.5) < 1.0, "the teleporter sends the player to its target: " + tele);
        Expect.that(nbt(c, 66, -60, 25).contains("siren_track_air_raid"), "the siren keeps its track: " + nbt(c, 66, -60, 25));
    }

    /** Battery charge gained over {@code ticks}. */
    private static long gain(Client c, int ticks) {
        long start = charge(c);
        c.waitTicks(ticks);
        return charge(c) - start;
    }

    private static long charge(Client c) {
        return num(c, 8, -60, 12, "\"hbm:battery_charge\"");
    }

    private static long num(Client c, int x, int y, int z, String key) {
        Matcher m = Pattern.compile("(?<!\\w)" + Pattern.quote(key) + ": (-?\\d+)").matcher(nbt(c, x, y, z));
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }

    /** Items of {@code id} across every slot of a block entity. */
    private static long count(Client c, int x, int y, int z, String id) {
        Matcher m = Pattern.compile("\\{[^{}]*count: (\\d+)[^{}]*id: \"" + Pattern.quote(id) + "\"").matcher(nbt(c, x, y, z).replace("\n", ""));
        long total = 0;
        while (m.find()) total += Long.parseLong(m.group(1));
        return total;
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") ? be.get("nbt").getAsString() : "";
    }

    private static void marker(Client c, String condition, int x, String what) {
        c.cmd(condition + " run setblock " + x + " -60 34 minecraft:gold_block");
        c.waitTicks(5);
        Expect.that(c.block(x, -60, 34).toString().contains("gold_block"), what);
    }

    private static void copy(Path from, Path to) throws IOException {
        try (Stream<Path> files = Files.walk(from)) {
            for (Path p : files.toList()) {
                if (p.getFileName().toString().equals("session.lock")) continue;
                Path target = to.resolve(from.relativize(p).toString());
                if (Files.isDirectory(p)) Files.createDirectories(target);
                else Files.copy(p, target);
            }
        }
    }

    private static void delete(Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            for (Path p : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }
}
