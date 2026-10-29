import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/**
 * Gas turbine ignition: opens the turbine GUI by a real right-click and presses
 * 1.7's round start/stop button (GUI 88,40) with real mouse clicks. Start must bring the turbine online after the
 * 580-tick startup; stop must then fire (1.7 gated the button on the client's counter, 0 or 579, never on the
 * server's) and run the 225-tick shutdown ramp; once the ramp ends, start must work again.
 *
 * <p>The saved NBT reports {@code state} 1 only while online (1.7's writeToNBT), so it is the positive signal here;
 * the screenshots show the startup display, the running gauges and the ramp down.
 */
public final class GasTurbineStartStop implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("fill -12 " + (Y - 1) + " -12 12 " + (Y + 14) + " 12 minecraft:air");
        Fixtures.platform(c, 0, Y, 0, 12);
        c.waitServerTicks(5);
        Fixtures.placeOnTop(c, "hbm:machine_turbine_gas", 0, Y, 0);
        c.waitServerTicks(10);

        int[] core = findCore(c);
        Expect.that(core != null, "gas turbine core block entity not found");
        c.cmd("data merge block " + core[0] + " " + core[1] + " " + core[2] + " {gas:50000,lube:4000,water:4000}");
        c.waitServerTicks(5);
        Expect.that(state(c, core) == 0, "turbine must start offline");

        open(c);
        c.screenshot("01-idle");
        pressIgnition(c);
        c.waitServerTicks(40);
        c.screenshot("02-startup");
        c.waitServerTicks(600);
        Expect.that(state(c, core) == 1, "start button did not bring the turbine online, nbt: " + nbt(c, core));
        c.screenshot("03-online");

        pressIgnition(c);
        c.waitServerTicks(5);
        Expect.that(state(c, core) == 0, "stop button did not stop the running turbine, nbt: " + nbt(c, core));
        c.waitServerTicks(60);
        c.screenshot("04-ramp-down");
        c.waitServerTicks(200);
        c.screenshot("05-stopped");

        pressIgnition(c);
        c.waitServerTicks(600);
        Expect.that(state(c, core) == 1, "start after a full shutdown did not bring the turbine online, nbt: " + nbt(c, core));
        c.screenshot("06-online-again");
        pressIgnition(c);
        c.waitServerTicks(5);
        Expect.that(state(c, core) == 0, "second stop did not fire");

        c.key("escape");
        c.waitServerTicks(5);
        c.cmd("fill -12 " + (Y - 1) + " -12 12 " + (Y + 14) + " 12 minecraft:air");
        Expect.logCleanSinceMark(c, "(?i)exception.*(gui|screen|render|hbm|turbine)");
    }

    private static void open(Client c) throws Exception {
        c.teleport(0.5, Y + 1, -3.5);
        Fixtures.interact(c, 0, Y + 1, 0);
        c.waitServerTicks(15);
        Expect.that(c.screenClass() != null, "turbine GUI did not open");
        System.out.println("[turbinegas] screen " + c.screenClass());
    }

    private static void pressIgnition(Client c) {
        JsonObject s = c.screen();
        c.click(s.get("left").getAsInt() + 88, s.get("top").getAsInt() + 40);
    }

    private static int[] findCore(Client c) {
        for (int dy = 0; dy <= 3; dy++)
            for (int dx = -6; dx <= 6; dx++)
                for (int dz = -6; dz <= 6; dz++) {
                    JsonObject be = c.blockEntity(dx, Y + 1 + dy, dz);
                    if (be != null && be.has("block_entity") && !be.get("block_entity").isJsonNull()
                            && be.get("block_entity").getAsString().endsWith("TileEntityMachineTurbineGas"))
                        return new int[]{dx, Y + 1 + dy, dz};
                }
        return null;
    }

    private static String nbt(Client c, int[] core) {
        return c.blockEntity(core[0], core[1], core[2]).get("nbt").getAsString().replaceAll("\\s+", "");
    }

    private static int state(Client c, int[] core) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[{,]state:(-?\\d+)").matcher(nbt(c, core));
        Expect.that(m.find(), "no state in turbine nbt");
        return Integer.parseInt(m.group(1));
    }
}
