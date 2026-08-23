package com.hbm.blocks.multiblock;

import com.hbm.Constants;
import com.mojang.serialization.Codec;
import dev.golem.core.world.CompatSavedData;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent CELL -> core ownership for folded multiblocks. The original stores this beside each chunk;
 * the compatibility-safe equivalent is one level SavedData map, with the same packed-position contract.
 */
public final class MultiblockCoreIndex extends CompatSavedData<MultiblockCoreIndex> {

    private static final String ID = "hbm_multiblock_core_index";
    /** A named field keeps the pre-26.1 SavedData encoding a CompoundTag root. */
    private static final Codec<MultiblockCoreIndex> CODEC = Codec.LONG.listOf().fieldOf("entries").codec().xmap(
            MultiblockCoreIndex::fromRows, MultiblockCoreIndex::toRows);
    private final Long2LongOpenHashMap entries = new Long2LongOpenHashMap();

    public MultiblockCoreIndex() {
        entries.defaultReturnValue(Long.MIN_VALUE);
    }

    public static MultiblockCoreIndex get(ServerLevel level) {
        return CompatSavedData.get(level, Constants.MOD_ID + ":" + ID, MultiblockCoreIndex::new, CODEC);
    }

    public static long lookup(ServerLevel level, BlockPos cell) {
        return get(level).entries.get(cell.asLong());
    }

    public static void put(ServerLevel level, BlockPos cell, BlockPos core) {
        MultiblockCoreIndex index = get(level);
        index.entries.put(cell.asLong(), core.asLong());
        index.setDirty();
    }

    public static void remove(ServerLevel level, BlockPos cell) {
        MultiblockCoreIndex index = get(level);
        if (index.entries.remove(cell.asLong()) != Long.MIN_VALUE) index.setDirty();
    }

    private static MultiblockCoreIndex fromRows(List<Long> rows) {
        MultiblockCoreIndex index = new MultiblockCoreIndex();
        for (int i = 0; i + 1 < rows.size(); i += 2) index.entries.put(rows.get(i), rows.get(i + 1));
        return index;
    }

    private List<Long> toRows() {
        List<Long> rows = new ArrayList<>(entries.size() * 2);
        entries.long2LongEntrySet().forEach(entry -> {
            rows.add(entry.getLongKey());
            rows.add(entry.getLongValue());
        });
        return rows;
    }

    @Override
    protected Codec<MultiblockCoreIndex> codec() {
        return CODEC;
    }
}
