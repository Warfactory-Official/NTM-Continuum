import com.google.gson.JsonArray;
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
 * GitHub issue 150: tracers unlit (amber 0xFFBF00 in daylight) and aimed along the step taken, recoil eased in over
 * the next tick, and the Liberator's unloaded shells hidden below the open breech until each slides in.
 */
public final class Gh150BulletTracers implements Scenario {

    private static final int Y = 100;
    private static final int AMBER_MIN = 40;
    private static final double MAX_STRAY = 0.5D;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        String hideGui = null;
        List<String> failures = new ArrayList<>();
        try {
            Fixtures.platform(c, 0, Y, 0, 8);
            c.cmd("gamemode survival");
            c.cmd("gamerule sendCommandFeedback false");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("time set 6000");
            c.cmd("weather clear");
            hideGui = c.setOption("hide_gui", "true");
            c.teleport(0.5, Y + 1, 0.5);
            int amber = tracers(c, failures);
            amber = Math.max(amber, recoil(c, failures));
            System.out.println("[Gh150] " + c.node() + " tracer amber pixels " + amber);
            if (amber < AMBER_MIN) failures.add("tracers are dark: " + amber + " amber pixels");
            c.setOption("hide_gui", hideGui);
            liberatorReload(c, failures);
            Expect.that(failures.isEmpty(), "tracers, recoil and the Liberator's shells behave as in 1.7: " + failures);
            Expect.logCleanSinceMark(c, "Exception|/(ERROR|FATAL)[]].*hbm");
        } finally {
            c.tickRate(20F);
            c.releaseAll();
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            c.cmd("clear @s");
            c.cmd("gamemode creative");
        }
    }

    /** STG77 rounds against the sky, facing north where a tracer's yaw wraps at 180. */
    private static int tracers(Client c, List<String> failures) throws Exception {
        equip(c, "gun_stg77", "ammo_standard_r556_sp 64");
        c.key("r");
        c.waitTicks(100);
        c.look(180, -25);
        c.waitTicks(20);
        BufferedImage idle = ImageIO.read(c.screenshot("stg77-idle").toFile());
        float rate = c.tickRate(1F);
        int amber = 0;
        double stray = 0D;
        int seen = 0;
        c.attack(1);
        for (int shot = 0; shot < 8; shot++) {
            BufferedImage frame = ImageIO.read(c.screenshot("stg77-" + shot).toFile());
            amber = Math.max(amber, amber(idle, frame));
            for (JsonElement e : c.query("hbm_bullets", new JsonObject()).getAsJsonArray("bullets")) {
                JsonObject b = e.getAsJsonObject();
                if (!b.has("stray")) continue;
                seen++;
                stray = Math.max(stray, b.get("stray").getAsDouble());
                System.out.println("[Gh150] " + c.node() + " bullet " + b);
            }
        }
        c.tickRate(rate);
        c.waitTicks(20);
        System.out.println("[Gh150] " + c.node() + " stg77 amber pixels " + amber + ", stray " + stray + " over " + seen);
        if (seen == 0) failures.add("no bullet in flight was seen");
        if (stray > MAX_STRAY) failures.add("tracer strays " + stray + " degrees from its path");
        return amber;
    }

    /** The Liberator kicks 5 degrees up; 1.7's camera trails the player's pitch by the tick's turn times (1 - partial). */
    private static int recoil(Client c, List<String> failures) throws Exception {
        equip(c, "gun_liberator", "ammo_standard_g12 64");
        c.key("r");
        c.waitTicks(120);
        c.look(180, 0);
        c.waitTicks(20);
        BufferedImage idle = ImageIO.read(c.screenshot("recoil-idle").toFile());
        float rate = c.tickRate(2F);
        c.attack(1);
        List<String> samples = new ArrayList<>();
        int amber = 0;
        double lag = 0D;
        for (int shot = 0; shot < 12; shot++) {
            BufferedImage frame = ImageIO.read(c.screenshot("recoil-" + shot).toFile());
            JsonObject q = c.query("hbm_bullets", new JsonObject());
            double behind = q.get("camera_pitch").getAsDouble() - q.get("pitch").getAsDouble();
            if (shot < 3) amber = Math.max(amber, amber(idle, frame));
            lag = Math.max(lag, Math.abs(behind));
            samples.add(String.format("t%d p%.2f cam%+.2f row%d", q.get("game_time").getAsLong(),
                    q.get("partial").getAsDouble(), behind, horizon(frame)));
        }
        c.tickRate(rate);
        c.waitTicks(40);
        System.out.println("[Gh150] " + c.node() + " recoil " + samples + ", largest lag " + lag + ", pellet amber " + amber);
        if (lag < 0.05D) failures.add("the camera sits on the raw pitch through the recoil: " + samples);
        return amber;
    }

    /** Empties the Liberator, then reloads it; more than one shell head as it breaks open means shells that never left. */
    private static void liberatorReload(Client c, List<String> failures) throws Exception {
        c.look(180, -10);
        for (int i = 0; i < 4; i++) {
            c.attack(1);
            c.waitTicks(15);
        }
        c.waitTicks(40);
        BufferedImage closed = ImageIO.read(c.screenshot("liberator-empty").toFile());
        c.key("r");
        int[] at = {1, 2, 4, 9, 14, 18, 24, 30, 40, 60, 100};
        int waited = 0;
        int openBrass = -1;
        int loadedBrass = -1;
        for (int tick : at) {
            c.waitTicks(tick - waited);
            waited = tick;
            BufferedImage frame = ImageIO.read(c.screenshot("liberator-reload-" + tick).toFile());
            JsonObject params = new JsonObject();
            JsonArray buses = new JsonArray();
            for (String bus : new String[] {"BREAK", "SHELL1", "SHELL2", "SHELL3", "SHELL4"}) buses.add(bus);
            params.add("buses", buses);
            JsonObject q = c.query("hbm_bullets", params);
            int brass = brass(closed, frame);
            System.out.println("[Gh150] " + c.node() + " reload tick " + tick + " brass " + brass + " " + q.get("buses"));
            // a slow client's animation clock runs ahead of its ticks, so only the first frame is surely before shell 2
            if (tick == 1) openBrass = brass;
            else loadedBrass = Math.max(loadedBrass, brass);
        }
        // one shell head is about 640 pixels; four that never left are about 2000
        if (openBrass > 700) failures.add("empty Liberator shows " + openBrass + " brass pixels as it breaks open");
        if (loadedBrass < 1000) failures.add("loading Liberator shows at most " + loadedBrass + " brass pixels");
    }

    private static void equip(Client c, String gun, String ammo) {
        c.cmd("clear @s");
        c.cmd("give @s hbm:" + gun);
        c.cmd("give @s hbm:" + ammo);
        c.hotbar(1);
        c.waitTicks(40);
    }

    /** New amber pixels (1.7's 0xFFBF00 tail, lit by daylight) where only the sky was. */
    private static int amber(BufferedImage idle, BufferedImage frame) {
        int count = 0;
        for (int y = 0; y < frame.getHeight(); y++) {
            for (int x = 0; x < frame.getWidth(); x++) {
                int a = idle.getRGB(x, y), b = frame.getRGB(x, y);
                int r = b >> 16 & 255, g = b >> 8 & 255, bl = b & 255;
                if (a != b && r >= 200 && g >= 120 && bl <= 110) count++;
            }
        }
        return count;
    }

    /** New brass-coloured pixels (the shells' heads) left of where 1.7 throws the spent casings out to the right. */
    private static int brass(BufferedImage before, BufferedImage frame) {
        int count = 0;
        for (int y = 0; y < frame.getHeight(); y++) {
            for (int x = 0; x < frame.getWidth() * 3 / 4; x++) {
                int b = frame.getRGB(x, y);
                int r = b >> 16 & 255, g = b >> 8 & 255, bl = b & 255;
                if (before.getRGB(x, y) != b && r >= 170 && g >= 120 && bl <= 90 && r > g) count++;
            }
        }
        return count;
    }

    /** The first row from the top in the leftmost tenth of the screen that is not sky blue: the platform's edge. */
    private static int horizon(BufferedImage frame) {
        int x = frame.getWidth() / 10;
        for (int y = 0; y < frame.getHeight(); y++) {
            int p = frame.getRGB(x, y);
            if ((p & 255) - (p >> 16 & 255) < 30) return y;
        }
        return -1;
    }
}
