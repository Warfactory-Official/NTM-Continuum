import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * In-game acceptance for the utility/diagnostic tools and the books: every id in the inventory; the ore density
 * scanner, pollution detector and Coltass on the info HUD; the analysis tool on a fluid duct; the power net tool
 * on a cable run; the multi identifier setting a duct and its picker screen; the settings tool copying one duct's
 * fluid onto another (checked in block-entity NBT); a pipette filled and emptied through a barrel's container
 * slots; the containment box storing uranium and damping its radiation; the pager's channel screen; the
 * digamma diagnostic, survey scanner and meteorite remote; the Polaroid's resistance below 10 health; every book
 * screen (RBMK and starter guides, a lore book, a holotape, a clay tablet) paged by real clicks; the Lemegeton
 * converting an iron ingot; and the legacy manual block placed by hand with its look overlay.
 */
public final class FinalUtilityTools implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    static final String[] IDS = {"hbm:analysis_tool", "hbm:coltan_tool", "hbm:containment_box",
            "hbm:digamma_diagnostic", "hbm:meltdown_tool", "hbm:fluid_identifier_multi", "hbm:meteor_remote",
            "hbm:ore_density_scanner", "hbm:survey_scanner", "hbm:pollution_detector", "hbm:power_net_tool",
            "hbm:settings_tool", "hbm:rtty_pager", "hbm:polaroid", "hbm:pipette", "hbm:pipette_boron",
            "hbm:pipette_laboratory", "hbm:book_guide", "hbm:book_lore", "hbm:holotape_image", "hbm:clay_tablet",
            "hbm:book_lemegeton", "hbm:book_guide_block"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 20);
        c.cmd("gamerule sendCommandFeedback false");

        inventory(c);
        hudTools(c);
        ductTools(c);
        pipetteThroughBarrel(c);
        containmentBox(c);
        pager(c);
        chatTools(c);
        polaroid(c);
        books(c);
        lemegeton(c);
        guideBlock(c);

        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]].*(hbm|golem|Exception)");
    }

    // ------------------------------------------------------------------------------------------------------

    private static void inventory(Client c) {
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.waitTicks(2);
        for (String id : IDS) c.cmd("give @s " + id + " 1");
        c.waitTicks(4);
        JsonObject inv = c.inventory();
        for (String id : IDS) Expect.that(inv.toString().contains("\"" + id + "\""), "inventory holds " + id);
        c.key("key.inventory");
        c.waitScreen("", 5000);
        c.waitTicks(10);
        c.screenshot("01-inventory");
        close(c);

        // the variant stacks: two guide books, a lore book, the restored holotape, the second clay tablet,
        // a multi identifier set to water/lava, all given through the server. Creative: the restored holotape is
        // a digamma source, and a survival carrier would be dying of it by the end of the scenario.
        c.cmd("gamemode creative");
        c.cmd("clear");
        util(c, "guide:1");
        util(c, "guide:3");
        util(c, "lore:beacon");
        util(c, "lore:book_syringe");
        util(c, "holotape:1");
        util(c, "tablet:1");
        util(c, "fluid_multi:WATER,LAVA");
        c.cmd("give @s hbm:pipette 1");
        c.cmd("give @s hbm:pipette_laboratory 1");
        c.waitTicks(20); // lore colours and the identifier tint are stored on the first ticks
        c.key("key.inventory");
        c.waitScreen("", 5000);
        c.waitTicks(10);
        c.screenshot("02-inventory-variants");
        close(c);
        c.cmd("clear");
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }

    private static void hudTools(Client c) {
        Fixtures.giveOnly(c, "hbm:ore_density_scanner", 1);
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(30);
        String info = info(c);
        Expect.that(info.contains("Tier"), "ore density scanner tier line on the HUD: " + info);
        Expect.that(info.contains("Light Metal") || info.contains("light"), "ore density type line: " + info);
        c.screenshot("10-hud-ore-density");

        Fixtures.giveOnly(c, "hbm:pollution_detector", 1);
        c.waitTicks(30);
        info = info(c);
        Expect.that(info.contains("Soot") && info.contains("Heavy metal"), "pollution lines on the HUD: " + info);
        c.screenshot("11-hud-pollution");

        Fixtures.giveOnly(c, "hbm:coltan_tool", 1);
        c.waitTicks(40);
        JsonObject held = util(c, null);
        Expect.that(held.has("coltass"), "the Coltass stored its deposit: " + held);
        info = info(c);
        Expect.that(info.matches("(?s).*\\d+m.*"), "Coltass distance on the HUD: " + info);
        c.look(0, 30);
        c.waitTicks(10);
        c.screenshot("12-hud-coltass-south");
        c.look(180, 30);
        c.waitTicks(30);
        c.screenshot("13-hud-coltass-north");
    }

    private static void ductTools(Client c) {
        int ax = X + 4, bx = X + 8, dz = Z + 4;
        for (int i = 0; i < 3; i++) Fixtures.setBlock(c, ax, Y + 1, dz + i, "hbm:fluid_duct_neo");
        Fixtures.setBlock(c, bx, Y + 1, dz, "hbm:fluid_duct_neo");
        for (int i = 0; i < 3; i++) Fixtures.setBlock(c, X - 4, Y + 1, dz + i, "hbm:red_cable");

        // the multi identifier types duct A water (plain click on the duct)
        c.cmd("clear");
        util(c, "fluid_multi:WATER,LAVA");
        c.hotbar(1);
        clickBlock(c, ax, dz, false);
        String a = c.blockEntity(ax, Y + 1, dz).toString();
        Expect.that(!a.contains("type:0") && !a.contains("\"type\":0"), "duct A was typed by the identifier: " + a);

        // its picker: sneak-use in the air, search "wat", pick the first swatch as primary
        c.teleport(X + 0.5, Y + 1, Z - 6.5);
        c.waitTicks(2);
        c.look(0, -60);
        c.keyDown("key.sneak");
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(3);
        c.keyUp("key.sneak");
        c.waitScreen("GUIScreenFluid", 5000);
        c.type("wat");
        c.waitTicks(5);
        int[] g = gui(c, 176, 54);
        c.click(g[0] + 16, g[1] + 38);
        c.waitTicks(5);
        c.mouseMove(g[0] + 16, g[1] + 38);
        c.waitTicks(3);
        c.screenshot("20-gui-fluid-picker");
        close(c);

        // analysis tool on duct A
        c.markLog();
        Fixtures.giveOnly(c, "hbm:analysis_tool", 1);
        clickBlock(c, ax, dz + 1, false);
        c.waitTicks(5);
        Expect.that(!c.logSinceMark("Links: ").isEmpty(), "analysis tool printed the duct net");
        Expect.that(!c.logSinceMark("Transfer: ").isEmpty(), "analysis tool printed the transfer line");

        // power net tool on the cable run
        Fixtures.giveOnly(c, "hbm:power_net_tool", 1);
        clickBlock(c, X - 4, dz + 1, false);
        c.waitTicks(5);
        Expect.that(!c.logSinceMark("Start of diagnostic for network").isEmpty(), "power net diagnostic header");
        Expect.that(!c.logSinceMark("Links: 3").isEmpty(), "power net counted three cables: "
                + c.logSinceMark("Links"));

        // settings tool: sneak-copy duct A, paste on duct B
        Fixtures.giveOnly(c, "hbm:settings_tool", 1);
        c.teleport(ax + 0.5, Y + 1, dz - 2.5);
        c.waitTicks(3);
        c.lookAt(ax + 0.5, Y + 1.5, dz + 0.5);
        c.keyDown("key.sneak");
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(3);
        c.keyUp("key.sneak");
        c.waitTicks(8);
        JsonObject tool = util(c, null);
        Expect.that(tool.has("settings") && tool.get("settings").getAsString().contains("fluidID"),
                "the settings tool copied the duct's fluid: " + tool);
        c.screenshot("21-hud-settings-copied");
        clickBlock(c, bx, dz, false);
        c.waitTicks(5);
        String b = c.blockEntity(bx, Y + 1, dz).toString();
        a = c.blockEntity(ax, Y + 1, dz).toString();
        Expect.that(typeOf(a) != 0 && typeOf(a) == typeOf(b), "duct B took duct A's fluid: A " + a + " / B " + b);
        worldShot(c, "22-world-ducts", ax - 3.5, Y + 4, dz - 4.5, ax + 2, Y + 1, dz + 1);
    }

    private static void pipetteThroughBarrel(Client c) {
        int px = X + 12, pz = Z + 4;
        Fixtures.setBlock(c, px, Y + 1, pz, "hbm:barrel_steel");
        // survival: creative inventory sync would echo the client's stack copies back over the server's
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.cmd("give @s minecraft:water_bucket 1");
        c.cmd("give @s hbm:pipette 1");
        c.waitTicks(3);
        c.teleport(px + 2.5, Y + 1, pz + 0.5);
        c.waitTicks(3);
        Fixtures.interact(c, px, Y + 1, pz);
        c.waitScreen("Barrel", 5000);
        clickFromInventory(c, "minecraft:water_bucket", 2);
        c.waitTicks(10);
        // the emptied bucket leaves the output slot, which the pipette's return trip will need
        c.clickSlot(3);
        c.waitTicks(1);
        c.clickSlot(32);
        c.waitTicks(2);
        clickFromInventory(c, "hbm:pipette", 4);
        c.waitTicks(10);
        c.screenshot("30-gui-barrel-pipette-filled");
        JsonObject s5 = c.slot(5);
        Expect.that(s5.toString().contains("hbm:pipette"), "the filled pipette came out in slot 5: " + s5);
        // back to the hotbar's first slot (menu index 6 + 27)
        c.clickSlot(5);
        c.waitTicks(1);
        c.clickSlot(33);
        c.waitTicks(2);
        close(c);
        c.hotbar(1);
        c.waitTicks(3);
        JsonObject pipette = util(c, null);
        Expect.that(pipette.has("pipette") && !emptyPipette(pipette),
                "the pipette holds water: " + pipette);
        c.screenshot("31-hand-pipette-filled");

        // and back into the barrel
        Fixtures.interact(c, px, Y + 1, pz);
        c.waitScreen("Barrel", 5000);
        clickFromInventory(c, "hbm:pipette", 2);
        c.waitTicks(10);
        c.screenshot("32-gui-barrel-pipette-emptied");
        if (c.slot(3).toString().contains("hbm:pipette")) {
            c.clickSlot(3);
            c.waitTicks(1);
            c.clickSlot(33);
            c.waitTicks(2);
        }
        close(c);
        selectHotbarWith(c, "hbm:pipette");
        pipette = util(c, null);
        Expect.that(pipette.has("pipette") && emptyPipette(pipette), "the pipette emptied into the barrel: " + pipette);

        // capacity step: empty pipette, right-click in the air
        c.markLog();
        c.look(0, -60);
        c.use(2);
        c.waitTicks(4);
        Expect.that(!c.logSinceMark("mB").isEmpty(), "the pipette reported its new capacity");
        c.cmd("gamemode creative");
    }

    private static void containmentBox(Client c) {
        c.cmd("clear");
        c.cmd("give @s hbm:containment_box 1");
        c.cmd("give @s hbm:ingot_uranium 1");
        c.waitTicks(3);
        c.hotbar(2);
        c.waitTicks(5);
        double ingot = util(c, null).get("radiation").getAsDouble();
        c.hotbar(1);
        c.look(0, -60);
        c.waitTicks(1);
        c.use(2);
        c.waitScreen("Containment Box", 5000);
        Expect.screenStaysOpen(c, "Containment Box", 10);
        clickFromInventory(c, "hbm:ingot_uranium", 0);
        c.waitTicks(5);
        c.screenshot("40-gui-containment-box");
        close(c);
        c.waitTicks(3);
        JsonObject box = util(c, null);
        Expect.that(box.get("box_filled").getAsInt() == 1, "the box holds the ingot: " + box);
        double boxed = box.get("radiation").getAsDouble();
        Expect.that(ingot > 0 && boxed > 0 && boxed < ingot,
                "the box damps the ingot's radiation (squirt): ingot " + ingot + ", boxed " + boxed);
    }

    private static void pager(Client c) {
        Fixtures.giveOnly(c, "hbm:rtty_pager", 1);
        c.look(0, -60);
        c.waitTicks(1);
        c.use(2);
        c.waitScreen("GUIScreenPager", 5000);
        int[] g = gui(c, 184, 42);
        c.click(g[0] + 60, g[1] + 28);
        c.waitTicks(2);
        c.type("tools");
        c.waitTicks(2);
        c.click(g[0] + 146, g[1] + 26);
        c.waitTicks(5);
        c.screenshot("50-gui-pager");
        close(c);
        JsonObject pager = util(c, null);
        Expect.that(pager.has("pager") && pager.get("pager").getAsString().equals("tools"),
                "the pager listens on the typed channel: " + pager);
    }

    private static void chatTools(Client c) {
        c.markLog();
        Fixtures.giveOnly(c, "hbm:digamma_diagnostic", 1);
        c.look(0, -60);
        c.use(2);
        c.waitTicks(5);
        Expect.that(!c.logSinceMark("=====").isEmpty(), "the digamma diagnostic printed its report");
        c.screenshot("60-chat-digamma");

        Fixtures.giveOnly(c, "hbm:survey_scanner", 1);
        c.use(2);
        c.waitTicks(5);

        Fixtures.giveOnly(c, "hbm:meltdown_tool", 1);
        c.use(2);
        c.waitTicks(3);

        Fixtures.giveOnly(c, "hbm:meteor_remote", 1);
        c.use(2);
        c.waitTicks(5);
        Expect.that(!c.logSinceMark("Watch your head!").isEmpty(), "the meteorite remote called one down");
        c.cmd("kill @e[type=hbm:entity_meteor]");
        c.waitTicks(3);
    }

    private static void polaroid(Client c) {
        c.cmd("gamemode survival");
        Fixtures.giveOnly(c, "hbm:polaroid", 1);
        c.cmd("effect clear @s");
        c.cmd("damage @s 12");
        c.waitTicks(10);
        JsonObject armor = c.query("hbm_armor", new JsonObject());
        Expect.that(armor.toString().contains("resistance"), "the Polaroid holds resistance below 10 health: " + armor);
        c.cmd("effect give @s minecraft:instant_health 1 5");
        c.cmd("gamemode creative");
        c.waitTicks(3);
    }

    private static void books(Client c) {
        // RBMK guide: cover, first spread, next spread
        c.cmd("clear");
        util(c, "guide:1");
        c.hotbar(1);
        c.look(0, -60);
        c.use(2);
        c.waitScreen("GUIScreenGuide", 5000);
        c.waitTicks(5);
        c.screenshot("70-gui-guide-rbmk-cover");
        int[] g = gui(c, 272, 182);
        c.click(g[0] + 136, g[1] + 90);
        c.waitTicks(3);
        c.screenshot("71-gui-guide-rbmk-page1");
        c.click(g[0] + 239, g[1] + 160);
        c.waitTicks(3);
        c.screenshot("72-gui-guide-rbmk-page2");
        close(c);

        c.cmd("clear");
        util(c, "guide:3");
        c.hotbar(1);
        c.use(2);
        c.waitScreen("GUIScreenGuide", 5000);
        c.waitTicks(5);
        c.screenshot("73-gui-guide-starter-cover");
        c.click(g[0] + 136, g[1] + 90);
        c.waitTicks(3);
        c.click(g[0] + 239, g[1] + 160);
        c.waitTicks(3);
        c.screenshot("74-gui-guide-starter-page2");
        close(c);

        // lore book: the beacon booklet, then an MKU note carrying its slot argument
        c.cmd("clear");
        util(c, "lore:beacon");
        c.hotbar(1);
        c.use(2);
        c.waitScreen("GUIBookLore", 5000);
        c.waitTicks(5);
        c.screenshot("75-gui-lore-beacon");
        c.click(g[0] + 239, g[1] + 160);
        c.waitTicks(3);
        c.screenshot("76-gui-lore-beacon-page2");
        close(c);
        c.cmd("clear");
        util(c, "lore:book_syringe");
        c.hotbar(1);
        c.use(2);
        c.waitScreen("GUIBookLore", 5000);
        c.waitTicks(5);
        c.screenshot("77-gui-lore-syringe");
        close(c);

        // holotape
        c.cmd("clear");
        util(c, "holotape:1");
        c.hotbar(1);
        c.use(2);
        c.waitScreen("GUIScreenHolotape", 5000);
        c.waitTicks(5);
        c.screenshot("78-gui-holotape");
        close(c);
        JsonObject tape = util(c, null);
        Expect.that(tape.get("digamma_hazard").getAsDouble() > 0, "the restored holotape carries digamma: " + tape);

        // clay tablet: the first read draws its seed
        c.cmd("clear");
        util(c, "tablet:1");
        c.hotbar(1);
        c.use(2);
        c.waitScreen("GUIScreenClayTablet", 5000);
        c.waitTicks(10);
        c.screenshot("79-gui-clay-tablet");
        close(c);
        Expect.that(util(c, null).has("tablet_seed"), "the clay tablet drew its seed");
    }

    private static void lemegeton(Client c) {
        c.cmd("clear");
        c.cmd("give @s hbm:book_lemegeton 1");
        c.cmd("give @s minecraft:iron_ingot 4");
        c.waitTicks(3);
        c.hotbar(1);
        c.use(2);
        c.waitScreen("Lemegeton", 5000);
        Expect.screenStaysOpen(c, "Lemegeton", 10);
        clickFromInventory(c, "minecraft:iron_ingot", 1);
        c.waitTicks(5);
        c.screenshot("80-gui-lemegeton");
        Expect.that(c.slot(0).toString().contains("hbm:ingot_steel"), "iron converts to steel: " + c.slot(0));
        close(c);
    }

    private static void guideBlock(Client c) {
        int gx = X - 8, gz = Z - 6;
        c.cmd("gamemode creative");
        Fixtures.placeOnTop(c, "hbm:book_guide_block", gx, Y, gz);
        Expect.block(c, gx, Y + 1, gz, "hbm:book_guide_block");
        c.hotbar(9);
        c.teleport(gx + 2.5, Y + 1, gz + 0.5);
        c.waitTicks(3);
        c.lookAt(gx + 0.5, Y + 1.5, gz + 0.5);
        c.waitTicks(5);
        c.screenshot("90-world-guide-block-overlay");
        worldShot(c, "91-world-guide-block", gx - 2.5, Y + 3, gz + 2.5, gx + 0.5, Y + 1.5, gz + 0.5);
    }

    // ------------------------------------------------------------------------------------------------------

    private static boolean emptyPipette(JsonObject held) {
        return held.get("pipette").getAsString().matches(".*fill:0s?[,}].*");
    }

    private static int typeOf(String nbt) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("type\"?:\\s*(-?\\d+)").matcher(nbt);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static JsonObject util(Client c, String give) {
        JsonObject params = new JsonObject();
        if (give != null) params.addProperty("give", give);
        JsonObject result = c.query("hbm_utility", params);
        if (give != null) {
            Expect.that(result.has("given") && result.get("given").getAsBoolean(), "given " + give + ": " + result);
            c.waitTicks(2);
        }
        return result;
    }

    /** The info HUD, polled for up to five seconds until it shows anything. */
    private static String info(Client c) {
        String info = "";
        for (int i = 0; i < 25; i++) {
            info = c.query("hbm_info", new JsonObject()).toString();
            if (!info.contains("\"messages\":[]")) break;
            c.waitTicks(4);
        }
        return info;
    }

    private static int[] gui(Client c, int w, int h) {
        JsonObject status = c.status();
        int gw = status.get("gui_width").getAsInt(), gh = status.get("gui_height").getAsInt();
        return new int[]{(gw - w) / 2, (gh - h) / 2};
    }

    /** Aims at the side of a block on the platform from two blocks south and right-clicks it. */
    private static void clickBlock(Client c, int x, int z, boolean sneak) {
        c.teleport(x + 0.5, Y + 1, z - 2.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, Y + 1.5, z + 0.5);
        c.waitTicks(1);
        if (sneak) c.keyDown("key.sneak");
        c.use(2);
        c.waitTicks(4);
        if (sneak) c.keyUp("key.sneak");
        c.waitTicks(2);
    }

    /** Selects the hotbar slot holding {@code id}, wherever the container put it back. */
    private static void selectHotbarWith(Client c, String id) {
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (id.equals(s.get("id").getAsString()) && s.get("slot").getAsInt() < 9) {
                c.hotbar(s.get("slot").getAsInt() + 1);
                c.waitTicks(3);
                return;
            }
        }
        Expect.that(false, id + " is on the hotbar: " + c.inventory());
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitTicks(5);
    }

    private static void clickFromInventory(Client c, String id, int machineSlot) {
        JsonObject slot = findSlot(c, id);
        Expect.that(slot != null, "inventory slot holding " + id);
        c.clickSlot(slot.get("index").getAsInt());
        c.waitTicks(1);
        c.clickSlot(machineSlot);
        c.waitTicks(1);
        JsonObject carried = c.screen().getAsJsonObject("carried");
        if (carried != null && carried.has("id") && !carried.get("id").getAsString().contains("air")) {
            c.clickSlot(slot.get("index").getAsInt());
            c.waitTicks(1);
        }
    }

    private static JsonObject findSlot(Client c, String id) {
        JsonObject screen = c.screen();
        if (!screen.has("slots")) return null;
        for (JsonElement e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && id.equals(s.get("id").getAsString())) return s;
        }
        return null;
    }

    private static void worldShot(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        c.hotbar(9);
        c.waitTicks(1);
        c.cmd("gamemode spectator");
        c.waitTicks(2);
        c.teleport(x, y, z);
        c.waitTicks(8);
        c.lookAt(tx, ty, tz);
        c.waitTicks(8);
        c.screenshot(name);
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }
}
