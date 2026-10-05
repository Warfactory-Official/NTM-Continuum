import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Wave 2 VISUAL (History §19ek-visual): the large or mounted multiblocks {@link VisualScreens} cannot open with one
 * fixed aim (soyuz launcher, large launch pad, particle-accelerator detector, ceiling-mounted mining laser, and the DFC
 * emitter/stabilizer that occasionally miss). Each is placed with room around it, then every solid cell near the core
 * is tried until a screen opens.
 */
public final class VisualScreensLarge implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("gamemode creative");
        List<String> failed = new ArrayList<>();
        int n = 0;
        for (String id : new String[] {"soyuz_launcher", "launch_pad_large", "pa_detector", "machine_mining_laser",
                "dfc_emitter", "dfc_stabilizer"}) {
            c.cmd("fill -14 " + (Y - 1) + " -14 14 " + (Y + 16) + " 14 minecraft:air");
            Fixtures.platform(c, 0, Y, 0, 14);
            c.waitTicks(5);
            if (id.equals("machine_mining_laser")) {
                // ceiling mount: a slab of stone at Y+6, the laser placed against its underside
                c.cmd("fill -3 " + (Y + 6) + " -3 3 " + (Y + 6) + " 3 minecraft:stone");
                Fixtures.giveOnly(c, "hbm:" + id, 1);
                c.teleport(2.5, Y + 1, 0.5);
                c.waitTicks(2);
                c.lookAt(0.5, Y + 6.02, 0.5);
                c.waitTicks(1);
                c.use(2);
                c.waitTicks(5);
            } else if (id.equals("pa_detector") || id.equals("launch_pad_large")) {
                // wide footprints: place from outside the footprint (the standard helper stands inside it), aiming at
                // the near edge of the top face so the click stays in reach
                Fixtures.giveOnly(c, "hbm:" + id, 1);
                c.teleport(0.5, Y + 1, -4.7);
                c.waitTicks(2);
                c.lookAt(0.5, Y + 0.98, 0.06);
                c.waitTicks(1);
                c.use(2);
                c.waitTicks(5);
            } else {
                Fixtures.placeOnTop(c, "hbm:" + id, 0, Y, 0);
            }
            c.waitTicks(10);
            boolean opened = false;
            outer:
            for (int dy = 1; dy <= 6 && !opened; dy++) {
                for (int dz = -6; dz <= 6; dz++) {
                    for (int dx = -6; dx <= 6; dx++) {
                        JsonObject b = c.block(dx, Y + dy, dz);
                        String block = b.has("id") ? b.get("id").getAsString() : (b.has("block") ? b.get("block").getAsString() : "");
                        if (!block.startsWith("hbm:")) continue;
                        c.teleport(dx + 0.5, Y + dy, dz - 2.5);
                        Fixtures.interact(c, dx, Y + dy, dz);
                        c.waitTicks(10);
                        if (c.screenClass() != null) { opened = true; break outer; }
                        c.teleport(dx + 0.5, Y + dy, dz + 3.5);
                        Fixtures.interact(c, dx, Y + dy, dz);
                        c.waitTicks(10);
                        if (c.screenClass() != null) { opened = true; break outer; }
                    }
                }
            }
            if (!opened) {
                failed.add(id);
                System.out.println("[visual] NO SCREEN " + id);
                c.screenshot(String.format("%02d-%s-NOSCREEN", n++, id));
                continue;
            }
            JsonObject s = c.screen();
            System.out.println("[visual] " + id + " -> " + c.screenClass() + " " + s.get("image_width") + "x" + s.get("image_height"));
            c.mouseMove(2, 2);
            c.waitTicks(5);
            c.screenshot(String.format("%02d-%s", n++, id));
            c.key("escape");
            c.waitTicks(5);
        }
        c.cmd("fill -14 " + (Y - 1) + " -14 14 " + (Y + 16) + " 14 minecraft:air");
        System.out.println("[visual] large screens failed to open: " + failed);
        Expect.that(failed.isEmpty(), "screens that did not open: " + failed);
    }
}
