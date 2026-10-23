import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * The mining helmet's lamp and the occult shotgun's flashlight in a closed stone room at night. With GemRender they are
 * spot lights ({@code lights_last_frame}); with {@code -Pgemrender.run=false} the 1.7 fallback writes block light at
 * the wall looked at and the light engine takes it back once the lamp is off. Compare {@code helmet} and
 * {@code flashlight} with {@code dark}.
 */
public final class LampLights implements Scenario {

    private static final int X = 1300;
    private static final int Y = 100;
    private static final int Z = 1300;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        String hideGui = c.setOption("hide_gui", "true");
        String forceload = (X - 8) + " " + (Z - 8) + " " + (X + 28) + " " + (Z + 28);
        try {
            c.cmd("forceload add " + forceload);
            c.cmd("gamemode creative");
            c.cmd("difficulty peaceful");
            c.cmd("time set 18000");
            c.cmd("gamerule advance_time false");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("fill " + X + " " + Y + " " + Z + " " + (X + 20) + " " + (Y + 8) + " " + (Z + 20) + " minecraft:stone hollow");
            c.cmd("clear @s");
            c.cmd("effect clear @s");
            c.teleport(X + 3.5, Y + 1, Z + 10.5);
            c.lookAt(X + 20, Y + 3, Z + 10.5);
            c.setOption("camera", "first_person");
            c.waitTicks(100);
            c.screenshot("dark");
            JsonObject dark = lamp(c);
            boolean gem = dark.get("shading").getAsBoolean();
            System.out.println("GemRender shading: " + gem + " " + dark);

            c.cmd("item replace entity @s armor.head with hbm:no9");
            c.waitTicks(40);
            JsonObject helmet = lamp(c);
            c.screenshot("helmet");
            System.out.println("helmet: " + helmet);
            Expect.that(helmet.get("helmet_lit").getAsBoolean(), "the worn helmet is lit: " + helmet);
            if (gem) {
                Expect.that(helmet.get("lights_last_frame").getAsInt() >= 1, "a spot light was submitted: " + helmet);
            } else {
                Expect.that(helmet.get("face_block_light").getAsInt() >= 10, "block light at the wall: " + helmet);
            }

            c.cmd("item replace entity @s armor.head with minecraft:air");
            c.waitTicks(30);
            JsonObject off = lamp(c);
            c.screenshot("helmet-off");
            System.out.println("helmet off: " + off);
            Expect.that(off.get("lights_last_frame").getAsInt() == 0, "no light without the helmet: " + off);
            if (!gem) {
                Expect.that(off.get("fallback_lit").getAsInt() == 0, "every lit block handed back: " + off);
                Expect.that(off.get("face_block_light").getAsInt() == 0, "the wall is dark again: " + off);
            }

            c.cmd("give @s hbm:gun_jshotgun");
            c.hotbar(1);
            c.waitTicks(10);
            c.use(2);
            c.waitTicks(40);
            JsonObject flash = lamp(c);
            c.screenshot("flashlight");
            System.out.println("flashlight: " + flash);
            Expect.that(flash.get("flashlight_on").getAsBoolean(), "right click turned the flashlight on: " + flash);
            if (gem) {
                Expect.that(flash.get("lights_last_frame").getAsInt() >= 1, "a spot light was submitted: " + flash);
            } else {
                Expect.that(flash.get("face_block_light").getAsInt() > 0, "block light at the wall: " + flash);
            }
            c.setOption("camera", "third_person_back");
            c.waitTicks(10);
            c.screenshot("flashlight-third-person");
            c.setOption("camera", "first_person");

            c.use(2);
            c.waitTicks(30);
            JsonObject flashOff = lamp(c);
            System.out.println("flashlight off: " + flashOff);
            Expect.that(!flashOff.get("flashlight_on").getAsBoolean(), "right click turned it off: " + flashOff);
            if (!gem) Expect.that(flashOff.get("fallback_lit").getAsInt() == 0, "every lit block handed back: " + flashOff);
            Expect.logCleanSinceMark(c, "(Exception|ERROR.*(hbm|golem|gemrender))");
        } finally {
            c.releaseAll();
            c.cmd("clear @s");
            c.cmd("time set 6000");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            c.cmd("forceload remove " + forceload);
        }
    }

    private static JsonObject lamp(Client c) {
        return c.query("hbm_lamp_lights", new JsonObject());
    }
}
