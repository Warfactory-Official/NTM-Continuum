import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Scenario;

/** GitHub issue 222: R reloads a flare gun from the inventory, and the loaded gun fires. */
public final class Gh222Reload implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.cmd("gamemode survival");
        c.cmd("clear @s");
        c.cmd("give @s hbm:gun_flaregun");
        c.cmd("give @s hbm:ammo_standard_g26_flare 4");
        c.hotbar(1);
        c.waitTicks(30);
        int before = ammo(c);
        c.key("r", 2);
        c.waitTicks(80);
        int after = ammo(c);
        System.out.println("[Gh222] flares in the inventory: " + before + " before R, " + after + " after");
        c.look(0, -60);
        c.waitTicks(5);
        c.markLog();
        c.attack();
        c.waitTicks(3);
        int flares = flares(c);
        System.out.println("[Gh222] projectiles after firing: " + flares);
        c.screenshot("after-fire");
        c.cmd("gamemode creative");
        if (after >= before) throw new AssertionError("R took no flare from the inventory: " + before + " -> " + after);
        if (flares == 0) throw new AssertionError("the reloaded flare gun fired nothing");
    }

    private static int ammo(Client c) {
        int n = 0;
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject slot = e.getAsJsonObject();
            if (slot.has("id") && slot.get("id").getAsString().equals("hbm:ammo_standard_g26_flare")) {
                n += slot.get("count").getAsInt();
            }
        }
        return n;
    }

    private static int flares(Client c) {
        JsonObject req = new JsonObject();
        req.addProperty("radius", 64);
        req.addProperty("server", true);
        JsonArray list = c.query("entities", req).getAsJsonArray("entities");
        int n = 0;
        if (list != null) for (JsonElement e : list) if (e.getAsJsonObject().get("type").getAsString().startsWith("hbm:")) n++;
        return n;
    }
}
