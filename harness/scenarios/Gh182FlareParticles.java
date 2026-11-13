import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 182: 1.7's flare stack burns with one gas flame a tick from 11.75 above its core, plus a pilot smoke
 * within 32 blocks; venting, it draws the gas tower column in the fluid's colour. It never makes vanilla flames, large
 * smoke or campfire smoke. Particles near the top are counted by class after a few seconds of each mode.
 */
public final class Gh182FlareParticles implements Scenario {

    private static final int Y = 100;
    private static final int L = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        List<String> failures = new ArrayList<>();
        try {
            Fixtures.platform(c, 0, Y, 0, 12);
            c.cmd("gamemode spectator");
            c.cmd("gamerule sendCommandFeedback false");
            c.cmd("time set 6000");
            c.cmd("weather clear");
            c.cmd("fill -6 " + L + " -6 6 " + (L + 24) + " 6 minecraft:air");
            c.teleport(0.5, L + 9, 12.5);
            c.lookAt(0.5, L + 13, 0.5);
            c.cmd("setblock 0 " + L + " 0 hbm:machine_flare[facing=north]{isOn:1b,doesBurn:1b,gas:50000,gas_max:64000,"
                    + "gas_type:\"hbm:gas\"}");
            c.waitTicks(80);
            c.screenshot("flare-burning");
            JsonObject burning = particles(c);
            c.cmd("data merge block 0 " + L + " 0 {doesBurn:0b}");
            c.waitTicks(100);
            c.screenshot("flare-venting");
            JsonObject venting = particles(c);
            System.out.println("[Gh182] " + c.node() + " burning " + burning + ", venting " + venting);
            if (count(burning, "ParticleGasFlame") < 10) failures.add("burning: gas flames " + burning);
            if (count(burning, "SmokeParticle") < 5) failures.add("burning: pilot smoke " + burning);
            if (count(venting, "ParticleCoolingTower") < 20) failures.add("venting: gas column " + venting);
            for (String stray : new String[] {"FlameParticle", "LargeSmokeParticle", "CampfireSmokeParticle"}) {
                if (count(burning, stray) + count(venting, stray) > 0) failures.add("vanilla stand-in " + stray);
            }
            Expect.that(failures.isEmpty(), "the flare stack makes 1.7's particles: " + failures);
            Expect.logCleanSinceMark(c, "Exception|/(ERROR|FATAL)[]].*hbm");
        } finally {
            c.cmd("fill -6 " + L + " -6 6 " + (L + 24) + " 6 minecraft:air");
            c.cmd("gamemode creative");
        }
    }

    private static JsonObject particles(Client c) {
        JsonObject params = new JsonObject();
        params.addProperty("x", 0.5);
        params.addProperty("y", L + 14);
        params.addProperty("z", 0.5);
        params.addProperty("r", 12);
        return c.query("hbm_turret_fx", params).getAsJsonObject("particles");
    }

    private static int count(JsonObject particles, String cls) {
        int n = 0;
        for (String key : particles.keySet()) if (key.equals(cls)) n += particles.get(key).getAsInt();
        return n;
    }
}
