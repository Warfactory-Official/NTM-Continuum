package com.hbm.packet;

import io.netty.buffer.ByteBuf;

/** Independent state codecs composed by a block entity's native full-packet writer. */
public interface SyncUnitSchema {
    int MACHINE_FLAGS_UNIT = 62;
    long syncUnitMask();
    void writeSyncUnit(int unit, ByteBuf output);
    void readSyncUnit(int unit, ByteBuf input);
}
