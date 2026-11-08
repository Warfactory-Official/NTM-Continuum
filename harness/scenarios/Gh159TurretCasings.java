import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 159, the artillery turret's siblings: every 1.7 turret that ejects brass ({@code usesCasings}) throws its
 * ammunition's casing, and each muzzle shows 1.7's {@code largeexplode}: one tinted flash and {@code count} grey puffs.
 */
public final class Gh159TurretCasings implements Scenario {

    private static final int Y = 100;
    private static final int Z = 1670;

    private record Case(String turret, String ammo, String casing, int puffs, long power) {}

    private static final Case[] CASES = {
            new Case("chekhov", "ammo_standard_bmg50_fmj 64", "bmg50fmj", 1, 10_000L),
            new Case("friendly", "ammo_standard_r556_fmj 64", "r556fmj", 1, 10_000L),
            new Case("sentry", "ammo_standard_p9_fmj 64", "p9fmj", 1, 10_000L),
            new Case("jeremy", "ammo_shell 8", "240standard", 5, 10_000L),
            new Case("howard", "ammo_dgk 2", "DGK", 1, 50_000L),
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 1670, Y, Z + 10, 40);
        c.cmd("time set 6000");
        c.cmd("difficulty normal");
        c.cmd("gamemode creative");
        c.setOption("camera", "first_person");
        List<String> wrong = new ArrayList<>();
        try {
            for (int i = 0; i < CASES.length; i++) duel(c, CASES[i], 1640 + i * 14, wrong);
        } finally {
            c.cmd("kill @e[type=minecraft:iron_golem]");
            c.cmd("difficulty peaceful");
        }
        Expect.that(wrong.isEmpty(), "1.7 turret casings and muzzle smoke: " + wrong);
        Expect.logCleanSinceMark(c, "hbm.*Exception|Exception.*hbm|/(ERROR|FATAL)[]].*hbm");
    }

    private static void duel(Client c, Case k, int x, List<String> wrong) throws Exception {
        c.cmd("kill @e[type=minecraft:iron_golem]");
        // the sentry is one block and has no facing
        Fixtures.setBlock(c, x, Y + 1, Z, "hbm:turret_" + k.turret + (k.turret.equals("sentry") ? "" : "[facing=north]"));
        c.cmd("data merge block " + x + " " + (Y + 1) + " " + Z + " {power:" + k.power
                + "L,isOn:1b,targetMobs:1b,targetAnimals:1b}");
        c.cmd("item replace block " + x + " " + (Y + 1) + " " + Z + " container.1 with hbm:" + k.ammo);
        c.cmd("summon minecraft:iron_golem " + (x + 0.5) + " " + (Y + 1) + " " + (Z + 20.5)
                + " {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
        String attr = c.node().startsWith("26.1") ? "minecraft:max_health" : "minecraft:generic.max_health";
        c.cmd("attribute @e[type=minecraft:iron_golem,limit=1] " + attr + " base set 1000");
        c.cmd("data merge entity @e[type=minecraft:iron_golem,limit=1] {Health:1000f}");
        c.teleport(x + 4.5, Y + 1, Z - 3.5);
        c.waitTicks(5);
        c.lookAt(x + 0.5, Y + 2, Z + 0.5);

        JsonObject around = new JsonObject();
        around.addProperty("x", x + 0.5);
        around.addProperty("y", Y + 2);
        around.addProperty("z", Z + 0.5);
        around.addProperty("r", 6);
        int flash = 0;
        int puff = 0;
        int casings = 0;
        String names = "";
        boolean shot = false;
        for (int i = 0; i < 400; i++) {
            JsonObject fx = c.query("hbm_turret_fx", around);
            int f = count(fx, "HugeExplosionParticle") + count(fx, "ExplosionLargeParticle");
            int p = count(fx, "ExplodeParticle") + count(fx, "ExplosionSmokeParticle");
            if (!shot && f + p > 0) {
                shot = true;
                c.screenshot(k.turret + "-muzzle");
            }
            if (f > 0 && f + p > flash + puff) {
                flash = f;
                puff = p;
            }
            JsonElement own = fx.getAsJsonObject("casings").get(k.casing);
            int n = own == null ? 0 : own.getAsInt();
            if (n > casings) {
                casings = n;
                names = fx.get("casings").toString();
            }
            if (shot && casings > 0 && i > 120) break;
            c.waitTicks(1);
        }
        c.lookAt(x + 0.5, Y + 1, Z - 1.5);
        c.waitTicks(3);
        c.screenshot(k.turret + "-casings");
        c.cmd("data merge block " + x + " " + (Y + 1) + " " + Z + " {isOn:0b}");
        System.out.println("[Gh159] " + c.node() + " " + k.turret + " shot " + shot + ", flash " + flash + ", puffs "
                + puff + ", casings " + casings + " " + names);
        if (!shot) {
            wrong.add(k.turret + " never fired");
            return;
        }
        if (casings == 0) wrong.add(k.turret + " threw no " + k.casing + " casing");
        if (puff < k.puffs) wrong.add(k.turret + " muzzle: " + flash + " flashes, " + puff + " puffs, 1.7 has "
                + k.puffs + " puffs per flash");
    }

    private static int count(JsonObject fx, String name) {
        JsonElement n = fx.getAsJsonObject("particles").get(name);
        return n == null ? 0 : n.getAsInt();
    }
}
