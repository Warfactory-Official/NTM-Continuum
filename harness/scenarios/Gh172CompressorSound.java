import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub issue 172: 1.7's compressor plays {@code hbm:item.boltgun} at volume 0.5, pitch 0.75, each time its piston
 * reaches the bottom. The piston climbs 0.05 a tick and falls 0.085 to 0.115, so a stroke lasts 20 + 9 to 12 ticks:
 * 200 ticks of running give 6 or 7 strokes.
 */
public final class Gh172CompressorSound implements Scenario {

    private static final int Y = 100;
    private static final int L = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        String capture = c.setOption("sound_capture", "true");
        try {
            Fixtures.platform(c, 0, Y, 0, 8);
            c.cmd("gamemode creative");
            c.cmd("gamerule sendCommandFeedback false");
            c.teleport(0.5, L + 2, 9.5);
            c.lookAt(0.5, L + 3, 0.5);
            String core = place(c, "hbm:machine_compressor[facing=north]", "TileEntityMachineCompressor");
            c.markLog();
            for (int i = 0; i < 10; i++) {
                c.cmd("data merge block " + core + " {power:100000L,\"0\":16000,\"0_type\":\"hbm:air\"}");
                c.waitTicks(20);
                if (i == 4) c.screenshot("compressor-running");
            }
            String nbt = c.blockEntity(Integer.parseInt(core.split(" ")[0]), L, Integer.parseInt(core.split(" ")[2]))
                    .get("nbt").getAsString();
            Matcher out = Pattern.compile("(?m)^ *1: ?([0-9]+)").matcher(nbt);
            int compressed = out.find() ? Integer.parseInt(out.group(1)) : 0;
            List<String> lines = c.logSinceMark("harness-sound.*boltgun");
            System.out.println("[Gh172] " + c.node() + " compressor boltgun strokes: " + lines.size()
                    + ", output " + compressed + " mB" + (lines.isEmpty() ? "" : ", e.g. " + lines.get(0)));
            Expect.that(compressed > 0, "the compressor ran: output tank " + compressed + " mB");
            Expect.that(lines.size() >= 5, "the running compressor plays a stroke sound about every 30 ticks, got "
                    + lines.size());
            Expect.that(lines.get(0).contains("pitch=0.75") && lines.get(0).contains("volume=0.5"),
                    "the stroke is 1.7's volume 0.5, pitch 0.75: " + lines.get(0));
        } finally {
            c.setOption("sound_capture", capture);
            c.cmd("fill -4 " + L + " -4 4 " + (L + 6) + " 4 minecraft:air");
        }
    }

    /** Sets the block and answers the coordinates of the block entity it made. */
    private static String place(Client c, String block, String cls) {
        c.cmd("setblock 0 " + L + " 0 " + block);
        c.waitTicks(8);
        for (int dy = 0; dy <= 2; dy++) for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            JsonObject be = c.blockEntity(dx, L + dy, dz);
            if (be.has("block_entity") && !be.get("block_entity").isJsonNull()
                    && be.get("block_entity").getAsString().endsWith(cls)) {
                return dx + " " + (L + dy) + " " + dz;
            }
        }
        throw new AssertionError("no " + cls + " placed by " + block);
    }
}
