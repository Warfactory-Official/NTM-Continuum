import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * GitHub issue 169: an HBM tool played a block's break sound twice when it was the right tool. Stone is broken with a
 * vanilla pickaxe and with HBM pickaxes (a plain, an ability and an electric one); each break must start one sound.
 */
public final class Gh169BreakSound implements Scenario {

    private static final int Y = 100, G = 101;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 6);
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("gamemode creative");
        String previous = c.setOption("sound_capture", "true");
        try {
            for (String tool : new String[] {"minecraft:iron_pickaxe", "hbm:steel_pickaxe", "hbm:starmetal_pickaxe",
                    "hbm:elec_pickaxe"}) {
                c.cmd("setblock 0 " + G + " 2 minecraft:stone");
                Fixtures.giveOnly(c, tool, 1);
                c.teleport(0.5, G, 0.5);
                c.waitTicks(3);
                c.lookAt(0.5, G + 0.5, 2.5);
                c.waitTicks(3);
                c.markLog();
                c.attack(1);
                c.waitTicks(10);
                Expect.equal("minecraft:air", c.block(0, G, 2).get("block").getAsString(), tool + " broke the stone");
                List<String> sounds = c.logSinceMark("\\[harness-sound\\] id=minecraft:block\\.stone\\.break");
                System.out.println("[Gh169] " + c.node() + " " + tool + ": " + sounds.size() + " break sound(s)");
                Expect.equal(1, sounds.size(), tool + " break sounds");
            }
        } finally {
            c.setOption("sound_capture", previous);
            c.cmd("gamerule sendCommandFeedback true");
        }
        System.out.println("[Gh169] PASS");
    }
}
