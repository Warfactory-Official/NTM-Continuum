import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * HBM fluids on the loaders' standard fluid capability (History section 19ek-fluids), in a real world, through the same
 * lookup another mod's pipe uses ({@code hbm_fluidcaps} query, Core {@code FluidCompat}):
 * <ol>
 *   <li>the 154 HBM token fluids are in the vanilla fluid registry ({@code hbm:diesel}; water is vanilla water);</li>
 *   <li>a vanilla water bucket's own handler empties into a water barrel (1000 mB, the bucket becomes a bucket);</li>
 *   <li>1500 mB diesel moves between two barrels through the capability alone, exact on both sides;</li>
 *   <li>JEI lists every HBM fluid as a fluid ingredient;</li>
 *   <li>the saved NBT still names fluids by registry name.</li>
 * </ol>
 */
public class ForeignFluidCaps implements Scenario {

    private static final int Y = 100;

    public void run(Client c) throws Exception {
        c.useFlatWorld();
        Fixtures.platform(c, 0, Y, 0, 10);
        c.cmd("fill -10 " + (Y + 1) + " -10 10 " + (Y + 6) + " 10 minecraft:air");
        c.waitTicks(5);

        JsonObject registry = q(c, "registry");
        Expect.that(registry.get("tokens").getAsInt() == 154 && registry.get("fluidTypes").getAsInt() == 157,
                "154 HBM token fluids registered for 157 fluid types: " + registry);
        Expect.that(registry.get("diesel").getAsString().equals("hbm:diesel")
                && registry.get("water").getAsString().equals("minecraft:water"), "fluid ids: " + registry);

        // a water barrel filled from a vanilla bucket, through both loader handlers
        c.cmd("setblock 0 " + (Y + 1) + " 0 hbm:barrel_steel{tank:0,tank_type:\"hbm:water\",mode:0s}");
        // two diesel barrels: an output one and an input one
        c.cmd("setblock -4 " + (Y + 1) + " 3 hbm:barrel_steel{tank:5000,tank_type:\"hbm:diesel\",mode:2s}");
        c.cmd("setblock -2 " + (Y + 1) + " 3 hbm:barrel_steel{tank:0,tank_type:\"hbm:diesel\",mode:0s}");
        c.teleport(0.5, Y + 1, -4.5);
        c.waitTicks(10);
        c.screenshot("placed");

        JsonObject bucket = q(c, "bucket", "x", 0, "y", Y + 1, "z", 0, "side", "up");
        Expect.that(bucket.get("moved").getAsInt() == 1000 && bucket.get("bucketAfter").getAsString().equals("minecraft:bucket"),
                "a water bucket emptied 1000 mB into the water barrel: " + bucket);
        Expect.that(bucket.getAsJsonArray("tanks").get(0).getAsJsonObject().get("amount").getAsInt() == 1000,
                "the barrel reads 1000 mB: " + bucket);
        JsonObject lava = q(c, "insert", "x", 0, "y", Y + 1, "z", 0, "side", "north", "fluid", "hbm:lava", "mb", 1000);
        Expect.that(lava.get("moved").getAsInt() == 0, "the water barrel refused lava: " + lava);

        JsonObject move = q(c, "move", "x", -4, "y", Y + 1, "z", 3, "side", "east",
                "tx", -2, "ty", Y + 1, "tz", 3, "tside", "west", "fluid", "hbm:diesel", "mb", 1500);
        Expect.that(move.get("moved").getAsInt() == 1500 && move.get("taken").getAsInt() == 1500, "1500 mB diesel moved: " + move);

        // the flow control pump's intake and outlet: GameTest foreign_fluids_pump_typed_sides

        // the saved form is unchanged
        c.waitTicks(10);
        String water = nbt(c, 0, Y + 1, 0), out = nbt(c, -4, Y + 1, 3), in = nbt(c, -2, Y + 1, 3);
        // placed with the old name hbm:water (still read, §19fa-m1); saved under vanilla's fluid id
        Expect.that(water.contains("tank_type:\"minecraft:water\"") && water.contains("tank:1000"), "water barrel save: " + water);
        Expect.that(out.contains("tank_type:\"hbm:diesel\"") && out.contains("tank:3500"), "source barrel save: " + out);
        Expect.that(in.contains("tank_type:\"hbm:diesel\"") && in.contains("tank:1500"), "sink barrel save: " + in);

        // JEI: every HBM fluid as a fluid ingredient
        JsonObject jei = new JsonObject();
        for (int i = 0; i < 60; i++) {
            jei = q(c, "jei");
            if (jei.has("ready") && jei.get("ready").getAsBoolean()) break;
            c.waitTicks(20);
        }
        Expect.that(jei.get("ready").getAsBoolean() && jei.get("count").getAsInt() == 154,
                "JEI lists the 154 HBM fluids: " + (jei.has("count") ? jei.get("count") : jei));
        Expect.that(jei.toString().contains("Diesel"), "JEI names diesel by its lang name");
        c.key("e");
        c.waitScreen("Inventory", 5000);
        c.waitTicks(20);
        c.screenshot("inventory-jei");
        c.key("escape");
        c.waitTicks(5);
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return (be.has("nbt") ? be.get("nbt").getAsString() : be.toString()).replaceAll("\\s+", "");
    }

    private static JsonObject q(Client c, String op, Object... kv) {
        JsonObject p = new JsonObject();
        p.addProperty("op", op);
        for (int i = 0; i < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v instanceof Number n) p.addProperty((String) kv[i], n);
            else p.addProperty((String) kv[i], String.valueOf(v));
        }
        JsonObject r = c.query("hbm_fluidcaps", p);
        Expect.that(!r.has("error"), "query " + op + " failed: " + r);
        return r;
    }
}
