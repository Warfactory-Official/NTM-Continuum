import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fix phase 3 (History §19ej-3): restored 1.7 machine-table entries used in the real machines.
 * <ul>
 *   <li>anvil construction (steel anvil, tier 2): the arc welder, 1.7 AnvilRecipes:383 (4 cast steel plates, 8 tungsten
 *       ingots, a transformer, 2 electrodes), picked through the GUI's search box, recipe grid and craft button;</li>
 *   <li>centrifuge: a euphemium cluster block, 1.7 CentrifugeRecipes:186, into 7 euphemium nuggets, 4 schrabidium
 *       powder, 2 starmetal ingots and 2 solinium nuggets.</li>
 * </ul>
 */
public final class Fix3Recipes implements Scenario {

    private static final int X = 0, Y = 100, Z = 0, L = 101;
    private static final int ANVIL_X = -6, CENT_X = 6;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 16);
        c.cmd("gamerule sendCommandFeedback false");

        anvil(c);
        centrifuge(c);

        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|unregistered hbm:|is not registered).*");
    }

    private void anvil(Client c) {
        Fixtures.placeOnTop(c, "hbm:anvil_steel", ANVIL_X, Y, Z);
        Expect.block(c, ANVIL_X, L, Z, "hbm:anvil_steel");
        c.cmd("clear");
        Fixtures.give(c, "hbm:plate_cast_steel", 4);
        Fixtures.give(c, "hbm:ingot_tungsten", 8);
        Fixtures.give(c, "hbm:machine_transformer", 1);
        Fixtures.give(c, "hbm:arc_electrode", 2);
        c.hotbar(9);
        c.teleport(ANVIL_X + 2.5, L, Z + 0.5);
        c.waitTicks(3);
        Fixtures.interact(c, ANVIL_X, L, Z);
        c.waitScreen("GUIAnvil", 5_000L);
        Expect.screenStaysOpen(c, "GUIAnvil", 5);
        JsonObject s = c.screen();
        int left = s.get("left").getAsInt(), top = s.get("top").getAsInt();
        JsonObject box = editBox(c);
        c.click(box.get("center_x").getAsDouble(), box.get("center_y").getAsDouble());
        c.waitTicks(2);
        c.type("arc welder");
        c.waitTicks(3);
        c.click(left + 24, top + 79);          // the first recipe of the filtered grid
        c.waitTicks(3);
        c.screenshot("10-anvil-arc-welder-selected");
        c.click(left + 61, top + 62);          // the craft button
        c.waitTicks(6);
        c.screenshot("11-anvil-arc-welder-crafted");
        close(c);
        Expect.that(inventoryCount(c, "hbm:machine_arc_welder") == 1 && inventoryCount(c, "hbm:plate_cast_steel") == 0
                        && inventoryCount(c, "hbm:ingot_tungsten") == 0 && inventoryCount(c, "hbm:arc_electrode") == 0
                        && inventoryCount(c, "hbm:machine_transformer") == 0,
                "the anvil built an arc welder from its 1.7 inputs: " + c.inventory());
    }

    private void centrifuge(Client c) {
        Fixtures.placeOnTop(c, "hbm:machine_centrifuge", CENT_X, Y, Z);
        int[] core = findCore(c, CENT_X, L, Z, "power");
        int cx = core[0], cy = core[1], cz = core[2];
        c.cmd("data merge block " + cx + " " + cy + " " + cz + " {power:100000L}");
        put(c, cx, cy, cz, 0, "hbm:block_euphemium_cluster", 1);
        int made = 0;
        for (int t = 0; t <= 600 && made < 7; t += 10) {
            c.waitTicks(10);
            c.cmd("data merge block " + cx + " " + cy + " " + cz + " {power:100000L}");
            made = count(nbt(c, cx, cy, cz), "hbm:nugget_euphemium");
        }
        c.hotbar(9);
        c.teleport(CENT_X + 2.5, L, Z + 0.5);
        c.waitTicks(3);
        Fixtures.interact(c, cx, cy, cz);
        c.waitScreen("GUICentrifuge", 5_000L);
        c.waitTicks(3);
        c.screenshot("20-centrifuge-euphemium-cluster");
        close(c);
        String n = nbt(c, cx, cy, cz);
        Expect.that(made == 7 && count(n, "hbm:powder_schrabidium") == 4 && count(n, "hbm:ingot_starmetal") == 2
                        && count(n, "hbm:nugget_solinium") == 2 && count(n, "hbm:block_euphemium_cluster") == 0,
                "the centrifuge split the euphemium cluster into 7/4/2/2; nbt " + n);
    }

    // ------------------------------------------------------------------ helpers (as FinalRecipeContent)

    private static void put(Client c, int x, int y, int z, int slot, String item, int n) {
        c.cmd("item replace block " + x + " " + y + " " + z + " container." + slot + " with " + item + " " + n);
        c.waitTicks(1);
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
    }

    private static int count(String nbt, String id) {
        int total = 0;
        Matcher m = Pattern.compile("\\{[^{}]*id:\"" + Pattern.quote(id) + "\"[^{}]*}").matcher(nbt);
        while (m.find()) {
            Matcher n = Pattern.compile("(?i)count:([0-9]+)").matcher(m.group());
            total += n.find() ? Integer.parseInt(n.group(1)) : 1;
        }
        return total;
    }

    private static int inventoryCount(Client c, String id) {
        int total = 0;
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.has("id") && id.equals(s.get("id").getAsString())) total += s.has("count") ? s.get("count").getAsInt() : 1;
        }
        return total;
    }

    private static int[] findCore(Client c, int x, int y, int z, String key) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 2; dy++) {
            if (nbt(c, x + dx, y + dy, z + dz).contains(key + ":")) return new int[]{x + dx, y + dy, z + dz};
        }
        throw new IllegalStateException("no block entity with " + key + " near " + x + " " + y + " " + z);
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(2);
    }

    private static JsonObject editBox(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("widgets")) {
            JsonObject w = e.getAsJsonObject();
            if (w.get("class").getAsString().contains("EditBox")) return w;
        }
        throw new IllegalStateException("no text field on " + c.screenClass());
    }
}
