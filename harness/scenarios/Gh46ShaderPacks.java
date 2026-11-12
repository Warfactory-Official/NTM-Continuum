import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 46: gem machines vanished under an Iris shader pack while still casting shadows. Needs
 * {@code -Pshaders.run=true} and {@code enableShaders=true} in the run's {@code config/iris.properties}; without a
 * pack it only shoots the row. Under a pack GemRender owns the machines only while a Flywheel compat backend
 * (Colorwheel, irisflw) is on; otherwise their renderers draw all of them. GemRender's 26.1 Flywheel fork keeps its own
 * backend on under a pack, so on 26.1 only a namespaced backend counts. Compare the shots against a
 * {@code -Pgemrender.run=false} run under the same pack.
 */
public final class Gh46ShaderPacks implements Scenario {

    private static final int X = 700;
    private static final int Y = 100;
    private static final int Z = 700;
    private static final int L = Y + 1;
    private static final String[] MACHINES = {
            "machine_chemical_plant[facing=north]", "machine_fluidtank[facing=north]", "machine_microwave[facing=north]",
            "machine_cyclotron[facing=north]", "machine_radar"};
    private static final int[] OFFSETS = {0, 6, 11, 17, 24};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String forceload = (X - 24) + " " + (Z - 24) + " " + (X + 48) + " " + (Z + 24);
        String hideGui = null;
        try {
            hideGui = c.setOption("hide_gui", "true");
            c.cmd("forceload add " + forceload);
            c.waitTicks(40);
            Fixtures.platform(c, X + 12, Y, Z, 24);
            c.cmd("time set 6000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("weather clear");
            for (int i = 0; i < MACHINES.length; i++) Fixtures.setBlock(c, X + OFFSETS[i], L, Z, "hbm:" + MACHINES[i]);
            c.cmd("gamemode spectator");
            c.waitTicks(200);
            c.teleport(X + 12.5, L + 7, Z + 20.5);
            c.lookAt(X + 12.5, L + 1.5, Z + 0.5);
            c.waitTicks(40);

            JsonObject report = gem(c);
            boolean shaders = report.has("shaderPack") && report.get("shaderPack").getAsBoolean();
            String backend = report.get("backend").getAsString();
            boolean instanced = report.get("visualizing").getAsBoolean()
                    && (backend.contains(":") || !c.node().startsWith("26."));
            c.chat("gh46: node=" + c.node() + " backend=" + backend
                    + " shaderPack=" + shaders + " instanced=" + instanced);
            c.screenshot("gh46-row");
            c.teleport(X + 19.5, L + 3, Z + 6.5);
            c.lookAt(X + 17.5, L + 1, Z + 0.5);
            c.waitTicks(20);
            c.screenshot("gh46-cutouts");
            c.cmd("time set 1500");
            c.teleport(X + 12.5, L + 12, Z - 14.5);
            c.lookAt(X + 12.5, L, Z + 2.5);
            c.waitTicks(40);
            c.screenshot("gh46-shadows");
            if (shaders) {
                List<String> wrong = new ArrayList<>();
                JsonArray positions = report.getAsJsonArray("positions");
                for (int i = 0; i < MACHINES.length; i++) {
                    JsonObject p = positions.get(i).getAsJsonObject();
                    if (p.get("owned").getAsBoolean() != instanced) wrong.add(MACHINES[i] + " " + p);
                    if (!instanced && p.get("replayed").getAsInt() < 10) wrong.add(MACHINES[i] + " " + p);
                }
                Expect.that(wrong.isEmpty(), "under a shader pack every machine is "
                        + (instanced ? "instanced" : "drawn by its renderer") + ": " + wrong);
            }
        } finally {
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            c.cmd("forceload remove " + forceload);
        }
    }

    private static JsonObject gem(Client c) {
        JsonObject params = new JsonObject();
        JsonArray positions = new JsonArray();
        for (int offset : OFFSETS) {
            JsonArray xyz = new JsonArray();
            xyz.add(X + offset);
            xyz.add(L);
            xyz.add(Z);
            positions.add(xyz);
        }
        params.addProperty("trace", true);
        params.add("positions", positions);
        c.query("gem", params);
        c.waitTicks(40);
        params.remove("trace");
        return c.query("gem", params);
    }
}
