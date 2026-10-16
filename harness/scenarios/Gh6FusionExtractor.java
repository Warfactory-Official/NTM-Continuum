import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 6: a conveyor ejector under the fusion reactor crashed the server, because the torus block handed the
 * hopper lookup a block entity that was not a sided container. 1.7 exposes only the output slot (2) on every face, so
 * the ejector must pull the product and leave the blueprint slot (1) alone.
 */
public final class Gh6FusionExtractor implements Scenario {

    private static final int Y = 100, G = 101;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 22);
        c.cmd("fill -22 " + G + " -22 22 " + (Y + 8) + " 22 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.markLog();

        Fixtures.giveOnly(c, "hbm:fusion_torus", 1);
        c.teleport(0.5, G, -9.5);
        c.waitTicks(3);
        c.lookAt(0.5, Y + 0.98, -6.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(10);
        Expect.block(c, 0, G, 0, "hbm:fusion_torus");
        Expect.that(nbt(c, 0, G, 0).contains("ft3_max"), "the torus formed its core at 0 " + G + " 0: " + nbt(c, 0, G, 0));

        // the core has a port on its own down face; an ejector pulls from the side its output arrow names
        c.cmd("setblock 0 " + Y + " 0 hbm:crane_extractor[input=down,output=up]");
        c.cmd("setblock 0 " + (Y - 1) + " 0 minecraft:stone");
        c.cmd("item replace block 0 " + G + " 0 container.2 with minecraft:diamond 5");
        c.cmd("item replace block 0 " + G + " 0 container.1 with minecraft:emerald 3");
        c.waitTicks(100);

        String torus = nbt(c, 0, G, 0);
        String crane = nbt(c, 0, Y, 0);
        System.out.println("[Gh6] torus " + torus);
        System.out.println("[Gh6] extractor " + crane);
        Expect.that(crane.contains("minecraft:diamond"), "the ejector pulled the product from slot 2: " + crane);
        Expect.that(!crane.contains("minecraft:emerald") && torus.contains("minecraft:emerald"),
                "the blueprint slot is not exposed: " + torus);
        Expect.that(c.inWorld(), "the client is still in the world");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|ClassCastException|Ticking block entity");
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return (be.has("nbt") ? be.get("nbt").getAsString() : be.toString()).replaceAll("\\s+", "").replace("\\\"", "\"");
    }
}
