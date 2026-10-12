import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * 1.7 dosimeter and ash glasses (plain HBM; NTM: Space's gates are in {@code new-hbm-space ForkContent3}):
 * <ol>
 *   <li>1.7 {@code ItemDosimeter.onUpdate}: carried in survival in strong radiation (rad buffer >= 2) it clicks
 *       {@code hbm:item.geiger3} on the fifth ticks, never geiger4..6 (the dosimeter's ladder stops at 3); with no
 *       radiation it only rarely clicks geiger1 (1 click tick in 100); a right-click plays {@code hbm:item.tech_boop}
 *       and prints the DOSIMETER readout.</li>
 *   <li>1.7 {@code ItemGeigerCounter.onUpdate}: the handheld counter in the same radiation (rad buffer >= 30) clicks
 *       only {@code hbm:item.geiger6} on the fifth ticks; with no radiation it rarely clicks geiger1 (1 click tick in
 *       50); a right-click plays {@code hbm:item.tech_boop} and prints the GEIGER COUNTER readout, without 1.12.2's
 *       "Total recieved radiation" line.</li>
 *   <li>1.7 {@code BlockGasBase.randomDisplayTick}: a pocket of radon gas shows no cloud to a bare head and green
 *       clouds to a player wearing ash glasses ({@code hbm_dosimeter_gas} {@code revealed} counter); screenshots of
 *       both.</li>
 * </ol>
 */
public final class DosimeterAshGlasses implements Scenario {

    private static final int X = 0, Y = 100, Z = 0, L = 101;

    static JsonObject q(Client c) {
        return c.query("hbm_dosimeter_gas", new JsonObject());
    }

    static void setRad(Client c, double rad) {
        JsonObject p = new JsonObject();
        p.addProperty("mode", "fog");
        p.addProperty("set_rad", rad);
        c.query("hbm_small", p);
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        if (c.screenClass() != null && c.screenClass().contains("DeathScreen")) { // an earlier run left the player dead
            c.waitTicks(30);
            c.clickWidget("Respawn");
            c.waitTicks(40);
        }
        for (int i = 0; i < 3 && c.screenClass() != null; i++) { c.key("escape"); c.waitTicks(5); }
        c.useFlatWorld();
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("gamerule doMobSpawning false");
        c.cmd("time set noon");
        c.cmd("weather clear");
        Fixtures.platform(c, X, Y, Z, 12);
        c.teleport(X + 0.5, L, Z + 0.5);
        c.waitTicks(20);
        c.markLog();
        c.setOption("sound_capture", "true");
        try {
            dosimeter(c);
            geigerCounter(c);
            glasses(c);
        } finally {
            c.setOption("sound_capture", "false");
            noRadiation(c);
            clearRads(c);
            c.cmd("gamerule randomTickSpeed 3");
            c.cmd("gamerule random_tick_speed 3");
            c.cmd("fill " + (X + 3) + " " + L + " " + (Z - 2) + " " + (X + 7) + " " + (L + 2) + " " + (Z + 2) + " minecraft:air");
            c.cmd("item replace entity @s armor.head with minecraft:air");
            c.cmd("clear");
            c.cmd("effect clear @s");
            c.cmd("gamemode creative");
        }
        List<String> unknown = c.log(4000, "Unable to play (unknown|empty) soundEvent: hbm:");
        Expect.that(unknown.isEmpty(), "no unknown HBM sound event: " + unknown);
    }

    private void dosimeter(Client c) {
        noRadiation(c);
        clearRads(c); // contamination left by an earlier run would kill the player as soon as it is in survival
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.cmd("give @s hbm:dosimeter 1");
        c.hotbar(1); // the harness numbers hotbar slots 1..9
        clearRads(c);
        setRad(c, 500.0);
        waitAlive(c, 30); // the rad buffer is refreshed once a second
        JsonObject state = q(c);
        System.out.println(c.node() + " dosimeter state: " + state);
        c.markLog();
        waitAlive(c, 80);
        List<String> clicks = c.logSinceMark("harness-sound.*id=hbm:item.geiger[1-6]");
        List<String> high = c.logSinceMark("harness-sound.*id=hbm:item.geiger[4-6]");
        System.out.println(c.node() + " dosimeter clicks in 80 ticks: " + clicks.size() + (clicks.isEmpty() ? "" : " e.g. " + clicks.get(0)));
        Expect.that(clicks.size() >= 8, "the carried dosimeter clicks every fifth tick in strong radiation: " + clicks.size());
        Expect.that(high.isEmpty(), "the dosimeter never plays geiger4..6: " + high);
        Expect.that(clicks.stream().allMatch(l -> l.contains("id=hbm:item.geiger3")),
                "a rad buffer >= 2 chooses only geiger3 (1.7 list [3]): " + clicks);

        c.markLog();
        c.use();
        c.waitTicks(10);
        List<String> boop = c.logSinceMark("harness-sound.*id=hbm:item.tech_boop");
        List<String> chat = c.logSinceMark("DOSIMETER");
        Expect.that(!boop.isEmpty(), "a right-click plays hbm:item.tech_boop");
        Expect.that(!chat.isEmpty(), "a right-click prints the DOSIMETER readout");
        c.screenshot("10-dosimeter-readout");

        noRadiation(c);
        waitAlive(c, 60);
        c.markLog();
        c.waitTicks(200); // 40 click ticks: 1.7 rolls geiger1 one in 100
        List<String> quiet = c.logSinceMark("harness-sound.*id=hbm:item.geiger[1-6]");
        System.out.println(c.node() + " dosimeter background clicks in 200 ticks: " + quiet.size());
        Expect.that(quiet.size() <= 4, "without radiation the dosimeter is almost silent: " + quiet.size());
        Expect.that(quiet.stream().allMatch(l -> l.contains("id=hbm:item.geiger1")), "background clicks are geiger1: " + quiet);
        c.cmd("clear");
        c.cmd("gamemode creative");
    }

    /**
     * Waits {@code ticks}, resetting the player's contamination every 20 ticks. 500 RAD/s kills a survival player in
     * about two seconds (1.7 too: the carrier's radiation is not the dosimeter's business); a dead player drops the
     * dosimeter and every click after that is missing, which is what failed this scenario on four nodes.
     */
    static void waitAlive(Client c, int ticks) {
        for (int left = ticks; left > 0; left -= 20) {
            c.waitTicks(Math.min(20, left));
            clearRads(c);
        }
    }

    /**
     * Removes all chunk radiation ({@code /ntmrad clear}, 1.7's command). Setting the player's chunk to 0 is not enough:
     * RadiationSystemNT has spread the 500 RAD/s into the neighbouring pockets, which flow back, so the rad buffer stayed
     * high and the "silent" window clicked on every click tick.
     */
    static void noRadiation(Client c) {
        setRad(c, 0.0);
        c.cmd("ntmrad clear");
    }

    /** Resets the player's contamination (hbm_props), so the strong-radiation phases cannot kill the player. */
    static void clearRads(Client c) {
        JsonObject p = new JsonObject();
        p.addProperty("set_rads", 0.0);
        c.query("hbm_props", p);
    }

    private void geigerCounter(Client c) {
        clearRads(c);
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.cmd("give @s hbm:geiger_counter 1");
        c.hotbar(1); // the harness numbers hotbar slots 1..9
        setRad(c, 500.0);
        waitAlive(c, 30); // the rad buffer is refreshed once a second
        System.out.println(c.node() + " geiger counter state: " + q(c));
        c.markLog();
        waitAlive(c, 80);
        List<String> clicks = c.logSinceMark("harness-sound.*id=hbm:item.geiger[1-6]");
        clearRads(c);
        System.out.println(c.node() + " geiger counter clicks in 80 ticks: " + clicks.size() + (clicks.isEmpty() ? "" : " e.g. " + clicks.get(0)));
        Expect.that(clicks.size() >= 8, "the carried geiger counter clicks every fifth tick in strong radiation: " + clicks.size());
        Expect.that(clicks.stream().allMatch(l -> l.contains("id=hbm:item.geiger6")),
                "a rad buffer >= 30 chooses only geiger6 (1.7 list [6]): " + clicks);

        c.markLog();
        c.use();
        c.waitTicks(10);
        List<String> boop = c.logSinceMark("harness-sound.*id=hbm:item.tech_boop");
        List<String> chat = c.logSinceMark("GEIGER COUNTER");
        List<String> received = c.logSinceMark("recieved radiation");
        Expect.that(!boop.isEmpty(), "a right-click plays hbm:item.tech_boop");
        Expect.that(!chat.isEmpty(), "a right-click prints the GEIGER COUNTER readout");
        Expect.that(received.isEmpty(), "1.7's readout has no received-radiation line: " + received);
        c.screenshot("15-geiger-counter-readout");

        noRadiation(c);
        waitAlive(c, 60);
        c.markLog();
        c.waitTicks(200); // 40 click ticks: 1.7 rolls geiger1 one in 50
        List<String> quiet = c.logSinceMark("harness-sound.*id=hbm:item.geiger[1-6]");
        System.out.println(c.node() + " geiger counter background clicks in 200 ticks: " + quiet.size());
        Expect.that(quiet.size() <= 5, "without radiation the geiger counter is almost silent: " + quiet.size());
        Expect.that(quiet.stream().allMatch(l -> l.contains("id=hbm:item.geiger1")), "background clicks are geiger1: " + quiet);
        c.cmd("clear");
        c.cmd("gamemode creative");
    }

    private void glasses(Client c) {
        c.cmd("gamerule randomTickSpeed 0"); // the gas would drift and decay (26.1 renamed the rule)
        c.cmd("gamerule random_tick_speed 0");
        c.cmd("fill " + (X + 3) + " " + L + " " + (Z - 2) + " " + (X + 7) + " " + (L + 2) + " " + (Z + 2) + " hbm:gas_radon");
        c.teleport(X + 0.5, L, Z + 0.5);
        c.lookAt(X + 5, L + 1, Z);
        c.cmd("item replace entity @s armor.head with minecraft:air");
        c.waitTicks(20);
        long before = q(c).get("revealed").getAsLong();
        c.waitTicks(100);
        long bare = q(c).get("revealed").getAsLong() - before;
        c.screenshot("20-radon-bare-head");
        Expect.that(bare == 0, "a bare head sees no gas cloud: " + bare);

        c.cmd("item replace entity @s armor.head with hbm:ashglasses");
        c.waitTicks(20);
        before = q(c).get("revealed").getAsLong();
        c.waitTicks(100);
        JsonObject after = q(c);
        long seen = after.get("revealed").getAsLong() - before;
        System.out.println(c.node() + " ash glasses: " + seen + " radon clouds in 100 ticks, chance " + after.get("chance"));
        c.screenshot("21-radon-ash-glasses");
        Expect.that(after.get("chance").getAsInt() == 1, "plain HBM shows a cloud on every display tick: " + after);
        Expect.that(seen >= 20, "the ash glasses show radon clouds (75 cells, about 1.7 a tick): " + seen);
    }
}
