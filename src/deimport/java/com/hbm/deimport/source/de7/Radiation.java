package com.hbm.deimport.source.de7;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * DE's chunk radiation payload ("NTX", format 8): per section either one uniform pocket or a mask of radiation-resistant
 * blocks whose 6-connected open regions are the pockets, numbered in block-index order, with a density per nonzero
 * pocket. Each pocket comes back as one block inside it and its density: Continuum floods its own pockets from the same
 * blocks, so setting that block sets the whole pocket.
 */
public final class Radiation {
    public record Pocket(BlockPos pos, double density) {}

    private static final int SECTION = 4096;

    private Radiation() {}

    /** The pockets of one chunk's payload, or an empty list if it is not DE's format 8. */
    public static List<Pocket> decode(int cx, int cz, byte[] raw) {
        List<Pocket> out = new ArrayList<>();
        if (raw.length < 5 || raw[0] != 'N' || raw[1] != 'T' || raw[2] != 'X' || raw[3] != 8) return out;
        ByteBuffer buf = ByteBuffer.wrap(raw, 4, raw.length - 4);
        int records = buf.get() & 0xFF;
        for (int r = 0; r < records; r++) {
            int sectionY = buf.getInt();
            int topology = buf.get() & 0xFF;
            int pocketCount = buf.getShort() & 0xFFFF;
            int nonZero = buf.getShort() & 0xFFFF;
            int[] firstBlock = new int[pocketCount];
            if (topology == 0) {
                firstBlock[0] = 0;
            } else {
                long[] resistant = new long[64];
                for (int w = 0; w < 64; w++) resistant[w] = buf.getLong();
                firstBlocks(resistant, firstBlock);
            }
            for (int i = 0; i < nonZero; i++) {
                int pocket = buf.getShort() & 0xFFFF;
                double density = buf.getDouble();
                int index = firstBlock[pocket];
                out.add(new Pocket(new BlockPos(cx * 16 + (index & 15), sectionY * 16 + ((index >> 8) & 15),
                        cz * 16 + ((index >> 4) & 15)), density));
            }
        }
        return out;
    }

    /** DE's flood fill: scanning block indices upward, each unvisited open block starts the next pocket. */
    private static void firstBlocks(long[] resistant, int[] firstBlock) {
        short[] pocketOf = new short[SECTION];
        Arrays.fill(pocketOf, (short) -1);
        int[] queue = new int[SECTION];
        int pockets = 0;
        for (int start = 0; start < SECTION; start++) {
            if (pocketOf[start] != -1 || closed(resistant, start)) continue;
            if (pockets < firstBlock.length) firstBlock[pockets] = start;
            int head = 0, tail = 0;
            queue[tail++] = start;
            pocketOf[start] = (short) pockets;
            while (head != tail) {
                int cur = queue[head++];
                int x = cur & 15, z = (cur >> 4) & 15, y = cur >> 8;
                for (int f = 0; f < 6; f++) {
                    int nx = x + (f == 0 ? -1 : f == 1 ? 1 : 0);
                    int ny = y + (f == 2 ? -1 : f == 3 ? 1 : 0);
                    int nz = z + (f == 4 ? -1 : f == 5 ? 1 : 0);
                    if (nx < 0 || nx > 15 || ny < 0 || ny > 15 || nz < 0 || nz > 15) continue;
                    int next = (ny << 8) | (nz << 4) | nx;
                    if (pocketOf[next] != -1 || closed(resistant, next)) continue;
                    pocketOf[next] = (short) pockets;
                    queue[tail++] = next;
                }
            }
            pockets++;
        }
    }

    private static boolean closed(long[] resistant, int index) {
        return (resistant[index >>> 6] & (1L << (index & 63))) != 0L;
    }
}
