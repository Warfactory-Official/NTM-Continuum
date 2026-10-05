import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * Wave 2 HOOKS (History §19ek-hooks), in the seeded world, through the new Golem Core hooks on every loader:
 * <ol>
 *   <li>JEI: the spent fuel pool drum's page (1.7 FuelPoolHandler) from the drum's uses, and the arc furnace's
 *       furnace-smelting pass (the log line counts it, the solid page shows it);</li>
 *   <li>block break (Core BlockBreaks): breaking a block in 30 heavy metal gives lead poisoning, amplifier 2; a broken coal ore
 *       puts coal dust into its air sides;</li>
 *   <li>smog (Core Fogs): 60 soot in the region, eased in at 0.05 a tick, pulls the fog in to
 *       {@code renderDistance / (1 + (soot - 35) * 5 / 120)} and greys it; screenshots before and after;</li>
 *   <li>a duck summoned on a {@code /fillbiome} snowy plains patch lays the plain egg (26.1 would otherwise give it the
 *       cold variant);</li>
 *   <li>cyanide (Core ItemUses): Plan C on a golden apple in the iron anvil's smithing slots, eaten in survival: dead.</li>
 * </ol>
 */
public final class HooksWave2 implements Scenario {

    private static final int X = 0, Y = 100, Z = 0, L = 101;

    static JsonObject hooks(Client c, Object... kv) {
        JsonObject p = new JsonObject();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v instanceof Number n) p.addProperty((String) kv[i], n);
            else p.addProperty((String) kv[i], String.valueOf(v));
        }
        return c.query("hbm_hooks", p);
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useSeededWorld();
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("gamerule doMobSpawning false");
        c.cmd("time set noon");
        c.cmd("weather clear");
        Fixtures.platform(c, X, Y, Z, 12);
        c.teleport(X + 0.5, L, Z + 0.5);
        c.waitTicks(40);
        c.markLog();

        jei(c);
        blockBreak(c);
        smog(c);
        duck(c);
        cyanide(c);

        Expect.logCleanSinceMark(c, "(?i)(exception|unregistered hbm:|is not registered).*");
    }

    // ---------------------------------------------------------------- 1: JEI

    private void jei(Client c) {
        List<String> pass = c.log(20_000, "arc furnace furnace pass");
        Expect.that(!pass.isEmpty(), "JEI ran the arc furnace furnace pass at join");
        String line = pass.get(pass.size() - 1);
        int n = Integer.parseInt(line.replaceAll(".*furnace pass: (\\d+) recipe.*", "$1"));
        Expect.that(n > 10, "the furnace pass lists the arc-smeltable furnace recipes: " + line);

        c.cmd("gamemode survival");
        c.cmd("clear");
        c.cmd("give @s hbm:machine_waste_drum 1");
        c.cmd("give @s hbm:machine_arc_furnace 1");
        c.waitTicks(4);
        uses(c, "hbm:machine_waste_drum", "10-jei-fuel-pool-drum");
        uses(c, "hbm:machine_arc_furnace", "11-jei-arc-furnace");
        c.cmd("clear");
        c.cmd("gamemode creative");
    }

    private static void uses(Client c, String item, String shot) {
        c.key("e");
        c.waitScreen("Inventory", 5_000L);
        c.waitTicks(10);
        JsonObject slot = c.slot(playerSlot(c, item));
        c.mouseMove(slot.get("center_x").getAsDouble(), slot.get("center_y").getAsDouble());
        c.waitTicks(5);
        c.key("u");
        c.waitTicks(20);
        String screen = c.screenClass();
        Expect.that(screen != null && screen.toLowerCase().contains("recipe"), "JEI opened " + item + "'s uses: " + screen);
        // the first tab lists block tags; the machine's own category (it is the crafting station) is the second
        JsonObject status = c.status();
        int w = status.get("gui_width").getAsInt(), h = status.get("gui_height").getAsInt();
        c.click(w / 2.0 - 61, h / 2.0 - 145);
        c.waitTicks(10);
        c.screenshot(shot);
        closeScreens(c);
    }

    // ---------------------------------------------------------------- 2: block break

    private void blockBreak(Client c) {
        int bx = X + 3, by = L, bz = Z;
        c.teleport(X + 0.5, L, Z + 0.5);
        c.waitTicks(5);
        hooks(c, "set_metal", 30);
        Fixtures.setBlock(c, bx, by, bz, "minecraft:dirt");
        c.lookAt(bx, by, bz);
        c.waitTicks(3);
        c.attack();
        c.waitTicks(5);
        JsonObject s = hooks(c).getAsJsonObject("server");
        Expect.block(c, bx, by, bz, "minecraft:air");
        Expect.that(s.get("effects").toString().contains("lead") && s.get("effects").toString().contains("x 3"),
                "breaking a block in 30 heavy metal: lead amplifier 2 (shown x 3): " + s.get("effects"));
        hooks(c, "set_metal", 0);

        // each air side gets coal dust 1 in 2: up to three ores, so a dust-free roll (1 in 32) does not fail the check
        int dust = 0;
        for (int attempt = 0; attempt < 3 && dust == 0; attempt++) {
            Fixtures.setBlock(c, bx, by + 1, bz, "minecraft:coal_ore");
            c.lookAt(bx, by + 1, bz);
            c.waitTicks(3);
            c.attack();
            c.waitTicks(5);
            for (int[] d : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
                JsonObject b = c.block(bx + d[0], by + 1 + d[1], bz + d[2]);
                if (b.toString().contains("hbm:gas_coal")) dust++;
            }
        }
        c.screenshot("20-coal-dust");
        Expect.that(dust > 0, "a broken coal ore puts coal dust into its air sides: " + dust);
        for (int[] d : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}})
            Fixtures.setBlock(c, bx + d[0], by + 1 + d[1], bz + d[2], "minecraft:air");
        c.cmd("effect clear @s");
    }

    // ---------------------------------------------------------------- 3: smog

    private void smog(Client c) {
        // same spot and view for both shots: the platform's edge, the hills beyond it
        c.teleport(X + 0.5, L, Z + 0.5);
        c.look(0, 0);
        c.waitTicks(20);
        c.screenshot("30-smog-before");
        // the region's soot diffuses and decays every 60 ticks (PollutionHandler), so it is held at 60 while the client eases
        hooks(c, "set_soot", 60);
        JsonObject client = null;
        for (int t = 0; t < 1400; t += 20) {
            c.waitTicks(20);
            client = hooks(c, "set_soot", 60).getAsJsonObject("client");
            if (client.get("render_soot").getAsFloat() >= 55F) break;
        }
        c.teleport(X + 0.5, L, Z + 0.5);
        c.look(0, 0);
        c.waitTicks(5);
        client = hooks(c, "set_soot", 60).getAsJsonObject("client");
        c.screenshot("31-smog-60-soot");
        float rd = client.get("render_distance_blocks").getAsFloat();
        float soot = client.get("render_soot").getAsFloat();
        float expected = rd / (1 + (soot - 35) * 5F / 120F);
        Expect.that(client.get("synced_soot").getAsFloat() >= 40F, "the region's soot reached the client: " + client);
        Expect.that(client.get("smog_active").getAsBoolean() && Math.abs(client.get("fog_far").getAsFloat() - expected) < 0.5F,
                "smog fog end " + expected + ": " + client);
        Expect.that(Math.abs(client.get("smog_tint").getAsFloat() - (soot - 35) / 120F) < 1e-3, "smog tint: " + client);
        hooks(c, "set_soot", 0);
        c.teleport(X + 0.5, L, Z + 0.5);
    }

    // ---------------------------------------------------------------- 4: duck

    private void duck(Client c) {
        // a snowy plains patch on the platform (/fillbiome): 26.1 would give a chicken spawned here the cold variant
        int dx = X + 6, dz = Z + 6;
        c.cmd("kill @e[type=hbm:entity_fucc_a_ducc]");
        c.cmd("fillbiome " + (dx - 4) + " " + (L - 4) + " " + (dz - 4) + " " + (dx + 4) + " " + (L + 12) + " " + (dz + 4)
                + " minecraft:snowy_plains");
        c.waitTicks(5);
        c.cmd("summon hbm:entity_fucc_a_ducc " + dx + " " + L + " " + dz);
        c.waitTicks(5);
        JsonObject r = hooks(c).getAsJsonObject("server");
        c.teleport(X + 0.5, L, Z + 0.5);
        c.lookAt(dx, L, dz);
        c.waitTicks(5);
        c.screenshot("40-duck-snowy-plains");
        boolean found = false;
        for (JsonElement e : r.getAsJsonArray("chickens")) {
            JsonObject ch = e.getAsJsonObject();
            if (!ch.get("type").getAsString().contains("ducc") || !ch.get("biome").getAsString().contains("snowy_plains")) continue;
            found = true;
            Expect.that(ch.get("plain_egg").getAsBoolean(), "a duck in the snowy plains lays the plain egg: " + ch);
        }
        Expect.that(found, "a duck stands in the snowy plains: " + r);
        c.cmd("kill @e[type=hbm:entity_fucc_a_ducc]");
    }

    // ---------------------------------------------------------------- 5: cyanide

    private void cyanide(Client c) {
        int ax = X - 4;
        Fixtures.placeOnTop(c, "hbm:anvil_iron", ax, Y, Z);
        Expect.block(c, ax, L, Z, "hbm:anvil_iron");
        c.cmd("gamemode survival");
        c.cmd("clear");
        Fixtures.give(c, "minecraft:golden_apple", 1);
        Fixtures.give(c, "hbm:plan_c", 1);
        c.teleport(ax + 2.5, L, Z + 0.5);
        c.waitTicks(5);
        Fixtures.interact(c, ax, L, Z);
        c.waitScreen("GUIAnvil", 5_000L);
        c.waitTicks(5);
        move(c, playerSlot(c, "minecraft:golden_apple"), 0);
        move(c, playerSlot(c, "hbm:plan_c"), 1);
        c.waitTicks(5);
        c.screenshot("50-anvil-cyanide");
        Expect.slotHolds(c, 2, "minecraft:golden_apple", 1);
        int hotbar = firstHotbarSlot(c);
        move(c, 2, hotbar);
        closeScreens(c);
        c.hotbar(1);
        c.look(0, -90); // at the sky, not the anvil: a right click on the anvil would open it again
        c.waitTicks(5);
        c.use(45);
        c.waitTicks(20);
        c.screenshot("51-cyanide-eaten");
        String screen = c.screenClass();
        Expect.that(screen != null && screen.contains("Death"), "eating the laced apple kills: screen " + screen);
        c.waitTicks(30);
        c.clickWidget("Respawn");
        c.waitScreen("none", 10_000L);
        c.waitTicks(10);
        c.cmd("gamemode creative");
        c.cmd("clear");
    }

    // ---------------------------------------------------------------- helpers

    private static void move(Client c, int from, int to) {
        c.clickSlot(from);
        c.waitTicks(2);
        c.clickSlot(to);
        c.waitTicks(3);
    }

    private static int playerSlot(Client c, String id) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && id.equals(s.get("id").getAsString())) {
                return s.get("index").getAsInt();
            }
        }
        throw new IllegalStateException("no player slot holding " + id);
    }

    /** The menu index of the first hotbar slot (the last nine player slots). */
    private static int firstHotbarSlot(Client c) {
        int last = -1;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean()) last = Math.max(last, s.get("index").getAsInt());
        }
        return last - 8;
    }

    private static void closeScreens(Client c) {
        for (int i = 0; i < 4 && c.screenClass() != null; i++) {
            c.key("escape");
            c.waitTicks(5);
        }
    }
}
