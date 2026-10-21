import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Release audit, power machines: places each generator or store by hand, opens its GUI with a real right-click and
 * takes a screenshot of the machine and of the screen. Soft checks, one entry per machine.
 */
public final class RcPowerTail2 implements Scenario {

    private static final int Y = 100;
    private static final String[] BLOCKS = {"machine_rtg_furnace_off"};
    private final List<String> failures = new ArrayList<>();

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 24);
        c.waitTicks(5);
        for (String id : BLOCKS) {
            try {
                one(c, id);
            } catch (RuntimeException e) {
                failures.add(id + ": " + e.getMessage());
                System.out.println("[FAIL] " + id + " " + e.getMessage());
                c.key("escape");
                c.waitTicks(3);
            }
        }
        storagePersist(c);
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(hbm|golem|render|screen|gui)");
        Expect.that(failures.isEmpty(), "soft checks failed: " + failures);
    }

    private void one(Client c, String id) {
        c.cmd("fill -16 " + (Y + 1) + " -16 16 " + (Y + 12) + " 16 minecraft:air");
        c.waitTicks(3);
        JsonObject placed = Fixtures.placeOnTop(c, "hbm:" + id, 0, Y, 0);
        System.out.println("[" + id + "] placed: " + placed);
        c.teleport(0.5, Y + 1, -3.5);
        c.look(0, 15);
        c.waitTicks(15);
        c.screenshot(id + "-world");
        int[] kk = findCore(c);
        JsonObject be = c.blockEntity(kk[0], kk[1], kk[2]);
        String klass = be != null && be.has("block_entity") && !be.get("block_entity").isJsonNull()
                ? be.get("block_entity").getAsString() : "none";
        System.out.println("[" + id + "] block entity: " + klass);
        int[] k = findCore(c);
        Fixtures.interact(c, k[0], k[1], k[2]);
        c.waitTicks(15);
        String screen = c.screenClass();
        System.out.println("[" + id + "] screen: " + screen);
        if (screen != null) {
            c.screenshot(id + "-gui");
            c.waitTicks(10);
            if (c.screenClass() == null) failures.add(id + ": screen closed by itself");
            c.key("escape");
            c.waitTicks(5);
        }
    }


    private void storagePersist(Client c) {
        try {
            c.cmd("fill -16 " + (Y + 1) + " -16 16 " + (Y + 12) + " 16 minecraft:air");
            c.waitTicks(3);
            Fixtures.placeOnTop(c, "hbm:machine_fensu", 0, Y, 0);
            c.waitTicks(10);
            int[] k = findCore(c);
            c.cmd("data merge block " + k[0] + " " + k[1] + " " + k[2] + " {power:123456789L}");
            c.waitTicks(10);
            c.cmd("save-all flush");
            c.waitTicks(30);
            String world = c.currentWorld();
            c.leaveWorld(60_000L);
            c.joinWorld(world, 120_000L);
            c.waitInWorld(120_000L);
            c.waitTicks(40);
            String nbt = c.blockEntity(k[0], k[1], k[2]).get("nbt").getAsString().replaceAll("\s+", "");
            System.out.println("[fensu persist] " + nbt.replaceAll(".*(power:[0-9]+L).*", "$1"));
            if (!nbt.contains("power:123456789L")) failures.add("charged FEnSU lost its charge over save and rejoin: " + nbt);
        } catch (RuntimeException e) {
            failures.add("storagePersist: " + e.getMessage());
        }
    }
    private static int[] findCore(Client c) {
        if (hasBe(c.blockEntity(0, Y + 1, 0))) return new int[]{0, Y + 1, 0};
        for (int dy = 0; dy <= 3; dy++)
            for (int dz = -4; dz <= 4; dz++)
                for (int dx = -4; dx <= 4; dx++) {
                    if (hasBe(c.blockEntity(dx, Y + 1 + dy, dz))) return new int[]{dx, Y + 1 + dy, dz};
                }
        return new int[]{0, Y + 1, 0};
    }

    private static boolean hasBe(JsonObject be) {
        return be != null && be.has("block_entity") && !be.get("block_entity").isJsonNull();
    }
}
