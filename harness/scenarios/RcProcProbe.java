import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/**
 * Release audit, item processing machines: places each module machine and prints its fresh block entity data and
 * the slot count of its GUI, to learn the keys a run scenario can merge. Optional environment RC_PROC_FILTER=regex.
 */
public final class RcProcProbe implements Scenario {

    private static final int Y = 100;
    private static final String[] BLOCKS = {
            "machine_crystallizer", "machine_mixer", "machine_electrolyser", "machine_purex", "machine_gascent",
            "machine_silex", "machine_fel", "machine_furnace_combination", "machine_pyrooven", "machine_rotary_furnace",
            "machine_precass", "machine_ore_slopper", "machine_rockmill", "machine_compressor", "machine_radiolysis",
            "machine_ammo_press", "machine_autocrafter", "machine_waste_drum", "machine_microwave",
            "machine_rtg_furnace_off", "machine_furnace_brick_off", "furnace_iron", "furnace_steel"
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.key("escape");
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 30);
        c.waitTicks(30);
        String filter = System.getenv("RC_PROC_FILTER");
        int i = 0;
        for (String id : BLOCKS) {
            if (filter != null && !filter.isBlank() && !id.matches(filter)) continue;
            int cx = -24 + (i % 6) * 10, cz = -20 + (i / 6) * 12;
            i++;
            c.teleport(cx + 2.5, Y + 1, cz + 0.5);
            c.waitTicks(2);
            Fixtures.giveOnly(c, "hbm:" + id, 1);
            c.lookAt(cx + 0.5, Y + 0.98, cz + 0.5);
            c.waitTicks(1);
            c.use(2);
            c.waitTicks(4);
            int[] core = null;
            for (int dy = 1; dy <= 5 && core == null; dy++)
                for (int r = 0; r <= 6 && core == null; r++)
                    for (int dz = -r; dz <= r && core == null; dz++)
                        for (int dx = -r; dx <= r; dx++) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                            JsonObject be = c.blockEntity(cx + dx, Y + dy, cz + dz);
                            if (be != null && be.has("block_entity") && !be.get("block_entity").isJsonNull()) {
                                core = new int[]{cx + dx, Y + dy, cz + dz};
                                String nbt = be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\s+", " ") : "(none)";
                                System.out.println("[" + id + "] core " + core[0] + "," + core[1] + "," + core[2] + " " + be.get("block_entity").getAsString() + " nbt " + (nbt.length() > 1400 ? nbt.substring(0, 1400) : nbt));
                                break;
                            }
                        }
            if (core == null) System.out.println("[" + id + "] no block entity");
        }
    }
}
