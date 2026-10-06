import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * The visual gaps of the final-content wave, each drawn where it is seen: the ported 1.7 item renderers
 * ({@code hbm:custom_item}) in the inventory, in first person and in third person, on the ground; the
 * meteorite swords' tier glints and the hot ingots' glow; the bobblehead and plushie items with their props;
 * the RBMK gauge and panel icons; and the deco emitter's beam before and after a dye.
 */
public final class FinalVisualGaps implements Scenario {

    private static final int X = 240;
    private static final int Y = 100;
    private static final int Z = 240;

    /** 1.7 IItemRenderers that declined the inventory: sprite in the slot, model in the hand. */
    static final String[] HELD = {"chainsaw", "crucible", "mese_gavel", "wood_gavel", "lead_gavel", "diamond_gavel",
            "shimmer_sledge", "shimmer_axe", "stopsign", "sopsign", "chernobylsign", "big_sword", "redstone_sword",
            "dnt_sword"};

    static final String[] TIERS = {"seared", "reforged", "hardened", "alloyed", "machined", "treated", "etched", "bred",
            "irradiated", "fused", "baleful"};

    static final String[] HOT = {"ingot_steel_dusted", "ingot_chainsteel", "ingot_meteorite", "ingot_meteorite_forged",
            "blade_meteorite"};

    static final String[] RBMK = {"rbmk_gauge", "rbmk_display_blank", "rbmk_display", "rbmk_key_pad", "rbmk_lever",
            "rbmk_numitron", "rbmk_graph", "rbmk_indicator"};

    /** FRIZZLE (gun + doubloons), PU238 (glow), VT (red name), BLUEHAT (helmet), VAER (cigarette), MELLOW, ABEL, BOB. */
    static final int[] BOBBLES = {1, 8, 9, 10, 11, 13, 14, 15, 17, 18, 23, 24};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 16);
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("time set 6000");
        c.cmd("weather clear");
        c.cmd("gamerule doMobSpawning false");

        inventories(c);
        held(c);
        ground(c);
        emitter(c);

        c.setOption("camera", "first_person");
        c.cmd("gamemode creative");
        c.cmd("clear");
        List<String> bad = c.logSinceMark("/(ERROR|FATAL)[]]|Failed to load texture|Missing model|custom item|Unknown hbm:custom_item");
        List<String> relevant = new ArrayList<>();
        for (String line : bad) if (!line.contains("Loaded ")) relevant.add(line);
        Expect.that(relevant.isEmpty(), "log clean since the mark: " + relevant);
    }

    // ------------------------------------------------------------------ inventory

    private void inventories(Client c) {
        c.cmd("gamemode survival");
        c.cmd("effect give @s minecraft:resistance 900 4 true");
        List<String> page0 = new ArrayList<>();
        for (String id : HELD) page0.add("hbm:" + id);
        page0.add(missile(c));
        page0.add("hbm:missile_custom"); // no parts: 1.7 drew nothing at all for it
        for (String t : TIERS) page0.add("hbm:meteorite_sword_" + t);
        page0.add("hbm:meteorite_sword");
        for (String id : HOT) page0.add(heat(c, "hbm:" + id));
        page(c, page0, "00-inventory-held-glint-hot");

        List<String> page1 = new ArrayList<>();
        for (int b : BOBBLES) page1.add(variant(c, "hbm:bobblehead", b));
        for (int p = 1; p <= 4; p++) page1.add(variant(c, "hbm:plushie", p));
        for (String id : RBMK) page1.add("hbm:" + id);
        page(c, page1, "01-inventory-bobbles-plushies-rbmk");
        c.cmd("clear");
    }

    private void page(Client c, List<String> items, String shot) {
        c.cmd("clear");
        c.waitTicks(2);
        for (String id : items) c.cmd("give @s " + id + " 1");
        c.waitTicks(10);
        c.setOption("gui_scale", "3");
        c.waitTicks(5);
        c.key("e");
        c.waitTicks(15);
        JsonObject status = c.status();
        int gw = status.get("gui_width").getAsInt();
        int gh = status.get("gui_height").getAsInt();
        c.screenshot(shot, new int[]{(gw - 176) / 2, (gh - 166) / 2 + 76, 176, 90});
        c.screenshot(shot + "-full");
        c.key("escape");
        c.waitTicks(3);
        c.setOption("gui_scale", "2");
        c.waitTicks(5);
    }

    // ------------------------------------------------------------------ hands

    private void held(Client c) {
        c.cmd("gamemode creative");
        List<String> shots = new ArrayList<>();
        for (String id : HELD) shots.add("hbm:" + id);
        shots.add(missile(c));
        shots.add("hbm:meteorite_sword_seared");
        shots.add("hbm:meteorite_sword_baleful");
        shots.add(variant(c, "hbm:bobblehead", 17));
        shots.add(variant(c, "hbm:plushie", 2));
        int n = 10;
        for (String item : shots) {
            String name = item.replaceAll("[^a-z0-9_]", "_").replace("hbm_", "");
            if (name.length() > 40) name = name.substring(0, 40);
            c.setOption("camera", "first_person");
            c.teleport(X + 0.5, Y + 1, Z + 0.5);
            c.look(0, 10);
            Fixtures.giveOnly(c, item, 1);
            c.waitTicks(20);
            c.screenshot((n) + "-fp-" + name, new int[]{320, 120, 320, 240});
            c.setOption("camera", "third_person_front");
            c.waitTicks(4);
            c.screenshot((n) + "-tp-" + name, new int[]{200, 60, 240, 240});
            n++;
        }
        c.setOption("camera", "first_person");
        // crucible without charge (old-hbm: crafted empty): lowered, the blade clipped away
        Fixtures.giveOnly(c, "hbm:crucible", 1);
        c.waitTicks(20);
        c.screenshot(n + "-fp-crucible-spent", new int[]{320, 120, 320, 240});
    }

    // ------------------------------------------------------------------ ground

    private void ground(Client c) {
        c.cmd("clear");
        c.cmd("kill @e[type=item]");
        String[] drops = {"hbm:chainsaw", "hbm:wood_gavel", "hbm:stopsign", "hbm:crucible", "hbm:big_sword",
                "hbm:shimmer_sledge"};
        for (int i = 0; i < drops.length; i++) {
            String nbt = c.node().startsWith("1.20")
                    ? "{Item:{id:\"" + drops[i] + "\",Count:1b},PickupDelay:32767,Age:-32768}"
                    : "{Item:{id:\"" + drops[i] + "\",count:1},PickupDelay:32767,Age:-32768}";
            c.cmd("summon minecraft:item " + (X - 3 + i * 1.2) + " " + (Y + 1) + " " + (Z + 3.5) + " " + nbt);
        }
        c.teleport(X + 0.5, Y + 2, Z + 0.5);
        c.waitTicks(20);
        c.lookAt(X + 0.5, Y + 1, Z + 3.5);
        c.waitTicks(3);
        c.screenshot("40-ground-items");
        c.cmd("kill @e[type=item]");
    }

    // ------------------------------------------------------------------ emitter

    private void emitter(Client c) {
        int ex = X + 6, ez = Z - 6;
        Fixtures.setBlock(c, ex, Y + 1, ez, "hbm:deco_emitter[facing=up]");
        Fixtures.setBlock(c, ex + 3, Y + 1, ez, "hbm:deco_emitter[facing=up]");
        c.waitTicks(45);
        Fixtures.giveOnly(c, "minecraft:red_dye", 1);
        c.teleport(ex + 0.5, Y + 1, ez + 2.5);
        c.waitTicks(2);
        c.lookAt(ex + 0.5, Y + 1.5, ez + 0.5);
        c.use(2);
        c.waitTicks(45);
        String be = c.blockEntity(ex, Y + 1, ez).toString();
        Expect.that(!be.contains("color:0,") && !be.contains("color:0}"), "the dye recoloured the emitter: " + be);
        c.teleport(ex + 1.5, Y + 2, ez + 10.5);
        c.waitTicks(5);
        c.lookAt(ex + 2.0, Y + 8, ez + 0.5);
        c.waitTicks(5);
        c.screenshot("50-emitter-dyed-left-undyed-right");
    }

    // ------------------------------------------------------------------ give syntax

    /** A fully assembled 1.7-style custom missile: chip, HE warhead, kerosene fuselage, flat fins, kerosene engine. */
    private static String missile(Client c) {
        String parts = "chip:\"hbm:mp_c_1\",warhead:\"hbm:mp_warhead_10_he\",fuselage:\"hbm:mp_fuselage_10_kerosene\","
                + "stability:\"hbm:mp_stability_10_flat\",thruster:\"hbm:mp_thruster_10_kerosene\"";
        return c.node().startsWith("1.20") ? "hbm:missile_custom{\"hbm:missile_custom_parts\":{" + parts + "}}"
                : "hbm:missile_custom[hbm:missile_custom_parts={" + parts + "}]";
    }

    private static String heat(Client c, String id) {
        return c.node().startsWith("1.20") ? id + "{\"hbm:heat\":100000}" : id + "[hbm:heat=100000]";
    }

    /** 1.7 (block, metadata): one item per subtype since History §19es-itemvariant (ntm-next ids). */
    private static String variant(Client c, String id, int variant) {
        return switch (id) {
            case "hbm:bobblehead" -> variant == 0 ? id : id + "_" + BOBBLE_NAMES[variant];
            case "hbm:snowglobe" -> variant == 0 ? id : id + "_" + new String[] {"none", "rivetcity", "tenpennytower", "lucky38", "sierramadre", "prydwen"}[variant];
            case "hbm:plushie" -> variant == 0 ? id : id + "_" + new String[] {"none", "yomi", "numbernine", "hundun", "derg"}[variant];
            case "hbm:filing_cabinet" -> id + (variant == 1 ? "_steel" : "_green");
            case "hbm:vending_machine" -> variant == 1 ? id + "_snacks" : id;
            case "hbm:glyphid_base", "hbm:glyphid_spawner" -> id + new String[] {"", "_infested", "_rad"}[variant];
            case "hbm:holotape_image" -> id + "_holo_digamma";
            default -> throw new IllegalArgumentException(id);
        };
    }

    /** BlockBobble.BobbleType names by ordinal. */
    private static final String[] BOBBLE_NAMES = {"none", "strength", "perception", "endurance", "charisma", "intelligence", "agility", "luck", "bob", "frizzle", "pu238", "vt", "doc", "bluehat", "pheo", "adam29", "uffr", "vaer", "nos", "drillgon", "cirno", "microwave", "peep", "mellow", "abel", "movblock", "leafia", "hacker6329", "eos"};
}
