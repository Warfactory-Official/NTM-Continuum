import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * GitHub issue 151 with the reporter's world: join it, list Sable's sub-levels, then save and quit and rejoin several
 * times. The reporter's game aborted in Sable's native physics while it saved the overworld's sub-levels on quit. Needs
 * the world copied to the run's {@code saves} as {@code GH151_WORLD} (default {@code gh151}) and Sable
 * ({@code ORG_GRADLE_PROJECT_sable.run=true}, 1.21.1 nodes).
 */
public final class Gh151WorldQuit implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        String world = System.getenv().getOrDefault("GH151_WORLD", "gh151");
        int rounds = Integer.parseInt(System.getenv().getOrDefault("GH151_ROUNDS", "5"));
        if (!world.equals(c.currentWorld())) {
            c.leaveWorld(180_000);
            c.joinWorld(world, 240_000);
        }
        c.waitTicks(100);
        c.markLog();
        c.cmd("sable info @e");
        c.waitTicks(10);
        List<String> info = c.logSinceMark("");
        for (String line : info) System.out.println("[Gh151] " + c.node() + " " + line);
        c.screenshot("joined");
        for (int round = 1; round <= rounds; round++) {
            c.leaveWorld(180_000);
            Expect.that(c.ping() != null, "the client survived saving and quitting, round " + round);
            System.out.println("[Gh151] " + c.node() + " quit " + round + " survived");
            c.joinWorld(world, 240_000);
            c.waitTicks(100 + round * 40);
        }
        c.screenshot("last");
    }
}
