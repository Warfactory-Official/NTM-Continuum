import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 156, by real clicks: the multi identifier's picker shows sulfuric acid once (a click on a second
 * result picks nothing) and, with Mekanism, Mekanism's brine once, the creative search shows no universal bucket, the 1.7 sulfuric acid bucket empties into a
 * sulfuric acid barrel, and an ethanol barrel fills no bucket (1.7 had no ethanol bucket).
 */
public final class Gh156Fluids implements Scenario {

    private static final int Y = 100, G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(180_000L);
        c.markLog();
        closeScreens(c);
        Fixtures.platform(c, 0, Y, 0, 8);
        c.cmd("gamemode creative");
        c.cmd("fill -6 " + G + " -6 6 " + (G + 3) + " 6 minecraft:air");
        c.waitTicks(5);

        // the identifier's picker: one Sulfuric Acid, nothing behind it
        Fixtures.giveOnly(c, "hbm:fluid_identifier_multi", 1);
        c.hotbar(1);
        String held = pick(c, "sulfuric", "01-picker-sulfuric");
        Expect.that(held.contains("hbm:sulfuric_acid") && !held.contains("sulfuric_acid_fluid"),
                "the second picker result chose a placed fluid: " + held);

        // with Mekanism (-Pmekanism.run=true): its brine, which HBM has no fluid for, is picked once
        c.cmd("clear @s");
        c.cmd("give @s mekanism:brine_bucket");
        c.waitTicks(5);
        boolean mekanism = c.inventory().toString().contains("mekanism:brine_bucket");
        System.out.println("[Gh156] Mekanism in this run: " + mekanism);
        if (mekanism) {
            Fixtures.giveOnly(c, "hbm:fluid_identifier_multi", 1);
            c.hotbar(1);
            held = pick(c, "brine", "01b-picker-brine");
            Expect.that(held.contains("primary: \"mekanism:brine\""), "the picker did not offer Mekanism's brine once: " + held);
        }

        // the creative search: no universal bucket for any fluid
        c.key("e");
        c.waitTicks(20);
        JsonObject inv = c.screen();
        c.click(inv.get("left").getAsInt() + inv.get("image_width").getAsInt() - 14, inv.get("top").getAsInt() - 14);
        c.waitTicks(10);
        String found = search(c, "Ethanol");
        c.screenshot("02-creative-ethanol");
        Expect.that(!found.contains("hbm:fluid_bucket,"), "the creative search lists a universal bucket: " + found);
        c.key("escape");
        c.waitTicks(10);

        // 1.7 FluidContainerRegistry: the sulfuric acid bucket into a sulfuric acid barrel
        c.cmd("setblock 0 " + G + " 2 hbm:barrel_steel{tank:0,tank_type:\"hbm:sulfuric_acid\",mode:0s}");
        c.cmd("setblock 2 " + G + " 2 hbm:barrel_steel{tank:5000,tank_type:\"hbm:ethanol\",mode:0s}");
        c.waitTicks(5);
        Fixtures.giveOnly(c, "hbm:bucket_sulfuric_acid", 1);
        placeInSlot(c, 0, 2, "hbm:bucket_sulfuric_acid", 2);
        c.waitTicks(20);
        c.screenshot("03-sulfuric-barrel");
        c.key("escape");
        c.waitTicks(10);
        String acid = c.blockEntity(0, G, 2).toString().replaceAll("\\s+", "");
        System.out.println("[Gh156] sulfuric barrel: " + acid);
        Expect.that(acid.contains("tank:1000") || acid.contains("\"tank\":1000"), "the sulfuric acid bucket did not empty: " + acid);

        Fixtures.giveOnly(c, "minecraft:bucket", 1);
        placeInSlot(c, 2, 2, "minecraft:bucket", 4);
        c.waitTicks(20);
        JsonObject out = c.screen();
        c.screenshot("04-ethanol-barrel");
        String slots = out.getAsJsonArray("slots").toString();
        c.key("escape");
        c.waitTicks(10);
        String ethanol = c.blockEntity(2, G, 2).toString().replaceAll("\\s+", "");
        System.out.println("[Gh156] ethanol barrel: " + ethanol);
        Expect.that(!slots.contains("hbm:fluid_bucket") && (ethanol.contains("tank:5000") || ethanol.contains("\"tank\":5000")),
                "the ethanol barrel filled a bucket: " + ethanol);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]].*(hbm|golem)");
    }

    /** Opens the picker, searches {@code text}, left-clicks the first and the second result; the identifier's data. */
    private static String pick(Client c, String text, String shot) {
        c.keyDown("key.sneak");
        c.waitTicks(3);
        c.use(1);
        c.waitTicks(3);
        c.keyUp("key.sneak");
        c.waitScreen("Fluid", 5000);
        // the search line sits at guiLeft + 46, guiTop + 11 (GUIScreenFluid)
        JsonObject box = c.screen().getAsJsonArray("widgets").get(0).getAsJsonObject();
        int left = box.get("x").getAsInt() - 46, top = box.get("y").getAsInt() - 11;
        c.type(text);
        c.waitTicks(5);
        c.screenshot(shot);
        c.click(left + 7 + 9, top + 29 + 9);
        c.waitTicks(5);
        c.click(left + 7 + 18 + 9, top + 29 + 9);
        c.waitTicks(10);
        c.key("escape");
        c.waitTicks(10);
        c.markLog();
        c.cmd("data get entity @s SelectedItem");
        c.waitTicks(10);
        String held = String.join(" ", c.logSinceMark("fluid_identifier_multi"));
        System.out.println("[Gh156] identifier after picking " + text + ": " + held);
        return held;
    }

    private static void closeScreens(Client c) {
        while (c.screen().get("open").getAsBoolean()) {
            c.key("escape");
            c.waitTicks(5);
        }
    }

    /** Opens the barrel at x, G, z and carries the hotbar's {@code id} into machine slot {@code slot} by two clicks. */
    private static void placeInSlot(Client c, int x, int z, String id, int slot) {
        c.teleport(x + 0.5, G, z - 1.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, G + 0.5, z + 0.5);
        c.waitTicks(2);
        Fixtures.interact(c, x, G, z);
        c.waitScreen("Barrel", 5000);
        int from = -1;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.has("id") && s.get("id").getAsString().equals(id) && s.get("player_inventory").getAsBoolean()) {
                from = s.get("index").getAsInt();
            }
        }
        Expect.that(from >= 0, "no " + id + " in the inventory: " + c.screen());
        c.clickSlot(from);
        c.waitTicks(3);
        c.clickSlot(slot);
        c.waitTicks(3);
    }

    /** The ids in the search grid for {@code text}, each followed by a comma. */
    private static String search(Client c, String text) {
        for (int i = 0; i < 34; i++) c.key("backspace");
        c.type(text);
        c.waitTicks(8);
        StringBuilder ids = new StringBuilder();
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.has("id") && !s.get("player_inventory").getAsBoolean()) ids.append(s.get("id").getAsString()).append(',');
        }
        return ids.toString();
    }
}
