import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 133 sibling audit: effects 1.7 emits that never reached the client. Smoke is measured as pixels darkened
 * against a white wall, compared with the same machine without the condition (blast furnace running with an empty flue
 * tank, empty crucible). Flames and giblets are counted through {@code hbm_effects}. By hand from 1.7's ClientProxy, an
 * iron golem (1.4 x 2.7) gives gW 5, gH 10, count 75: the Maxwell throws 75, the turbofan's cDiv 5 leaves 15 a tick.
 */
public class Gh133MachineEffects implements Scenario {

    private static final int Y = 100, G = 101, WALL_Z = 7;
    private static final int SMOKE_PIXELS = 3000;
    private final ThreadLocal<List<String>> failures = ThreadLocal.withInitial(ArrayList::new);

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 16);
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("gamerule advance_time false");
        c.cmd("weather clear");
        c.cmd("gamemode spectator");
        String hideGui = c.setOption("hide_gui", "true");
        try {
            blastFurnace(c);
            crucible(c);
            arcFurnace(c);
            turbofan(c);
            maxwell(c);
            debris(c);
        } finally {
            clear(c);
            c.cmd("kill @e[type=minecraft:iron_golem]");
            c.cmd("time set 6000");
            c.cmd("gamemode creative");
            c.cmd("gamerule sendCommandFeedback true");
            c.setOption("hide_gui", hideGui);
        }
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[ERROR\\]).*(hbm|golem|particle)");
        Expect.that(failures.get().isEmpty(), "effects missing: " + failures.get());
        System.out.println("[Gh133Fx] PASS");
    }

    private void blastFurnace(Client c) throws Exception {
        clear(c);
        wall(c);
        int[] p = place(c, "hbm:machine_blast_furnace[facing=north]", "TileEntityMachineBlastFurnace");
        camera(c, G + 10, -13, G + 11);
        c.cmd("data merge block " + at(p) + " {fuel:64000,t1:0,inventory:{Items:["
                + stack(1, "minecraft:iron_ingot", 64) + "," + stack(2, "minecraft:sand", 64) + "]}}");
        c.waitTicks(25);
        BufferedImage lavaOnly = ImageIO.read(c.screenshot("blast-00-lava-only").toFile());
        c.cmd("data merge block " + at(p) + " {t1:1000}");
        smoke(c, "blast", lavaOnly);
    }

    private void crucible(Client c) throws Exception {
        clear(c);
        wall(c);
        int[] p = place(c, "hbm:machine_crucible[facing=north]", "TileEntityCrucible");
        camera(c, G + 3, -10, G + 4);
        c.waitTicks(10);
        BufferedImage empty = ImageIO.read(c.screenshot("crucible-00-empty").toFile());
        c.cmd("data merge block " + at(p) + " {rec:[{mat:\"iron\",amount:720}]}");
        smoke(c, "crucible", empty);
    }

    private void smoke(Client c, String label, BufferedImage baseline) throws Exception {
        int best = 0;
        for (int i = 1; i <= 14; i++) {
            c.waitTicks(6);
            BufferedImage frame = ImageIO.read(c.screenshot(label + "-" + String.format("%02d", i)).toFile());
            best = Math.max(best, darkened(baseline, frame));
        }
        System.out.println("[Gh133Fx] " + label + " smoke: " + best + " px darkened");
        if (best < SMOKE_PIXELS) failures.get().add(label + " smoke " + best + " px of " + SMOKE_PIXELS);
    }

    private void arcFurnace(Client c) throws Exception {
        clear(c);
        c.cmd("time set 18000");
        int[] p = place(c, "hbm:machine_arc_furnace[facing=north]", "TileEntityMachineArcFurnaceLarge");
        camera(c, G + 3, -11, G + 3);
        c.cmd("data merge block " + at(p) + " {power:100000000L,lid:1f,delay:0,inventory:{Items:["
                + stack(0, "hbm:arc_electrode", 1) + "," + stack(1, "hbm:arc_electrode", 1) + "," + stack(2, "hbm:arc_electrode", 1)
                + "," + stack(25, "minecraft:flint", 64) + "]}}");
        int max = 0;
        for (int i = 1; i <= 12; i++) {
            c.waitTicks(4);
            max = Math.max(max, effects(c, "flames"));
            c.screenshot("arc-" + String.format("%02d", i));
        }
        System.out.println("[Gh133Fx] arc furnace flames, most at once: " + max);
        if (max < 1) failures.get().add("arc furnace flames 0");
    }

    private void turbofan(Client c) throws Exception {
        clear(c);
        waitForNoGiblets(c);
        c.cmd("time set 6000");
        int[] p = place(c, "hbm:machine_turbofan[facing=north]", "TileEntityMachineTurbofan");
        c.cmd("data merge block " + at(p) + " {tank:24000,tank_type:\"hbm:kerosene\"}");
        camera(c, G + 3, -10, G + 1);
        c.waitTicks(10);
        golem(c, p[0] + 4.1, G, p[2] + 0.5);
        int gibs = waitForGiblets(c, "turbofan");
        System.out.println("[Gh133Fx] turbofan giblets: " + gibs);
        c.screenshot("turbofan-gibs");
        // 1.7 gibs a body lying dead in the blades every tick, 15 at a time
        if (gibs < 15 || gibs % 15 != 0) failures.get().add("turbofan giblets " + gibs + ", not bursts of 15");
    }

    private void maxwell(Client c) throws Exception {
        clear(c);
        waitForNoGiblets(c);
        c.cmd("difficulty normal");
        Fixtures.setBlock(c, 0, G, 0, "hbm:turret_maxwell");
        c.cmd("data merge block 0 " + G + " 0 {power:10000000L,isOn:1b,targetMobs:1b,targetAnimals:1b}");
        camera(c, G + 3, -8, G + 1);
        golem(c, 0.5, G, 9.5);
        c.cmd("data merge entity @e[type=minecraft:iron_golem,limit=1] {Health:5f}");
        int gibs = waitForGiblets(c, "maxwell");
        System.out.println("[Gh133Fx] maxwell giblets: " + gibs);
        c.cmd("difficulty peaceful");
        c.screenshot("maxwell-gibs");
        if (gibs != 75) failures.get().add("maxwell giblets " + gibs + " of 75");
    }

    private void debris(Client c) throws Exception {
        clear(c);
        c.cmd("time set 18000");
        camera(c, G + 3, -11, G + 2);
        for (int x = -8; x <= 8; x++) Fixtures.setBlock(c, x, G, 3, "hbm:rbmk_debris_burning");
        c.waitTicks(30);
        int flames = effects(c, "flames");
        System.out.println("[Gh133Fx] debris flames: " + flames);
        c.screenshot("debris-flames");
        if (flames < 1) failures.get().add("burning debris flames 0");
    }

    private void golem(Client c, double x, double y, double z) {
        c.cmd("summon minecraft:iron_golem " + x + " " + y + " " + z + " {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
    }

    private int waitForGiblets(Client c, String label) {
        // The burst arrives once, so read until the golem is gone and the count has settled.
        int gibs = 0;
        for (int i = 0; i < 60; i++) {
            c.waitTicks(5);
            gibs = effects(c, "giblets");
            if (gibs > 0 || !golemAlive(c)) break;
        }
        c.waitTicks(10);
        return Math.max(gibs, effects(c, "giblets"));
    }

    private void waitForNoGiblets(Client c) {
        for (int i = 0; i < 40 && effects(c, "giblets") > 0; i++) c.waitTicks(5);
        Expect.equal(0, effects(c, "giblets"), "giblets from the previous kill are gone");
    }

    private static boolean golemAlive(Client c) {
        JsonObject params = new JsonObject();
        params.addProperty("radius", 60);
        params.addProperty("type", "minecraft:iron_golem");
        params.addProperty("server", true);
        JsonObject result = c.query("entities", params);
        JsonObject side = result.has("entities") ? result : result.getAsJsonObject("server");
        for (JsonElement e : side.getAsJsonArray("entities")) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("health") && o.get("health").getAsDouble() > 0) return true;
        }
        return false;
    }

    private static int effects(Client c, String key) {
        return c.query("hbm_effects", new JsonObject()).get(key).getAsInt();
    }

    private static int[] place(Client c, String block, String cls) {
        c.cmd("setblock 0 " + G + " 0 " + block);
        c.waitTicks(8);
        for (int dy = 0; dy <= 2; dy++) for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            JsonObject be = c.blockEntity(dx, G + dy, dz);
            if (be.has("block_entity") && !be.get("block_entity").isJsonNull() && be.get("block_entity").getAsString().endsWith(cls)) {
                return new int[]{dx, G + dy, dz};
            }
        }
        throw new AssertionError("no " + cls + " placed by " + block);
    }

    private static void clear(Client c) {
        c.cmd("fill -14 " + G + " -14 14 " + (G + 12) + " 14 minecraft:air");
        c.cmd("fill -14 " + (G + 13) + " -14 14 " + (G + 26) + " 14 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.waitTicks(3);
    }

    private static void wall(Client c) {
        c.cmd("fill -14 " + G + " " + WALL_Z + " 14 " + (G + 26) + " " + WALL_Z + " minecraft:white_concrete");
        c.cmd("time set 6000");
    }

    private static void camera(Client c, int y, int z, int lookY) {
        c.teleport(0.5, y, z + 0.5);
        c.waitTicks(4);
        c.lookAt(0.5, lookY, 0.5);
        c.send("clear_chat");
        c.waitTicks(4);
    }

    private static String at(int[] p) {
        return p[0] + " " + p[1] + " " + p[2];
    }

    private static String stack(int slot, String id, int count) {
        return "{Slot:" + slot + "b,id:\"" + id + "\",count:" + count + ",Count:" + count + "b}";
    }

    /** Pixels the frame pushes well below the baseline, over the central part of the view. */
    private static int darkened(BufferedImage base, BufferedImage frame) {
        int w = Math.min(base.getWidth(), frame.getWidth()), h = Math.min(base.getHeight(), frame.getHeight());
        int count = 0;
        for (int y = h / 10; y < h * 9 / 10; y++) {
            for (int x = w / 5; x < w * 4 / 5; x++) {
                if (luma(base.getRGB(x, y)) - luma(frame.getRGB(x, y)) > 12) count++;
            }
        }
        return count;
    }

    private static int luma(int rgb) {
        return ((rgb >> 16 & 255) * 3 + (rgb >> 8 & 255) * 6 + (rgb & 255)) / 10;
    }
}
