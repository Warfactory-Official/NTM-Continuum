import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 162: looking up an HBM fluid in EMI found none of HBM's recipes, because HBM's slots held fluid icon items,
 * and EMI listed the fluid with its tank texture. The slots are EMI fluid stacks drawn as the 1.7 droplet now, and the
 * droplet takes the fluid's place in the list. Needs {@code ORG_GRADLE_PROJECT_emi.run=true harness up --rebuild}
 * (1.21.1 and 1.20.1).
 */
public final class Gh162EmiFluids implements Scenario {

    private static JsonObject emi(Client c, String... kv) {
        JsonObject p = new JsonObject();
        for (int i = 0; i + 1 < kv.length; i += 2) p.addProperty(kv[i], kv[i + 1]);
        return c.query("emi", p);
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(180_000L);
        c.waitTicks(40);
        JsonObject status = emi(c, "op", "status");
        for (int i = 0; i < 60 && status.get("ids").toString().contains("hbm:centrifuge=0"); i++) {
            c.waitTicks(40);
            status = emi(c, "op", "status");
        }

        JsonObject index = emi(c, "op", "index", "namespace", "hbm", "item", "hbm:fluid_icon");
        System.out.println("[Gh162] " + c.node() + " index: " + index);

        JsonObject acid = emi(c, "op", "lookup", "fluid", "hbm:sulfuric_acid");
        JsonObject hotoil = emi(c, "op", "lookup", "fluid", "hbm:hotoil");
        JsonObject water = emi(c, "op", "lookup", "fluid", "minecraft:water");
        System.out.println("[Gh162] " + c.node() + " sulfuric acid: " + acid);
        System.out.println("[Gh162] " + c.node() + " hot oil: " + hotoil);
        System.out.println("[Gh162] " + c.node() + " water: " + water);
        c.key("e");
        c.waitTicks(10);
        emi(c, "op", "search", "text", "crude oil");
        c.waitTicks(20);
        c.screenshot("index-crude-oil");
        emi(c, "op", "lookup", "fluid", "hbm:hotoil", "show", "uses");
        c.waitTicks(20);
        c.screenshot("hotoil-uses");
        emi(c, "op", "lookup", "fluid", "hbm:sulfuric_acid", "show", "recipes");
        c.waitTicks(20);
        c.screenshot("sulfuric-acid-recipes");
        for (int i = 0; i < 4 && c.screenClass() != null; i++) {
            c.key("escape");
            c.waitTicks(10);
        }
        emi(c, "op", "search", "text", "");

        JsonObject fluids = index.getAsJsonObject("fluids");
        int icons = fluids.has("IconFluidStack") ? fluids.get("IconFluidStack").getAsInt() : 0;
        Expect.that(icons >= 150, "HBM's fluids are listed as droplets: " + index);
        // the placed world fluids (hbm:mud_fluid ...) are fluids of their own, with their own icon-less buckets
        boolean worldOnly = true;
        for (var id : index.getAsJsonArray("plain")) worldOnly &= id.getAsString().endsWith("_fluid");
        Expect.that(worldOnly, "no HBM network fluid keeps EMI's tank-texture entry: " + index);
        Expect.that(index.has("saved") && index.get("saved").getAsString().equals("IconFluidStack true"),
                "a favourite droplet reads back as the droplet: " + index);
        Expect.that(index.get("items").getAsInt() == 0, "no droplet item is listed twice: " + index);
        Expect.that(acid.getAsJsonObject("recipes").has("hbm:chemical_plant"), "sulfuric acid's recipes show the chemical plant: " + acid);
        Expect.that(acid.getAsJsonObject("uses").keySet().stream().anyMatch(id -> id.startsWith("hbm:")),
                "sulfuric acid's uses show HBM machines: " + acid);
        Expect.that(hotoil.getAsJsonObject("uses").has("hbm:refinery"), "hot oil's uses show the refinery: " + hotoil);
        Expect.that(hotoil.getAsJsonObject("recipes").keySet().stream().anyMatch(id -> id.startsWith("hbm:")),
                "hot oil's recipes show an HBM machine: " + hotoil);
        Expect.that(water.getAsJsonObject("uses").keySet().stream().anyMatch(id -> id.startsWith("hbm:")),
                "water's uses show HBM machines: " + water);
    }
}
