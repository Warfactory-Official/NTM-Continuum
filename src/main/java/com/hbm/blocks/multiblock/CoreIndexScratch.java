package com.hbm.blocks.multiblock;

import net.minecraft.core.BlockPos;

/** Per-thread scratch used while a folded core writes its cell ownership records. */
public final class CoreIndexScratch {

    private static final ThreadLocal<CoreIndexScratch> LOCAL = ThreadLocal.withInitial(CoreIndexScratch::new);

    private long[] cells = new long[16];
    private int size;
    private boolean inUse;

    private CoreIndexScratch() {}

    public static CoreIndexScratch borrow() {
        CoreIndexScratch scratch = LOCAL.get();
        if (scratch.inUse) return new CoreIndexScratch();
        scratch.inUse = true;
        return scratch;
    }

    public void add(BlockPos pos) {
        if (size == cells.length) {
            long[] grown = new long[cells.length * 3 / 2 + 8];
            System.arraycopy(cells, 0, grown, 0, cells.length);
            cells = grown;
        }
        cells[size++] = pos.asLong();
    }

    public int size() {
        return size;
    }

    public long get(int index) {
        return cells[index];
    }

    public void release() {
        size = 0;
        inUse = false;
    }
}
