import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** GitHub issue 171, cranes: 1.7's ejector reaches a centrifuge at its core, not at the cells above (no proxy there). */
public final class Gh171CraneCells implements Scenario {

    private static final int Y = 100, G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(180_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 8);
        c.cmd("gamemode creative");
        c.cmd("fill -6 " + G + " -6 6 " + (G + 6) + " 6 minecraft:air");
        c.cmd("kill @e[type=item]");
        Fixtures.placeOnTop(c, "hbm:machine_centrifuge", 0, Y, 0);
        c.waitTicks(5);
        Expect.that(c.blockEntity(0, G, 0).toString().contains("TileEntityMachineCentrifuge"),
                "the centrifuge core is at 0 " + G + " 0: " + c.block(0, G, 0));
        c.cmd("item replace block 0 " + G + " 0 container.2 with minecraft:cobblestone 16");

        place(c, 1, G + 1);
        c.waitTicks(80);
        int upper = count(c);
        System.out.println("[Gh171crane] " + c.node() + " after the upper ejector " + upper);
        Expect.that(upper == 16, "an ejector at the centrifuge's second cell pulled " + (16 - upper) + " items; 1.7 has no inventory there");

        c.cmd("setblock 1 " + (G + 1) + " 0 minecraft:air");
        place(c, 1, G);
        c.waitTicks(80);
        int core = count(c);
        System.out.println("[Gh171crane] " + c.node() + " after the core ejector " + core);
        c.teleport(4.5, G + 3, -3.5);
        c.lookAt(1.5, G, 0.5);
        c.waitTicks(5);
        c.screenshot("gh171-crane");
        Expect.that(core < 16, "an ejector at the centrifuge's core pulled nothing");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }

    private static void place(Client c, int x, int y) {
        c.cmd("setblock " + x + " " + y + " 0 hbm:crane_extractor[input=east,output=west]");
        c.cmd("fill " + (x + 1) + " " + y + " 0 " + (x + 4) + " " + y + " 0 hbm:conveyor[facing=east]");
        c.cmd("setblock " + (x + 5) + " " + y + " 0 minecraft:chest");
        if (y > G) c.cmd("fill " + (x + 1) + " " + (y - 1) + " 0 " + (x + 5) + " " + (y - 1) + " 0 minecraft:stone");
    }

    private static final Pattern STACK = Pattern.compile("\\{[^{}]*minecraft:cobblestone[^{}]*\\}");
    private static final Pattern COUNT = Pattern.compile("[Cc]ount:(\\d+)");

    private static int count(Client c) {
        JsonObject be = c.blockEntity(0, G, 0);
        String nbt = be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
        int total = 0;
        Matcher stack = STACK.matcher(nbt);
        while (stack.find()) {
            Matcher count = COUNT.matcher(stack.group());
            total += count.find() ? Integer.parseInt(count.group(1)) : 1;
        }
        return total;
    }
}
