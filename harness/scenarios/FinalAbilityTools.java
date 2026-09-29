import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * The ability tool family (ModAbilityToolItems): every id given and shown in the inventory; preset
 * cycling by right-click with the info-HUD message checked by query; the vein miner, the 3x3x3 hammer,
 * silk touch, the auto-smelter and the auto-shredder mining prepared blocks in survival, with the removed
 * blocks and the collected drops checked; the preset editor opened with the ability-alt key; and the
 * steel sword's stun landing on a pig.
 */
public final class FinalAbilityTools implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;
    /** Eye level: nothing behind the targets, so a held attack cannot start on a second block. */
    private static final int T = Y + 2;

    static final String[] IDS = {"schrabidium_sword", "schrabidium_pickaxe", "schrabidium_axe", "schrabidium_shovel",
            "schrabidium_hoe", "titanium_sword", "titanium_pickaxe", "titanium_axe", "titanium_shovel", "titanium_hoe",
            "steel_sword", "steel_pickaxe", "steel_axe", "steel_shovel", "steel_hoe", "alloy_sword", "alloy_pickaxe",
            "alloy_axe", "alloy_shovel", "alloy_hoe", "cmb_sword", "cmb_pickaxe", "cmb_axe", "cmb_shovel", "cmb_hoe",
            "elec_sword", "elec_pickaxe", "elec_axe", "elec_shovel", "desh_sword", "desh_pickaxe", "desh_axe",
            "desh_shovel", "desh_hoe", "cobalt_sword", "cobalt_pickaxe", "cobalt_axe", "cobalt_shovel", "cobalt_hoe",
            "cobalt_decorated_sword", "cobalt_decorated_pickaxe", "cobalt_decorated_axe", "cobalt_decorated_shovel",
            "cobalt_decorated_hoe", "starmetal_sword", "starmetal_pickaxe", "starmetal_axe", "starmetal_shovel",
            "starmetal_hoe", "centri_stick", "smashing_hammer", "bismuth_pickaxe", "bismuth_axe", "volcanic_pickaxe",
            "volcanic_axe", "chlorophyte_pickaxe", "chlorophyte_axe", "mese_pickaxe", "mese_axe", "dnt_sword",
            "dwarven_pickaxe", "mese_gavel", "chainsaw", "redstone_sword", "crucible", "ingot_meteorite",
            "ingot_meteorite_forged", "blade_meteorite", "meteorite_sword", "meteorite_sword_seared",
            "meteorite_sword_reforged", "meteorite_sword_hardened", "meteorite_sword_alloyed",
            "meteorite_sword_machined", "meteorite_sword_treated", "meteorite_sword_etched", "meteorite_sword_bred",
            "meteorite_sword_irradiated", "meteorite_sword_fused", "meteorite_sword_baleful"};

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 12);
        c.cmd("gamemode survival");
        c.cmd("effect give @s minecraft:resistance 600 4 true");
        c.cmd("effect give @s minecraft:saturation 600 4 true");
        // The harness never grabs the mouse, so vanilla's held-attack mining never continues; Haste 101
        // makes every test block break on the first click (progress >= 1 in one tick), in survival.
        c.cmd("effect give @s minecraft:haste 600 100 true");
        c.waitTicks(2);

        inventoryPages(c);
        veinMiner(c);
        hammer(c);
        silk(c);
        smelter(c);
        shredder(c);
        editor(c);
        stun(c);

        c.cmd("gamemode creative");
        c.cmd("kill @e[type=!player]");
        c.waitTicks(5);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture|Missing model|Unable to play unknown soundEvent");
    }

    /** Every id in batches of 36, each batch shown in the survival inventory screen and checked by query. */
    private void inventoryPages(Client c) {
        for (int page = 0; page * 36 < IDS.length; page++) {
            c.cmd("clear");
            c.waitTicks(2);
            List<String> batch = new ArrayList<>();
            for (int i = page * 36; i < Math.min(IDS.length, (page + 1) * 36); i++) {
                c.cmd("give @s hbm:" + IDS[i] + " 1");
                batch.add("hbm:" + IDS[i]);
            }
            c.waitTicks(5);
            for (String id : batch) {
                Expect.that(count(c, id) == 1, id + " was given and is in the inventory");
            }
            c.key("e");
            c.waitTicks(10);
            c.screenshot("0" + page + "-inventory-page-" + page);
            c.key("escape");
            c.waitTicks(3);
        }
        c.cmd("clear");
        c.waitTicks(2);
    }

    private void stand(Client c) {
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(3);
    }

    /** Right-click into the air {@code times} times; returns the last info-HUD messages. */
    private List<String> cycle(Client c, int times) {
        List<String> last = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            c.look(0, -80);
            c.waitTicks(1);
            c.use(2);
            c.waitTicks(4);
            last = info(c);
        }
        return last;
    }

    private List<String> info(Client c) {
        JsonObject json = c.query("hbm_info", new JsonObject());
        List<String> out = new ArrayList<>();
        JsonArray messages = json.getAsJsonArray("messages");
        if (messages != null) for (JsonElement e : messages) out.add(e.getAsString());
        return out;
    }

    private void expectInfo(List<String> messages, String text) {
        Expect.that(messages.stream().anyMatch(m -> m.contains(text)),
                "info HUD shows \"" + text + "\", got " + messages);
    }

    private void mine(Client c, int x, int y, int z, int ticks) {
        c.lookAt(x + 0.5, y + 0.5, z + 0.01);
        c.waitTicks(2);
        c.attack(1);
        c.waitTicks(4);
    }

    private void collect(Client c, int x, int z) {
        c.teleport(x + 0.5, Y + 1, z + 0.5);
        c.waitTicks(30);
    }

    /** Steel pickaxe, vein miner (3): four coal ores in a line all go, four coal collected. */
    private void veinMiner(Client c) {
        stand(c);
        Fixtures.giveOnly(c, "hbm:steel_pickaxe", 1);
        expectInfo(cycle(c, 1), "[Enabled Vein Miner (3)");
        c.screenshot("10-vein-miner-enabled");
        for (int z = 3; z <= 6; z++) Fixtures.setBlock(c, X, T, z, "minecraft:coal_ore");
        stand(c);
        mine(c, X, T, 3, 16);
        for (int z = 3; z <= 6; z++) Expect.block(c, X, T, z, "minecraft:air");
        collect(c, X, 3);
        Expect.that(count(c, "minecraft:coal") == 4, "vein miner yields 4 coal, got " + count(c, "minecraft:coal"));
        c.screenshot("11-vein-miner-result");
    }

    /** Desh pickaxe, AoE (1): the 3x3x3 around the mined front-centre block clears two layers of a 3x3x3 cube. */
    private void hammer(Client c) {
        c.cmd("clear");
        stand(c);
        Fixtures.giveOnly(c, "hbm:desh_pickaxe", 1);
        expectInfo(cycle(c, 2), "[Enabled AoE (1)");
        for (int x = -1; x <= 1; x++)
            for (int y = T - 1; y <= T + 1; y++)
                for (int z = 3; z <= 5; z++) Fixtures.setBlock(c, x, y, z, "minecraft:stone");
        stand(c);
        c.lookAt(X + 0.5, T + 0.5, 3.01);
        c.waitTicks(2);
        c.screenshot("20-hammer-before-crosshair-icon");
        mine(c, X, T, 3, 9);
        int cleared = 0;
        for (int x = -1; x <= 1; x++)
            for (int y = T - 1; y <= T + 1; y++)
                for (int z = 3; z <= 4; z++) if ("minecraft:air".equals(c.block(x, y, z).get("block").getAsString())) cleared++;
        Expect.that(cleared == 18, "AoE (1) clears the 3x3x2 in front of the back layer, cleared " + cleared);
        int back = 0;
        for (int x = -1; x <= 1; x++)
            for (int y = T - 1; y <= T + 1; y++) if ("minecraft:stone".equals(c.block(x, y, 5).get("block").getAsString())) back++;
        Expect.that(back == 9, "the back layer outside the range stays, " + back + " of 9");
        c.screenshot("21-hammer-result");

        // Sneak-click goes back to the first preset.
        stand(c);
        c.look(0, -80);
        c.keyDown("key.sneak");
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(4);
        c.keyUp("key.sneak");
        expectInfo(info(c), "[Tool ability deactivated]");
        c.screenshot("22-sneak-reset");
        c.cmd("fill -1 " + (T - 1) + " 3 1 " + (T + 1) + " 5 minecraft:air");
        c.cmd("kill @e[type=item]");
    }

    /** Cobalt pickaxe, silk touch: stone drops stone, not cobblestone. */
    private void silk(Client c) {
        c.cmd("clear");
        stand(c);
        Fixtures.giveOnly(c, "hbm:cobalt_pickaxe", 1);
        expectInfo(cycle(c, 2), "[Enabled Silk Touch");
        Fixtures.setBlock(c, X, T, 3, "minecraft:stone");
        stand(c);
        mine(c, X, T, 3, 8);
        Expect.block(c, X, T, 3, "minecraft:air");
        collect(c, X, 3);
        Expect.that(count(c, "minecraft:stone") == 1 && count(c, "minecraft:cobblestone") == 0,
                "silk touch yields stone, got stone " + count(c, "minecraft:stone") + " cobble " + count(c, "minecraft:cobblestone"));
        c.screenshot("30-silk-result");
    }

    /** CMB pickaxe, auto-smelter: iron ore yields an iron ingot, not raw iron. */
    private void smelter(Client c) {
        c.cmd("clear");
        stand(c);
        Fixtures.giveOnly(c, "hbm:cmb_pickaxe", 1);
        expectInfo(cycle(c, 4), "[Enabled Auto-Smelter");
        Fixtures.setBlock(c, X, T, 3, "minecraft:iron_ore");
        stand(c);
        mine(c, X, T, 3, 6);
        Expect.block(c, X, T, 3, "minecraft:air");
        collect(c, X, 3);
        Expect.that(count(c, "minecraft:iron_ingot") == 1 && count(c, "minecraft:raw_iron") == 0,
                "auto-smelter yields an iron ingot, got ingot " + count(c, "minecraft:iron_ingot") + " raw " + count(c, "minecraft:raw_iron"));
        c.screenshot("40-smelter-result");
    }

    /** Smashing hammer, auto-shredder: stone shreds to gravel. */
    private void shredder(Client c) {
        c.cmd("clear");
        stand(c);
        Fixtures.giveOnly(c, "hbm:smashing_hammer", 1);
        expectInfo(cycle(c, 1), "[Enabled Auto-Shredder");
        Fixtures.setBlock(c, X, T, 3, "minecraft:stone");
        stand(c);
        mine(c, X, T, 3, 9);
        Expect.block(c, X, T, 3, "minecraft:air");
        collect(c, X, 3);
        Expect.that(count(c, "minecraft:gravel") == 1 && count(c, "minecraft:cobblestone") == 0,
                "auto-shredder yields gravel, got gravel " + count(c, "minecraft:gravel"));
        c.screenshot("50-shredder-result");
    }

    /** The ability-alt key opens the preset editor for the held tool. */
    private void editor(Client c) {
        c.cmd("clear");
        stand(c);
        Fixtures.giveOnly(c, "hbm:schrabidium_pickaxe", 1);
        c.look(0, 0);
        c.key("key.hbm.abilityAlt");
        c.waitScreen("GUIScreenToolAbility", 5000);
        c.waitTicks(5);
        c.screenshot("60-ability-editor");
        c.key("escape");
        c.waitTicks(5);
    }

    /** Steel sword, stunning (2): one hit leaves the pig slowed (the harness world is peaceful). */
    private void stun(Client c) {
        c.cmd("clear");
        stand(c);
        Fixtures.giveOnly(c, "hbm:steel_sword", 1);
        c.cmd("summon minecraft:pig 0.5 " + (Y + 1) + " 2.5 {NoAI:1b,PersistenceRequired:1b}");
        c.waitTicks(5);
        c.lookAt(0.5, Y + 1.5, 2.5);
        c.waitTicks(2);
        c.attack(1);
        c.waitTicks(5);
        c.screenshot("70-stun-hit");
        String effects = c.node().startsWith("1.20.") ? "ActiveEffects" : "active_effects";
        c.cmd("data get entity @e[type=minecraft:pig,limit=1,sort=nearest] " + effects);
        c.waitTicks(5);
        // The saved form above is only a transcript aid: 1.20.1 saves a numeric Id, and only Forge adds a named
        // "forge:id". The check reads the pig's live effects on the server, where every node names them alike.
        JsonObject req = new JsonObject();
        req.addProperty("radius", 16);
        req.addProperty("type", "minecraft:pig");
        req.addProperty("server", true);
        JsonObject res = c.query("entities", req);
        JsonArray pigs = res.getAsJsonArray("entities");
        Expect.that(pigs != null && pigs.size() == 1 && pigs.get(0).getAsJsonObject().has("effects"),
                "one pig on the server, with its effects: " + res);
        boolean slowed = false;
        for (JsonElement e : pigs.get(0).getAsJsonObject().getAsJsonArray("effects")) {
            JsonObject fx = e.getAsJsonObject();
            // 1.7 IWeaponAbility stun: slowness amplifier 4 (and weakness) for the ability's duration
            if (fx.get("id").getAsString().endsWith(".slowness") && fx.get("amplifier").getAsInt() == 4
                    && fx.get("duration").getAsInt() > 0) slowed = true;
        }
        Expect.that(slowed, "the stun hit left the pig with slowness V: " + pigs);
    }

    private static int count(Client c, String itemId) {
        JsonObject inventory = c.inventory();
        int count = 0;
        for (var e : inventory.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.has("id") && itemId.equals(s.get("id").getAsString())) count += s.get("count").getAsInt();
        }
        return count;
    }
}
