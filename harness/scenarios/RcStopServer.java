import dev.golem.harness.api.Client;
import dev.golem.harness.api.Rcon;
import dev.golem.harness.api.Scenario;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stops the dedicated server of this node over rcon (rcon.port from its server.properties). */
public final class RcStopServer implements Scenario {
    @Override
    public void run(Client c) throws Exception {
        int port = 0;
        for (String line : Files.readAllLines(Path.of("versions", c.node(), "runs", "server", "server.properties"))) {
            if (line.startsWith("rcon.port=")) port = Integer.parseInt(line.substring(10).trim());
        }
        try (Rcon rcon = new Rcon("127.0.0.1", port, "rc")) {
            rcon.command("stop");
        }
    }
}
