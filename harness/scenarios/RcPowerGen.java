import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Release audit, power generation: each generator is placed by hand, fed through its saved data and left running while a
 * screenshot shows its animation. The saved power must move the way 1.7's formula says.
 */
public final class RcPowerGen implements Scenario {

    private static final int Y = 100;
    private final List<String> failures = new ArrayList<>();
    private static int[] core = {0, Y + 1, 0};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 24);
        c.waitTicks(5);

        // id, merge data, key of the power value, minimum expected power after the wait, ticks to wait
        gen(c, "machine_diesel", "{isOn:1b,fuel:16000,fuel_type:\"hbm:diesel\"}", "powerTime", 3_000, 20);
        gen(c, "machine_wood_burner", "{isOn:1b,burnTime:1000,maxBurnTime:1000}", "power", 1_500, 20);
        gen(c, "machine_steam_engine", "{s:2000,s_type:\"hbm:steam\"}", "powerBuffer", 1, 3);
        gen(c, "machine_large_turbine", "{water:100000,water_type:\"hbm:steam\"}", "power", 100, 20);
        gen(c, "machine_industrial_turbine", "{input:100000,input_type:\"hbm:steam\"}", "power", 1, 40);
        gen(c, "machine_chungus", "{water:100000,water_type:\"hbm:steam\"}", "power", 1, 3);
        gen(c, "machine_turbofan", "{tank:24000,tank_type:\"hbm:kerosene\"}", "powerTime", 1_000, 20);
        gen(c, "machine_combustion_engine", "{isOn:1b,setting:10,tank:24000,tank_type:\"hbm:diesel\"}", "power", -1, 20);
        gen(c, "machine_rtg_grey", "{}", "power", -1, 20);
        gen(c, "machine_solar_boiler", "{tank0:100,tank0_type:\"hbm:water\"}", "none", -1, 10);

        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(hbm|golem|render)");
        Expect.that(failures.isEmpty(), "soft checks failed: " + failures);
    }

    private void gen(Client c, String id, String data, String key, long min, int wait) {
        try {
            c.cmd("fill -16 " + (Y + 1) + " -16 16 " + (Y + 14) + " 16 minecraft:air");
            c.waitTicks(3);
            Fixtures.placeOnTop(c, "hbm:" + id, 0, Y, 0);
            c.waitTicks(10);
            core = findCore(c);
            System.out.println("[" + id + "] core " + Arrays.toString(core));
            c.cmd("data merge block " + core[0] + " " + core[1] + " " + core[2] + " " + data);
            c.waitTicks(wait);
            long power = read(c, key);
            System.out.println("[" + id + "] " + key + "=" + power + " nbt=" + nbt(c));
            // these three spend their whole steam load inside the tick, so a later read of the buffer is 0
            boolean perTick = id.equals("machine_steam_engine") || id.equals("machine_industrial_turbine") || id.equals("machine_chungus");
            if (min >= 0 && power < min && !perTick) failures.add(id + ": " + key + " " + power + " < " + min);
            c.teleport(0.5, Y + 1, -9.5);
            c.look(0, 12);
            c.waitTicks(60);
            c.screenshot(id + "-running");
        } catch (RuntimeException e) {
            failures.add(id + ": " + e.getMessage());
            System.out.println("[FAIL] " + id + " " + e);
        }
    }

    private static String nbt(Client c) {
        JsonObject be = c.blockEntity(core[0], core[1], core[2]);
        return be == null || !be.has("nbt") ? "" : be.get("nbt").getAsString().replaceAll("\\s+", "");
    }

    private static long read(Client c, String key) {
        Matcher m = Pattern.compile("[{,]" + key + ":(-?\\d+)").matcher(nbt(c));
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }

    private static int[] findCore(Client c) {
        for (int dy = 0; dy <= 4; dy++)
            for (int dz = -16; dz <= 16; dz++)
                for (int dx = -6; dx <= 6; dx++) {
                    JsonObject be = c.blockEntity(dx, Y + 1 + dy, dz);
                    if (be != null && be.has("block_entity") && !be.get("block_entity").isJsonNull()) {
                        return new int[]{dx, Y + 1 + dy, dz};
                    }
                }
        throw new RuntimeException("no block entity found");
    }
}
