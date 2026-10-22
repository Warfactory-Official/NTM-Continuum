import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * How many of MachineFps's ten machines the frustum culls when the camera faces them and when it faces away
 * (26.1 only: the query reads the renderer's shouldRender, which the other eras expose without a camera).
 */
public final class RcCullCount implements Scenario {

    private static final int X = 700;
    private static final int Y = 100;
    private static final int Z = 700;
    private static final int L = Y + 1;

    private static final String[][] MACHINES = {
            {"machine_chungus", "" + (X - 24), "" + Z},
            {"machine_industrial_turbine", "" + X, "" + Z},
            {"machine_steam_engine", "" + (X + 12), "" + Z},
            {"machine_battery_socket", "" + (X + 24), "" + Z},
            {"machine_assembly_machine", "" + (X - 30), "" + (Z + 16)},
            {"machine_chemical_plant", "" + (X - 20), "" + (Z + 16)},
            {"machine_pumpjack", "" + (X - 10), "" + (Z + 16)},
            {"machine_stirling", "" + (X + 4), "" + (Z + 16)},
            {"machine_battery_redd", "" + (X + 16), "" + (Z + 16)},
            {"machine_fluidtank", "" + (X + 30), "" + (Z + 16)}};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String forceload = (X - 48) + " " + (Z - 40) + " " + (X + 48) + " " + (Z + 60);
        boolean backendSwitched = false;
        try {
            c.cmd("forceload add " + forceload);
            Fixtures.platform(c, X, Y, Z + 8, 60);
            JsonArray positions = new JsonArray();
            for (String[] m : MACHINES) {
                Fixtures.setBlock(c, Integer.parseInt(m[1]), L, Integer.parseInt(m[2]), "hbm:" + m[0] + "[facing=north]");
                JsonArray p = new JsonArray();
                p.add(Integer.parseInt(m[1]));
                p.add(L);
                p.add(Integer.parseInt(m[2]));
                positions.add(p);
            }
            c.cmd("gamemode spectator");
            c.cmd("flywheel backend off");
            backendSwitched = true;
            c.waitTicks(60);
            String facing = count(c, positions, X + 0.5, L + 14, Z + 44.5, 180, 15);
            String away = count(c, positions, X + 0.5, L + 14, Z + 44.5, 0, 15);
            String left = count(c, positions, X + 0.5, L + 14, Z + 44.5, 180 + 50, 15);
            for (String r : new String[] {"facing " + facing, "away " + away, "turned50 " + left}) {
                System.out.println("[cull-count] " + c.node() + " " + r);
            }
            Expect.that(true, "counted");
        } finally {
            if (backendSwitched) c.cmd("flywheel backend DEFAULT");
            c.cmd("gamemode creative");
            c.cmd("forceload remove " + forceload);
        }
    }

    private static String count(Client c, JsonArray positions, double x, double y, double z, double yaw, double pitch) {
        c.teleport(x, y, z);
        c.look(yaw, pitch);
        c.waitTicks(40);
        JsonObject req = new JsonObject();
        req.add("positions", positions);
        JsonObject out = c.query("gem", req);
        int drawn = 0;
        int culled = 0;
        StringBuilder names = new StringBuilder();
        if (System.getenv("CC_DUMP") != null) System.out.println("[cull-count] raw " + out);
        for (JsonElement e : out.getAsJsonArray("positions")) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("should_render") && !o.get("should_render").getAsBoolean()) {
                culled++;
                names.append(o.get("renderer").getAsString()).append(' ');
            } else {
                drawn++;
            }
        }
        return "drawn " + drawn + " culled " + culled + " [" + names.toString().trim() + "]";
    }
}
