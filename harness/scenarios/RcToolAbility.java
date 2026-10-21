import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Ability tools in a real client: right-click cycles the preset (info HUD line, glint, sound), sneaking resets to the
 * first, the ability screen opens on Left Alt, and the selected preset survives leaving and rejoining the world.
 */
public final class RcToolAbility implements Scenario {

    private final List<String> bad = new ArrayList<>();

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, 0, 100, 0, 8);
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.cmd("give @s hbm:schrabidium_pickaxe");
        c.cmd("give @s hbm:cmb_axe");
        c.waitTicks(6);
        c.hotbar(1);
        c.waitTicks(3);
        c.look(0, -40);

        List<String> seen = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            c.use();
            c.waitTicks(6);
            String msg = info(c);
            seen.add(msg);
            System.out.println(c.node() + " use " + i + ": " + msg);
            if (i == 3) c.screenshot("ability-hud-" + i);
        }
        // 8 presets: none + 3 area + 4 harvest; the ninth use wraps to none
        check(seen.get(0).contains("Enabled"), "first use enables a preset: " + seen.get(0));
        check(seen.get(7).contains("deactivated"), "eight uses wrap to none: " + seen.get(7));
        check(seen.get(8).contains("Enabled"), "ninth use enables again: " + seen.get(8));

        // sneak + use goes back to the first preset
        c.keyDown("key.sneak");
        c.waitTicks(2);
        c.use();
        c.waitTicks(6);
        String reset = info(c);
        System.out.println(c.node() + " sneak-use: " + reset);
        check(reset.contains("deactivated"), "sneak resets to none: " + reset);
        c.keyUp("key.sneak");
        c.waitTicks(2);

        c.use();
        c.waitTicks(6);
        c.use();
        c.waitTicks(6);
        String atTwo = info(c);
        System.out.println(c.node() + " before leaving: " + atTwo);
        c.screenshot("ability-held");

        c.key("key.hbm.abilityAlt");
        c.waitTicks(10);
        System.out.println(c.node() + " alt screen: " + c.screenClass());
        c.screenshot("ability-screen");
        c.key("escape");
        c.waitTicks(5);

        c.leaveWorld(30000);
        c.joinWorld("harness", 60000);
        c.waitInWorld(60000);
        c.waitTicks(20);
        c.hotbar(1);
        c.waitTicks(3);
        c.use();
        c.waitTicks(6);
        String afterRejoin = info(c);
        System.out.println(c.node() + " after rejoin, next use: " + afterRejoin);
        check(!afterRejoin.equals(atTwo) && afterRejoin.contains("Enabled"), "rejoin keeps the preset index: " + afterRejoin);

        for (String b : bad) System.out.println("MISMATCH " + b);
        Expect.that(bad.isEmpty(), "ability mismatches: " + bad);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }

    private String info(Client c) {
        JsonObject r = c.query("hbm_info", new JsonObject());
        JsonArray msgs = r.has("messages") ? r.getAsJsonArray("messages") : new JsonArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < msgs.size(); i++) sb.append(msgs.get(i).getAsString()).append(" / ");
        return sb.toString();
    }

    private void check(boolean ok, String what) {
        if (!ok) bad.add(what);
    }
}
