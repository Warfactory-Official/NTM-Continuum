package com.hbm.tileentity.machine.rbmk;

/** Tick contract used by the RBMK blocks instead of ntm-next's registration-owned ticker. */
public interface IRBMKTicking {
    void tickServer();

    default void tickClient() {}
}
