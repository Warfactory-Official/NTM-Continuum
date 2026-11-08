import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 158: under a shader pack the mining helmet lit nothing on 1.20.1 Forge. GemRender's lights are off under
 * a pack, but its 1.20.1 Forge build cannot see Oculus, so HBM skipped the 1.7 block-light fallback as well. Under a
 * pack the lamp must be the fallback. Needs {@code ORG_GRADLE_PROJECT_shaders.run=true harness up --rebuild} and the
 * pack selected in the run's {@code config/oculus.properties} (Forge) or {@code config/iris.properties}.
 */
public final class Gh158LampShaders implements Scenario {

    private static final int X = 1300;
    private static final int Y = 100;
    private static final int Z = 1300;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String hideGui = c.setOption("hide_gui", "true");
        String forceload = (X - 8) + " " + (Z - 8) + " " + (X + 28) + " " + (Z + 28);
        try {
            c.cmd("forceload add " + forceload);
            c.cmd("gamemode creative");
            c.cmd("difficulty peaceful");
            c.cmd("time set 18000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("fill " + X + " " + Y + " " + Z + " " + (X + 20) + " " + (Y + 8) + " " + (Z + 20) + " minecraft:stone hollow");
            c.cmd("clear @s");
            c.cmd("effect clear @s");
            c.teleport(X + 3.5, Y + 1, Z + 10.5);
            c.lookAt(X + 20, Y + 3, Z + 10.5);
            c.setOption("camera", "first_person");
            c.waitTicks(100);
            boolean pack = c.query("gem", new JsonObject()).get("shaderPack").getAsBoolean();
            c.screenshot("dark");

            c.cmd("item replace entity @s armor.head with hbm:no9");
            c.waitTicks(60);
            JsonObject helmet = lamp(c);
            c.screenshot("helmet");
            System.out.println("[Gh158] " + c.node() + " shader pack " + pack + ", helmet: " + helmet);

            c.cmd("item replace entity @s armor.head with minecraft:air");
            c.waitTicks(30);
            JsonObject off = lamp(c);
            System.out.println("[Gh158] " + c.node() + " helmet off: " + off);

            c.cmd("give @s hbm:gun_jshotgun");
            c.hotbar(1);
            c.waitTicks(10);
            c.use(2);
            c.waitTicks(40);
            JsonObject flash = lamp(c);
            c.screenshot("flashlight");
            System.out.println("[Gh158] " + c.node() + " flashlight: " + flash);
            c.use(2);
            c.waitTicks(30);

            Expect.that(pack, "a shader pack is in use");
            Expect.that(!helmet.get("shading").getAsBoolean(), "no GemRender light is counted on under the pack: " + helmet);
            Expect.that(helmet.get("helmet_lit").getAsBoolean(), "the worn helmet is lit: " + helmet);
            Expect.that(helmet.get("face_block_light").getAsInt() >= 10, "the 1.7 block light reaches the wall: " + helmet);
            Expect.that(off.get("face_block_light").getAsInt() == 0, "the wall is dark again: " + off);
            Expect.that(flash.get("flashlight_on").getAsBoolean() && flash.get("face_block_light").getAsInt() > 0,
                    "the flashlight lights the wall: " + flash);
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
