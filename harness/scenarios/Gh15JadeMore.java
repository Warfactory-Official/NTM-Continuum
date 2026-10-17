import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * GitHub issue 15: more HBM readouts in Jade. Places the fusion torus, plasma forge, fusion boiler and MHD turbine,
 * the ICF and the SILEX by hand, and a DFC emitter, receiver and core by command, seeds what can be seeded (the D-D
 * cycle, ICF heat, UF6 drawn into the SILEX pool, an emitter beam into a receiver, a restriction field), and checks the
 * lines {@code hbm_block_info} reports, which are what Jade is sent, in 1.7's units: TU, TU/t, Spk and the pool in mB.
 * Each machine is also looked at and screenshotted; with Jade in the run the pictures show the HUD.
 */
public final class Gh15JadeMore implements Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 16);
        clear(c);
        c.cmd("gamemode creative");
        c.cmd("gamerule sendCommandFeedback false");

        List<String> jade = c.log(4000, "(?i)golem\\.core\\.jade|jade.*plugin");
        boolean withJade = !jade.isEmpty();
        System.out.println("[Gh15] Jade " + (withJade ? "present" : "absent"));
        c.markLog();

        // torus with the D-D cycle: klystron energy against its ignition, plasma against its output
        JsonObject torus = place(c, "hbm:fusion_torus", "TileEntityFusionTorus");
        merge(c, torus, "{recipe0:\"fus.dd\"}");
        torus = info(c, torus);
        Expect.that(gauge(torus, "", "KyU").get("max").getAsLong() > 0, "torus KyU has the recipe's ignition: " + torus);
        Expect.that(gauge(torus, "", "TU").get("max").getAsLong() > 0, "torus plasma TU has the recipe's output: " + torus);
        look(c, torus, "01-torus");
        clear(c);

        JsonObject forge = place(c, "hbm:fusion_plasma_forge", "TileEntityFusionPlasmaForge");
        gauge(forge, "", "TU");
        look(c, forge, "02-plasma-forge");
        clear(c);

        JsonObject boiler = place(c, "hbm:fusion_boiler", "TileEntityFusionBoiler");
        Expect.equal(0L, gauge(boiler, "", "TU").get("max").getAsLong(), "boiler plasma is a plain TU line");
        look(c, boiler, "03-fusion-boiler");
        clear(c);

        JsonObject mhdt = place(c, "hbm:fusion_mhdt", "TileEntityFusionMHDT");
        Expect.equal(5_000_000L, gauge(mhdt, "", "TU/t").get("max").getAsLong(), "MHDT plasma against 1.7's minimum");
        look(c, mhdt, "04-fusion-mhdt");
        clear(c);

        // ICF: heat against 1.7's 1 TTU; no laser line while no laser is linked
        JsonObject icf = place(c, "hbm:icf", "TileEntityICF");
        merge(c, icf, "{heat:500000000000L}");
        icf = info(c, icf);
        JsonObject heat = gauge(icf, "", "TU");
        Expect.equal(1_000_000_000_000L, heat.get("max").getAsLong(), "ICF heat max");
        Expect.that(heat.get("value").getAsLong() > 400_000_000_000L, "ICF heat seeded: " + heat);
        Expect.that(find(icf, "", "TU/t") == null, "an unlinked ICF shows no laser line: " + icf);
        look(c, icf, "05-icf");
        clear(c);

        // SILEX: UF6 in the tank moves into the pool at 50 mB a tick; tank and pool hold the 8,000 mB between them
        JsonObject silex = place(c, "hbm:machine_silex", "TileEntitySILEX");
        merge(c, silex, "{tankNew:8000,tankNew_type:\"hbm:uf6\"}");
        c.waitTicks(40);
        silex = info(c, silex);
        long uf6 = 0;
        int pools = 0;
        for (JsonElement e : silex.getAsJsonArray("tanks")) {
            JsonObject tank = e.getAsJsonObject();
            if (!tank.get("fluid").getAsString().equals("hbm:uf6")) continue;
            uf6 += tank.get("mb").getAsLong();
            pools++;
        }
        Expect.that(pools == 2 && uf6 == 8_000L, "SILEX tank and pool both UF6, 8,000 mB in all: " + silex);
        look(c, silex, "06-silex");
        clear(c);

        // DFC: an emitter at 1% fires 100 Spk a tick east into a receiver, which catches 95; a core holds its field
        c.cmd("setblock -3 " + G + " 0 hbm:dfc_emitter[facing=east]");
        c.cmd("setblock 3 " + G + " 0 hbm:dfc_receiver[facing=west]");
        c.cmd("setblock 0 " + G + " 5 hbm:dfc_core");
        c.waitTicks(5);
        c.cmd("data merge block 3 " + G + " 0 {tank:64000}");
        c.cmd("data merge block 0 " + G + " 5 {field:100}");
        c.cmd("data merge block -3 " + G + " 0 {tank:64000,power:1000000000L,watts:1,isOn:1b}");
        c.waitTicks(20);
        JsonObject emitter = info(c, -3, G, 0);
        JsonObject receiver = info(c, 3, G, 0);
        JsonObject core = info(c, 0, G, 5);
        Expect.equal(100L, gauge(emitter, "Output", "Spk").get("value").getAsLong(), "emitter output: " + emitter);
        Expect.equal(95L, gauge(receiver, "Input", "Spk").get("value").getAsLong(), "receiver input: " + receiver);
        Expect.equal(100L, gauge(core, "Restriction Field", "%").get("max").getAsLong(), "core field: " + core);
        gauge(core, "Heat Saturation", "%");
        look(c, emitter, "07-dfc-emitter");
        look(c, receiver, "08-dfc-receiver");
        look(c, core, "09-dfc-core");
        c.cmd("data merge block -3 " + G + " 0 {isOn:0b}");
        c.waitTicks(2);
        clear(c);

        if (withJade) Expect.logCleanSinceMark(c, "(?i)(ERROR|FATAL).*(jade|golem)");
        else Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    private static void clear(Client c) {
        c.cmd("fill -16 " + G + " -16 16 " + (Y + 12) + " 16 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.waitTicks(3);
    }

    /**
     * Places a multiblock by hand on the platform three blocks ahead of a player standing south of the origin, then
     * walks the centre line away from the player until a cell reports the expected block entity: the front-most cell.
     */
    private static JsonObject place(Client c, String item, String blockEntity) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(0.5, G, -5.5);
        c.waitTicks(5);
        c.lookAt(0.5, Y + 0.98, -2.5);
        for (int attempt = 0; attempt < 3; attempt++) {
            c.waitTicks(5);
            c.use(2);
            c.waitTicks(10);
            for (int z = -3; z <= 14; z++) for (int y = G; y <= G + 2; y++) {
                if (c.block(0, y, z).get("block").getAsString().equals("minecraft:air")) continue;
                JsonObject found = info(c, 0, y, z);
                if (blockEntity.equals(found.get("block_entity").getAsString())) return found;
            }
        }
        Expect.that(false, item + " placed by hand formed a core");
        return null;
    }

    private static JsonObject info(Client c, int x, int y, int z) {
        JsonObject params = new JsonObject();
        params.addProperty("x", x);
        params.addProperty("y", y);
        params.addProperty("z", z);
        for (int attempt = 0; attempt < 20; attempt++) {
            JsonObject result = c.query("hbm_block_info", params);
            if (!result.has("pending")) {
                result.add("at", params);
                return result;
            }
            c.waitTicks(5);
        }
        Expect.that(false, "hbm_block_info at " + x + "," + y + "," + z + " stayed pending");
        return null;
    }

    /** Reads the same cell again. */
    private static JsonObject info(Client c, JsonObject previous) {
        JsonObject at = previous.getAsJsonObject("at");
        return info(c, at.get("x").getAsInt(), at.get("y").getAsInt(), at.get("z").getAsInt());
    }

    private static void merge(Client c, JsonObject info, String nbt) {
        c.cmd("data merge block " + info.get("target").getAsString().replace(',', ' ') + " " + nbt);
        c.waitTicks(5);
    }

    private static JsonObject find(JsonObject info, String label, String unit) {
        JsonArray gauges = info.getAsJsonArray("gauges");
        for (JsonElement e : gauges) {
            JsonObject gauge = e.getAsJsonObject();
            if (gauge.get("label").getAsString().equals(label) && gauge.get("unit").getAsString().equals(unit)) {
                return gauge;
            }
        }
        return null;
    }

    private static JsonObject gauge(JsonObject info, String label, String unit) {
        JsonObject gauge = find(info, label, unit);
        Expect.that(gauge != null, "a " + label + " " + unit + " line on " + info.get("block_entity") + ": " + info);
        return gauge;
    }

    /** Looks at the south face of the reported cell from two and a half blocks away and screenshots the HUD. */
    private static void look(Client c, JsonObject info, String shot) {
        JsonObject at = info.getAsJsonObject("at");
        int x = at.get("x").getAsInt(), y = at.get("y").getAsInt(), z = at.get("z").getAsInt();
        c.teleport(x + 0.5, G, z - 2.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, y + 0.5, z - 0.02);
        c.waitTicks(20);
        c.screenshot(shot);
    }
}
