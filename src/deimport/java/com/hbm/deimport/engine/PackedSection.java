package com.hbm.deimport.engine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

/**
 * A chunk section's {@code block_states} unpacked to one palette index per block, and packed back the way vanilla's
 * {@code PalettedContainer} saves it: no data for one entry, else {@code max(4, ceillog2(n))} bits, no spanning.
 */
public final class PackedSection {
    public static final int SIZE = 4096;
    public final ListTag palette;
    public final int[] index = new int[SIZE];

    public PackedSection(CompoundTag blockStates) {
        palette = blockStates.getList("palette", Tag.TAG_COMPOUND);
        long[] data = blockStates.getLongArray("data");
        int bits = bits(palette.size());
        if (bits == 0 || data.length == 0) return;
        int perLong = 64 / bits;
        long mask = (1L << bits) - 1;
        for (int i = 0; i < SIZE; i++) index[i] = (int) ((data[i / perLong] >>> ((i % perLong) * bits)) & mask);
    }

    public static int bits(int paletteSize) {
        return paletteSize <= 1 ? 0 : Math.max(4, Mth.ceillog2(paletteSize));
    }

    public static int indexOf(int x, int y, int z) {
        return ((y & 15) * 16 + (z & 15)) * 16 + (x & 15);
    }

    /** Merges equal palette entries, drops unused ones and writes palette and data back into {@code blockStates}. */
    public void store(CompoundTag blockStates) {
        ListTag merged = new ListTag();
        int[] remap = new int[palette.size()];
        boolean[] used = new boolean[palette.size()];
        for (int i : index) used[i] = true;
        for (int i = 0; i < palette.size(); i++) {
            remap[i] = -1;
            if (!used[i]) continue;
            Tag entry = palette.get(i);
            int found = merged.indexOf(entry);
            if (found < 0) {
                merged.add(entry);
                found = merged.size() - 1;
            }
            remap[i] = found;
        }
        blockStates.put("palette", merged);
        int bits = bits(merged.size());
        if (bits == 0) {
            blockStates.remove("data");
            return;
        }
        int perLong = 64 / bits;
        long[] data = new long[(SIZE + perLong - 1) / perLong];
        for (int i = 0; i < SIZE; i++) data[i / perLong] |= (long) remap[index[i]] << ((i % perLong) * bits);
        blockStates.putLongArray("data", data);
    }
}
