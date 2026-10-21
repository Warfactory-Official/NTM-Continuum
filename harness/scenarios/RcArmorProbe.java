import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/** Scratch probe: health tick by tick around a stimpak. */
public final class RcArmorProbe implements Scenario {
    @Override
    public void run(Client c) throws Exception {
        Fixtures.platform(c, 0, 100, 0, 10);
        c.cmd("gamemode survival");
        c.cmd("effect clear @s");
        c.cmd("clear");
        c.teleport(0.5, 101, 0.5);
        c.look(0, 30);
        c.cmd("effect give @s minecraft:instant_health 1 10 true");
        c.waitTicks(10);
        c.cmd("damage @s 10 minecraft:magic");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) { c.waitTicks(1); sb.append(h(c)).append(' '); }
        c.cmd("give @s hbm:syringe_metal_stimpak");
        c.waitTicks(3);
        c.hotbar(1);
        c.waitTicks(2);
        sb.append("| use | ");
        c.use();
        for (int i = 0; i < 12; i++) { c.waitTicks(1); sb.append(h(c)).append(' '); }
        System.out.println("PROBE " + sb);
        c.cmd("gamemode creative");
    }
    private double h(Client c) { return c.query("hbm_armor_set", new JsonObject()).get("health").getAsDouble(); }
}
