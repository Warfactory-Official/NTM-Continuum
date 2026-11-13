import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 174, the express delivery shell against 1.7: filling an empty shell in the crafting grid gives the
 * closed {@code ammo_arty_cargo_full} sprite, and an artillery turret firing it lands a shell that sticks where it
 * hit and hands its cargo to the player who right-clicks it.
 */
public final class Gh174DeliveryShell implements Scenario {

    private static final int X = 1730;
    private static final int Y = 100;
    private static final int Z = 1730;
    private static final int RANGE = 40;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z + RANGE / 2, RANGE);
        Fixtures.fill(c, X - RANGE, Y - 2, Z + RANGE / 2 - RANGE, X + RANGE, Y - 1, Z + RANGE / 2 + RANGE, "minecraft:stone");
        c.cmd("time set 6000");
        c.cmd("gamemode creative");
        c.cmd("kill @e[type=hbm:entity_artillery_shell]");
        List<String> wrong = new ArrayList<>();
        try {
            fill(c, wrong);
            fire(c, wrong);
        } finally {
            c.cmd("kill @e[type=hbm:entity_artillery_shell]");
            c.cmd("setblock " + X + " " + (Y + 1) + " " + Z + " minecraft:air");
        }
        Expect.that(wrong.isEmpty(), "1.7 delivery shell: " + wrong);
        Expect.logCleanSinceMark(c, "hbm.*Exception|Exception.*hbm|/(ERROR|FATAL)[]].*hbm");
    }

    /** The 2x2 grid of the player's own inventory: the empty shell and a diamond make a closed shell. */
    private static void fill(Client c, List<String> wrong) throws Exception {
        c.cmd("clear @s");
        c.cmd("give @s hbm:ammo_arty_cargo 1");
        c.cmd("give @s minecraft:diamond 1");
        c.cmd("gamemode survival");
        c.waitTicks(5);
        c.key("key.inventory");
        c.waitScreen("Inventory", 5000);
        c.clickSlot(36);
        c.clickSlot(1);
        c.clickSlot(37);
        c.clickSlot(2);
        c.waitTicks(5);
        c.screenshot("01-grid");
        c.clickSlot(0);
        c.clickSlot(38);
        c.waitTicks(3);
        c.key("escape");
        c.cmd("gamemode creative");
        c.hotbar(3);
        c.waitTicks(5);
        c.screenshot("02-hotbar");
        JsonObject held = c.query("hbm_held_state", new JsonObject());
        System.out.println("[Gh174] " + c.node() + " hotbar states " + held + " held " + c.player().get("main_hand"));
        JsonArray states = held.getAsJsonArray("hotbar");
        if (!"hbm:ammo_arty_cargo".equals(c.player().getAsJsonObject("main_hand").get("id").getAsString())) {
            wrong.add("crafting gave " + c.player().get("main_hand"));
        } else if (states.get(2).getAsInt() != 1) {
            wrong.add("the filled shell draws model state " + states.get(2) + ", 1.7 draws ammo_arty_cargo_full");
        }
    }

    private static void fire(Client c, List<String> wrong) throws Exception {
        Fixtures.setBlock(c, X, Y + 1, Z, "hbm:turret_arty[facing=north]");
        c.cmd("data merge block " + X + " " + (Y + 1) + " " + Z + " {power:100000L,isOn:1b,mode:2s}");
        c.cmd("item replace block " + X + " " + (Y + 1) + " " + Z + " container.1 from entity @s hotbar.2");
        c.teleport(X + 0.5, Y + 1, Z + RANGE - 12.5);
        c.waitTicks(5);
        JsonObject cmd = new JsonObject();
        JsonArray a = new JsonArray();
        for (int v : new int[]{X, Y + 1, Z, X, Y + 1, Z + RANGE}) a.add(v);
        cmd.add("command", a);
        System.out.println("[Gh174] " + c.node() + " command " + c.query("hbm_gapsc", cmd).get("command_accepted"));

        JsonObject find = new JsonObject();
        find.addProperty("server", true);
        find.addProperty("radius", 4000);
        find.addProperty("type", "artillery_shell");
        boolean flew = false;
        for (int i = 0; i < 400 && !flew; i++) {
            flew = !c.query("entities", find).getAsJsonArray("entities").isEmpty();
            if (!flew) c.waitTicks(1);
        }
        if (!flew) {
            wrong.add("the turret never fired");
            return;
        }
        c.lookAt(X + 0.5, Y + 1, Z + RANGE + 0.5);
        // the shell climbs about 2,500 blocks and comes down some 160 ticks later; wait until it stops moving
        double lastY = Double.NaN;
        for (int i = 0; i < 40; i++) {
            c.waitTicks(10);
            JsonArray now = c.query("entities", find).getAsJsonArray("entities");
            if (now.isEmpty()) break;
            double y = now.get(0).getAsJsonObject().get("y").getAsDouble();
            System.out.println("[Gh174] " + c.node() + " flight y " + y);
            if (y == lastY) break;
            lastY = y;
        }
        c.waitTicks(20);
        JsonArray shells = c.query("entities", find).getAsJsonArray("entities");
        System.out.println("[Gh174] " + c.node() + " shells after landing " + shells);
        if (shells.isEmpty()) {
            wrong.add("no shell left after landing");
            return;
        }
        JsonObject shell = shells.get(0).getAsJsonObject();
        double sx = shell.get("x").getAsDouble();
        double sy = shell.get("y").getAsDouble();
        double sz = shell.get("z").getAsDouble();
        if (Math.abs(sy - (Y + 1)) > 0.5) wrong.add("the shell stuck at y " + sy + ", the ground is at " + (Y + 1));
        if (Math.hypot(sx - (X + 0.5), sz - (Z + RANGE + 0.5)) > 3D) {
            wrong.add("the shell stuck at " + sx + ", " + sz + ", far from the target");
        }
        JsonObject clientFind = find.deepCopy();
        clientFind.addProperty("server", false);
        JsonArray seen = c.query("entities", clientFind).getAsJsonArray("entities");
        System.out.println("[Gh174] " + c.node() + " client shells " + seen);
        if (seen.isEmpty() || Math.abs(seen.get(0).getAsJsonObject().get("y").getAsDouble() - sy) > 0.5) {
            wrong.add("the client does not show the shell where it stuck: " + seen);
        }
        c.teleport(sx + 0.5, Y + 1, sz - 2.5);
        c.waitTicks(5);
        c.lookAt(sx, sy + 0.25, sz);
        c.waitTicks(3);
        c.screenshot("03-stuck");
        c.cmd("clear @s");
        c.use(1);
        c.waitTicks(10);
        String got = c.inventory().toString();
        System.out.println("[Gh174] " + c.node() + " after opening " + got);
        if (!got.contains("minecraft:diamond")) wrong.add("opening the shell gave no diamond");
        if (!c.query("entities", find).getAsJsonArray("entities").isEmpty()) wrong.add("the opened shell is still there");
    }
}
