import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/** 1.12.2's "[Radiation Shielding]" and "Blast Resistance" block tooltip lines, hovered in the survival inventory. */
public final class BlockTraitTooltips implements Scenario {

    private static final String[] ITEMS = {"hbm:brick_concrete", "hbm:block_lead", "hbm:crate_tungsten",
            "hbm:reinforced_glass", "hbm:concrete_smooth", "minecraft:obsidian"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("gamemode survival");
        c.cmd("clear");
        try {
            for (int i = 0; i < ITEMS.length; i++) c.cmd("item replace entity @s hotbar." + i + " with " + ITEMS[i]);
            c.key("e");
            c.waitTicks(10);
            for (String item : ITEMS) {
                boolean found = false;
                for (JsonElement e : c.screen().getAsJsonArray("slots")) {
                    JsonObject s = e.getAsJsonObject();
                    if (!s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(item)) {
                        c.mouseMove(s.get("center_x").getAsDouble(), s.get("center_y").getAsDouble());
                        found = true;
                        break;
                    }
                }
                Expect.that(found, item + " is in the inventory");
                c.waitTicks(5);
                c.screenshot(item.replaceAll("[^a-z_]", "_") + "-tooltip");
            }
            c.key("escape");
        } finally {
            c.cmd("clear");
            c.cmd("gamemode creative");
        }
    }
}
