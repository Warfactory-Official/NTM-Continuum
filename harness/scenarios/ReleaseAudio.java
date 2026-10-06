import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * Pre-release client wave (History §19es-client): HBM sounds proven by the sound events the client starts (Core's
 * {@code [harness-sound]} log; nobody listens to the clients), against the 1.7 call sites.
 * <ol>
 *   <li>Equip sounds (§19cp "intermittent equip audio"): the AJR-O power suit, whose battery the full set drains every
 *       tick (25 HE/t, so its stack data changes every tick), is put on piece by piece and worn 10 s while idle. Vanilla
 *       plays an equip sound when a slot's stack changes; a changing charge must not count as a change: exactly one
 *       {@code item.armor.equip_*} sound per piece put on, none while worn.</li>
 *   <li>1.7 ArmorFSB.handleTick/steppy, handleJump, handleFall for the AJR-O ({@code setStep("hbm:step.metal")},
 *       {@code setJump("hbm:step.iron_jump")}, {@code setFall("hbm:step.iron_land")}): walking plays step.metal,
 *       a jump plays step.iron_jump, landing plays step.iron_land.</li>
 *   <li>1.7 ArmorFSB.onArmorTick: a suit with a built-in counter ({@code setHasGeigerSound}) clicks
 *       {@code hbm:item.geiger1..6} every 5 ticks while its rad buffer is above zero.</li>
 *   <li>No HBM sound event is unknown to the client ("Unable to play unknown soundEvent: hbm:...").</li>
 * </ol>
 */
public final class ReleaseAudio implements Scenario {

    private static final int X = 0, Y = 100, Z = 0, L = 101;
    private static final String[] PIECES = {"head:ajro_helmet", "chest:ajro_plate", "legs:ajro_legs", "feet:ajro_boots"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        for (int i = 0; i < 3 && c.screenClass() != null; i++) { c.key("escape"); c.waitTicks(5); }
        c.useFlatWorld();
        Fixtures.platform(c, X, Y, Z, 12);
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.markLog();
        c.setOption("sound_capture", "true");
        try {
            equip(c);
            steps(c);
            geiger(c);
        } finally {
            c.setOption("sound_capture", "false");
            for (String p : PIECES) c.cmd("item replace entity @s armor." + p.split(":")[0] + " with minecraft:air");
            c.cmd("gamemode creative");
        }
        List<String> unknown = c.log(4000, "Unable to play (unknown|empty) soundEvent: hbm:");
        Expect.that(unknown.isEmpty(), "every HBM sound event resolves on the client: " + unknown);
        Expect.logCleanSinceMark(c, "(?i)(exception|unregistered hbm:).*");
    }

    private void equip(Client c) {
        int equips = 0;
        for (String p : PIECES) {
            String[] sp = p.split(":");
            c.markLog();
            c.cmd("item replace entity @s armor." + sp[0] + " with hbm:" + sp[1]);
            c.waitTicks(10);
            List<String> got = c.logSinceMark("harness-sound.*id=minecraft:item.armor.equip");
            System.out.println(c.node() + " equip " + sp[1] + ": " + got);
            Expect.that(got.size() == 1, "putting on " + sp[1] + " plays one equip sound: " + got);
            equips++;
        }
        c.screenshot("10-ajro-worn");
        c.markLog();
        c.waitTicks(200);
        List<String> idle = c.logSinceMark("harness-sound.*id=minecraft:item.armor.equip");
        System.out.println(c.node() + " equip sounds while worn 10 s: " + idle.size());
        Expect.that(idle.isEmpty(), "no equip sound while the drained suit is worn idle: " + idle.size() + " " + idle);
    }

    private void steps(Client c) {
        c.teleport(X + 0.5, L, Z - 8.5);
        c.look(0, 0);
        c.waitTicks(10);
        c.markLog();
        c.keyDown("key.forward");
        c.waitTicks(30);
        c.keyUp("key.forward");
        c.waitTicks(5);
        List<String> step = c.logSinceMark("harness-sound.*id=hbm:step.metal");
        System.out.println(c.node() + " steps: " + step.size());
        Expect.that(step.size() >= 2, "walking in the AJR-O plays hbm:step.metal: " + step.size());

        c.markLog();
        c.key("key.jump", 2);
        c.waitTicks(20);
        List<String> jump = c.logSinceMark("harness-sound.*id=hbm:step.iron_jump");
        Expect.that(!jump.isEmpty(), "jumping plays hbm:step.iron_jump");

        c.markLog();
        c.teleport(X + 0.5, L + 6, Z + 0.5);
        c.waitTicks(40);
        List<String> land = c.logSinceMark("harness-sound.*id=hbm:step.iron_land");
        Expect.that(!land.isEmpty(), "landing plays hbm:step.iron_land");
        System.out.println(c.node() + " jump " + jump + " land " + land);
    }

    private void geiger(Client c) {
        c.teleport(X + 0.5, L, Z + 0.5);
        JsonObject p = new JsonObject();
        p.addProperty("mode", "fog");
        p.addProperty("set_rad", 500.0);
        c.query("hbm_small", p);
        c.markLog();
        List<String> clicks = List.of();
        for (int i = 0; i < 20 && clicks.size() < 3; i++) {
            c.waitTicks(10);
            clicks = c.logSinceMark("harness-sound.*id=hbm:item.geiger[1-6]");
        }
        System.out.println(c.node() + " geiger clicks: " + clicks.size() + (clicks.isEmpty() ? "" : " e.g. " + clicks.get(0)));
        Expect.that(clicks.size() >= 3, "the AJR-O's built-in counter clicks hbm:item.geiger1..6 in radiation: " + clicks.size());
        p.addProperty("set_rad", 0.0);
        c.query("hbm_small", p);
        c.cmd("effect clear @s");
    }
}
