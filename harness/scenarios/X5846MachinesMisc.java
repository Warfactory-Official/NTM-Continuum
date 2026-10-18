import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The 1.7 X5808 to X5846 door skins (sliding seal trefoil, industrial sliding blast door, black sliding door, red vehicle
 * door), ULTRA_LARP_MODE refusing a hand-opened door, the particle accelerator dipole menu showing its gold coil, and
 * the compact compressor's own model.
 */
public final class X5846MachinesMisc implements Scenario {

    private static final int Y = 100, L = 101, Z = 0;
    private static final String[][] DOORS = {
            {"hbm:sliding_seal_door", "-24", "2"},
            {"hbm:sliding_blast_door", "-12", "1"},
            {"hbm:qe_sliding_door", "0", "1"},
            {"hbm:large_vehicle_door", "14", "3"}};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 30);
        c.cmd("fill -30 " + L + " -30 30 " + (L + 12) + " 30 minecraft:air");
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("time set 6000");

        List<int[]> cores = new ArrayList<>();
        for (String[] door : DOORS) {
            int x = Integer.parseInt(door[1]);
            placeSouth(c, door[0], x);
            int[] core = findDoorCore(c, x);
            Expect.that(core != null, door[0] + " placed a door core");
            c.cmd("data merge block " + core[0] + " " + core[1] + " " + core[2] + " {skin:" + door[2] + "b}");
            cores.add(core);
        }
        c.waitTicks(10);
        for (int i = 0; i < DOORS.length; i++) {
            int[] core = cores.get(i);
            Expect.that(nbt(c, core[0], core[1], core[2]).contains("skin:" + DOORS[i][2] + "b"),
                    DOORS[i][0] + " keeps skin " + DOORS[i][2]);
        }
        for (int i = 0; i < DOORS.length; i++) {
            int[] core = cores.get(i);
            int back = i == 3 ? 9 : 5;
            c.teleport(core[0] + 0.5, L + 1.5, core[2] + back + 0.5);
            c.lookAt(core[0] + 0.5, L + 1.5, core[2] + 0.5);
            c.waitTicks(20);
            c.screenshot("0" + i + "-door-" + DOORS[i][0].substring(4) + "-skin" + DOORS[i][2]);
        }

        ultraLarp(c, cores.get(1));
        dipole(c);
        compressor(c);

        c.cmd("ntmserver set ULTRA_LARP_MODE false");
        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(door|dipole|compressor)");
    }

    /** With ULTRA_LARP_MODE on, a right-click leaves the sliding blast door shut; off, it starts to open. */
    private void ultraLarp(Client c, int[] core) {
        c.cmd("ntmserver set ULTRA_LARP_MODE true");
        c.hotbar(9);
        standAt(c, -11.5, L, Z + 3.5);
        Fixtures.interact(c, core[0], core[1] + 1, core[2]);
        c.waitTicks(10);
        Expect.that(nbt(c, core[0], core[1], core[2]).contains("state:0b"), "ULTRA_LARP_MODE keeps the door shut: " + nbt(c, core[0], core[1], core[2]));
        c.cmd("ntmserver set ULTRA_LARP_MODE false");
        Fixtures.interact(c, core[0], core[1] + 1, core[2]);
        c.waitTicks(10);
        Expect.that(!nbt(c, core[0], core[1], core[2]).contains("state:0b"), "by hand the door opens again: " + nbt(c, core[0], core[1], core[2]));
        c.waitTicks(40);
        c.screenshot("10-blast-door-opened-by-hand");
    }

    private void dipole(Client c) {
        int x = 0, z = -14;
        Fixtures.giveOnly(c, "hbm:pa_dipole", 1);
        standAt(c, x + 0.5, L, z + 3.5);
        c.lookAt(x + 0.5, Y + 0.98, z + 1.5);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(4);
        c.hotbar(9);
        Fixtures.interact(c, x, L, z + 1);
        c.waitScreen("GUIPADipole", 5_000L);
        Expect.screenStaysOpen(c, "GUIPADipole", 5);
        give(c, "hbm:pa_coil_gold");
        c.waitTicks(5);
        boolean coil = false;
        for (JsonObject s : machineSlots(c)) coil |= s.has("id") && s.get("id").getAsString().equals("hbm:pa_coil_gold");
        Expect.that(coil, "the gold coil sits in the dipole");
        c.screenshot("11-dipole-gold-coil");
        c.key("escape");
        c.waitScreen("none", 5_000L);
    }

    private void compressor(Client c) {
        int x = 14, z = -14;
        Fixtures.giveOnly(c, "hbm:machine_compressor_compact", 1);
        standAt(c, x + 0.5, L, z + 3.5);
        c.lookAt(x + 0.5, Y + 0.98, z + 1.5);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(10);
        c.teleport(x + 4.5, L + 3, z + 5.5);
        c.lookAt(x + 0.5, L + 1, z + 0.5);
        c.waitTicks(20);
        c.screenshot("12-compact-compressor-model");
    }

    private static int[] findDoorCore(Client c, int x) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    if (nbt(c, x + dx, L + dy, Z + 1 + dz).contains("openTicks")) return new int[] {x + dx, L + dy, Z + 1 + dz};
                }
            }
        }
        return null;
    }

    private static void placeSouth(Client c, String item, int x) {
        Fixtures.giveOnly(c, item, 1);
        standAt(c, x + 0.5, L, Z + 1 + 3 + 0.5);
        c.lookAt(x + 0.5, Y + 0.98, Z + 1 + 0.5);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(4);
    }

    private static void standAt(Client c, double x, double y, double z) {
        c.cmd("attribute @s minecraft:player.block_interaction_range base set 6");
        c.teleport(x, y, z);
        c.waitTicks(4);
    }

    private static void give(Client c, String id) {
        c.cmd("give @s " + id + " 1");
        c.waitTicks(5);
        int from = -1;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && s.get("id").getAsString().equals(id)) from = s.get("index").getAsInt();
        }
        Expect.that(from >= 0, id + " visible in the player inventory");
        c.slotClick(from, 0, "QUICK_MOVE");
    }

    private static List<JsonObject> machineSlots(Client c) {
        List<JsonObject> out = new ArrayList<>();
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("player_inventory").getAsBoolean()) out.add(s);
        }
        return out;
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
    }
}
