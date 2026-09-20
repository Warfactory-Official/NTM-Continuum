package com.hbm.api.block;

/** 1.7 {@code api.hbm.block.IPileNeutronReceiver}: a legacy graphite-pile tile that absorbs cast neutrons. */
@Deprecated
public interface IPileNeutronReceiver {

    void receiveNeutrons(int n);
}
