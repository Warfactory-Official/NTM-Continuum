import dev.golem.harness.api.*;

/**
 * A DE save converted on load (1.21.1 NeoForge only, where the importer exists): the fixture command copies the harness
 * world, turns the copy into a DE 1.0.0-beta.12 save and the client opens it. Checks the converted blocks, block
 * entities, rebuilt multiblocks, refunds, DE components and the player, opens converted machines, then rejoins to prove
 * the second start does nothing.
 */
public class DeImportWorld implements Scenario {
    public void run(Client c) throws Exception {
        if (!c.node().startsWith("1.21.1-neoforge")) return;
        String world = "de_fixture_" + System.currentTimeMillis();
        Fixtures.platform(c, 8, 100, 8, 9);
        c.cmd("hbm deimport_fixture " + world + " 2 101 2");
        c.waitTicks(20);
        Expect.logMatches(c, "DE fixture written", 200);

        c.leaveWorld(60000);
        c.markLog();
        c.joinWorld(world, 180000);
        c.waitInWorld(180000);
        c.waitTicks(40);
        Expect.logMatches(c, "DE import: converting", 4000);
        // the live phase finishes over a few server ticks after the join
        for (int i = 0; i < 120 && c.logSinceMark("DE import done").isEmpty(); i++) c.waitTicks(10);
        String done = Expect.logMatches(c, "DE import done", 4000);
        System.out.println(done);
        Expect.that(done.contains("multiblock cells blocked 0"), "every DE multiblock rebuilt whole: " + done);

        String centrifuge = c.block(2, 101, 2).toString();
        Expect.that(centrifuge.contains("hbm:machine_centrifuge"), "centrifuge core: " + centrifuge);
        String above = c.block(2, 102, 2).toString();
        Expect.that(!above.contains("minecraft:air"), "centrifuge cell rebuilt above the core: " + above);
        String cbe = c.blockEntity(2, 101, 2).toString();
        Expect.that(cbe.contains("canned_conserve_ass") && cbe.contains("battery_lead"), "centrifuge stacks: " + cbe);
        String tank = c.blockEntity(7, 101, 3).toString();
        Expect.that(tank.contains("16000") && tank.contains("hbm:oil"), "fluid tank keeps 16000 mB oil: " + tank);
        Expect.that(c.block(12, 101, 12).toString().contains("hbm:machine_zirnox"), "ZIRNOX renamed");
        String duct = c.blockEntity(2, 101, 6).toString();
        Expect.that(duct.contains("hbm:oil"), "duct keeps its fluid: " + duct);
        Expect.that(c.block(2, 101, 8).toString().contains("hbm:concrete_colored"), "concrete split");
        Expect.that(c.block(3, 101, 8).toString().contains("hbm:lamp_tritium_blue_on"), "lit lamp");
        Expect.that(c.block(4, 101, 8).toString().contains("hbm:sellafield_3"), "sellafield level");
        String refund = c.blockEntity(2, 101, 11).toString();
        Expect.that(refund.contains("hbm:ingot_steel"), "steam turbine refunded into a chest: " + refund);
        String chest = c.blockEntity(5, 101, 11).toString();
        Expect.that(chest.contains("crashed_bomb_nuke") && chest.contains("machine_capacitor_copper"), "chest stacks: " + chest);
        Expect.that(chest.contains("canister_full") && chest.contains("hbm:diesel"), "filled canister: " + chest);
        Expect.that(chest.contains("ingot_uranium") && !chest.contains("locked_contents"), "sealed crate opened: " + chest);
        Expect.that(done.contains("radiation pockets 2"), "DE radiation decoded: " + done);
        c.markLog();
        c.cmd("hbm rad get 25 96 0");
        c.waitTicks(5);
        String rad = Expect.logMatches(c, "Radiation at .* RAD/s", 50);
        double value = Double.parseDouble(rad.replaceAll(".*= ([0-9.Ee-]+) RAD/s.*", "$1"));
        Expect.that(value > 1.0, "DE's radiation is in Continuum's pocket: " + rad);

        String inventory = c.inventory().toString();
        Expect.that(inventory.contains("hbm:apple_lead") && inventory.contains("hbm:cell"), "player stacks: " + inventory);

        c.teleport(4.5, 101, -3.5);
        c.lookAt(6.0, 102.0, 4.0);
        c.screenshot("converted-machines");
        c.teleport(2.5, 101, -0.5);
        Fixtures.interact(c, 2, 101, 2);
        c.waitScreen("Centrifuge", 10000);
        c.screenshot("centrifuge-gui");
        c.key("escape");
        c.teleport(7.5, 101, -1.5);
        Fixtures.interact(c, 7, 101, 3);
        c.waitTicks(10);
        c.screenshot("fluid-tank-gui");
        c.key("escape");

        c.leaveWorld(60000);
        c.markLog();
        c.joinWorld(world, 180000);
        c.waitInWorld(180000);
        c.waitTicks(40);
        Expect.logCleanSinceMark(c, "DE import");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]].*(hbm|deimport|Skipping|invalid item)");
    }
}
