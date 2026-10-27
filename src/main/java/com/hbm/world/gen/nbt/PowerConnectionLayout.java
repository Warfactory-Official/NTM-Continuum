package com.hbm.world.gen.nbt;

/** Six Direction.ordinal connection bits carried by a block entity without multiplying its block states. */
public interface PowerConnectionLayout {
    String KEY = "power_connections";

    /** Attached entities may query the current neighborhood; detached template entities retain the recorded mask. */
    int getPowerConnectionMask();

    void setPowerConnectionMask(int mask);
}
