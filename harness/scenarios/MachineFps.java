import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Frame rate with gem-declared machines in view versus out of view, with the instancing engine on and off.
 *
 * <p>Not a pass/fail check: it reports four numbers in chat and the report so a "machines in view tank the frame
 * rate" claim can be measured instead of eyeballed. Run with {@code up --show} (an iconified window is capped at
 * 10 fps) and one client, so the two are not sharing the GPU. The scene is RenderFallback's ten machines.
 */
public final class MachineFps implements Scenario {

    private static final int X = 700;
    private static final int Y = 100;
    private static final int Z = 700;
    private static final int L = Y + 1;
    private static final int ROW_A = Z;
    private static final int ROW_B = Z + 16;

    private static final String[][] MACHINES = {
            {"machine_chungus", "" + (X - 24), "" + ROW_A},
            {"machine_industrial_turbine", "" + X, "" + ROW_A},
            {"machine_steam_engine", "" + (X + 12), "" + ROW_A},
            {"machine_battery_socket", "" + (X + 24), "" + ROW_A},
            {"machine_assembly_machine", "" + (X - 30), "" + ROW_B},
            {"machine_chemical_plant", "" + (X - 20), "" + ROW_B},
            {"machine_pumpjack", "" + (X - 10), "" + ROW_B},
            {"machine_stirling", "" + (X + 4), "" + ROW_B},
            {"machine_battery_redd", "" + (X + 16), "" + ROW_B},
            {"machine_fluidtank", "" + (X + 30), "" + ROW_B}};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String forceload = (X - 48) + " " + (Z - 16) + " " + (X + 48) + " " + (Z + 40);
        String hideGui = null, renderDistance = null, fov = null, maxFps = null, vsync = null, inactivity = null;
        boolean backendSwitched = false;
        List<String> results = new ArrayList<>();
        try {
            c.cmd("forceload add " + forceload);
            Fixtures.platform(c, X, Y, Z + 8, 44);
            c.cmd("time set 6000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("weather clear");
            hideGui = c.setOption("hide_gui", "true");
            renderDistance = c.setOption("render_distance", "12");
            fov = c.setOption("fov", "70");
            maxFps = c.setOption("max_fps", "260");
            vsync = c.setOption("vsync", "false");
            inactivity = c.setOption("inactivity_fps", "minimized");

            String which = System.getenv().getOrDefault("MF_MACHINES", "all");
            for (String[] m : MACHINES) {
                if (!which.equals("all") && !which.contains(m[0])) continue;
                Fixtures.setBlock(c, Integer.parseInt(m[1]), L, Integer.parseInt(m[2]), "hbm:" + m[0] + "[facing=north]");
            }
            // MF_FILL=hbm:some_block: a 24x24 carpet of one small gem block (576 block entities), as in a built-up base
            String fill = System.getenv("MF_FILL");
            if (fill != null && !fill.isEmpty()) {
                c.cmd("fill " + (X - 12) + " " + L + " " + (Z + 4) + " " + (X + 11) + " " + L + " " + (Z + 27) + " " + fill);
            }
            c.cmd("gamemode spectator");
            c.waitTicks(40);

            results.add("engine-on  facing machines: " + fps(c, true));
            results.add("engine-on  facing away    : " + fps(c, false));
            results.add("gem: " + c.query("gem", new JsonObject()));

            c.cmd("flywheel backend off");
            backendSwitched = true;
            c.waitTicks(20);
            results.add("engine-off facing machines: " + fps(c, true));
            results.add("engine-off facing away    : " + fps(c, false));

            for (String r : results) c.chat("machine-fps: " + r);
            Expect.that(true, String.join(" | ", results));
        } finally {
            if (backendSwitched) c.cmd("flywheel backend DEFAULT");
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            if (renderDistance != null) c.setOption("render_distance", renderDistance);
            if (fov != null) c.setOption("fov", fov);
            if (maxFps != null) c.setOption("max_fps", maxFps);
            if (vsync != null) c.setOption("vsync", vsync);
            if (inactivity != null) c.setOption("inactivity_fps", inactivity);
            c.cmd("forceload remove " + forceload);
            for (String r : results) System.out.println("[machine-fps] " + c.node() + " " + r);
        }
    }

    /** Minecraft's own per-second fps counter, read five times after a settle. */
    private static String fps(Client c, boolean facing) {
        // MF_CLOSE=1: stand a few blocks from the Chungus so machines fill the screen (fragment-bound cost)
        boolean close = System.getenv("MF_CLOSE") != null;
        if (close) c.teleport(X - 24.5, L + 4, ROW_A - 6.5);
        else c.teleport(X + 0.5, L + 14, Z + 44.5);
        if (facing) c.lookAt(close ? X - 24 : X + 0.5, L + 2, close ? ROW_A + 0.5 : Z + 8.5);
        else c.lookAt(X + 0.5, L + 14, close ? ROW_A - 200.5 : Z + 200.5);
        c.waitTicks(60);
        List<Integer> samples = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            c.waitTicks(22);
            samples.add(c.status().get("fps").getAsInt());
        }
        if (facing) c.screenshot(facing ? "facing" : "away");
        return samples.toString();
    }
}
