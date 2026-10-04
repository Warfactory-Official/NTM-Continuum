import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Fix wave phase 5 (History section 19ej-5): fired through the real input path.
 *
 * <ul>
 *   <li>Liberator: one trigger pull fires all four barrels (1.7 {@code rounds(4)}); with 12 gauge buckshot (8 pellets)
 *       that is 32 projectiles on the server at once, where the old port fired 8.</li>
 *   <li>Airdrop flare: 40 ticks after firing, a C-130 is spawned (1.7 {@code LAMBDA_SPAWN_C130_SUPPLIESS}).</li>
 *   <li>Flamer: the client flame trail of the diesel round (1.7 {@code LAMBDA_FIRE}), photographed mid-burst.</li>
 * </ul>
 */
public final class Fix5Guns implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 12);
        c.cmd("fill -12 " + (Y + 1) + " -12 12 " + (Y + 12) + " 12 minecraft:air");
        c.cmd("difficulty peaceful");
        c.cmd("gamerule doMobSpawning false");
        c.cmd("time set 18000");
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("kill @e[type=hbm:c130]");
        c.waitTicks(2);

        // --- Liberator: 4 rounds x 8 pellets ---
        load(c, "hbm:gun_liberator", "hbm:ammo_standard_g12", 8, 140);
        c.look(0, -90);
        c.waitTicks(5);
        c.attack();
        c.waitTicks(1);
        int pellets = count(c, "hbm:entity_bullet_mk4", 400);
        Expect.that(pellets == 32, "Liberator fires 4 barrels x 8 pellets = 32 projectiles in one pull, saw " + pellets);

        // --- Airdrop flare: C-130 after 40 ticks ---
        c.cmd("kill @e[type=hbm:entity_bullet_mk4]");
        load(c, "hbm:gun_flaregun", "hbm:ammo_standard_g26_flare_supply", 2, 80);
        c.look(0, -60);
        c.waitTicks(5);
        c.attack();
        c.waitTicks(60);
        int planes = count(c, "hbm:c130", 1000);
        Expect.that(planes == 1, "the supply flare called exactly one C-130 after 40 ticks, saw " + planes);
        c.cmd("kill @e[type=hbm:c130]");

        // --- Flamer: diesel trail on the client ---
        load(c, "hbm:gun_flamer", "hbm:ammo_standard_flame_diesel", 1, 140);
        c.look(0, 10);
        c.waitTicks(5);
        c.keyDown("mouse.left");
        c.waitTicks(12);
        c.screenshot("flamer-diesel-trail");
        c.waitTicks(6);
        c.screenshot("flamer-diesel-trail-2");
        c.keyUp("mouse.left");
        c.releaseAll();

        Expect.logCleanSinceMark(c, "(Exception|ERROR.*hbm)");
    }

    /** Clears the inventory, gives the gun in slot 0 and its ammo, then reloads with R and waits for the reload. */
    private static void load(Client c, String gun, String ammo, int ammoCount, int reloadTicks) {
        c.cmd("clear @s");
        c.cmd("give @s " + gun);
        c.cmd("give @s " + ammo + " " + ammoCount);
        c.hotbar(1);
        c.waitTicks(30); // draw
        c.key("r", 2);
        c.waitTicks(reloadTicks);
    }

    private static int count(Client c, String type, int radius) {
        JsonObject req = new JsonObject();
        req.addProperty("radius", radius);
        req.addProperty("type", type);
        req.addProperty("server", true);
        JsonObject res = c.query("entities", req);
        JsonArray list = res.getAsJsonArray("entities");
        int n = 0;
        if (list != null) for (JsonElement e : list) if (type.equals(e.getAsJsonObject().get("type").getAsString())) n++;
        return n;
    }
}
