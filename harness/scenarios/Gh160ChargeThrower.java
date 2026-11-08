import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 160: the charge thrower's grappling hook latches into a wall and holding fire reels the player in, as
 * 1.7's {@code ItemGunChargeThrower.onUpdate} does, and once a round is fired the hook or charge is no longer drawn on
 * the launcher (1.7 {@code ItemRenderChargeThrower} draws it only while the magazine holds one or a reload runs).
 */
public final class Gh160ChargeThrower implements Scenario {

    private static final int X = 1610;
    private static final int Y = 100;
    private static final int Z = 1610;
    private static final int WALL = 14;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 20);
        Fixtures.fill(c, X - 4, Y + 1, Z + WALL, X + 4, Y + 8, Z + WALL, "minecraft:stone");
        c.cmd("time set 6000");
        c.cmd("gamemode survival");
        c.setOption("camera", "first_person");
        List<String> wrong = new ArrayList<>();
        try {
            c.teleport(X + 0.5, Y + 1, Z + 0.5);
            c.look(0, -8);
            load(c, "ct_hook");
            c.screenshot("hook-loaded");
            tap(c);
            c.waitTicks(30);
            c.screenshot("hook-fired");
            JsonObject before = c.query("hbm_bullets", new JsonObject());
            JsonObject hook = hook(before);
            System.out.println("[Gh160] " + c.node() + " after firing " + before);
            if (hook == null) {
                wrong.add("no hook in flight or stuck");
            } else if (hook.get("velocity").getAsDouble() >= 0.01D) {
                wrong.add("the hook did not latch: " + hook);
            }
            double startZ = before.get("player_z").getAsDouble();
            c.keyDown("mouse.left");
            c.waitTicks(40);
            c.screenshot("reeling");
            c.keyUp("mouse.left");
            JsonObject after = c.query("hbm_bullets", new JsonObject());
            double pulled = after.get("player_z").getAsDouble() - startZ;
            System.out.println("[Gh160] " + c.node() + " reeled " + pulled + " blocks: " + after);
            if (pulled < 3D) wrong.add("holding fire reeled the player only " + pulled + " blocks");

            c.cmd("kill @e[type=hbm:entity_bullet_mk4]");
            c.teleport(X + 0.5, Y + 1, Z + 0.5);
            c.look(0, -30);
            load(c, "ct_mortar");
            c.screenshot("mortar-loaded");
            tap(c);
            c.waitTicks(30);
            c.screenshot("mortar-fired");
        } finally {
            c.releaseAll();
            c.cmd("gamemode creative");
            c.cmd("clear @s");
        }
        Expect.that(wrong.isEmpty(), "1.7 charge thrower: " + wrong);
        Expect.logCleanSinceMark(c, "hbm.*Exception|Exception.*hbm|/(ERROR|FATAL)[]].*hbm");
    }

    private static void load(Client c, String ammo) throws Exception {
        c.cmd("clear @s");
        c.cmd("give @s hbm:gun_charge_thrower");
        c.cmd("give @s hbm:ammo_standard_" + ammo + " 1");
        c.hotbar(1);
        c.waitTicks(40);
        c.key("r");
        c.waitTicks(100);
    }

    private static void tap(Client c) throws Exception {
        c.keyDown("mouse.left");
        c.waitTicks(3);
        c.keyUp("mouse.left");
    }

    private static JsonObject hook(JsonObject bullets) {
        for (JsonElement e : bullets.getAsJsonArray("bullets")) {
            JsonObject b = e.getAsJsonObject();
            if (b.get("hook").getAsBoolean()) return b;
        }
        return null;
    }
}
