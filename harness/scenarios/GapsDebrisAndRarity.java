import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * 1.7 ExplosionEffectStandard's per-block poof and smoke debris on a gold creeper's blast, next to a vanilla creeper
 * (no debris on modern versions), and 1.7 ItemCustomLore rarity colours and rune glint in tooltips.
 */
public final class GapsDebrisAndRarity implements Scenario {

    private static final int X = 420;
    private static final int Y = 100;
    private static final int Z = 420;
    private static final String[] ITEMS = {"hbm:ingot_schrabidium", "hbm:powder_iodine", "hbm:billet_flashlead",
            "hbm:rune_dagaz", "hbm:ingot_steel"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 12);
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("gamerule doMobSpawning false");
        c.cmd("weather clear");
        c.cmd("time set 6000");
        c.cmd("difficulty normal");
        try {
            blast(c, "hbm:entity_mob_gold_creeper", "gold-creeper");
            blast(c, "minecraft:creeper", "vanilla-creeper");
            // creative: the shared harness player carries radiation from earlier scenarios that kills in survival
            c.cmd("gamemode creative");
            c.cmd("clear");
            c.waitTicks(20);
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
            c.cmd("difficulty peaceful");
        }
    }

    private static void blast(Client c, String entity, String name) throws Exception {
        c.cmd("fill " + (X - 6) + " " + (Y + 1) + " " + (Z - 6) + " " + (X + 6) + " " + (Y + 6) + " " + (Z + 6) + " air");
        c.cmd("fill " + (X - 6) + " " + (Y - 3) + " " + (Z - 6) + " " + (X + 6) + " " + Y + " " + (Z + 6) + " stone");
        c.cmd("gamemode spectator");
        c.teleport(X + 0.5, Y + 6, Z - 11.5);
        c.lookAt(X + 0.5, Y + 1.5, Z + 0.5);
        c.waitTicks(10);
        c.cmd("summon " + entity + " " + (X + 0.5) + " " + (Y + 1) + " " + (Z + 0.5) + " {NoAI:1b,ignited:1b,Fuse:30s}");
        c.waitTicks(31);
        c.screenshot(name + "-a");
        c.waitTicks(4);
        c.screenshot(name + "-b");
        c.waitTicks(10);
        c.screenshot(name + "-c");
        c.waitTicks(40);
    }
}
