import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1.7 X5846 details: the small cooling tower takes steam only at its perimeter ports, the placement highlight marks
 * port faces yellow and heat ports orange, the survey scanner marks bedrock oil and the chunk-centre bedrock ore, and
 * door items cycle their skins once a second.
 */
public final class X5846ReviewFixes implements Scenario {

    private static final int Y = 100, L = 101;
    private static final Pattern TANK = Pattern.compile("(?<![\\w_])tank\\s*:\\s*(-?\\d+)");

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 24);
        c.cmd("fill -24 " + L + " -24 24 " + (L + 12) + " 24 minecraft:air");
        c.cmd("fill -24 " + (L + 13) + " -24 24 " + (L + 22) + " 24 minecraft:air");
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("time set 6000");

        tower(c);
        highlights(c);
        scanner(c);
        doors(c);

        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(tower|highlight|scanner|door|marker)");
    }

    /** Small tower placed by hand facing east (core two blocks west of the click), offered steam under the core and at a side port. */
    private void tower(Client c) {
        int gx = 10, gz = -10;
        Fixtures.setBlock(c, gx, Y, gz, "minecraft:gold_block");
        Fixtures.placeOnTop(c, "hbm:machine_tower_small", gx, Y, gz);
        int cx = gx - 2, cz = gz;
        c.waitTicks(10);
        Expect.that(c.blockEntity(cx, L, cz).toString().contains("input"), "the tower core is two blocks west of the click: " + c.blockEntity(cx, L, cz));
        // a barrel's own port joins a facing port with no duct: one right under the core, one at the east port
        barrel(c, cx, Y, cz);
        barrel(c, cx + 3, L, cz);
        c.waitTicks(60);
        int under = fill(c, cx, Y, cz);
        int side = fill(c, cx + 3, L, cz);
        Expect.that(under == 16_000, "the barrel under the core fed the tower: " + under);
        Expect.that(side >= 0 && side < 16_000, "the barrel at the east port fed nothing: " + side);
        c.teleport(cx + 6.5, L + 2, cz + 4.5);
        c.lookAt(cx + 0.5, L + 1, cz + 0.5);
        c.waitTicks(10);
        c.screenshot("00-tower-side-port");
    }

    private void highlights(Client c) {
        int gx = -10, gz = -10;
        Fixtures.setBlock(c, gx, Y, gz, "minecraft:gold_block");
        String[][] machines = {{"hbm:machine_assembly_machine", "10-assembler"}, {"hbm:heat_boiler", "11-heat-boiler"},
                {"hbm:machine_ashpit", "12-ashpit"}, {"hbm:heater_firebox", "13-firebox"}, {"hbm:machine_tower_small", "14-tower-small"}};
        for (String[] m : machines) {
            Fixtures.giveOnly(c, m[0], 1);
            c.hotbar(1);
            shot(c, m[1], gx + 0.5, L + 1, gz - 3.5, gx + 0.5, Y + 0.98, gz + 0.5);
        }
        c.hotbar(9);
    }

    /** Bedrock ore at the chunk centre's floor, bedrock oil on a scanned column, then a scan from inside chunk (0, 0). */
    private void scanner(Client c) {
        c.cmd("setblock 8 -64 8 hbm:ore_bedrock_block");
        c.cmd("setblock 9 -64 7 hbm:ore_bedrock_block");
        c.cmd("setblock 5 98 0 hbm:ore_bedrock_oil");
        Fixtures.giveOnly(c, "hbm:survey_scanner", 1);
        c.hotbar(1);
        c.teleport(0.5, L, 0.5);
        c.look(0, 90);
        c.waitTicks(4);
        c.markLog();
        c.use(2);
        c.waitTicks(10);
        Expect.that(!c.logSinceMark("Found BEDROCK OIL").isEmpty(), "the scanner reported no bedrock oil");
        c.hotbar(9);
        shot(c, "20-bedrock-ore-markers", 3.5, -56, 1.5, 8.5, -63.5, 8.5);
        shot(c, "21-bedrock-oil-marker", 0.5, 96, -4.5, 5.5, 98.5, 0.5);
    }

    /** The door items in the hotbar, a second apart: each shows its next skin. */
    private void doors(Client c) {
        c.cmd("clear @s");
        String[] doors = {"secure_access_door", "fire_door", "sliding_blast_door", "large_vehicle_door", "water_door",
                "qe_containment", "qe_sliding_door", "round_airlock_door", "sliding_seal_door"};
        for (String door : doors) c.cmd("give @s hbm:" + door);
        c.hotbar(9);
        c.teleport(0.5, L, 0.5);
        c.look(0, 0);
        c.waitTicks(10);
        c.screenshot("30-door-icons-a");
        c.waitTicks(20);
        c.screenshot("31-door-icons-b");
        c.waitTicks(20);
        c.screenshot("32-door-icons-c");
        c.cmd("clear @s");
    }

    private static void shot(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        c.teleport(x, y, z);
        c.lookAt(tx, ty, tz);
        c.waitTicks(8);
        c.teleport(x, y, z);
        c.lookAt(tx, ty, tz);
        c.waitTicks(2);
        c.screenshot(name);
    }

    private static void barrel(Client c, int x, int y, int z) {
        c.cmd("setblock " + x + " " + y + " " + z + " hbm:barrel_steel{mode:2s,tank:16000,tank_max:16000,tank_type:\"hbm:spentsteam\"}");
    }

    private static int fill(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        if (!be.has("nbt") || be.get("nbt").isJsonNull()) return -1;
        Matcher m = TANK.matcher(be.get("nbt").getAsString());
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }
}
