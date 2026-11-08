import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * GitHub issue 158: the helmet's fallback light on the wall-adjacent layer equals a placed light of the lamp's level,
 * also after looking elsewhere, for the flashlight and after the helmet comes off; walking reports the spread's cost.
 * Needs the fallback: {@code -Pshaders.run=true} with a pack, or {@code -Pgemrender.run=false}.
 */
public final class Gh158LampSpread implements Scenario {

    private static final int X0 = 1300, Y0 = 100, Z0 = 1300, X1 = 1320, Y1 = 108, Z1 = 1320;
    private static final int[] FACE = {1319, 103, 1310};
    private static final int[] MOVED = {1319, 103, 1305};
    private static final int PZ0 = 1303, PZ1 = 1317, PY0 = 101, PY1 = 107;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String hideGui = c.setOption("hide_gui", "true");
        String forceload = (X0 - 8) + " " + (Z0 - 8) + " " + (X1 + 8) + " " + (Z1 + 8);
        try {
            c.cmd("forceload add " + forceload);
            c.cmd("gamemode creative");
            c.cmd("difficulty peaceful");
            c.cmd("time set 18000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("fill " + X0 + " " + Y0 + " " + Z0 + " " + X1 + " " + Y1 + " " + Z1 + " minecraft:stone hollow");
            c.cmd("clear @s");
            c.cmd("effect clear @s");
            c.teleport(X0 + 3.5, Y0 + 1, Z0 + 10.5);
            c.lookAt(X1, Y0 + 3.5, Z0 + 10.5);
            c.setOption("camera", "first_person");
            c.waitTicks(100);

            c.cmd("item replace entity @s armor.head with hbm:no9");
            c.waitTicks(60);
            String[] helmet = plane(c);
            c.screenshot("helmet");

            c.lookAt(X1, Y0 + 3.5, MOVED[2] + 0.5);
            c.waitTicks(8);
            String[] moved = plane(c);
            c.lookAt(X1, Y0 + 3.5, Z0 + 10.5);
            c.waitTicks(20);

            JsonObject before = lamp(c);
            for (int step = 0; step < 24; step++) {
                c.teleport(X0 + 3.5, Y0 + 1, Z0 + 4.5 + step * 0.5);
                c.lookAt(X1, Y0 + 3.5, Z0 + 4.5 + step * 0.5);
                c.waitTicks(2);
            }
            JsonObject walked = lamp(c);
            c.teleport(X0 + 3.5, Y0 + 1, Z0 + 10.5);
            c.lookAt(X1, Y0 + 3.5, Z0 + 10.5);
            c.waitTicks(20);
            String[] back = plane(c);
            JsonObject still = lamp(c);
            c.waitTicks(40);
            JsonObject stillLater = lamp(c);

            c.cmd("item replace entity @s armor.head with minecraft:air");
            c.waitTicks(8);
            String[] off = plane(c);
            JsonObject offLamp = lamp(c);

            c.cmd("give @s hbm:gun_jshotgun");
            c.hotbar(1);
            c.waitTicks(10);
            c.use(2);
            c.waitTicks(40);
            String[] flashlight = plane(c);
            c.use(2);
            c.waitTicks(10);
            c.cmd("clear @s");

            c.cmd("setblock " + FACE[0] + " " + FACE[1] + " " + FACE[2] + " minecraft:light[level=15]");
            c.waitTicks(60);
            String[] source = plane(c);
            c.screenshot("light15");
            c.cmd("setblock " + FACE[0] + " " + FACE[1] + " " + FACE[2] + " minecraft:air");

            String[] bfs15 = expected(spread(FACE, 15));
            String[] bfsMoved = expected(spread(MOVED, 15));
            String[] bfs11 = expected(spread(FACE, 11));
            print(c, "helmet (client)", helmet);
            print(c, "light block (client)", source);
            print(c, "level-15 spread", bfs15);
            print(c, "helmet after looking 5 blocks along (client)", moved);
            print(c, "flashlight, level 11 (client)", flashlight);
            print(c, "helmet off after 8 ticks (client)", off);
            System.out.println("[Gh158] " + c.node() + " before walking: " + before);
            System.out.println("[Gh158] " + c.node() + " after 24 half-block steps along the wall, 2 ticks apart: " + walked);
            System.out.println("[Gh158] " + c.node() + " standing still: " + still + " then 40 ticks later " + stillLater);
            System.out.println("[Gh158] " + c.node() + " helmet off: " + offLamp);
            Expect.that(String.join("|", source).equals(String.join("|", bfs15)), "the read-back matches vanilla propagation");
            Expect.that(String.join("|", helmet).equals(String.join("|", bfs15)), "the helmet lights like a level-15 light");
            Expect.that(String.join("|", moved).equals(String.join("|", bfsMoved)), "nothing is left at the old spot");
            Expect.that(String.join("|", back).equals(String.join("|", bfs15)), "back at the first spot after walking");
            Expect.that(String.join("|", flashlight).equals(String.join("|", bfs11)), "the flashlight lights like a level-11 light");
            Expect.that(String.join("", off).replace(" ", "").replace("0", "").isEmpty(), "the lamp's light is gone when off");
            Expect.that(offLamp.get("fallback_lit").getAsInt() == 0, "every lit cell handed back: " + offLamp);
            Expect.that(stillLater.get("spreads").getAsInt() == still.get("spreads").getAsInt(),
                    "standing still does not redo the spread: " + still + " / " + stillLater);
        } finally {
            c.releaseAll();
            c.cmd("clear @s");
            c.cmd("time set 6000");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            c.cmd("forceload remove " + forceload);
        }
    }

    private static JsonObject lamp(Client c) {
        return c.query("hbm_lamp_lights", new JsonObject());
    }

    private static String[] plane(Client c) {
        JsonObject p = new JsonObject();
        p.addProperty("x", FACE[0]);
        p.addProperty("y0", PY0);
        p.addProperty("y1", PY1);
        p.addProperty("z0", PZ0);
        p.addProperty("z1", PZ1);
        JsonArray rows = c.query("hbm_lamp_lights", p).getAsJsonArray("plane");
        String[] out = new String[rows.size()];
        for (int i = 0; i < out.length; i++) out[i] = rows.get(i).getAsString();
        return out;
    }

    private static void print(Client c, String what, String[] rows) {
        System.out.println("[Gh158] " + c.node() + " " + what + ", x=" + FACE[0] + ", y " + PY1 + ".." + PY0 + ", z " + PZ0 + ".." + PZ1);
        for (String row : rows) System.out.println("[Gh158]   " + row);
    }

    private static boolean opaque(int x, int y, int z) {
        boolean inside = x > X0 && x < X1 && y > Y0 && y < Y1 && z > Z0 && z < Z1;
        return !inside && x >= X0 && x <= X1 && y >= Y0 && y <= Y1 && z >= Z0 && z <= Z1;
    }

    /** A placed light of {@code level} at {@code at}: one less per block, stopped by the room's walls. */
    private static Map<String, Integer> spread(int[] at, int level) {
        Map<String, Integer> light = new HashMap<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        light.put(at[0] + "," + at[1] + "," + at[2], level);
        queue.add(new int[] {at[0], at[1], at[2], level});
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            int l = p[3] - 1;
            if (l <= 0) continue;
            for (int[] d : dirs) {
                int x = p[0] + d[0], y = p[1] + d[1], z = p[2] + d[2];
                String key = x + "," + y + "," + z;
                if (opaque(x, y, z) || light.getOrDefault(key, 0) >= l) continue;
                light.put(key, l);
                queue.add(new int[] {x, y, z, l});
            }
        }
        return light;
    }

    private static String[] expected(Map<String, Integer> light) {
        String[] rows = new String[PY1 - PY0 + 1];
        for (int y = PY1; y >= PY0; y--) {
            StringBuilder row = new StringBuilder();
            for (int z = PZ0; z <= PZ1; z++) row.append(String.format("%3d", light.getOrDefault(FACE[0] + "," + y + "," + z, 0)));
            rows[PY1 - y] = row.toString();
        }
        return rows;
    }
}
