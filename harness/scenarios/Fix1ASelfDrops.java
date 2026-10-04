import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Fix wave 1A (2026-10-03): machines and rails that dropped nothing now drop themselves, exactly once, as 1.7's
 * {@code BlockDummyable.onBlockHarvested} did. Each block is hand-placed (so a multiblock builds its cells), then
 * mined in survival with a diamond pickaxe (Efficiency V, Haste: an instant break); the player must end up with exactly one of the item and the block
 * must be gone.
 */
public final class Fix1ASelfDrops implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        String[] ids = {"hbm:geiger", "hbm:furnace_iron", "hbm:rail_large_straight", "hbm:machine_armor_table"};
        int x = 0;
        for (String id : ids) {
            int z = 0;
            Fixtures.platform(c, x, Y, z, 7);
            c.cmd("gamemode creative");
            c.cmd("kill @e[type=item]");
            JsonObject placed = Fixtures.placeOnTop(c, id, x, Y, z);
            c.waitTicks(5);
            Expect.that(placed.toString().contains(id), id + " placed: " + placed);
            c.screenshot(String.format("%02d-placed-%s", x / 20, id.substring(4)));

            c.cmd("gamemode survival");
            c.cmd("clear");
            // A minimized, unfocused client never holds a continuous attack, so mining must finish on the first
            // tick: Efficiency V plus Haste 128 makes hardness 15 (the geiger) an instant survival break.
            c.cmd("effect give @s minecraft:haste 60 127 true");
            Fixtures.giveOnly(c, "minecraft:diamond_pickaxe", 1);
            c.hotbar(1);
            c.waitTicks(2);
            c.cmd("enchant @s minecraft:efficiency 5");
            c.teleport(x + 2.5, Y + 1, z + 0.5);
            c.waitTicks(3);
            c.lookAt(x + 0.5, Y + 1.3, z + 0.5);
            c.waitTicks(2);
            c.attack(2);
            c.waitTicks(5);
            c.teleport(x + 0.5, Y + 1, z + 0.5); // walk onto the drop so survival pickup collects it
            c.waitTicks(30);
            int held = count(c, id);
            int loose = 0;
            for (JsonElement e : c.entities(12)) {
                JsonObject o = e.getAsJsonObject();
                if (o.get("type").getAsString().equals("minecraft:item")) loose++;
            }
            JsonObject after = c.block(x, Y + 1, z);
            c.screenshot(String.format("%02d-mined-%s", x / 20, id.substring(4)));
            Expect.that(!after.toString().contains(id), id + " was mined: " + after);
            Expect.that(held == 1 && loose == 0, id + " drops itself exactly once (held " + held + ", loose item entities " + loose + ")");
            x += 20;
        }
        c.cmd("gamemode creative");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    private static int count(Client c, String itemId) {
        int count = 0;
        for (var e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.has("id") && itemId.equals(s.get("id").getAsString())) count += s.get("count").getAsInt();
        }
        return count;
    }
}
