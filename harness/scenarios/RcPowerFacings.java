import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/** Release audit: multiblock generators placed from four sides, plus the turbofan with a flame pony upgrade burning kerosene. */
public final class RcPowerFacings implements Scenario {

    private static final int Y = 100;
    private static final String[] IDS = {"machine_combustion_engine", "machine_wood_burner", "machine_large_turbine",
            "machine_turbofan", "machine_steam_engine", "machine_turbine_gas", "machine_industrial_turbine", "machine_chungus"};
    private static final double[][] SIDE = {{2.5, 0.5}, {-1.5, 0.5}, {0.5, 2.5}, {0.5, -1.5}};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 24);
        c.waitTicks(5);
        for (String id : IDS) {
            for (int f = 0; f < 4; f++) {
                c.cmd("fill -16 " + (Y + 1) + " -16 16 " + (Y + 14) + " 16 minecraft:air");
                c.waitTicks(3);
                Fixtures.giveOnly(c, "hbm:" + id, 1);
                c.teleport(SIDE[f][0], Y + 1, SIDE[f][1]);
                c.waitTicks(2);
                c.lookAt(0.5, Y + 0.98, 0.5);
                c.waitTicks(1);
                c.use(2);
                c.waitTicks(15);
                c.teleport(9.5, Y + 4, -9.5);
                c.lookAt(0.5, Y + 2, 0.5);
                c.waitTicks(10);
                c.screenshot(id + "-f" + f);
            }
        }
        flames(c);
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(hbm|golem|render)");
    }

    private void flames(Client c) {
        c.cmd("fill -16 " + (Y + 1) + " -16 16 " + (Y + 14) + " 16 minecraft:air");
        c.waitTicks(3);
        Fixtures.placeOnTop(c, "hbm:machine_turbofan", 0, Y, 0);
        c.waitTicks(10);
        int[] k = core(c);
        c.cmd("data merge block " + k[0] + " " + k[1] + " " + k[2] + " {tank:24000,tank_type:\"hbm:kerosene\"}");
        c.cmd("clear");
        c.cmd("give @s hbm:flame_pony 1");
        c.waitTicks(3);
        c.teleport(0.5, Y + 1, -3.5);
        Fixtures.interact(c, k[0], k[1], k[2]);
        c.waitTicks(10);
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && !s.get("empty").getAsBoolean()) {
                c.slotClick(s.get("index").getAsInt(), 0, "QUICK_MOVE");
            }
        }
        c.waitTicks(3);
        c.key("escape");
        c.waitTicks(30);
        c.teleport(9.5, Y + 3, -9.5);
        c.lookAt(0.5, Y + 2, 0.5);
        c.waitTicks(20);
        c.screenshot("turbofan-afterburner-a");
        c.waitTicks(5);
        c.screenshot("turbofan-afterburner-b");
        System.out.println("[turbofan] nbt " + c.blockEntity(k[0], k[1], k[2]).get("nbt").getAsString().replaceAll("\s+", "").substring(0, 120));
    }

    private static int[] core(Client c) {
        for (int dy = 0; dy <= 3; dy++)
            for (int dz = -4; dz <= 4; dz++)
                for (int dx = -4; dx <= 4; dx++) {
                    JsonObject be = c.blockEntity(dx, Y + 1 + dy, dz);
                    if (be != null && be.has("block_entity") && !be.get("block_entity").isJsonNull()) return new int[]{dx, Y + 1 + dy, dz};
                }
        return new int[]{0, Y + 1, 0};
    }
}
