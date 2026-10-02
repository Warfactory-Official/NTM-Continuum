import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * icon-grenade (2026-10-01): the universal grenade's 1.7 ItemRenderGrenade / RenderGrenadeUniversal port. One stack per
 * shell (frag, stick, tech, nuke, each with a different filling and fuze so every tint pass shows) in the hotbar, then
 * per shell: first person during the equip animation and after it, third person front, and the same shell thrown
 * (S15 fuze, killed long before it fires): just after release and lying where it landed, the camera moved next to it.
 * Finally a dropped frag on the ground.
 */
public final class GrenadeRender implements Scenario {

    private static final String[][] HELD = {
            {"frag", "he", "s3"}, {"stick", "demo", "s7"}, {"tech", "emp", "airburst"}, {"nuke", "nuclear", "impact"}};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, 100, 0, 24);
        c.cmd("kill @e[type=hbm:grenade_universal]");
        for (String[] g : HELD) c.cmd("give @s " + stack(c, g[0], g[1], g[2]));
        c.hotbar(9);
        c.look(0, 10);
        c.waitTicks(10);
        c.screenshot("00-hotbar");

        for (int i = 0; i < HELD.length; i++) {
            String name = HELD[i][0];
            c.hotbar(i + 1);
            c.waitTicks(10); // 500 ms into the equip animation: body moving, ring still on
            c.screenshot(String.format("%02d-%s-first-anim", i + 1, name));
            c.waitTicks(50);
            c.screenshot(String.format("%02d-%s-first-idle", i + 1, name));
            c.key("f5");
            c.key("f5");
            c.waitTicks(4);
            c.screenshot(String.format("%02d-%s-third-front", i + 1, name));
            c.key("f5");
            c.waitTicks(2);
        }

        // thrown: a long fuze in the same slot, so the shell's draw time is passed while it is held
        for (int i = 0; i < HELD.length; i++) {
            String name = HELD[i][0];
            c.cmd("clear");
            c.teleport(0.5, 101, -18.5);
            c.look(0, -15);
            c.cmd("give @s " + stack(c, name, HELD[i][1], "s15"));
            c.hotbar(1);
            c.waitTicks(60);
            c.use();
            c.waitTicks(3);
            c.screenshot(String.format("%02d-%s-thrown-flight", i + 5, name));
            c.waitTicks(60);
            JsonObject grenade = null;
            for (JsonElement e : c.entities(64)) {
                JsonObject o = e.getAsJsonObject();
                if (o.has("type") && o.get("type").getAsString().endsWith("grenade_universal")) grenade = o;
            }
            Expect.that(grenade != null, "a thrown " + name + " grenade exists");
            double x = grenade.get("x").getAsDouble(), y = grenade.get("y").getAsDouble(), z = grenade.get("z").getAsDouble();
            c.teleport(x + 1.2, y, z - 0.9);
            c.lookAt(x, y + 0.05, z);
            c.waitTicks(6);
            c.screenshot(String.format("%02d-%s-thrown-landed", i + 5, name));
            c.cmd("kill @e[type=hbm:grenade_universal]");
        }

        // dropped
        c.cmd("clear");
        c.teleport(0.5, 101, 0.5);
        c.look(0, 0);
        c.cmd("give @s " + stack(c, "frag", "he", "s3"));
        c.hotbar(1);
        c.waitTicks(4);
        c.key("q");
        c.waitTicks(40);
        JsonObject item = null;
        for (JsonElement e : c.entities(32)) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("type") && o.get("type").getAsString().endsWith(":item")) item = o;
        }
        Expect.that(item != null, "the dropped grenade item exists");
        double ix = item.get("x").getAsDouble(), iy = item.get("y").getAsDouble(), iz = item.get("z").getAsDouble();
        c.teleport(ix + 2.4, iy, iz - 2.0); // outside pickup range
        c.lookAt(ix, iy + 0.15, iz);
        c.waitTicks(6);
        c.screenshot("09-frag-dropped");
        c.cmd("kill @e[type=item]");
        c.cmd("clear");
    }

    /** The give argument for a grenade stack: an item component from 1.20.5 on, the stack's tag before. */
    private static String stack(Client c, String shell, String filling, String fuze) {
        String data = "{shell:\"" + shell + "\",filling:\"" + filling + "\",fuze:\"" + fuze + "\"}";
        if (c.node().startsWith("1.20.1")) return "hbm:grenade_universal{\"hbm:grenade\":" + data + "} 1";
        return "hbm:grenade_universal[hbm:grenade=" + data + "] 1";
    }
}
