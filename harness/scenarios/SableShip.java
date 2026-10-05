import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * An HBM machine on a Sable sub-level (Create Aeronautics' engine), used the way a player does: a centrifuge placed
 * by hand on an iron deck, the deck and machine assembled into a sub-level with Sable's own {@code /sable assemble},
 * then the machine right-clicked where it is seen. Its menu must open and stay open (the reach check is measured in
 * the world, not at the plot where Sable keeps the blocks), and the machine's blocks must have left the world grid.
 *
 * <p>1.21.1 NeoForge only, and needs Sable in the run: start the client with
 * {@code ORG_GRADLE_PROJECT_sable.run=true} in the environment and {@code up --rebuild --nodes 1.21.1-neoforge}.
 * The GameTests ({@code hbm:sable_*}) cover inventories, cell indexes, cables, pylons, missiles and the radar
 * through Sable's assembly and disassembly; this covers a placed machine, a real client and its menu.
 */
public class SableShip implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    @Override
    public void run(Client c) throws Exception {
        // a sub-level left by an earlier run sits where the deck goes and is not cleared by the platform's fill
        c.key("escape");
        c.waitTicks(2);
        c.cmd("sable remove @e");
        c.waitTicks(5);
        // marked after the cleanup: removing an earlier run's ship makes Sable's client log an ERROR for movement
        // packets still in flight for it
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 8);

        // an iron deck, and a centrifuge placed on it by hand (setPlacedBy builds the whole multiblock)
        c.cmd("fill " + (X - 2) + " " + (Y + 1) + " " + (Z + 3) + " " + (X + 2) + " " + (Y + 1) + " " + (Z + 7)
                + " minecraft:iron_block");
        c.waitTicks(2);
        JsonObject placed = Fixtures.placeOnTop(c, "hbm:machine_centrifuge", X, Y + 1, Z + 5);
        Expect.equal("hbm:machine_centrifuge", placed.get("block").getAsString(), "centrifuge placed by hand on the deck");
        c.waitTicks(10);
        c.teleport(X + 0.5, Y + 1, Z - 2.5);
        c.look(0, 15);
        c.waitTicks(5);
        c.screenshot("10-before-assembly");

        // Sable's own assembly of the deck and everything on it
        c.cmd("sable assemble area " + (X - 2) + " " + (Y + 1) + " " + (Z + 3) + " " + (X + 2) + " " + (Y + 8) + " " + (Z + 7));
        c.waitTicks(40);
        JsonObject left = c.block(X, Y + 2, Z + 5);
        Expect.equal("minecraft:air", left.get("block").getAsString(),
                "the centrifuge's world-grid block is empty once Sable carries it");
        c.screenshot("20-assembled");

        // the machine as the player sees it: walk up to it (within vanilla reach), right-click it, the menu opens and the
        // server keeps it open
        c.teleport(X + 0.5, Y + 1, Z + 1.5);
        c.waitTicks(5);
        Fixtures.interact(c, X, Y + 2, Z + 5);
        c.waitScreen("Centrifuge", 5000);
        Expect.screenStaysOpen(c, "Centrifuge", 20);
        c.screenshot("30-menu-on-sub-level");
        c.key("escape");
        c.waitTicks(5);

        // Placement previews aimed at the deck. Its blocks now live in Sable's plot, far from where they are drawn; the
        // previews must be drawn on the deck (look at the screenshots: the lines/ghosts sit on the iron blocks).
        double top = Y + 1.98;
        Fixtures.giveOnly(c, "hbm:machine_centrifuge", 1);
        c.teleport(X + 0.5, Y + 1, Z + 1.5);
        c.waitTicks(3);
        c.lookAt(X - 1.5, top, Z + 3.5);
        c.waitTicks(5);
        c.screenshot("40-placement-highlight-on-deck");

        Fixtures.giveOnly(c, "hbm:conveyor_wand_regular", 64);
        c.lookAt(X - 1.5, top, Z + 3.5);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(4);
        c.lookAt(X + 1.5, top, Z + 3.5);
        c.waitTicks(5);
        c.screenshot("41-conveyor-ghost-on-deck");

        c.cmd("clear");
        c.cmd("give @s hbm:rebar_placer 1");
        c.cmd("give @s hbm:rebar 64");
        c.waitTicks(3);
        c.hotbar(1);
        c.lookAt(X - 1.5, top, Z + 3.5);
        c.waitTicks(2);
        c.use(1);
        c.waitTicks(3);
        c.lookAt(X + 1.5, top, Z + 4.5);
        c.waitTicks(5);
        c.screenshot("42-rebar-box-on-deck");

        // The same with the ship tilted 30 degrees (Sable reads "30 0" here as a pitch) and held in the air: the previews
        // must tilt with it.
        c.cmd("sable paused true");
        c.cmd("sable teleport @e " + (X + 0.5) + " " + (Y + 3.5) + " " + (Z + 5.5) + " 30 0");
        c.waitTicks(20);
        c.teleport(X + 0.5, Y + 5, Z + 0.5);
        c.waitTicks(3);
        Fixtures.giveOnly(c, "hbm:machine_centrifuge", 1);
        c.lookAt(X - 0.5, Y + 3.2, Z + 4.0);
        c.waitTicks(5);
        c.screenshot("43-placement-highlight-tilted");
        Fixtures.giveOnly(c, "hbm:conveyor_wand_regular", 64);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
        c.lookAt(X + 1.5, Y + 3.2, Z + 4.5);
        c.waitTicks(5);
        c.screenshot("44-conveyor-ghost-tilted");
        c.cmd("sable paused false");
        c.cmd("clear");
        c.waitTicks(2);

        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }
}
