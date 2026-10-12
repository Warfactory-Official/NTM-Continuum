import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Playtest 2026-10-12: "power and fluid can be stolen from machines; once in a machine, it cannot be taken out
 * through the network". 1.7's networks only ever take from a provider ({@code IEnergyProviderMK2.tryProvide},
 * {@code getSendingTanks}); a consumer's buffer and input tanks are never supply. Hand-placed, in a live world:
 * <ol>
 *   <li>an electric furnace holding 50,000 HE beside an empty input battery on one red cable keeps every HE, and the
 *       battery switched to output then feeds the furnace (the cable is live);</li>
 *   <li>an output battery beside the RF-to-HE converter (a provider only) keeps its charge, the converter takes nothing;</li>
 *   <li>a diesel generator (off) holding 8,000 mB diesel beside an empty input barrel and an empty buffer barrel on one
 *       diesel duct keeps every mB; a foreign extract on its fuel face moves 0 mB while a foreign insert is taken; an
 *       output barrel then fills it (the duct is live).</li>
 *   <li>per-port roles (1.7 {@code tryProvide}/{@code trySubscribe} at each port, {@code PortClaims}): a gas turbine placed
 *       by hand holding dense steam sends it into the duct on its steam outlet and none into a dense-steam duct on a
 *       water inlet, although its block accepts any fluid on either port.</li>
 * </ol>
 */
public class Pt1012NetworkTheft implements Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;

    public void run(Client c) throws Exception {
        c.useFlatWorld();
        Fixtures.platform(c, 0, Y, 0, 10);
        c.cmd("fill -10 " + G + " -10 10 " + (Y + 6) + " 10 minecraft:air");
        c.waitTicks(5);

        // 1. power: furnace (consumer) -- cable x3 -- input battery
        c.cmd("setblock -3 " + G + " 0 hbm:machine_electric_furnace_off{power:50000L}");
        for (int x = -2; x <= 0; x++) c.cmd("setblock " + x + " " + G + " 0 hbm:red_cable");
        c.cmd("setblock 1 " + G + " 0 hbm:machine_battery{power:0L,redLow:0s,redHigh:0s}");

        // 2. generator side: output battery -- cable x3 -- RF-to-HE converter (provider only)
        c.cmd("setblock -3 " + G + " 3 hbm:machine_battery{power:500000L,redLow:2s,redHigh:2s}");
        for (int x = -2; x <= 0; x++) c.cmd("setblock " + x + " " + G + " 3 hbm:red_cable");
        c.cmd("setblock 1 " + G + " 3 hbm:machine_converter_rf_he{power:0L}");

        // 3. fluid: diesel generator (off, 8000 mB) -- diesel duct -- input barrel; a buffer barrel on the duct's south
        c.cmd("setblock -3 " + G + " 6 hbm:machine_diesel{fuel:8000,fuel_type:\"hbm:diesel\",isOn:0b}");
        c.cmd("setblock -2 " + G + " 6 hbm:fluid_duct_neo");
        c.cmd("setblock -1 " + G + " 6 hbm:barrel_steel{tank:0,tank_type:\"hbm:diesel\",mode:0s}");
        c.cmd("setblock -2 " + G + " 7 hbm:barrel_steel{tank:0,tank_type:\"hbm:diesel\",mode:1s}");
        c.waitTicks(3);
        // a duct joins its net's fluid only when typed in game: one click with a diesel fluid identifier
        String identifier = c.node().startsWith("1.20.") ? "hbm:fluid_identifier{\"hbm:fluid\":\"hbm:diesel\"}"
                : "hbm:fluid_identifier[hbm:fluid=\"hbm:diesel\"]";
        Fixtures.giveOnly(c, identifier, 1);
        c.teleport(-1.5, G, 4.5);
        c.waitTicks(3);
        c.lookAt(-1.5, G + 0.5, 6.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(3);
        Expect.that(nbt(c, -2, G, 6).contains("type:\"hbm:diesel\""), "the duct was typed diesel: " + nbt(c, -2, G, 6));

        c.teleport(-1.0, G + 3, -5.0);
        c.lookAt(-1.0, G, 3.5);
        c.waitTicks(80);
        c.screenshot("placed");

        long furnace = longOf(nbt(c, -3, G, 0), "power");
        long inputBattery = longOf(nbt(c, 1, G, 0), "power");
        Expect.that(inputBattery == 0L, "an input battery drew " + inputBattery + " HE out of a consumer");
        Expect.that(furnace == 50000L, "the furnace lost power to the net: " + furnace);

        long outputBattery = longOf(nbt(c, -3, G, 3), "power");
        long converter = longOf(nbt(c, 1, G, 3), "power");
        Expect.that(converter == 0L, "a provider-only converter took " + converter + " HE from the net");
        Expect.that(outputBattery == 500000L, "an output battery was debited with no receiver on the net: " + outputBattery);

        long fuel = longOf(nbt(c, -3, G, 6), "fuel");
        long inBarrel = longOf(nbt(c, -1, G, 6), "tank");
        long bufBarrel = longOf(nbt(c, -2, G, 7), "tank");
        Expect.that(inBarrel == 0L && bufBarrel == 0L,
                "barrels drew the generator's input through the duct: input " + inBarrel + ", buffer " + bufBarrel);
        Expect.that(fuel == 8000L, "the generator lost fuel to the net: " + fuel);

        // a foreign pipe on the generator's west face: never drains its input tank, still fills it
        JsonObject extract = q(c, "extract", "x", -3, "y", G, "z", 6, "side", "west", "fluid", "hbm:diesel", "mb", 1000);
        Expect.that(extract.get("moved").getAsLong() == 0L, "a foreign extract took fuel from an input tank: " + extract);
        JsonObject insert = q(c, "insert", "x", -3, "y", G, "z", 6, "side", "west", "fluid", "hbm:diesel", "mb", 500);
        Expect.that(insert.get("moved").getAsLong() == 500L, "a foreign insert into the input tank: " + insert);

        // positive controls: the same runs do carry from a real provider
        c.cmd("data merge block 1 " + G + " 0 {power:500000L,redLow:2s,redHigh:2s}");
        c.cmd("data merge block -3 " + G + " 3 {power:0L,redLow:0s,redHigh:0s}");
        c.cmd("data merge block 1 " + G + " 3 {power:1000L}");
        c.cmd("data merge block -1 " + G + " 6 {tank:1000,mode:2s}");
        c.waitTicks(60);
        c.screenshot("controls");
        long furnaceAfter = longOf(nbt(c, -3, G, 0), "power");
        long batteryFromConverter = longOf(nbt(c, -3, G, 3), "power");
        long fuelAfter = longOf(nbt(c, -3, G, 6), "fuel");
        Expect.that(furnaceAfter > 50000L, "the output battery never reached the furnace (cable not live): " + furnaceAfter);
        Expect.that(batteryFromConverter > 0L, "the converter never fed the input battery (cable not live)");
        Expect.that(fuelAfter > 8500L, "the output barrel never reached the generator (duct not live): " + fuelAfter);
        System.out.println("[Pt1012NetworkTheft] furnace " + furnace + " -> " + furnaceAfter + ", converter-fed battery "
                + batteryFromConverter + ", diesel " + fuel + " -> " + fuelAfter);

        portRoles(c);
    }

    /** 4. A hand-placed gas turbine: its dense steam leaves at the steam outlet only, never at a water inlet. */
    private static void portRoles(Client c) {
        int tx = 0, tz = -6;
        Fixtures.giveOnly(c, "hbm:machine_turbine_gas", 1);
        c.teleport(tx + 0.5, G, tz - 4.0);
        c.waitTicks(2);
        c.lookAt(tx + 0.5, Y + 0.98, tz + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
        int[] core = null;
        for (int dx = -6; dx <= 6 && core == null; dx++) {
            for (int dz = -6; dz <= 6 && core == null; dz++) {
                JsonObject b = c.block(tx + dx, G, tz + dz);
                if (b.has("block_entity") && !b.get("block_entity").isJsonNull()
                        && b.get("block_entity").getAsString().endsWith("TileEntityMachineTurbineGas")) core = new int[]{tx + dx, G, tz + dz};
            }
        }
        Expect.that(core != null, "the gas turbine was not placed");
        String facing = c.block(core[0], core[1], core[2]).getAsJsonObject("properties").get("facing").getAsString();
        int[] dir = offset(facing);
        int[] rot = offset(switch (facing) { case "north" -> "east"; case "east" -> "south"; case "south" -> "west"; default -> "north"; });
        int x = core[0], y = core[1], z = core[2];
        // TileEntityMachineTurbineGas.getSteamPos / getWaterPos()[0]: a duct cell and the side it leaves the machine by
        int[] steam = {x + dir[1] * 6, y + 1, z - dir[0] * 6, -rot[0], -rot[1]};
        int[] water = {x - dir[0] * 2 - rot[0] * 4, y, z - dir[1] * 2 - rot[1] * 4, -dir[0], -dir[1]};
        for (int[] p : new int[][]{steam, water}) {
            c.cmd("setblock " + p[0] + " " + p[1] + " " + p[2] + " hbm:fluid_duct_neo");
            c.cmd("setblock " + p[0] + " " + (p[1] + 1) + " " + p[2] + " hbm:barrel_steel{tank:0,tank_type:\"hbm:hotsteam\",mode:0s}");
        }
        c.waitTicks(3);
        String identifier = c.node().startsWith("1.20.") ? "hbm:fluid_identifier{\"hbm:fluid\":\"hbm:hotsteam\"}"
                : "hbm:fluid_identifier[hbm:fluid=\"hbm:hotsteam\"]";
        Fixtures.giveOnly(c, identifier, 1);
        for (int[] p : new int[][]{steam, water}) {
            // stand three out along the port's side, so nothing stands between the eye and the duct
            c.teleport(p[0] + p[3] * 3 + 0.5, G, p[2] + p[4] * 3 + 0.5);
            c.waitTicks(3);
            c.lookAt(p[0] + 0.5, p[1] + 0.5, p[2] + 0.5);
            c.waitTicks(1);
            c.use(1);
            c.waitTicks(3);
            Expect.that(nbt(c, p[0], p[1], p[2]).contains("type:\"hbm:hotsteam\""), "the duct was typed dense steam: " + nbt(c, p[0], p[1], p[2]));
        }
        c.cmd("data merge block " + x + " " + y + " " + z + " {densesteam:8000}");
        // the snapshot's stone floor ends before z - 9: without a block to stand on the camera falls to the world floor
        c.cmd("setblock " + x + " " + (G + 3) + " " + (z - 9) + " minecraft:glass");
        c.teleport(x + 0.5, G + 4, z - 9.0 + 0.5);
        c.lookAt(x + 0.5, G, z + 0.5);
        c.waitTicks(80);
        c.screenshot("port_roles");
        long atOutlet = longOf(nbt(c, steam[0], steam[1] + 1, steam[2]), "tank");
        long atInlet = longOf(nbt(c, water[0], water[1] + 1, water[2]), "tank");
        Expect.that(atOutlet > 0L, "the steam outlet's duct got no steam (net not live): " + atOutlet);
        Expect.that(atInlet == 0L, "the turbine's steam left through a water inlet: " + atInlet + " mB");
        System.out.println("[Pt1012NetworkTheft] gas turbine (" + facing + ") dense steam: outlet " + atOutlet
                + " mB, water inlet " + atInlet + " mB");
    }

    private static int[] offset(String facing) {
        return switch (facing) {
            case "north" -> new int[]{0, -1};
            case "south" -> new int[]{0, 1};
            case "west" -> new int[]{-1, 0};
            default -> new int[]{1, 0};
        };
    }

    private static long longOf(String nbt, String key) {
        Matcher m = Pattern.compile("(?:^|[{,])" + Pattern.quote(key) + ":(-?\\d+)[LlSsBb]?(?=[,}])").matcher(nbt);
        Expect.that(m.find(), "no " + key + " in " + nbt);
        return Long.parseLong(m.group(1));
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
