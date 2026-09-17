package com.hbm.main;

/** ntm-next's spelling of the 1.7 {@code MainRegistry.polaroidID} balefire-day guard. */
public final class Polaroid {

    private Polaroid() {}

    /** 1.7's {@code MainRegistry.polaroidID == 11} guard, which every balefire site ORs with its own roll. */
    public static boolean isBalefireDay() {
        return MainRegistry.polaroidID == 11;
    }
}
