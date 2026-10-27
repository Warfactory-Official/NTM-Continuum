package com.hbm.deimport.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/** Reads and writes single block states in a saved chunk compound. */
public final class ChunkBlocks {
    private ChunkBlocks() {}

    public static @Nullable CompoundTag get(CompoundTag chunk, BlockPos pos) {
        CompoundTag states = states(chunk, pos);
        if (states == null) return null;
        PackedSection packed = new PackedSection(states);
        return packed.palette.getCompound(packed.index[PackedSection.indexOf(pos.getX(), pos.getY(), pos.getZ())]);
    }

    /** False when the chunk has no section at that height. */
    public static boolean set(CompoundTag chunk, BlockPos pos, CompoundTag state) {
        CompoundTag states = states(chunk, pos);
        if (states == null) return false;
        PackedSection packed = new PackedSection(states);
        int slot = packed.palette.indexOf(state);
        if (slot < 0) {
            packed.palette.add(state);
            slot = packed.palette.size() - 1;
        }
        packed.index[PackedSection.indexOf(pos.getX(), pos.getY(), pos.getZ())] = slot;
        packed.store(states);
        return true;
    }

    private static @Nullable CompoundTag states(CompoundTag chunk, BlockPos pos) {
        for (Tag t : chunk.getList("sections", Tag.TAG_COMPOUND)) {
            CompoundTag section = (CompoundTag) t;
            if (section.getByte("Y") == pos.getY() >> 4 && section.contains("block_states", Tag.TAG_COMPOUND))
                return section.getCompound("block_states");
        }
        return null;
    }
}
