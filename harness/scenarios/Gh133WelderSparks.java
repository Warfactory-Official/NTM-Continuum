import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub issue 133: the running arc welder and soldering station showed nothing. 1.7 draws both statically and shows
 * work through sparks: the welder "hadron" every 2 ticks and "tau" every 20, the soldering station "tau" every 20.
 * Each machine is filmed with its recipe loaded and no power, then running. A flash must brighten many pixels near the
 * machine against that baseline. The Tauon turret sends the same effect per shot (its beam also brightens the frame, so
 * that one is checked by rounds fired and by eye). The derrick is static in 1.7 and is only filmed.
 */
public class Gh133WelderSparks implements Scenario {

    private static final int Y = 100, G = 101;
    private static final int FLASH_PIXELS = 150;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 10);
        c.cmd("fill -10 " + G + " -10 10 " + (Y + 12) + " 10 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("time set 18000");
        c.waitTicks(5);

        film(c, "hbm:machine_arc_welder", "welder", 4, new Object[][]{{"hbm:wire_fine_copper", 0}}, 0);
        film(c, "hbm:machine_soldering_station", "soldering", 7, new Object[][]{
                {"hbm:circuit_vacuum_tube", 0}, {"hbm:circuit_capacitor", 1}, {"hbm:circuit_pcb", 3}, {"hbm:wire_fine_lead", 5}}, 25);

        tauon(c);
        derrick(c);

        c.cmd("time set 6000");
        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[ERROR\\]).*(hbm|golem|particle)");
        System.out.println("[Gh133] PASS");
    }

    private static void film(Client c, String item, String label, int batterySlot, Object[][] inputs, int gap) throws Exception {
        c.cmd("fill -10 " + G + " -10 10 " + (Y + 12) + " 10 minecraft:air");
        c.waitTicks(3);
        Fixtures.giveOnly(c, item, 1);
        c.teleport(0.5, G, -3.5);
        c.waitTicks(3);
        c.lookAt(0.5, Y + 0.98, 0.5);
        c.use(2);
        c.waitTicks(6);
        Expect.equal(item, c.block(0, G, 0).get("block").getAsString(), item + " placed by hand");

        c.cmd("clear");
        c.teleport(0.5, G, -3.5);
        c.waitTicks(3);
        Fixtures.interact(c, 0, G, 0);
        c.waitTicks(20);
        for (Object[] input : inputs) insert(c, (String) input[0], 64, (Integer) input[1]);
        c.key("escape");
        c.waitTicks(5);

        aim(c);
        BufferedImage idle = ImageIO.read(c.screenshot(label + "-00-idle").toFile());

        Fixtures.interact(c, 0, G, 0);
        c.waitTicks(10);
        insert(c, "hbm:battery_creative", 1, batterySlot);
        c.key("escape");
        c.waitTicks(5);
        aim(c);

        int best = 0, shots = 0;
        for (int i = 0; i < 12; i++) {
            c.waitTicks(3 + gap / 12);
            BufferedImage frame = ImageIO.read(c.screenshot(label + "-" + String.format("%02d", i + 1) + "-running").toFile());
            shots++;
            best = Math.max(best, brighter(idle, frame));
        }
        String nbt = nbt(c, 0, G, 0);
        int progress = progress(nbt);
        System.out.println("[Gh133] " + label + " brightest of " + shots + " frames: " + best + " px, progress " + progress);
        Expect.that(best >= FLASH_PIXELS, label + " sparks: " + best + " px brightened of " + FLASH_PIXELS + " wanted");
    }

    private static void tauon(Client c) throws Exception {
        c.cmd("fill -10 " + G + " -10 10 " + (Y + 12) + " 10 minecraft:air");
        c.cmd("difficulty normal");
        Fixtures.setBlock(c, 0, G, 0, "hbm:turret_tauon");
        c.cmd("data merge block 0 " + G + " 0 {power:100000L,isOn:1b,targetMobs:1b,targetAnimals:1b}");
        c.cmd("item replace block 0 " + G + " 0 container.1 with hbm:ammo_standard_tau_uranium 64");
        c.cmd("summon minecraft:iron_golem 0.5 " + G + " 9.5 {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
        String attr = c.node().startsWith("26.1") ? "minecraft:max_health" : "minecraft:generic.max_health";
        c.cmd("attribute @e[type=minecraft:iron_golem,limit=1] " + attr + " base set 1000");
        c.cmd("data merge entity @e[type=minecraft:iron_golem,limit=1] {Health:1000f}");
        c.cmd("setblock 3 " + G + " -2 minecraft:stone");
        c.teleport(3.5, G + 1, -1.5);
        c.waitTicks(4);
        c.lookAt(0.5, G + 1, 3.5);
        c.send("clear_chat");
        c.waitTicks(30);
        for (int i = 1; i <= 8; i++) {
            c.screenshot("tauon-" + i);
            c.waitTicks(2);
        }
        double health = golemHealth(c);
        System.out.println("[Gh133] tauon target health " + health);
        Expect.that(health < 1000, "the turret hit its target");
        c.cmd("kill @e[type=minecraft:iron_golem]");
        c.cmd("difficulty peaceful");
    }

    private static double golemHealth(Client c) {
        JsonObject params = new JsonObject();
        params.addProperty("radius", 60);
        params.addProperty("type", "minecraft:iron_golem");
        params.addProperty("server", true);
        JsonObject result = c.query("entities", params);
        JsonObject side = result.has("entities") ? result : result.getAsJsonObject("server");
        for (JsonElement e : side.getAsJsonArray("entities")) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("health")) return o.get("health").getAsDouble();
        }
        Expect.that(false, "no golem health: " + result);
        return 0;
    }

    private static void derrick(Client c) throws Exception {
        c.cmd("fill -10 " + G + " -10 10 " + (Y + 12) + " 10 minecraft:air");
        Fixtures.setBlock(c, 0, G, 0, "hbm:machine_well");
        c.teleport(0.5, G + 1, -7.5);
        c.lookAt(0.5, G + 5, 0.5);
        c.send("clear_chat");
        c.waitTicks(8);
        c.screenshot("derrick");
    }

    private static void aim(Client c) {
        c.cmd("setblock 0 " + G + " -3 minecraft:stone");
        c.teleport(0.5, G + 1, -2.5);
        c.waitTicks(6);
        c.lookAt(0.5, G + 1.25, 0.5);
        c.waitTicks(4);
        c.send("clear_chat");
        c.waitTicks(2);
    }

    /** Pixels the running frame lifts well above the unpowered one, over the central part of the view. */
    private static int brighter(BufferedImage idle, BufferedImage frame) {
        int w = Math.min(idle.getWidth(), frame.getWidth()), h = Math.min(idle.getHeight(), frame.getHeight());
        int count = 0;
        for (int y = h / 5; y < h * 4 / 5; y++) {
            for (int x = w / 5; x < w * 4 / 5; x++) {
                if (luma(frame.getRGB(x, y)) - luma(idle.getRGB(x, y)) > 40) count++;
            }
        }
        return count;
    }

    private static int luma(int rgb) {
        return ((rgb >> 16 & 255) * 3 + (rgb >> 8 & 255) * 6 + (rgb & 255)) / 10;
    }

    private static void insert(Client c, String item, int count, int slot) {
        c.cmd("give @s " + item + " " + count);
        c.waitTicks(10);
        int source = -1;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && !s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(item)) {
                source = s.get("index").getAsInt();
                break;
            }
        }
        Expect.that(source >= 0, "no player slot holding " + item);
        c.clickSlot(source);
        c.waitTicks(2);
        c.clickSlot(slot);
        c.waitTicks(5);
        Expect.slotHolds(c, slot, item, count);
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : be.toString();
    }

    private static int progress(String nbt) {
        Matcher m = Pattern.compile("progress:(-?[0-9]+)").matcher(nbt);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }
}
