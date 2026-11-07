import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 134. A screwdriver clicked on a crane sets the input side without sneaking and the output side sneaking,
 * and an empty hand still opens the GUI. The same fault's siblings: the wand on a crane and a router, the screwdriver on a
 * compressor tube.
 */
public final class Gh134CraneScrewdriver implements Scenario {

    private static final int Y = 100, G = Y + 1;
    private static final String[] CRANES = {"crane_inserter", "crane_extractor", "crane_grabber", "crane_boxer", "crane_unboxer"};

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(180_000L);
        Fixtures.platform(c, 0, Y, 0, 8);
        c.cmd("gamemode creative");
        c.cmd("gamerule sendCommandFeedback false");

        for (String crane : CRANES) crane(c, crane);
        wandOn(c, "crane_inserter[input=north,output=south]");
        wandOn(c, "crane_router");
        tube(c, "pneumatic_tube");
        tube(c, "pneumatic_tube_paintable");

        c.cmd("gamerule sendCommandFeedback true");
    }

    /** Clicks the block's top from straight above, so the clicked face is up. */
    private static void clickTop(Client c) {
        c.teleport(0.5, G + 2, 0.5);
        c.lookAt(0.5, G + 1, 0.5);
        Fixtures.waitForTarget(c, 0, G, 0);
        c.use(2);
        c.waitTicks(8);
    }

    private static void noMenu(Client c, String what) {
        Expect.that(!c.screen().get("open").getAsBoolean(), what + " opens no GUI, got " + c.screen());
    }

    private static void crane(Client c, String id) {
        Fixtures.setBlock(c, 0, G, 0, "hbm:" + id + "[input=north,output=south]");
        Fixtures.giveOnly(c, "hbm:screwdriver", 1);
        c.hotbar(1);
        c.waitTicks(5);

        clickTop(c);
        noMenu(c, "the screwdriver on " + id);
        String state = c.block(0, G, 0).toString();
        System.out.println("[Gh134] " + id + " screwdriver: " + state);
        Expect.that(state.contains("input=up"), id + ": the clicked face became the input: " + state);
        c.screenshot("gh134-" + id + "-input");

        Fixtures.setBlock(c, 0, G, 0, "hbm:" + id + "[input=north,output=south]");
        c.keyDown("key.sneak");
        c.waitTicks(5);
        clickTop(c);
        c.keyUp("key.sneak");
        c.waitTicks(5);
        noMenu(c, "the sneaking screwdriver on " + id);
        state = c.block(0, G, 0).toString();
        Expect.that(state.contains("output=up") && state.contains("input=north"), id + ": sneaking sets the output: " + state);

        c.cmd("clear @s");
        c.waitTicks(5);
        clickTop(c);
        Expect.that(c.screen().get("open").getAsBoolean(), id + ": an empty hand opens the GUI");
        c.screenshot("gh134-" + id + "-gui");
        c.key("escape");
        c.waitTicks(5);
    }

    private static void wandOn(Client c, String block) {
        Fixtures.setBlock(c, 0, G, 0, "hbm:" + block);
        Fixtures.giveOnly(c, "hbm:conveyor_wand_regular", 1);
        c.hotbar(1);
        c.waitTicks(5);
        clickTop(c);
        noMenu(c, "the conveyor wand on " + block);
        c.cmd("clear @s");
    }

    /** The first click makes a chest-side tube a compressor, the second turns a compressor's insertion side. */
    private static void tube(Client c, String id) {
        Fixtures.setBlock(c, 1, G, 0, "minecraft:chest");
        Fixtures.setBlock(c, 0, G, 0, "hbm:" + id);
        Fixtures.giveOnly(c, "hbm:screwdriver", 1);
        c.hotbar(1);
        c.waitTicks(5);
        clickTop(c);
        noMenu(c, "the first screwdriver click on " + id);
        String first = c.blockEntity(0, G, 0).toString();
        clickTop(c);
        noMenu(c, "the second screwdriver click on " + id);
        String second = c.blockEntity(0, G, 0).toString();
        System.out.println("[Gh134] " + id + ": " + first + " then " + second);
        Expect.that(!first.equals(second), id + ": the second click turned the insertion side");

        c.cmd("clear @s");
        c.waitTicks(5);
        Fixtures.setBlock(c, 1, G, 0, "minecraft:air");
        c.cmd("setblock 0 " + G + " 0 minecraft:air");
    }
}
