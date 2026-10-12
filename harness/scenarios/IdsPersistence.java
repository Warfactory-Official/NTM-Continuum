import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Numeric ids are never persisted (History section 19ej-ids): a filled fluid tank, a barrel, a valve, ducts typed with
 * a fluid identifier and a configured structure wand survive save & quit with their identity, and what is on disk names
 * them by registry name (the block-entity NBT query reads the saved form), never by a number.
 */
public class IdsPersistence implements Scenario {

    private static final int Y = 100;
    private static final int TX = 7, TZ = 0;          // machine_fluidtank core
    private static final int BX = -5, BZ = 0;         // barrel
    private static final int DZ = 6;                  // duct row z, x = -3..3
    private static final int VX = 5;                  // valve on the duct row
    private static final int WX = -6, WZ = -5;        // gold block the wand blacklists
    /** A numeric value under a fluid key: {@code tank_type:20}, {@code type:7}. */
    private static final Pattern NUMERIC_FLUID = Pattern.compile("(^|[,{])(\\w*_type|type):-?\\d+[bsL]?[,}]");

    public void run(Client c) throws Exception {
        c.useFlatWorld();
        Fixtures.platform(c, 0, Y, 0, 12);
        c.cmd("fill -12 " + (Y + 1) + " -12 12 " + (Y + 8) + " 12 minecraft:air");
        c.waitTicks(5);

        // a filled multiblock tank
        placeFar(c, "hbm:machine_fluidtank", TX, Y, TZ, 4);
        JsonObject tank = gapsB(c, "machine", TX, Y + 1, TZ, "fillWith", "DIESEL", "amount", 123456);
        Expect.that(tank.get("present").getAsBoolean(), "tank placed: " + tank);

        // a barrel holding kerosene (the saved form is the registry name)
        c.cmd("setblock " + BX + " " + (Y + 1) + " " + BZ + " hbm:barrel_steel{tank:4321,tank_type:\"hbm:kerosene\",mode:2s}");

        // a duct row typed in game with a fluid identifier, and a valve on the end
        c.cmd("fill -3 " + (Y + 1) + " " + DZ + " 3 " + (Y + 1) + " " + DZ + " hbm:fluid_duct_neo");
        c.cmd("setblock " + VX + " " + (Y + 1) + " " + DZ + " hbm:fluid_valve{type:\"hbm:water\"}");
        c.waitTicks(5);
        boolean legacy = c.node().startsWith("1.20.");
        Fixtures.giveOnly(c, legacy ? "hbm:fluid_identifier{\"hbm:fluid\":\"hbm:steam\"}"
                : "hbm:fluid_identifier[hbm:fluid=\"hbm:steam\"]", 1);
        c.teleport(0.5, Y + 1, DZ - 2.5);
        c.waitTicks(3);
        // crouching retypes the whole run (1.7's identifier sneak spread); a plain click types the one duct
        sneakUse(c, 0.5, Y + 1.5, DZ + 0.5);
        c.waitTicks(10);
        Expect.that(nbt(c, 0, Y + 1, DZ).contains("type:\"hbm:steam\""), "the identifier typed the duct: " + nbt(c, 0, Y + 1, DZ));

        // a structure wand with a blacklisted block
        c.cmd("setblock " + WX + " " + (Y + 1) + " " + WZ + " minecraft:gold_block");
        c.waitTicks(5);
        Fixtures.giveOnly(c, "hbm:wand_s", 1);
        c.teleport(WX + 0.5, Y + 1, WZ - 1.5);
        c.waitTicks(10);
        c.markLog();
        sneakUse(c, WX + 0.5, Y + 1.5, WZ + 0.02);
        expectChat(c, "Added to blacklist block.minecraft.gold_block");
        String wandBefore = selectedItem(c);
        Expect.that(wandBefore.contains("minecraft:gold_block") && !wandBefore.contains("[I;"),
                "the wand saves its blacklist by state name, not as a state-id array: " + wandBefore);

        c.screenshot("before-reload");
        c.leaveWorld(60_000L);
        Expect.that(!c.inWorld(), "the client left the world");
        c.joinWorld("harness", 180_000L);
        c.waitTicks(40);
        c.teleport(0.5, Y + 1, DZ - 2.5);
        c.waitTicks(20);

        // identity after save & quit, and the saved form
        JsonObject tankAfter = gapsB(c, "machine", TX, Y + 1, TZ);
        Expect.that(tankAfter.get("fluid").getAsString().equals("DIESEL") && tankAfter.get("fill").getAsInt() == 123456,
                "the tank kept 123456 mB of diesel: " + tankAfter);
        String barrel = nbt(c, BX, Y + 1, BZ);
        Expect.that(barrel.contains("tank_type:\"hbm:kerosene\"") && barrel.contains("tank:4321"),
                "the barrel kept 4321 mB of kerosene by name: " + barrel);
        for (int x = -3; x <= 3; x++) {
            String duct = nbt(c, x, Y + 1, DZ);
            Expect.that(duct.contains("type:\"hbm:steam\""), "duct " + x + " kept steam by name: " + duct);
        }
        String valve = nbt(c, VX, Y + 1, DZ);
        // placed with the old name hbm:water (still read, §19fa-m1); saved under vanilla's fluid id
        Expect.that(valve.contains("type:\"minecraft:water\""), "the valve kept water by name: " + valve);
        for (String saved : List.of(barrel, valve, nbt(c, 0, Y + 1, DZ))) {
            Expect.that(!NUMERIC_FLUID.matcher(saved.replaceAll("\\s+", "")).find(), "no numeric fluid id on disk: " + saved);
        }

        String wandAfter = selectedItem(c);
        c.screenshot("after-reload");
        Expect.that(wandAfter.contains("minecraft:gold_block") && !wandAfter.contains("[I;"),
                "the wand's blacklist survived save & quit by name: " + wandAfter);
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        // the query pretty-prints SNBT; values here hold no spaces, so whitespace is dropped before matching
        return (be.has("nbt") ? be.get("nbt").getAsString() : be.toString()).replaceAll("\\s+", "");
    }

    private static String selectedItem(Client c) throws Exception {
        c.markLog();
        c.cmd("data get entity @s SelectedItem");
        for (int i = 0; i < 10; i++) {
            c.waitTicks(2);
            List<String> chat = c.logSinceMark("CHAT");
            if (!chat.isEmpty()) return String.join(" ", chat);
        }
        return "";
    }

    private static JsonObject gapsB(Client c, String op, int x, int y, int z, Object... kv) {
        JsonObject p = new JsonObject();
        p.addProperty("op", op);
        p.addProperty("x", x);
        p.addProperty("y", y);
        p.addProperty("z", z);
        for (int i = 0; i < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v instanceof Number n) p.addProperty((String) kv[i], n);
            else p.addProperty((String) kv[i], String.valueOf(v));
        }
        JsonObject r = c.query("hbm_gapsb", p);
        Expect.that(!r.has("error"), "query " + op + " failed: " + r);
        return r;
    }

    private static void placeFar(Client c, String item, int x, int y, int z, int dist) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(x + dist + 0.5, y + 1, z + 0.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, y + 0.98, z + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(5);
    }

    private static void sneakUse(Client c, double tx, double ty, double tz) {
        c.keyDown("key.sneak");
        c.waitTicks(5);
        c.lookAt(tx, ty, tz);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(4);
        c.keyUp("key.sneak");
        c.waitTicks(3);
    }

    private static void expectChat(Client c, String text) throws Exception {
        for (int i = 0; i < 10; i++) {
            if (!c.logSinceMark(Pattern.quote(text)).isEmpty()) return;
            c.waitTicks(2);
        }
        Expect.that(false, "chat shows \"" + text + "\": " + c.logSinceMark("CHAT"));
    }
}
