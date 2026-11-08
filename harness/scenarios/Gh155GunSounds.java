import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 155: the lever action rifle and Lincoln's repeater fire 1.7's {@code weapon.fire.rifle}, which resolves to
 * {@code weapon/fire/rifle.ogg} (pitch 1.0 and 1.25), and the Star F's reload plays the revolver close and the magazine
 * removal together at tick 5, as 1.7 does. Read from the client's sound log, including the file each sound resolved to.
 */
public final class Gh155GunSounds implements Scenario {

    private static final int X = 1550;
    private static final int Y = 100;
    private static final int Z = 1550;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, X, Y, Z, 12);
        c.cmd("time set 6000");
        c.cmd("gamemode survival");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.setOption("camera", "first_person");
        String capture = c.setOption("sound_capture", "true");
        List<String> wrong = new ArrayList<>();
        try {
            fire(c, "gun_henry", "1.000", wrong);
            fire(c, "gun_henry_lincoln", "1.250", wrong);

            c.cmd("clear @s");
            c.cmd("give @s hbm:gun_star_f");
            c.cmd("give @s hbm:ammo_standard_p22_sp 32");
            c.hotbar(1);
            c.waitTicks(40);
            c.markLog();
            c.key("r");
            c.waitTicks(40);
            List<String> reload = c.logSinceMark("harness-sound");
            System.out.println("[Gh155] " + c.node() + " star_f reload " + reload);
            if (reload.stream().noneMatch(l -> l.contains("id=hbm:weapon.reload.magremove")))
                wrong.add("star_f reload without the magazine removal: " + reload);
        } finally {
            if (capture != null) c.setOption("sound_capture", capture);
            c.releaseAll();
            c.cmd("gamemode creative");
        }
        Expect.that(wrong.isEmpty(), "1.7 gun sounds: " + wrong);
    }

    private static void fire(Client c, String gun, String pitch, List<String> wrong) throws Exception {
        c.cmd("clear @s");
        c.cmd("give @s hbm:" + gun);
        c.cmd("give @s hbm:ammo_standard_m44_sp 32");
        c.hotbar(1);
        c.look(0, 0);
        c.waitTicks(40);
        c.key("r");
        c.waitTicks(60);
        c.markLog();
        // The first press ends the one-round-at-a-time reload, the second fires.
        for (int i = 0; i < 2; i++) {
            c.keyDown("mouse.left");
            c.waitTicks(3);
            c.keyUp("mouse.left");
            c.waitTicks(40);
        }
        List<String> shots = c.logSinceMark("harness-sound.*weapon.fire");
        System.out.println("[Gh155] " + c.node() + " " + gun + " " + shots);
        if (shots.isEmpty()) {
            wrong.add(gun + " fired no sound");
            return;
        }
        String shot = shots.get(0);
        if (!shot.contains("id=hbm:weapon.fire.rifle") || !shot.contains("file=hbm:weapon/fire/rifle")
                || !shot.contains("pitch=" + pitch)) wrong.add(gun + " " + shot);
    }
}
