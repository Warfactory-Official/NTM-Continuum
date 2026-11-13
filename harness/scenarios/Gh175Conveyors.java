import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 175, conveyors against 1.7. A wand run started on the side of an ejector starts beside that side, not on
 * top of the ejector. A box rides the belt as 1.7's {@code RenderMovingPackage} draws it (the wooden crate, twice the
 * framed block size), and a moving stack draws 1.7's mini-copies for its count.
 */
public final class Gh175Conveyors implements Scenario {

    private static final int X = 1830;
    private static final int Y = 100;
    private static final int Z = 1830;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 14);
        c.cmd("fill " + (X - 14) + " " + (Y + 1) + " " + (Z - 14) + " " + (X + 14) + " " + (Y + 6) + " " + (Z + 14) + " minecraft:air");
        c.cmd("gamemode creative");
        c.cmd("time set 6000");
        List<String> wrong = new ArrayList<>();
        try {
            for (String side : new String[]{"north", "west"}) placement(c, side, wrong);
            render(c);
        } finally {
            c.cmd("kill @e[type=hbm:entity_c_item]");
            c.cmd("kill @e[type=hbm:entity_c_package]");
        }
        Expect.that(wrong.isEmpty(), "1.7 conveyors: " + wrong);
        Expect.logCleanSinceMark(c, "/(Exception|ERROR|FATAL)[]]?");
    }

    /** Clicks the ejector's {@code side} face, then the floor four blocks out from it. */
    private static void placement(Client c, String side, List<String> wrong) throws Exception {
        c.cmd("fill " + (X - 8) + " " + (Y + 1) + " " + (Z - 8) + " " + (X + 8) + " " + (Y + 6) + " " + (Z + 8) + " minecraft:air");
        Fixtures.setBlock(c, X, Y + 1, Z, "hbm:crane_extractor[input=south,output=north]");
        Fixtures.giveOnly(c, "hbm:conveyor_wand_regular", 1);
        c.hotbar(1);
        int dx = side.equals("west") ? -1 : 0;
        int dz = side.equals("north") ? -1 : 0;
        c.teleport(X + 0.5 + dx * 3, Y + 1, Z + 0.5 + dz * 3);
        c.waitTicks(3);
        c.lookAt(X + 0.5 + dx * 0.5, Y + 1.5, Z + 0.5 + dz * 0.5);
        c.waitTicks(3);
        JsonObject aim = c.player();
        System.out.println("[Gh175] " + c.node() + " " + side + " aim " + aim.get("target") + " "
                + aim.get("target_x") + " " + aim.get("target_y") + " " + aim.get("target_z"));
        c.use(2);
        c.waitTicks(4);
        // the run's end: the floor four blocks out from the clicked side, its top face
        int ex = X + dx * 4 + (side.equals("north") ? 3 : 0);
        int ez = Z + dz * 4 + (side.equals("west") ? 3 : 0);
        c.teleport(ex + 0.5 - dx * 2, Y + 1, ez + 0.5 - dz * 2);
        c.waitTicks(3);
        c.lookAt(ex + 0.5, Y + 0.98, ez + 0.5);
        c.waitTicks(10);
        c.screenshot("ghost-" + side);
        c.use(2);
        c.waitTicks(10);
        String beside = c.block(X + dx, Y + 1, Z + dz).toString();
        String above = c.block(X, Y + 2, Z).toString();
        System.out.println("[Gh175] " + c.node() + " " + side + " beside " + beside + " above " + above);
        c.teleport(X + 0.5 + dx * 4 - 3, Y + 4, Z + 0.5 + dz * 4 - 3);
        c.lookAt(X + 0.5 + dx * 2, Y + 1, Z + 0.5 + dz * 2);
        c.waitTicks(5);
        c.screenshot("built-" + side);
        if (!beside.contains("conveyor")) wrong.add(side + " side: no belt beside the clicked face (" + beside + ")");
        if (!above.contains("minecraft:air")) wrong.add(side + " side: the run started on top of the ejector (" + above + ")");
    }

    private static void render(Client c) throws Exception {
        c.cmd("fill " + (X - 8) + " " + (Y + 1) + " " + (Z - 8) + " " + (X + 8) + " " + (Y + 6) + " " + (Z + 8) + " minecraft:air");
        String belt = "hbm:conveyor[facing=east]";
        c.cmd("fill " + (X - 6) + " " + (Y + 1) + " " + Z + " " + (X + 6) + " " + (Y + 1) + " " + Z + " " + belt);
        c.cmd("fill " + (X - 6) + " " + (Y + 1) + " " + (Z + 2) + " " + (X + 6) + " " + (Y + 1) + " " + (Z + 2) + " " + belt);
        c.cmd("setblock " + (X + 7) + " " + (Y + 1) + " " + Z + " minecraft:chest");
        c.cmd("setblock " + (X + 7) + " " + (Y + 1) + " " + (Z + 2) + " minecraft:chest");
        c.teleport(X - 3, Y + 3, Z - 1.5);
        c.lookAt(X - 3.5, Y + 1.2, Z + 1.5);
        c.waitTicks(5);
        String hide = c.setOption("hide_gui", "true");
        try {
            int[] counts = {1, 2, 6, 21, 41, 64};
            for (int i = 0; i < counts.length; i++) {
                c.cmd("summon hbm:entity_c_item " + (X - 6 + i * 0.9) + " " + (Y + 1.25) + " " + (Z + 0.5)
                        + " {Item:{id:\"minecraft:gray_terracotta\",count:" + counts[i] + ",Count:" + counts[i] + "b}}");
                c.cmd("summon hbm:entity_c_item " + (X - 6 + i * 0.9) + " " + (Y + 1.25) + " " + (Z + 2.5)
                        + " {Item:{id:\"minecraft:coal\",count:" + counts[i] + ",Count:" + counts[i] + "b}}");
            }
            c.cmd("summon hbm:entity_c_package " + (X + 0.8) + " " + (Y + 1.25) + " " + (Z + 0.5));
            c.waitTicks(3);
            c.screenshot("belt-stacks");
            c.teleport(X - 3, Y + 1.2, Z - 2.5);
            c.lookAt(X - 3.5, Y + 1.3, Z + 1.5);
            c.waitTicks(3);
            c.screenshot("belt-stacks-side");
            c.teleport(X + 1.5, Y + 2.2, Z - 1.5);
            c.lookAt(X + 1.0, Y + 1.3, Z + 0.5);
            c.waitTicks(2);
            c.screenshot("box");
        } finally {
            if (hide != null) c.setOption("hide_gui", hide);
        }
    }
}
