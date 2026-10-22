import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;
import java.util.regex.Pattern;

/**
 * The chat notice that GemRender is missing: shown on the first join of a new world only when GemRender is not
 * installed, and not again when the same world is joined a second time. Run once with GemRender and once with
 * {@code -Pgemrender.run=false}.
 */
public final class GemRenderNotice implements Scenario {

    private static final String NOTICE = "GemRender is not installed";

    @Override
    public void run(Client c) throws Exception {
        boolean available = c.query("gem", new JsonObject()).get("available").getAsBoolean();
        String world = "gem_notice_" + System.currentTimeMillis();

        c.leaveWorld(60_000L);
        c.markLog();
        c.createWorld(world, 240_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(60);
        boolean first = !c.logSinceMark(Pattern.quote(NOTICE)).isEmpty();
        c.screenshot("first-join");
        Expect.that(first != available, "first join: notice shown " + first + " with GemRender available " + available);

        c.leaveWorld(60_000L);
        c.markLog();
        c.joinWorld(world, 120_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(60);
        Expect.that(c.logSinceMark(Pattern.quote(NOTICE)).isEmpty(), "the notice came again on a second join");

        c.leaveWorld(60_000L);
        c.useFlatWorld();
    }
}
