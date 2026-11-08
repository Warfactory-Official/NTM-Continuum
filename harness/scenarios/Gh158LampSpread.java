import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * GitHub issue 158, the lamp's spread: the client block light the mining helmet writes on the wall-adjacent air layer
 * must equal 1.7 {@code ArmorNo9.lightUpRecursively}, a depth-first walk whose breadcrumb keeps the first, often low,
 * value a block gets. A level-15 light block at the same cell is the control (vanilla propagation, 15 minus distance).
 * Shots {@code helmet} and {@code light15} are taken from the same spot. Needs the fallback: {@code -Pshaders.run=true}
 * with a pack selected, or {@code -Pgemrender.run=false}.
 */
public final class Gh158LampSpread implements Scenario {

    private static final int X0 = 1300, Y0 = 100, Z0 = 1300, X1 = 1320, Y1 = 108, Z1 = 1320;
    private static final int[] FACE = {1319, 103, 1310};
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
            c.cmd("item replace entity @s armor.head with minecraft:air");
            c.waitTicks(40);
            String[] off = plane(c);

            c.cmd("setblock " + FACE[0] + " " + FACE[1] + " " + FACE[2] + " minecraft:light[level=15]");
            c.waitTicks(60);
            String[] source = plane(c);
            c.screenshot("light15");
            c.cmd("setblock " + FACE[0] + " " + FACE[1] + " " + FACE[2] + " minecraft:air");

            String[] dfs = expected(walk17());
            String[] bfs = expected(source15());
            print(c, "helmet (client)", helmet);
            print(c, "1.7 walk", dfs);
            print(c, "light block (client)", source);
            print(c, "level-15 source", bfs);
            print(c, "helmet off", off);
            Expect.that(String.join("|", helmet).equals(String.join("|", dfs)), "the helmet's light is 1.7's walk");
            Expect.that(String.join("|", source).equals(String.join("|", bfs)), "the read-back matches vanilla propagation");
            Expect.that(String.join("", off).replace(" ", "").replace("0", "").isEmpty(), "the lamp's light is gone when off");
        } finally {
            c.cmd("clear @s");
            c.cmd("time set 6000");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            c.cmd("forceload remove " + forceload);
        }
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

    /** 1.7's walk from the face block with level 15 (the hit is 16.5 blocks away: min(15, 25 - 16.5 * 25 / 50)). */
    private static Map<String, Integer> walk17() {
        Map<String, Integer> light = new HashMap<>();
        walk(light, new HashSet<>(), FACE[0], FACE[1], FACE[2], 15);
        return light;
    }

    private static void walk(Map<String, Integer> light, Set<String> crumb, int x, int y, int z, int l) {
        if (l <= 0) return;
        String key = x + "," + y + "," + z;
        if (!crumb.add(key)) return;
        if (opaque(x, y, z)) return;
        light.merge(key, l, Math::max);
        walk(light, crumb, x + 1, y, z, l - 1);
        walk(light, crumb, x - 1, y, z, l - 1);
        walk(light, crumb, x, y + 1, z, l - 1);
        walk(light, crumb, x, y - 1, z, l - 1);
        walk(light, crumb, x, y, z + 1, l - 1);
        walk(light, crumb, x, y, z - 1, l - 1);
    }

    private static Map<String, Integer> source15() {
        Map<String, Integer> light = new HashMap<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        light.put(FACE[0] + "," + FACE[1] + "," + FACE[2], 15);
        queue.add(new int[] {FACE[0], FACE[1], FACE[2], 15});
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
