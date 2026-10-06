import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game acceptance for the storage crates, decorative props and simple blocks: every item (and every
 * subtype) in the inventory; every block placed by hand and photographed; the tungsten crate, safe and filing
 * cabinet opened by a real right-click, filled by clicks, and the crate and safe broken and set back down with
 * their contents; the bobblehead, snowglobe and FM radio screens opened by a right-click; a plushie squeezed; an
 * emitter dyed; the vending machine fed a token; an elite radiation absorber draining its chunk against an
 * untouched control chunk; and a glyphid hive spawner spawning its swarm.
 */
public final class FinalDecoCrates implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    static final String[] PLAIN = {"hbm:crate", "hbm:crate_can", "hbm:crate_tungsten", "hbm:safe", "hbm:broadcaster_pc",
            "hbm:radiobox", "hbm:radiorec", "hbm:deco_emitter", "hbm:part_emitter", "hbm:hadron_coil_alloy",
            "hbm:hadron_coil_gold", "hbm:hadron_coil_neodymium", "hbm:hadron_coil_magtung", "hbm:hadron_coil_schrabidium",
            "hbm:hadron_coil_schrabidate", "hbm:hadron_coil_starmetal", "hbm:hadron_coil_chlorophyte", "hbm:hadron_coil_mese",
            "hbm:block_cap_nuka", "hbm:block_cap_quantum", "hbm:block_cap_sparkle", "hbm:block_cap_rad",
            "hbm:block_cap_korl", "hbm:block_cap_fritz", "hbm:pink_log", "hbm:wood_structure_roof",
            "hbm:wood_structure_scaffold", "hbm:wood_structure_ceiling", "hbm:rad_absorber", "hbm:rad_absorber_red",
            "hbm:rad_absorber_green", "hbm:rad_absorber_pink", "hbm:field_disturber"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 30);
        c.cmd("gamerule doMobSpawning false");

        inventories(c);

        // --- placement by hand ---------------------------------------------------------------------------
        c.cmd("gamemode creative");
        for (int i = 0; i < PLAIN.length; i++) place(c, PLAIN[i], px(i), Y, pz(i));
        // wood structures joined: three roofs and a two-high scaffold stack beside the singles
        place(c, "hbm:wood_structure_roof", X + 20, Y, Z + 8);
        place(c, "hbm:wood_structure_roof", X + 21, Y, Z + 8);
        place(c, "hbm:wood_structure_scaffold", X + 23, Y, Z + 8);
        place(c, "hbm:wood_structure_scaffold", X + 24, Y, Z + 8);
        Fixtures.setBlock(c, X + 23, Y + 2, Z + 8, "hbm:wood_structure_scaffold");
        for (int i = 0; i < PLAIN.length; i++) {
            Expect.block(c, px(i), Y + 1, pz(i), PLAIN[i]);
        }
        worldShot(c, "10-row-west", X - 16.5, Y + 4, Z + 11.5, X - 16.5, Y + 1, Z + 4.5);
        worldShot(c, "11-row-east", X + 8.5, Y + 4, Z + 11.5, X + 8.5, Y + 1, Z + 4.5);
        worldShot(c, "11b-row-simple", X - 18.5, Y + 4, Z + 14.5, X - 18.5, Y + 1, Z + 8.5);
        worldShot(c, "12-wood-joined", X + 22.5, Y + 3, Z + 12.5, X + 22.5, Y + 1.5, Z + 8.5);
        // the corrupted broadcaster melts brains within 15 blocks and nauseates within 25 (1.7, faithfully): it has
        // been placed and photographed; take it away before anyone stands near it in survival
        Expect.that(c.blockEntity(px(4), Y + 1, pz(4)).toString().contains("TileEntityBroadcaster"), "broadcaster block entity");
        Fixtures.setBlock(c, px(4), Y + 1, pz(4), "minecraft:air");
        c.cmd("effect clear @s");

        // props with subtypes: bobbleheads 1..24, snowglobes 1..5, plushies 1..4, both vending machines,
        // both filing cabinets, the glyphid hive blocks
        int x = X - 26;
        for (int t = 1; t <= 24; t++) {
            placeVariant(c, "hbm:bobblehead", t, x, Y, Z - 4);
            x += 2;
        }
        x = X - 26;
        for (int t = 1; t <= 5; t++) { placeVariant(c, "hbm:snowglobe", t, x, Y, Z - 8); x += 2; }
        for (int t = 1; t <= 4; t++) { placeVariant(c, "hbm:plushie", t, x, Y, Z - 8); x += 2; }
        for (int t = 0; t < 2; t++) { placeVariant(c, "hbm:vending_machine", t, x, Y, Z - 8); x += 2; }
        for (int t = 0; t < 2; t++) { placeVariant(c, "hbm:filing_cabinet", t, x, Y, Z - 8); x += 2; }
        for (int t = 0; t < 3; t++) { placeVariant(c, "hbm:glyphid_base", t, x, Y, Z - 8); x += 2; }
        Expect.block(c, X - 26, Y + 1, Z - 4, "hbm:bobblehead");
        Expect.block(c, X - 26, Y + 1, Z - 8, "hbm:snowglobe");
        Expect.block(c, X - 16, Y + 1, Z - 8, "hbm:plushie");
        Expect.block(c, X - 8, Y + 1, Z - 8, "hbm:vending_machine");
        Expect.block(c, X - 4, Y + 1, Z - 8, "hbm:filing_cabinet");
        Expect.block(c, X, Y + 1, Z - 8, "hbm:glyphid_base");
        String bobbleNbt = c.blockEntity(X - 26 + 2 * 7, Y + 1, Z - 4).toString();
        // the figure is saved by name since History §19es-itemvariant (1.7 saved the ordinal byte 8)
        Expect.that(bobbleNbt.replaceAll("\\s", "").replace("\\", "").contains("type:\"bob\""), "bobblehead 8 (BOB) keeps its type: " + bobbleNbt);
        worldShot(c, "20-bobbleheads-west", X - 21.5, Y + 2.2, Z - 1.5, X - 21.5, Y + 1.2, Z - 4.5);
        worldShot(c, "21-bobbleheads-mid", X - 9.5, Y + 2.2, Z - 1.5, X - 9.5, Y + 1.2, Z - 4.5);
        worldShot(c, "22-bobbleheads-east", X + 16.5, Y + 2.2, Z - 1.5, X + 16.5, Y + 1.2, Z - 4.5);
        worldShot(c, "23-globes-plushies", X - 20.5, Y + 2.5, Z - 5.5, X - 20.5, Y + 1.2, Z - 8.5);
        worldShot(c, "24-vending-cabinets-hive", X - 4.5, Y + 3, Z - 3.5, X - 4.5, Y + 1.5, Z - 8.5);

        // --- GUIs by right-click ---------------------------------------------------------------------------
        openAt(c, X - 22, Z + 4, "Tungsten Crate");                // crate_tungsten
        c.screenshot("30-gui-crate-tungsten");
        close(c);
        openAt(c, X - 20, Z + 4, "Safe");
        c.screenshot("31-gui-safe");
        close(c);
        openAt(c, X - 4, Z - 8, "Filing Cabinet");
        c.screenshot("32-gui-filing-cabinet");
        close(c);
        openAt(c, X - 26, Z - 4, "GUIScreenBobble");
        c.screenshot("33-gui-bobblehead");
        close(c);
        openAt(c, X - 26 + 14, Z - 4, "GUIScreenBobble");
        c.screenshot("33b-gui-bobblehead-bob");
        close(c);
        openAt(c, X - 26, Z - 8, "GUIScreenSnowglobe");
        c.screenshot("34-gui-snowglobe");
        close(c);
        openAt(c, X - 14, Z + 4, "GUIRadioRec");
        c.screenshot("35-gui-radio");
        close(c);

        // --- contents ride on the broken crate and safe -----------------------------------------------------
        keepsContents(c, "hbm:crate_tungsten", X - 22, "Tungsten Crate", "36");
        keepsContents(c, "hbm:safe", X - 20, "Safe", "37");

        // --- interactions ----------------------------------------------------------------------------------
        // plushie squeeze (Yomi): the squeak plays; the block survives
        c.teleport(X - 16 + 2.5, Y + 1, Z - 8 + 0.5);
        Fixtures.interact(c, X - 16, Y + 1, Z - 8);
        Expect.block(c, X - 16, Y + 1, Z - 8, "hbm:plushie");
        // dye the emitter red and photograph the beam
        Fixtures.giveOnly(c, "minecraft:red_dye", 1);
        c.teleport(X - 12 + 2.5, Y + 1, Z + 4 + 0.5);
        c.waitTicks(2);
        c.lookAt(X - 12 + 0.5, Y + 1.5, Z + 4 + 0.5);
        c.use(2);
        c.waitTicks(25);
        String emitter = c.blockEntity(X - 12, Y + 1, Z + 4).toString();
        Expect.that(!emitter.contains("color:0,") && !emitter.contains("color:0}"), "the dye recoloured the emitter: " + emitter);
        worldShot(c, "40-emitter-beam", X - 6.5, Y + 3, Z + 12.5, X - 12.5, Y + 1.5, Z + 1.5);

        // vending machine: a token for a drink
        c.cmd("kill @e[type=item]");
        Fixtures.giveOnly(c, "hbm:coin_token", 4);
        // 4.5 out, not 3.5: the drink leaves the slot with 1.7's zero pickup delay and a random toss of up to ~1.2
        // blocks, and from 3.5 out a toss toward the player lands it in the inventory (seen on 1.20.1 Forge)
        c.teleport(X - 8 + 0.5, Y + 1, Z - 8 + 4.5);
        c.waitTicks(2);
        c.lookAt(X - 8 + 0.5, Y + 1.5, Z - 8 + 0.5);
        c.use(2);
        c.waitTicks(10);
        boolean soda = false;
        for (JsonElement e : c.entities(8)) {
            if (e.getAsJsonObject().get("type").getAsString().equals("minecraft:item")) soda = true;
        }
        Expect.that(soda, "the vending machine dropped a drink for the token");
        int tokens = 0;
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("id").getAsString().equals("hbm:coin_token")) tokens += s.get("count").getAsInt();
        }
        Expect.that(tokens == 3, "the machine took exactly one of the four tokens, " + tokens + " left");
        c.screenshot("41-vending-drop");

        // --- radiation absorber against a control chunk ---------------------------------------------------
        int ax = X + 40, az = Z + 40;     // an absorber chunk and, 32 blocks off, a control chunk
        Fixtures.platform(c, ax, Y, az, 20);
        c.cmd("hbm rad set " + ax + " " + (Y + 1) + " " + az + " 5000");
        c.cmd("hbm rad set " + (ax + 32) + " " + (Y + 1) + " " + az + " 5000");
        Fixtures.setBlock(c, ax, Y + 1, az, "hbm:rad_absorber_pink");
        c.waitTicks(60);
        c.cmd("hbm rad get " + ax + " " + (Y + 1) + " " + az);
        c.cmd("hbm rad get " + (ax + 32) + " " + (Y + 1) + " " + az);
        c.waitTicks(4);
        List<String> rad = c.log(40, "Radiation at");
        Expect.that(rad.size() >= 2, "both radiation readings logged: " + rad);
        double absorbed = value(rad.get(rad.size() - 2));
        double control = value(rad.get(rad.size() - 1));
        Expect.that(absorbed < 100 && control > 1000, "the absorber drained its chunk (" + absorbed + ") but not the control (" + control + ")");

        // --- glyphid hive spawner ----------------------------------------------------------------------------
        c.cmd("difficulty normal");
        c.cmd("gamemode creative");
        int gx = X - 40, gz = Z - 40;
        Fixtures.platform(c, gx, Y, gz, 12);
        // 1.7 trySpawnEntity tries each member from two blocks BELOW the hive upward, so under a one-block floating
        // platform the first free slot is the air under it and the swarm falls to the superflat floor (seen on
        // 1.21.1: 5 glyphids at y=-60). A hive sits in terrain; give the platform solid ground under it.
        c.cmd("fill " + (gx - 12) + " " + (Y - 3) + " " + (gz - 12) + " " + (gx + 12) + " " + (Y - 1) + " " + (gz + 12) + " minecraft:stone");
        place(c, "hbm:glyphid_spawner", gx, Y, gz);
        c.waitTicks(40);
        int glyphids = 0;
        for (JsonElement e : c.entities(40)) {
            if (e.getAsJsonObject().get("type").getAsString().contains("glyphid")) glyphids++;
        }
        if (glyphids == 0) {
            for (JsonElement e : c.query("hbm_living", new JsonObject()).getAsJsonArray("living")) {
                if (e.getAsJsonObject().get("type").getAsString().contains("glyphid")) glyphids++;
            }
        }
        Expect.that(glyphids > 0, "the hive spawner spawned a swarm (" + glyphids + " glyphids); spawner "
                + c.blockEntity(gx, Y + 1, gz) + " difficulty " + c.log(20, "ifficulty"));
        worldShot(c, "50-glyphid-swarm", gx + 8.5, Y + 5, gz + 8.5, gx + 0.5, Y + 1, gz + 0.5);
        c.cmd("kill @e[type=!player]");
        c.cmd("difficulty peaceful");

        Expect.logCleanSinceMark(c, "(Exception|ERROR.*hbm|Missing model|missing texture)");
    }

    /** The first 24 plain blocks along z + 4, the rest along z + 8 (inside the 30-block platform). */
    private static int px(int i) {
        return i < 24 ? X - 26 + 2 * i : X - 26 + 2 * (i - 24);
    }

    private static int pz(int i) {
        return i < 24 ? Z + 4 : Z + 8;
    }

    private static double value(String line) {
        int eq = line.lastIndexOf('=');
        String tail = line.substring(eq + 1).trim().split(" ")[0];
        return Double.parseDouble(tail);
    }

    private static void inventories(Client c) {
        List<String> batchA = new ArrayList<>();
        for (int t = 1; t <= 24; t++) batchA.add(variant(c, "hbm:bobblehead", t));
        for (int t = 1; t <= 5; t++) batchA.add(variant(c, "hbm:snowglobe", t));
        for (int t = 1; t <= 4; t++) batchA.add(variant(c, "hbm:plushie", t));
        List<String> batchB = new ArrayList<>(List.of(PLAIN).subList(0, 24));
        for (int t = 0; t < 2; t++) batchB.add(variant(c, "hbm:filing_cabinet", t));
        for (int t = 0; t < 2; t++) batchB.add(variant(c, "hbm:vending_machine", t));
        List<String> batchC = new ArrayList<>(List.of(PLAIN).subList(24, PLAIN.length));
        for (int t = 0; t < 3; t++) batchC.add(variant(c, "hbm:glyphid_base", t));
        for (int t = 0; t < 3; t++) batchC.add(variant(c, "hbm:glyphid_spawner", t));
        inventory(c, batchA, "01-inventory-props");
        inventory(c, batchB, "02-inventory-crates-coils-caps");
        inventory(c, batchC, "03-inventory-simple");
    }

    private static void inventory(Client c, List<String> items, String shot) {
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.waitTicks(2);
        for (String id : items) c.cmd("give @s " + id + " 1");
        c.waitTicks(4);
        JsonObject inv = c.inventory();
        for (String id : items) {
            String bare = id.contains("[") ? id.substring(0, id.indexOf('[')) : id.contains("{") ? id.substring(0, id.indexOf('{')) : id;
            Expect.that(inv.toString().contains("\"" + bare + "\""), "inventory holds " + id);
        }
        c.key("key.inventory");
        c.waitScreen("", 5000);
        c.waitTicks(10);
        c.screenshot(shot);
        close(c);
        c.cmd("clear");
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }

    /** The give syntax for a subtype: 26.1/1.21 components, 1.20.1 the legacy NBT key. */
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

    private static void placeVariant(Client c, String id, int variant, int x, int y, int z) {
        c.cmd("clear");
        c.waitTicks(1);
        c.cmd("give @s " + variant(c, id, variant) + " 1");
        c.hotbar(1);
        c.waitTicks(2);
        use(c, x, y, z, false);
    }

    /**
     * The crates 1.7 registers with {@code ItemBlockStorageCrate}: under {@code /ntmserver CRATE_OPEN_HELD} (on by
     * default) a plain right-click opens the held crate and only a sneak-click places it.
     */
    static final java.util.Set<String> SNEAK_PLACED = java.util.Set.of("hbm:crate_tungsten", "hbm:safe");

    private static void place(Client c, String item, int x, int y, int z) {
        Fixtures.giveOnly(c, item, 1);
        use(c, x, y, z, SNEAK_PLACED.contains(item));
    }

    /** From the south, looking north at the top face: every placed block faces the camera rows. */
    private static void use(Client c, int x, int y, int z, boolean sneak) {
        c.teleport(x + 0.5, y + 1, z + 3.5);
        c.waitTicks(3);
        if (sneak) {
            c.keyDown("key.sneak");
            c.waitTicks(5);              // lands the player if flying; the eye drops to sneak height
            Expect.that(c.player().get("sneaking").getAsBoolean(), "holding the sneak key makes the player sneak");
        }
        c.lookAt(x + 0.5, y + 0.98, z + 0.9);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
        if (sneak) {
            c.keyUp("key.sneak");
            c.waitTicks(3);
        }
    }

    private static void openAt(Client c, int x, int z, String title) {
        c.teleport(x + 0.5, Y + 1, z + 3.5);
        c.waitTicks(3);
        c.hotbar(9);
        c.waitTicks(1);
        c.lookAt(x + 0.5, Y + 1.3, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitScreen(title, 5000);
        Expect.screenStaysOpen(c, title, 10);
        c.waitTicks(5);
    }

    private static void keepsContents(Client c, String id, int x, String title, String shot) {
        int z = Z + 4;
        c.cmd("clear");
        c.cmd("give @s minecraft:diamond 7");
        c.waitTicks(2);
        openAt(c, x, z, title);
        JsonObject slot = null;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && s.get("id").getAsString().equals("minecraft:diamond")) slot = s;
        }
        Expect.that(slot != null, "a diamond stack in the player half of " + title);
        c.clickSlot(slot.get("index").getAsInt());
        c.waitTicks(1);
        c.clickSlot(0);
        c.waitTicks(2);
        c.screenshot(shot + "-gui-filled");
        close(c);
        Expect.that(c.blockEntity(x, Y + 1, z).toString().contains("diamond"), title + " holds the diamonds");

        // break it the way a harvest does (drops through the loot table), pick it up, set it back down
        c.cmd("kill @e[type=item]");
        c.cmd("clear");
        c.cmd("gamemode survival");
        c.cmd("setblock " + x + " " + (Y + 1) + " " + z + " air destroy");
        c.waitTicks(5);
        c.teleport(x + 0.5, Y + 1, z + 0.5);
        c.waitTicks(20);
        JsonObject inv = c.inventory();
        Expect.that(inv.toString().contains("\"" + id + "\""), "the broken " + title + " was picked up: " + inv);
        c.key("key.inventory");
        c.waitScreen("", 5000);
        c.waitTicks(5);
        c.screenshot(shot + "-carried-item");
        close(c);
        c.cmd("gamemode creative");
        c.hotbar(1);
        c.waitTicks(1);
        use(c, x, Y, z, SNEAK_PLACED.contains(id));
        Expect.block(c, x, Y + 1, z, id);
        String nbt = c.blockEntity(x, Y + 1, z).toString();
        Expect.that(nbt.contains("diamond"), "the replaced " + title + " still holds the diamonds: " + nbt);
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitTicks(5);
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
