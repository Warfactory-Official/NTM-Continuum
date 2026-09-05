package com.hbm.api.redstoneoverradio;

public interface IRORValueProvider extends IRORInfo {
    /** Reads a value without changing the component. */
    String provideRORValue(String name);
}
