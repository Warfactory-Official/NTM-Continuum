package com.hbm.api.tile;

/**
 * A BlockEntity holding a heat reservoir ("TU") that heat consumers can drain, the 1.12.2
 * {@code IHeatSource}. Heat is not networked: a consumer (crucible, another heater) polls the
 * BlockEntity <b>directly below itself</b>, reads {@link #getHeatStored()}, transfers some fraction and
 * calls {@link #useUpHeat(int)}.
 */
public interface IHeatSource {

    int getHeatStored();

    void useUpHeat(int heat);
}
