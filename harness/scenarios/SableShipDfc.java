import dev.golem.harness.api.Client;
import dev.golem.harness.api.Scenario;

/**
 * Player report: the DFC does not work on a Create Aeronautics ship. A DFC emitter firing into a receiver on an iron
 * deck, screenshotted before and after {@code /sable assemble}: the emitter, the receiver and the beam must still draw.
 * Needs Sable in the run ({@code ORG_GRADLE_PROJECT_sable.run=true}, 1.21.1 nodes).
 */
public final class SableShipDfc implements Scenario {

    private static final int Y = 100, D = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.key("escape");
        c.waitTicks(2);
        c.cmd("sable remove @e");
        c.waitTicks(5);
        c.cmd("fill -12 " + Y + " -12 12 " + Y + " 16 minecraft:stone");
        c.cmd("gamemode creative");
        c.cmd("fill -12 " + D + " -12 12 " + (D + 12) + " 16 minecraft:air");
        c.cmd("fill -3 " + D + " 6 3 " + D + " 8 minecraft:iron_block");
        c.cmd("setblock -2 " + (D + 1) + " 7 hbm:dfc_emitter[facing=east]{isOn:1b,watts:100,power:1000000000000L,"
                + "tank:64000,tank_type:\"hbm:cryogel\"}");
        c.cmd("setblock 2 " + (D + 1) + " 7 hbm:dfc_receiver[facing=west]{tank:64000,tank_type:\"hbm:cryogel\"}");
        c.teleport(0.5, D + 4, -1.5);
        c.lookAt(0.5, D + 1.5, 7.5);
        c.waitTicks(40);
        c.screenshot("10-before");

        c.cmd("sable assemble area -3 " + D + " 6 3 " + (D + 3) + " 8");
        c.waitTicks(60);
        c.teleport(0.5, D + 4, -1.5);
        c.lookAt(0.5, D + 1.5, 7.5);
        c.waitTicks(40);
        c.screenshot("20-on-ship");
    }
}
