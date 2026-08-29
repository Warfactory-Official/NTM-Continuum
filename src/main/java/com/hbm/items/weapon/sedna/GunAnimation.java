package com.hbm.items.weapon.sedna;

/**
 * The animation cues a gun's state machine emits.
 *
 * <p>Ported from {@code AnimationEnums.GunAnimation} in 1.12.2. The server-side state machine emits
 * these cues at exactly the right moments; the client binds them to the per-gun bus-animation tables.
 *
 * <p>Ordinals are load-bearing — they travel over the wire in the animation payload — so entries are
 * kept in upstream's order, {@code RELOAD_EMPTY} included even though upstream deprecates it.
 */
public enum GunAnimation {
    /** A full reload, or the start of a sequential one. */
    RELOAD,
    /** @deprecated as upstream: kept only to preserve the ordinals that follow it. */
    @Deprecated RELOAD_EMPTY,
    /** Plays once per individual round on tube-fed guns. */
    RELOAD_CYCLE,
    /** The transition out of a sequential reload back to idle. */
    RELOAD_END,
    /** Every firing cycle. */
    CYCLE,
    /** The last round in the magazine. */
    CYCLE_EMPTY,
    /** A trigger pull with nothing to fire. */
    CYCLE_DRY,
    /** Alternate-fire cycles. */
    ALT_CYCLE,
    SPINUP,
    SPINDOWN,
    /** Drawing the weapon. */
    EQUIP,
    /** Looking the weapon over — also the "you have no ammo" response. */
    INSPECT,
    JAMMED;

    public static final GunAnimation[] VALUES = values();

    public static GunAnimation byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : CYCLE;
    }
}
