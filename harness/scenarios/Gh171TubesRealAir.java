import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub issue 171 with the air brought the way a player does: a compressor placed by hand, kept fed with air at
 * pressure 0 and power, and a duct run from its nearest port to the compressor tube's free side. Tubes laid by hand
 * and set with the screwdriver; chest to chest, then crate to crate.
 */
public final class Gh171TubesRealAir implements Scenario {

    private static final int Y = 100, G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(180_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 16);
        c.cmd("gamemode creative");
        c.cmd("fill -16 " + G + " -16 16 " + (G + 5) + " 16 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.waitTicks(5);
        List<String> wrong = new ArrayList<>();
        line(c, "minecraft:chest", 0, "chest", wrong);
        c.cmd("fill -16 " + G + " -16 16 " + (G + 5) + " 16 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.waitTicks(5);
        line(c, "hbm:crate_iron", 0, "crate", wrong);
        Expect.that(wrong.isEmpty(), "tubes with air from a compressor: " + wrong);
        Expect.logCleanSinceMark(c, "(?i)exception.*(hbm|pneum|tube)");
    }

    private static void line(Client c, String inventory, int z, String tag, List<String> wrong) {
        for (int x = -1; x <= 1; x++) Fixtures.placeOnTop(c, "hbm:pneumatic_tube", x, Y, z);
        Fixtures.setBlock(c, -2, G, z, inventory);
        Fixtures.setBlock(c, 2, G, z, inventory);
        c.cmd("item replace block -2 " + G + " " + z + " container.0 with minecraft:cobblestone 16");
        Fixtures.giveOnly(c, "hbm:screwdriver", 1);
        c.hotbar(1);
        c.waitTicks(5);
        screw(c, -1, z, false);
        screw(c, 1, z, true);

        // the compressor, by hand, six blocks north of the line
        int mz = z - 7;
        Fixtures.giveOnly(c, "hbm:machine_compressor", 1);
        c.hotbar(1);
        c.teleport(-0.5, G, mz - 3.5);
        c.waitTicks(3);
        c.lookAt(-0.5, Y + 0.98, mz + 0.5);
        Fixtures.waitForTarget(c, -1, Y, mz);
        c.use(2);
        c.waitTicks(10);
        int[] core = null;
        for (int x = -4; x <= 3 && core == null; x++) {
            for (int zz = mz - 4; zz <= mz + 4 && core == null; zz++) {
                for (int y = G; y <= G + 2 && core == null; y++) {
                    String nbt = c.blockEntity(x, y, zz).toString();
                    if (nbt.contains("0_type")) core = new int[]{x, y, zz};
                }
            }
        }
        if (core == null) {
            wrong.add(tag + ": the compressor was not placed");
            return;
        }
        System.out.println("[Gh171air] " + c.node() + " " + tag + " compressor core " + core[0] + " " + core[1] + " " + core[2]);

        // ducts past every side of the core (its ports are among them), then a run to the tube's north side
        int nx = -1, nz = z - 1;
        int[] start = null;
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            // a port is a machine cell beside the core; its duct goes on the first free block past it
            int px = core[0] + d[0], pz = core[2] + d[1];
            for (int step = 0; step < 3 && !c.block(px, core[1], pz).toString().contains("minecraft:air"); step++) {
                px += d[0];
                pz += d[1];
            }
            if (!c.block(px, core[1], pz).toString().contains("minecraft:air")) continue;
            duct(c, px, core[1], pz);
            if (start == null || Math.abs(px - nx) + Math.abs(pz - nz) < Math.abs(start[0] - nx) + Math.abs(start[1] - nz)) {
                start = new int[]{px, pz};
            }
        }
        if (start == null) {
            wrong.add(tag + ": no free side at the compressor core");
            return;
        }
        int x = start[0], zz = start[1], y = core[1];
        while (y > G) duct(c, x, --y, zz);
        while (zz != nz) duct(c, x, G, zz += Integer.signum(nz - zz));
        while (x != nx) duct(c, x += Integer.signum(nx - x), G, zz);

        int near = 0;
        int far = 0;
        for (int i = 0; i < 12; i++) {
            c.cmd("data merge block " + core[0] + " " + core[1] + " " + core[2]
                    + " {power:1000000L,\"0\":16000,\"0_type\":\"hbm:air\",\"0_p\":0s}");
            c.waitTicks(20);
        }
        String tube = c.blockEntity(-1, G, z).toString().replaceAll("\\s+", "");
        Matcher tank = Pattern.compile("tank\\D*(\\d+)").matcher(tube);
        far = count(c, 2, z);
        near = count(c, -2, z);
        c.teleport(0.5, G + 4, z + 5.5);
        c.lookAt(0.5, G, z - 3.5);
        c.waitTicks(5);
        c.screenshot("gh171air-" + tag);
        System.out.println("[Gh171air] " + c.node() + " " + tag + " near " + near + " far " + far + " tube " + tube);
        if (far == 0) wrong.add(tag + ": nothing reached the far " + tag + " (near " + near + ", tube "
                + (tank.find() ? tank.group(1) : "?") + ")");
        if (far + near != 16) wrong.add(tag + ": items lost or made " + near + " + " + far);
        c.cmd("clear @s");
    }

    private static void duct(Client c, int x, int y, int z) {
        c.cmd("setblock " + x + " " + y + " " + z + " hbm:fluid_duct_neo{type:\"hbm:air\"}");
    }

    private static void screw(Client c, int x, int z, boolean sneak) {
        c.teleport(x + 0.5, G + 2, z + 0.5);
        c.lookAt(x + 0.5, G + 1, z + 0.5);
        Fixtures.waitForTarget(c, x, G, z);
        if (sneak) c.keyDown("key.sneak");
        c.waitTicks(3);
        c.use(1);
        c.waitTicks(6);
        if (sneak) c.keyUp("key.sneak");
        c.waitTicks(3);
    }

    private static final Pattern STACK = Pattern.compile("\\{[^{}]*minecraft:cobblestone[^{}]*\\}");
    private static final Pattern COUNT = Pattern.compile("[Cc]ount:(\\d+)");

    private static int count(Client c, int x, int z) {
        JsonObject be = c.blockEntity(x, G, z);
        String nbt = be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
        int total = 0;
        Matcher stack = STACK.matcher(nbt);
        while (stack.find()) {
            Matcher count = COUNT.matcher(stack.group());
            total += count.find() ? Integer.parseInt(count.group(1)) : 1;
        }
        return total;
    }
}
