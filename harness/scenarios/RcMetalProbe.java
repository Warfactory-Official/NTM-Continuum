import dev.golem.harness.api.Client;
import dev.golem.harness.api.Scenario;

public final class RcMetalProbe implements Scenario {
    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        System.out.println("[probe] " + c.blockEntity(25, 101, 10));
        System.out.println("[probe] " + c.blockEntity(10, 101, 10));
    }
}
