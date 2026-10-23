import dev.golem.harness.api.Client;
import dev.golem.harness.api.Scenario;

/**
 * Terrain and entities under the same GemRender spot: the mining helmet lights an armor stand in front of a wall in a
 * closed room at night, looked at head-on, from two yaws and in third person. The stand must take the beam where the
 * wall around it does; a stand lit as if it stood elsewhere means entity vertices reach the light shader in another frame.
 */
public final class LampLightsEntities implements Scenario {

    private static final int X = 1300;
    private static final int Y = 100;
    private static final int Z = 1340;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String forceload = (X - 8) + " " + (Z - 8) + " " + (X + 28) + " " + (Z + 28);
        try {
            c.cmd("forceload add " + forceload);
            c.cmd("gamemode creative");
            c.cmd("difficulty peaceful");
            c.cmd("time set 18000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("fill " + X + " " + Y + " " + Z + " " + (X + 20) + " " + (Y + 8) + " " + (Z + 20) + " minecraft:stone hollow");
            c.cmd("kill @e[type=minecraft:armor_stand]");
            c.cmd("summon minecraft:armor_stand " + (X + 10.5) + " " + (Y + 1) + " " + (Z + 10.5)
                    + " {NoGravity:1b,Rotation:[90f,0f]}");
            c.cmd("clear @s");
            c.cmd("item replace entity @s armor.head with hbm:no9");
            c.cmd("item replace entity @s weapon.mainhand with minecraft:stone");
            c.teleport(X + 3.5, Y + 1, Z + 10.5);
            c.setOption("camera", "first_person");
            c.lookAt(X + 10.5, Y + 2, Z + 10.5);
            c.waitTicks(100);
            c.screenshot("stand-center");
            c.look(-70, 10);
            c.waitTicks(10);
            c.screenshot("stand-yaw-left");
            c.look(-110, 10);
            c.waitTicks(10);
            c.screenshot("stand-yaw-right");
            c.lookAt(X + 10.5, Y + 2, Z + 10.5);
            c.setOption("camera", "third_person_back");
            c.waitTicks(10);
            c.screenshot("stand-third-person");
            c.setOption("camera", "first_person");
        } finally {
            c.cmd("kill @e[type=minecraft:armor_stand]");
            c.cmd("clear @s");
            c.cmd("time set 6000");
            c.cmd("forceload remove " + forceload);
        }
    }
}
