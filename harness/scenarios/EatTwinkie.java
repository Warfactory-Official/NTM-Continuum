import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/** A hungry survival player eats a Twinkie, then vanilla bread as the control (Discord: "I cannot eat twinkie"). */
public final class EatTwinkie implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.cmd("gamemode survival");
        c.cmd("difficulty normal");
        try {
            for (String item : new String[] {"hbm:twinkie", "minecraft:bread"}) {
                c.cmd("clear");
                c.cmd("effect give @s minecraft:hunger 4 255");
                c.waitTicks(90);
                c.cmd("effect clear @s");
                c.cmd("item replace entity @s hotbar.0 with " + item + " 4");
                c.hotbar(1);
                c.waitTicks(5);
                int before = food(c);
                c.use(45);
                c.waitTicks(10);
                int after = food(c);
                String held = c.query("inventory", new JsonObject()).toString();
                System.out.println(c.node() + " " + item + ": food " + before + " -> " + after + ", inventory "
                        + held.substring(0, Math.min(held.length(), 300)));
                Expect.that(before < 20, item + ": the player was hungry (" + before + ")");
                Expect.that(after > before, item + ": eating raised food " + before + " -> " + after);
            }
        } finally {
            c.cmd("clear");
            c.cmd("gamemode creative");
            c.cmd("difficulty peaceful");
        }
    }

    private static int food(Client c) {
        JsonObject p = c.player();
        return p.get("food").getAsInt();
    }
}
