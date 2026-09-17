package com.hbm.handler.radiation;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * One chunk's parked radiation as bytes: the per-section pocket densities, in the pocket order the
 * flood fill produces. The indices are only meaningful against the same blocks, which is exactly the
 * guarantee a chunk gives while it is unloaded.
 *
 * <p>Nothing here is NBT. The predecessor stored every density as a boxed {@code Codec.DOUBLE} inside a
 * list tag, which costs a {@code Double} per pocket on both sides of every save; these are the same
 * eight bytes the simulation already holds, written straight out.
 *
 * <p>A leading version byte exists so a later format can be told apart from this one rather than
 * misread as it. Anything that does not parse is reported as absent — a chunk losing its parked
 * radiation is recoverable, and the alternative is a malformed payload sizing an array.
 */
public final class RadiationPayload {

    private RadiationPayload() {}

    private static final byte VERSION = 1;

    /** No section holds more pockets than it has blocks, and a chunk has one row per section. */
    private static final int MAX_POCKETS = 4096;
    private static final int MAX_SECTIONS = 1024;

    /** The payload for a chunk's snapshot, or null when no section carries anything worth storing. */
    public static byte[] encode(double[][] sections) {
        if (sections == null) return null;
        int present = 0;
        int densities = 0;
        for (double[] row : sections) {
            if (row == null) continue;
            present++;
            densities += row.length;
        }
        if (present == 0) return null;

        ByteBuffer buffer = ByteBuffer.allocate(1 + Integer.BYTES + present * 2 * Integer.BYTES
                + densities * Double.BYTES);
        buffer.put(VERSION);
        buffer.putInt(present);
        for (int i = 0; i < sections.length; i++) {
            double[] row = sections[i];
            if (row == null) continue;
            buffer.putInt(i);
            buffer.putInt(row.length);
            for (double density : row) buffer.putDouble(density);
        }
        return buffer.array();
    }

    /**
     * A payload's identity, for telling "this chunk's radiation moved" from "this chunk was written
     * again". Never {@code 0}, so a real payload can never be mistaken for the absence of one.
     */
    public static long fingerprint(byte[] payload) {
        long hash = 0xCBF29CE484222325L;
        for (byte b : payload) {
            hash ^= b & 0xFFL;
            hash *= 0x100000001B3L;
        }
        return hash == 0L ? 1L : hash;
    }

    /** The snapshot a payload carries, indexed by section, or null if it cannot be read as one. */
    public static double[][] decode(byte[] payload) {
        if (payload == null || payload.length < 1 + Integer.BYTES) return null;
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        try {
            if (buffer.get() != VERSION) return null;
            int present = buffer.getInt();
            if (present <= 0 || present > MAX_SECTIONS) return null;

            int[] indices = new int[present];
            double[][] rows = new double[present][];
            int highest = -1;
            for (int i = 0; i < present; i++) {
                int index = buffer.getInt();
                int pockets = buffer.getInt();
                if (index < 0 || index >= MAX_SECTIONS || pockets < 0 || pockets > MAX_POCKETS) return null;
                double[] row = new double[pockets];
                for (int p = 0; p < pockets; p++) row[p] = buffer.getDouble();
                indices[i] = index;
                rows[i] = row;
                highest = Math.max(highest, index);
            }

            double[][] sections = new double[highest + 1][];
            for (int i = 0; i < present; i++) sections[indices[i]] = rows[i];
            return sections;
        } catch (BufferUnderflowException truncated) {
            return null;
        }
    }
}
