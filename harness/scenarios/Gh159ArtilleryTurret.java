import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GitHub issue 159, the artillery turret against 1.7: each shot throws a 16" casing seven ticks later and puffs 1.7's
 * {@code largeexplode} (one tinted flash plus five grey smoke puffs, not five white flash balls); the plain shell's blast
 * throws no visible ground chunks (1.7 debris size 0) while the HE shell's does; the chunks are lit where they fly; and
 * the shell moves on the client only inside the client tick, interpolated towards the server.
 */
public final class Gh159ArtilleryTurret implements Scenario {

    private static final int X = 1630;
    private static final int Y = 100;
    private static final int Z = 1630;
    private static final int RANGE = 40;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z + RANGE / 2, RANGE);
        Fixtures.fill(c, X - RANGE, Y - 2, Z + RANGE / 2 - RANGE, X + RANGE, Y - 1, Z + RANGE / 2 + RANGE, "minecraft:stone");
        c.cmd("time set 6000");
        c.cmd("gamemode creative");
        c.setOption("camera", "first_person");
        List<String> wrong = new ArrayList<>();
        try {
            smoothness(c, wrong);
            shot(c, "ammo_arty", false, wrong);
            shot(c, "ammo_arty_he", true, wrong);
        } finally {
            c.tickRate(20F);
            c.cmd("kill @e[type=hbm:entity_artillery_shell]");
            c.cmd("setblock " + X + " " + (Y + 1) + " " + Z + " minecraft:air");
        }
        Expect.that(wrong.isEmpty(), "1.7 artillery turret: " + wrong);
        Expect.logCleanSinceMark(c, "hbm.*Exception|Exception.*hbm|/(ERROR|FATAL)[]].*hbm");
    }

    private static void smoothness(Client c, List<String> wrong) throws Exception {
        c.teleport(X + 0.5, Y + 1, Z - 8.5);
        c.look(0, -30);
        c.waitTicks(10);
        JsonObject start = new JsonObject();
        start.addProperty("type", "hbm:entity_artillery_shell");
        c.query("hbm_frame_moves", start);
        float rate = c.tickRate(4F);
        c.cmd("summon hbm:entity_artillery_shell " + (X + 0.5) + " " + (Y + 3) + " " + (Z + 0.5)
                + " {Motion:[0.0d,1.2d,0.8d],targetX:" + (X + 0.5) + "d,targetY:" + (Y + 1) + "d,targetZ:" + (Z + RANGE + 0.5) + "d}");
        c.waitTicks(12);
        JsonObject result = c.query("hbm_frame_moves", new JsonObject());
        c.tickRate(rate);
        c.cmd("kill @e[type=hbm:entity_artillery_shell]");
        c.waitTicks(10);
        System.out.println("[Gh159] " + c.node() + " shell frames " + result);
        int moves = result.get("moves").getAsInt();
        if (result.get("frames").getAsInt() < 10) wrong.add("shell: only " + result.get("frames") + " frames");
        if (moves > 0) wrong.add("shell moved between client ticks in " + moves + " frames (largest "
                + result.get("largest") + ")");
    }

    private static void shot(Client c, String ammo, boolean he, List<String> wrong) throws Exception {
        String tag = ammo.substring("ammo_".length());
        Fixtures.setBlock(c, X, Y + 1, Z, "minecraft:air");
        c.waitTicks(2);
        Fixtures.setBlock(c, X, Y + 1, Z, "hbm:turret_arty[facing=north]");
        c.cmd("data merge block " + X + " " + (Y + 1) + " " + Z + " {power:100000L,isOn:1b,mode:2s}");
        c.cmd("item replace block " + X + " " + (Y + 1) + " " + Z + " container.1 with hbm:" + ammo + " 1");
        c.teleport(X + 0.5, Y + 1, Z - 11.5);
        c.waitTicks(5);
        c.lookAt(X + 0.5, Y + 12, Z + 0.5);
        JsonObject cmd = new JsonObject();
        JsonArray a = new JsonArray();
        for (int v : new int[]{X, Y + 1, Z, X, Y + 1, Z + RANGE}) a.add(v);
        cmd.add("command", a);
        System.out.println("[Gh159] " + c.node() + " " + tag + " command " + c.query("hbm_gapsc", cmd).get("command_accepted"));

        // the muzzle: count what the shot leaves in the six blocks around the barrel's tip
        JsonObject muzzle = point(X + 0.5, Y + 13, Z + 0.5, 7);
        JsonObject fx = null;
        for (int i = 0; i < 400 && fx == null; i++) {
            JsonObject now = c.query("hbm_turret_fx", muzzle);
            if (flashes(now) + puffs(now) > 0) fx = now;
            else c.waitTicks(1);
        }
        if (fx == null) {
            wrong.add(tag + ": the turret never fired");
            return;
        }
        c.screenshot(tag + "-muzzle");
        JsonObject find = new JsonObject();
        find.addProperty("server", true);
        find.addProperty("radius", 4000);
        find.addProperty("type", "artillery_shell");
        JsonArray shells = c.query("entities", find).getAsJsonArray("entities");
        double[] landing = shells.isEmpty() ? null : landing(shells.get(0).getAsJsonObject());
        int flash = 0;
        int puff = 0;
        for (int i = 0; i < 4; i++) {
            JsonObject now = c.query("hbm_turret_fx", muzzle);
            flash = Math.max(flash, flashes(now));
            puff = Math.max(puff, puffs(now));
            System.out.println("[Gh159] " + c.node() + " " + tag + " muzzle " + now.get("particles"));
            c.waitTicks(1);
        }
        // the flash lives 6 to 9 ticks and a minimized 26.1 client can poll after it is gone; the puffs live longer
        if (flash > 1 || puff < 5) wrong.add(tag + ": muzzle shows " + flash + " flashes and " + puff
                + " smoke puffs, 1.7 shows 1 and 5");

        c.waitTicks(20);
        c.lookAt(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(3);
        c.screenshot(tag + "-casing");
        JsonObject casings = c.query("hbm_turret_fx", muzzle).getAsJsonObject("casings");
        System.out.println("[Gh159] " + c.node() + " " + tag + " casings " + casings);
        if (!casings.has(ammo)) wrong.add(tag + ": no spent casing " + ammo + " (" + casings + ")");

        c.teleport(X + 0.5, Y + 1, Z + RANGE - 15.5);
        c.waitTicks(3);
        c.lookAt(X + 0.5, Y + 4, Z + RANGE + 0.5);
        JsonObject far = point(X + 0.5, Y + 1, Z + RANGE + 0.5, 30);
        List<JsonArray> debris = new ArrayList<>();
        for (int i = 0; i < 600 && debris.isEmpty(); i++) {
            for (JsonElement e : c.query("hbm_turret_fx", far).getAsJsonArray("debris")) debris.add(e.getAsJsonArray());
            if (debris.isEmpty()) c.waitTicks(1);
        }
        c.waitTicks(3);
        c.screenshot(tag + "-impact-a");
        JsonObject flight = c.query("hbm_turret_fx", far);
        c.waitTicks(5);
        c.screenshot(tag + "-impact-b");
        System.out.println("[Gh159] " + c.node() + " " + tag + " debris at impact " + debris + ", 3 ticks later "
                + flight.get("debris"));
        if (debris.isEmpty()) {
            wrong.add(tag + ": no blast debris at all");
            return;
        }
        // the blast's debris leaves from the impact point; 1.7 flies the shell with nothing but gravity
        double hitX = debris.get(0).get(3).getAsDouble();
        double hitZ = debris.get(0).get(5).getAsDouble();
        double offTarget = Math.hypot(hitX - (X + 0.5), hitZ - (Z + RANGE + 0.5));
        System.out.println("[Gh159] " + c.node() + " " + tag + " impact " + hitX + ", " + hitZ + "; gravity-only flight from "
                + "the first sample predicts " + (landing == null ? "nothing" : landing[0] + ", " + landing[1])
                + "; " + offTarget + " from the target");
        if (landing == null) wrong.add(tag + ": the shell was never seen in flight");
        else if (Math.hypot(hitX - landing[0], hitZ - landing[1]) > 0.5) wrong.add(tag + ": the shell landed "
                + Math.hypot(hitX - landing[0], hitZ - landing[1]) + " from its gravity-only path");
        if (offTarget > 2D) wrong.add(tag + ": the shell landed " + offTarget + " from the target");
        int drawn = 0;
        int dark = 0;
        for (JsonElement e : flight.getAsJsonArray("debris")) {
            JsonArray d = e.getAsJsonArray();
            if (d.get(0).getAsInt() == 0) continue;
            drawn++;
            if (Math.max(d.get(1).getAsInt(), d.get(2).getAsInt()) < 12) dark++;
        }
        if (!he && drawn > 0) wrong.add(tag + ": " + drawn + " visible ground chunks, 1.7 throws none");
        if (he && drawn == 0) wrong.add(tag + ": no visible ground chunks");
        if (dark > 0) wrong.add(tag + ": " + dark + " of " + drawn + " chunks drawn with light below 12 in open air");
        c.waitTicks(80);
    }

    /** 1.7 {@code EntityThrowableNT}: move by the motion (stopping where the step meets the ground), then gravity. */
    private static double[] landing(JsonObject shell) {
        double x = shell.get("x").getAsDouble();
        double y = shell.get("y").getAsDouble();
        double z = shell.get("z").getAsDouble();
        double vx = shell.get("vx").getAsDouble();
        double vy = shell.get("vy").getAsDouble();
        double vz = shell.get("vz").getAsDouble();
        double ground = Y + 1;
        for (int tick = 0; tick < 2000; tick++) {
            if (y + vy <= ground) {
                double f = (ground - y) / vy;
                return new double[] {x + vx * f, z + vz * f};
            }
            x += vx;
            y += vy;
            z += vz;
            vy -= 9.81D * 0.05D;
        }
        return null;
    }

    private static JsonObject point(double x, double y, double z, double r) {
        JsonObject p = new JsonObject();
        p.addProperty("x", x);
        p.addProperty("y", y);
        p.addProperty("z", z);
        p.addProperty("r", r);
        return p;
    }

    /** Vanilla's large explosion particle (1.7 {@code EntityLargeExplodeFX}), by its Mojang or Yarn name. */
    private static int flashes(JsonObject fx) {
        return count(fx, "HugeExplosionParticle") + count(fx, "ExplosionLargeParticle");
    }

    /** Vanilla's poof particle (1.7 {@code EntityExplodeFX}). */
    private static int puffs(JsonObject fx) {
        return count(fx, "ExplodeParticle") + count(fx, "ExplosionSmokeParticle");
    }

    private static int count(JsonObject fx, String name) {
        JsonObject particles = fx.getAsJsonObject("particles");
        int n = 0;
        for (Map.Entry<String, JsonElement> e : particles.entrySet()) {
            if (e.getKey().equals(name)) n += e.getValue().getAsInt();
        }
        return n;
    }
}
