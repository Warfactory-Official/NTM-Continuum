import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 137: 1.7 scales a beam across its own axis, so its fade only narrows it. A beam runs along the view, so
 * on screen it joins the muzzle's fixed spot to the centre: its screen angle does not depend on pitch. A beam scaled in
 * world axes steers with pitch instead. Slow ticks hold a five-tick beam for the camera, and a frame diff against the
 * unfired view gives its pixels. Tesla, tau and the 35-800 are photographed for reading: their arcs wander by design.
 */
public final class Gh137BeamAim implements Scenario {

    private static final int Y = 100;
    private static final int PAD = Y + 40;
    private static final double[] PITCHES = {-50, -20, 25};
    private static final double MAX_STEER_DEGREES = 3D;

    private record Gun(String id, String ammo, boolean measured) {}

    private static final Gun[] GUNS = {
            new Gun("hbm:gun_laser_pistol", "hbm:ammo_standard_capacitor", false),
            new Gun("hbm:gun_lasrifle", "hbm:ammo_standard_capacitor", true),
            new Gun("hbm:gun_tesla_cannon", "hbm:ammo_standard_capacitor", false),
            new Gun("hbm:gun_tau", "hbm:ammo_standard_tau_uranium", false),
            new Gun("hbm:gun_aberrator", "hbm:ammo_secret_p35_800", false),
    };

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        String hideGui = null;
        List<String> failures = new ArrayList<>();
        try {
            Fixtures.platform(c, 0, Y, 0, 8);
            c.cmd("gamemode creative");
            c.cmd("gamerule sendCommandFeedback false");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("time set 18000");
            c.cmd("weather clear");
            c.cmd("setblock 0 " + (PAD - 1) + " 0 minecraft:stone");
            hideGui = c.setOption("hide_gui", "true");
            for (Gun gun : GUNS) {
                c.cmd("clear @s");
                c.cmd("give @s " + gun.id());
                c.cmd("give @s " + gun.ammo() + " 64");
                c.hotbar(1);
                c.teleport(0.5, PAD, 0.5);
                c.waitTicks(50);
                c.key("r", 2);
                c.waitTicks(100);
                String name = gun.id().substring(4);
                double reference = Double.NaN;
                for (double pitch : PITCHES) {
                    c.look(20, pitch);
                    c.waitTicks(20);
                    BufferedImage idle = ImageIO.read(c.screenshot(name + "-" + (int) pitch + "-idle").toFile());
                    float rate = c.tickRate(2F);
                    c.attack(1);
                    double first = Double.NaN;
                    for (int shot = 0; shot < 2; shot++) {
                        // Recoil turns the camera away from the beam, which stays where it was fired.
                        c.look(20, pitch);
                        BufferedImage frame = ImageIO.read(c.screenshot(name + "-" + (int) pitch + "-" + shot).toFile());
                        double angle = axisDegrees(idle, frame);
                        if (Double.isNaN(first)) {
                            first = angle;
                            // Four additive layers overlap at the core; drawn opaque it stays the layer colour.
                            if (gun.measured() && !Double.isNaN(angle) && peak(frame) < 300) {
                                failures.add(name + " pitch " + pitch + " is not additive, peak " + peak(frame));
                            }
                        }
                        System.out.println("[Gh137] " + c.node() + " " + name + " pitch " + pitch + " shot " + shot
                                + " screen angle " + angle);
                    }
                    c.tickRate(rate);
                    c.waitTicks(30);
                    if (!gun.measured()) continue;
                    if (Double.isNaN(first)) failures.add(name + " pitch " + pitch + " no beam seen");
                    else if (Double.isNaN(reference)) reference = first;
                    else if (Math.abs(first - reference) > MAX_STEER_DEGREES) {
                        failures.add(name + " pitch " + pitch + " at " + first + " degrees, not " + reference);
                    }
                }
            }
            Expect.that(failures.isEmpty(), "the laser rifle's beam runs along the view at every pitch: " + failures);
            Expect.logCleanSinceMark(c, "Exception|/(ERROR|FATAL)[]].*hbm");
        } finally {
            c.tickRate(20F);
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            c.teleport(0.5, Y + 1, 0.5);
            c.cmd("clear @s");
        }
    }

    /**
     * The screen angle of the principal axis of the changed area touching the view's centre, NaN for too little. Stars
     * and spent casings change the frame too, in areas of their own.
     */
    private static double axisDegrees(BufferedImage idle, BufferedImage frame) {
        int w = frame.getWidth(), h = frame.getHeight();
        boolean[][] changed = new boolean[h][w];
        int startX = -1, startY = -1;
        long nearest = Long.MAX_VALUE;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = idle.getRGB(x, y), b = frame.getRGB(x, y);
                changed[y][x] = Math.abs((a >> 16 & 255) - (b >> 16 & 255)) + Math.abs((a >> 8 & 255) - (b >> 8 & 255))
                        + Math.abs((a & 255) - (b & 255)) >= 40;
                long distance = (long) (x - w / 2) * (x - w / 2) + (long) (y - h / 2) * (y - h / 2);
                if (changed[y][x] && distance < nearest) {
                    nearest = distance;
                    startX = x;
                    startY = y;
                }
            }
        }
        if (startX < 0) return Double.NaN;
        List<int[]> points = new ArrayList<>();
        ArrayDeque<int[]> open = new ArrayDeque<>();
        open.add(new int[] {startX, startY});
        changed[startY][startX] = false;
        double sx = 0, sy = 0;
        while (!open.isEmpty()) {
            int[] p = open.poll();
            points.add(p);
            sx += p[0];
            sy += p[1];
            for (int dy = -2; dy <= 2; dy++) {
                for (int dx = -2; dx <= 2; dx++) {
                    int x = p[0] + dx, y = p[1] + dy;
                    if (x < 0 || y < 0 || x >= w || y >= h || !changed[y][x]) continue;
                    changed[y][x] = false;
                    open.add(new int[] {x, y});
                }
            }
        }
        double n = points.size();
        if (n < 60) return Double.NaN;
        double mx = sx / n, my = sy / n, xx = 0, xy = 0, yy = 0;
        for (int[] p : points) {
            xx += (p[0] - mx) * (p[0] - mx);
            xy += (p[0] - mx) * (p[1] - my);
            yy += (p[1] - my) * (p[1] - my);
        }
        return Math.toDegrees(0.5 * Math.atan2(2 * xy, xx - yy));
    }

    private static int peak(BufferedImage image) {
        int peak = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int c = image.getRGB(x, y);
                peak = Math.max(peak, (c >> 16 & 255) + (c >> 8 & 255) + (c & 255));
            }
        }
        return peak;
    }
}
