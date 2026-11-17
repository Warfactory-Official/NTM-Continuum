import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Frame and tick cost of common scenes, one {@code /golem profiler 15} capture each, written to the run's {@code golem}
 * folder in this order: 48 idle machines in view, machines running (turbofans burning kerosene, arc furnaces, blast
 * furnaces and crucibles smoking, burning RBMK debris), and a crowd of HBM mobs. Asserts nothing.
 */
public final class GeneralProfile implements Scenario {

    private static final int X = 1500;
    private static final int Y = 100;
    private static final int Z = 1500;
    private static final int G = Y + 1;
    private static final String[] IDLE = {
            "machine_chemical_plant", "machine_mixer", "machine_crystallizer", "machine_cyclotron", "machine_radgen",
            "machine_liquefactor", "machine_assembly_machine", "machine_pumpjack"};

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        String forceload = (X - 48) + " " + (Z - 48) + " " + (X + 48) + " " + (Z + 48);
        String hideGui = null, maxFps = null, vsync = null, inactivity = null, distance = null;
        try {
            hideGui = c.setOption("hide_gui", "true");
            maxFps = c.setOption("max_fps", "260");
            vsync = c.setOption("vsync", "false");
            inactivity = c.setOption("inactivity_fps", "minimized");
            distance = c.setOption("render_distance", "12");
            c.cmd("gamerule sendCommandFeedback false");
            c.cmd("forceload add " + forceload);
            c.waitTicks(40);
            Fixtures.platform(c, X, Y, Z, 44);
            c.cmd("time set 6000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("weather clear");

            for (int row = 0; row < 6; row++) {
                for (int column = 0; column < 8; column++) {
                    String machine = IDLE[(row * 8 + column) % IDLE.length];
                    Fixtures.setBlock(c, X - 35 + column * 10, G, Z - 25 + row * 10, "hbm:" + machine + "[facing=north]");
                }
            }
            c.cmd("gamemode spectator");
            view(c, X, G + 30, Z - 50, X, G, Z);
            capture(c, "1-idle-machines");

            clear(c);
            for (int i = 0; i < 4; i++) {
                int[] p = place(c, X - 24 + i * 12, Z - 10, "hbm:machine_turbofan[facing=north]", "TileEntityMachineTurbofan");
                c.cmd("data merge block " + at(p) + " {tank:24000,tank_type:\"hbm:kerosene\"}");
            }
            for (int i = 0; i < 2; i++) {
                int[] p = place(c, X - 12 + i * 24, Z + 6, "hbm:machine_arc_furnace[facing=north]", "TileEntityMachineArcFurnaceLarge");
                c.cmd("data merge block " + at(p) + " {power:100000000L,lid:1f,delay:0,inventory:{Items:["
                        + stack(0, "hbm:arc_electrode", 1) + "," + stack(1, "hbm:arc_electrode", 1) + ","
                        + stack(2, "hbm:arc_electrode", 1) + "," + stack(25, "minecraft:flint", 64) + "]}}");
            }
            for (int i = 0; i < 2; i++) {
                int[] p = place(c, X - 30 + i * 60, Z + 6, "hbm:machine_blast_furnace[facing=north]", "TileEntityMachineBlastFurnace");
                c.cmd("data merge block " + at(p) + " {fuel:64000,t1:1000,inventory:{Items:["
                        + stack(1, "minecraft:iron_ingot", 64) + "," + stack(2, "minecraft:sand", 64) + "]}}");
            }
            for (int i = 0; i < 2; i++) {
                int[] p = place(c, X - 20 + i * 40, Z + 18, "hbm:machine_crucible[facing=north]", "TileEntityCrucible");
                c.cmd("data merge block " + at(p) + " {rec:[{mat:\"iron\",amount:720}]}");
            }
            for (int x = -16; x <= 16; x += 2) Fixtures.setBlock(c, X + x, G, Z + 26, "hbm:rbmk_debris_burning");
            view(c, X, G + 22, Z - 40, X, G, Z + 6);
            c.screenshot("2-running-machines");
            capture(c, "2-running-machines");

            clear(c);
            c.cmd("difficulty normal");
            for (int i = 0; i < 48; i++) {
                String mob = i % 3 == 0 ? "hbm:entity_glyphid_brawler" : i % 3 == 1 ? "hbm:entity_glyphid" : "hbm:entity_mob_nuclear_creeper";
                c.cmd("summon " + mob + " " + (X - 21 + (i % 8) * 6) + " " + G + " " + (Z - 15 + (i / 8) * 6)
                        + " {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
            }
            view(c, X, G + 14, Z - 34, X, G, Z);
            capture(c, "3-mobs");
            c.cmd("kill @e[type=!minecraft:player]");
        } finally {
            c.cmd("difficulty peaceful");
            c.cmd("gamemode creative");
            c.cmd("gamerule sendCommandFeedback true");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            if (maxFps != null) c.setOption("max_fps", maxFps);
            if (vsync != null) c.setOption("vsync", vsync);
            if (inactivity != null) c.setOption("inactivity_fps", inactivity);
            if (distance != null) c.setOption("render_distance", distance);
            c.cmd("forceload remove " + forceload);
        }
    }

    private static void view(Client c, double x, double y, double z, double tx, double ty, double tz) {
        c.teleport(x + 0.5, y, z + 0.5);
        c.lookAt(tx + 0.5, ty, tz + 0.5);
        c.waitTicks(100);
    }

    private static void capture(Client c, String name) throws Exception {
        c.screenshot(name);
        c.cmd("golem profiler 15");
        c.waitTicks(20 * 20);
    }

    private static void clear(Client c) {
        c.cmd("kill @e[type=!minecraft:player]");
        for (int y = G; y < G + 24; y += 4) {
            c.cmd("fill " + (X - 44) + " " + y + " " + (Z - 44) + " " + (X + 44) + " " + (y + 3) + " " + (Z + 44) + " minecraft:air");
        }
        c.cmd("kill @e[type=item]");
        c.waitTicks(10);
    }

    private static int[] place(Client c, int x, int z, String block, String cls) {
        c.cmd("setblock " + x + " " + G + " " + z + " " + block);
        c.waitTicks(8);
        for (int dy = 0; dy <= 2; dy++) for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            JsonObject be = c.blockEntity(x + dx, G + dy, z + dz);
            if (be.has("block_entity") && !be.get("block_entity").isJsonNull() && be.get("block_entity").getAsString().endsWith(cls)) {
                return new int[]{x + dx, G + dy, z + dz};
            }
        }
        throw new AssertionError("no " + cls + " placed by " + block);
    }

    private static String at(int[] p) {
        return p[0] + " " + p[1] + " " + p[2];
    }

    private static String stack(int slot, String id, int count) {
        return "{Slot:" + slot + "b,id:\"" + id + "\",count:" + count + ",Count:" + count + "b}";
    }
}
