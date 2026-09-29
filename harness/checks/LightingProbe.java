import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Scenario;
import java.nio.file.Files;

public final class LightingProbe implements Scenario {
    public void run(Client c) throws Exception {
        c.waitInWorld(120000);
        c.cmd("gamemode creative");
        c.cmd("clear");
        c.hotbar(9);
        c.waitTicks(2);
        c.cmd("gamemode spectator");
        c.teleport(256.5,118,213.5);
        c.waitTicks(40);
        c.lookAt(256,101,249);
        c.waitTicks(20);
        c.screenshot("10-after-rejoin");
        StringBuilder rows = new StringBuilder();
        for(int x=224;x<=288;x+=8) for(int z=224;z<=288;z+=8) {
            JsonObject args = new JsonObject();
            args.addProperty("x",x); args.addProperty("y",101); args.addProperty("z",z);
            args.addProperty("server",false);
            JsonObject client = c.query("block",args);
            args.addProperty("server",true);
            JsonObject server = c.query("block",args);
            rows.append(x+","+z+" client="+client+" server="+server+"\n");
        }
        Files.writeString(c.screenshotDir().resolve("light-grid.txt"), rows.toString());
        c.teleport(276.5,108,256.5); c.waitTicks(20); c.lookAt(274,107,281); c.waitTicks(20);
        c.screenshot("20-tower-after-rejoin");
        c.cmd("gamemode creative");
    }
}
