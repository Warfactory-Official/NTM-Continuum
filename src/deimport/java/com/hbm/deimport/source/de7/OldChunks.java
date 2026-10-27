package com.hbm.deimport.source.de7;

import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * DE's own chunk upgrades ({@code HbmDataFixers}), for chunks DE last saved below data version 7: DE runs them when it
 * reads such a chunk, so a chunk never reloaded since a DE update still holds the older shapes.
 */
public final class OldChunks {
    private static final Map<String, String[]> TRINKETS = Map.of(
            "hbm:bobblehead", new String[]{"none", "strength", "perception", "endurance", "charisma", "intelligence", "agility",
                    "luck", "bob", "frizzle", "pu238", "vt", "doc", "bluehat", "pheo", "adam29", "uffr", "vaer", "nos",
                    "drillgon", "cirno", "microwave", "peep", "mellow", "abel"},
            "hbm:plushie", new String[]{"none", "yomi", "numbernine", "hundun", "derg"});
    private static final String[] PAD_SIZES = {"ANY", "NONE", "SIZE_10", "SIZE_15", "SIZE_20"};

    private OldChunks() {}

    public static void upgrade(CompoundTag chunk, int version) {
        for (Tag t : chunk.getList("block_entities", Tag.TAG_COMPOUND)) {
            CompoundTag be = (CompoundTag) t;
            String id = be.getString("id");
            // v2: a power lock's interaction
            if (version < 2 && id.equals("hbm:logic_block") && be.getString("actionID").equals("POWER_LOCK") && !be.contains("interactionID"))
                be.putString("interactionID", "POWER_LOCK");
            // v4 and v5: the glyphid spawner's delay, then its first spawn
            if (version < 5 && id.equals("hbm:glyphid_spawner")) {
                be.remove("initialSpawn");
                be.putBoolean("initial_spawn", true);
                be.putInt("spawn_delay", 0);
            }
            // v6: trinket and pad size numbers to names
            String[] names = TRINKETS.get(id);
            if (version < 6 && names != null && be.contains("type", Tag.TAG_ANY_NUMERIC))
                be.putString("type", names[Math.abs(be.getByte("type")) % names.length]);
            if (version < 6 && id.equals("hbm:large_launch_table") && be.contains("padSize", Tag.TAG_ANY_NUMERIC))
                be.putString("padSize", PAD_SIZES[Math.clamp(be.getInt("padSize"), 0, PAD_SIZES.length - 1)]);
        }
    }
}
