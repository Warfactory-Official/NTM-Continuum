package com.hbm.tileentity.machine;

/** Client-visible rotor state shared by the electric and steam groundwater pumps. */
public interface PumpAnimationState {

    float getRotor();

    float getLastRotor();
}
